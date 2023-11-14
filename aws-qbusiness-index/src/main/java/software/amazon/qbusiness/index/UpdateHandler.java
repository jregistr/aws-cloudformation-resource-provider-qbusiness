package software.amazon.qbusiness.plugin;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.UpdatePluginRequest;
import software.amazon.awssdk.services.qbusiness.model.UpdatePluginResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

import java.time.Duration;

import static software.amazon.qbusiness.plugin.Constants.API_UPDATE_PLUGIN;

public class UpdateHandler extends BaseHandlerStd {
  private Logger logger;

  private static final Constant DEFAULT_BACK_OFF_STRATEGY = Constant.of()
          .timeout(Duration.ofHours(4))
          .delay(Duration.ofMinutes(2))
          .build();
  private final Constant backOffStrategy;

  public UpdateHandler() {
    this(DEFAULT_BACK_OFF_STRATEGY);
  }

  public UpdateHandler(Constant backOffStrategy) {
    this.backOffStrategy = backOffStrategy;
  }


  protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
          final AmazonWebServicesClientProxy proxy,
          final ResourceHandlerRequest<ResourceModel> request,
          final CallbackContext callbackContext,
          final ProxyClient<QBusinessClient> proxyClient,
          final Logger logger) {

    this.logger = logger;

    this.logger.log("[INFO] - [StackId: %s, ApplicationId: %s, PluginId: %s] Entering Update Handler"
            .formatted(request.getStackId(), request.getDesiredResourceState().getApplicationId(), request.getDesiredResourceState().getPluginId()));

    return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
            .then(progress ->
                    proxy.initiate("AWS-QBusiness-Plugin::Update", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                            .translateToServiceRequest(model -> Translator.translateToUpdateRequest(model))
                            .backoffDelay(backOffStrategy)
                            .makeServiceCall((awsRequest, clientProxyClient) -> callUpdatePlugin(awsRequest, clientProxyClient))
                            .handleError((describeApplicationRequest, error, client, model, context) ->
                                    handleError(describeApplicationRequest, model, error, context, logger, API_UPDATE_PLUGIN))
                            .progress()
            )
            .then(progress ->
                    new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger)
            );
  }

  private UpdatePluginResponse callUpdatePlugin(UpdatePluginRequest request,
                                                ProxyClient<QBusinessClient> client) {
    return client.injectCredentialsAndInvokeV2(request, client.client()::updatePlugin);
  }

}
