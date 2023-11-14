package software.amazon.qbusiness.datasource;

import static software.amazon.qbusiness.datasource.Constants.API_DELETE_DATASOURCE;

import java.time.Duration;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.DeleteDataSourceRequest;
import software.amazon.awssdk.services.qbusiness.model.DeleteDataSourceResponse;
import software.amazon.awssdk.services.qbusiness.model.ResourceNotFoundException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

public class DeleteHandler extends BaseHandlerStd {

  private static final Constant DEFAULT_BACK_OFF_STRATEGY = Constant.of()
      .timeout(Duration.ofHours(4))
      // Deleting a datasource usually takes about 20 minutes. Let's check every 5 minutes
      .delay(Duration.ofMinutes(5))
      .build();

  private final Constant backOffStrategy;

  private Logger logger;

  public DeleteHandler() {
    this(DEFAULT_BACK_OFF_STRATEGY);
  }

  public DeleteHandler(Constant backOffStrategy) {
    this.backOffStrategy = backOffStrategy;
  }

  protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      final AmazonWebServicesClientProxy proxy,
      final ResourceHandlerRequest<ResourceModel> request,
      final CallbackContext callbackContext,
      final ProxyClient<QBusinessClient> proxyClient,
      final Logger logger) {

    this.logger = logger;

    {
      var model = request.getDesiredResourceState();
      logger.log("[INFO] Initiating delete of %s in Stack: %s for ID: %s, application: %s, index: %s".formatted(
          ResourceModel.TYPE_NAME,
          request.getStackId(),
          model.getDataSourceId(),
          model.getApplicationId(),
          model.getIndexId()
      ));
    }

    return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
        .then(progress ->
            proxy.initiate("AWS-QBusiness-DataSource::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .backoffDelay(backOffStrategy)
                .makeServiceCall(this::callDeleteDataSource)
                .stabilize((deleteReq, deleteRes, client, model, context) -> isStabilized(client, model))
                .handleError((deleteReq, error, clientProxyClient, model, context) -> handleError(
                    deleteReq, model, error, context, logger, API_DELETE_DATASOURCE
                ))
                .done(deleteResponse -> ProgressEvent.defaultSuccessHandler(null))
        );
  }

  private boolean isStabilized(
      ProxyClient<QBusinessClient> proxyClient,
      ResourceModel model
  ) {
    try {
      getDataSource(model, proxyClient);
      logger.log("[INFO] Delete of %s still stabilizing for Resource id: %s, application: %s, index: %s"
          .formatted(ResourceModel.TYPE_NAME, model.getDataSourceId(), model.getApplicationId(), model.getIndexId()));
      return false;
    } catch (ResourceNotFoundException e) {
      logger.log("[INFO] Delete process of %s has stabilized for Resource id: %s, application: %s, index: %s"
          .formatted(ResourceModel.TYPE_NAME, model.getDataSourceId(), model.getApplicationId(), model.getIndexId()));
      return true;
    }
  }

  private DeleteDataSourceResponse callDeleteDataSource(
      final DeleteDataSourceRequest request,
      final ProxyClient<QBusinessClient> proxyClient
  ) {
    return proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::deleteDataSource);
  }
}
