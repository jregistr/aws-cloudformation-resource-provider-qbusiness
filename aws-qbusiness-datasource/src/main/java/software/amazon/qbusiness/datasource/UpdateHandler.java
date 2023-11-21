package software.amazon.qbusiness.datasource;

import static software.amazon.qbusiness.datasource.Constants.API_UPDATE_DATASOURCE;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.DataSourceStatus;
import software.amazon.awssdk.services.qbusiness.model.GetDataSourceResponse;
import software.amazon.awssdk.services.qbusiness.model.TagResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.TagResourceResponse;
import software.amazon.awssdk.services.qbusiness.model.UntagResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.UntagResourceResponse;
import software.amazon.awssdk.services.qbusiness.model.UpdateDataSourceRequest;
import software.amazon.awssdk.services.qbusiness.model.UpdateDataSourceResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

public class UpdateHandler extends BaseHandlerStd {

  public static final Constant DEFAULT_BACK_OFF_STRATEGY = Constant.of()
      .timeout(Duration.ofHours(4))
      .delay(Duration.ofMinutes(1))
      .build();

  private final Constant backOffStrategy;
  private final TagHelper tagHelper;
  private Logger logger;

  public UpdateHandler() {
    this(DEFAULT_BACK_OFF_STRATEGY, new TagHelper());
  }

  public UpdateHandler(Constant backOffStrategy, TagHelper tagHelper) {
    this.backOffStrategy = backOffStrategy;
    this.tagHelper = tagHelper;
  }

  protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      final AmazonWebServicesClientProxy proxy,
      final ResourceHandlerRequest<ResourceModel> request,
      final CallbackContext callbackContext,
      final ProxyClient<QBusinessClient> proxyClient,
      final Logger logger) {

    this.logger = logger;

    logger.log("[INFO] Starting Update for %s with ID: %s, ApplicationId: %s and IndexId: %s in stack: %s".formatted(
        ResourceModel.TYPE_NAME,
        request.getDesiredResourceState().getDataSourceId(),
        request.getDesiredResourceState().getApplicationId(),
        request.getDesiredResourceState().getIndexId(),
        request.getStackId()
    ));

    return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
        .then(progress ->
            proxy.initiate("AWS-QBusiness-DataSource::Update", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToUpdateRequest)
                .backoffDelay(backOffStrategy)
                .makeServiceCall(this::updateDataSource)
                .stabilize((updateReq, updateRes, clientProxyClient, model, context) -> isStabilized(clientProxyClient, model))
                .handleError((updateReq, error, clientProxyClient, model, context) -> handleError(
                    updateReq, model, error, context, logger, API_UPDATE_DATASOURCE
                ))
                .progress()
        )
        .then(progress -> {
          if (!tagHelper.shouldUpdateTags(request)) {
            // No updates to tags needed, return early with get application. Since ReadHandler will return Done, this will be the last step
            return readHandler(proxy, request, callbackContext, proxyClient, logger);
          }

          Map<String, String> tagsToAdd = tagHelper.generateTagsToAdd(
              tagHelper.getPreviouslyAttachedTags(request),
              tagHelper.getNewDesiredTags(request)
          );

          if (tagsToAdd == null || tagsToAdd.isEmpty()) {
            return progress;
          }

          return proxy.initiate("AWS-QBusiness-DataSource::TagResource", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
              .translateToServiceRequest(model -> Translator.tagResourceRequest(request, model, tagsToAdd))
              .makeServiceCall(this::callTagResource)
              .progress();
        })
        .then(progress -> {
          Set<String> tagsToRemove = tagHelper.generateTagsToRemove(
              tagHelper.getPreviouslyAttachedTags(request),
              tagHelper.getNewDesiredTags(request)
          );

          if (CollectionUtils.isEmpty(tagsToRemove)) {
            return progress;
          }

          return proxy.initiate("AWS-QBusiness-Application::UnTagResource", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
              .translateToServiceRequest(model -> Translator.untagResourceRequest(request, model, tagsToRemove))
              .makeServiceCall(this::callUntagResource)
              .progress();
        })
        .then(progress -> new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger));
  }

  private UpdateDataSourceResponse updateDataSource(UpdateDataSourceRequest request, ProxyClient<QBusinessClient> proxyClient) {
    return proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::updateDataSource);
  }

  private boolean isStabilized(
      ProxyClient<QBusinessClient> proxyClient,
      ResourceModel model
  ) {
    GetDataSourceResponse getDataSourceResponse = getDataSource(model, proxyClient);
    var status = getDataSourceResponse.status();
    var hasStabilized = DataSourceStatus.ACTIVE.equals(status);

    logger.log("[INFO] Update has %s for %s with ID: %s, ApplicationId: %s and IndexId: %s".formatted(
        hasStabilized ? "stabilized" : "not stabilized yet",
        ResourceModel.TYPE_NAME, model.getDataSourceId(), model.getApplicationId(), model.getIndexId()));

    return hasStabilized;
  }

  private ProgressEvent<ResourceModel, CallbackContext> readHandler(
      final AmazonWebServicesClientProxy proxy,
      final ResourceHandlerRequest<ResourceModel> request,
      final CallbackContext callbackContext,
      final ProxyClient<QBusinessClient> proxyClient,
      final Logger logger
  ) {
    return new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger);
  }

  private TagResourceResponse callTagResource(TagResourceRequest request, ProxyClient<QBusinessClient> proxyClient) {
    if (!request.hasTags()) {
      return TagResourceResponse.builder().build();
    }
    return proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::tagResource);
  }

  private UntagResourceResponse callUntagResource(UntagResourceRequest request, ProxyClient<QBusinessClient> proxyClient) {
    if (!request.hasTagKeys()) {
      return UntagResourceResponse.builder().build();
    }
    return proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::untagResource);
  }
}
