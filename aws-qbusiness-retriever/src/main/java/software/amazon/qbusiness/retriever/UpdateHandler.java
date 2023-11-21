package software.amazon.qbusiness.retriever;

import org.apache.commons.collections4.CollectionUtils;
import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.TagResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.TagResourceResponse;
import software.amazon.awssdk.services.qbusiness.model.UntagResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.UntagResourceResponse;
import software.amazon.awssdk.services.qbusiness.model.UpdateRetrieverRequest;
import software.amazon.awssdk.services.qbusiness.model.UpdateRetrieverResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

import java.time.Duration;
import java.util.Map;
import java.util.Set;

import static software.amazon.qbusiness.retriever.Constants.API_UPDATE_RETRIEVER;

public class UpdateHandler extends BaseHandlerStd {
  private static final Constant DEFAULT_BACK_OFF_STRATEGY = Constant.of()
      .timeout(Duration.ofHours(4))
      .delay(Duration.ofMinutes(2))
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
    this.logger.log("[INFO] - [StackId: %s, ApplicationId: %s, RetrieverId: %s] Entering Update Handler"
        .formatted(request.getStackId(), request.getDesiredResourceState().getApplicationId(), request.getDesiredResourceState().getRetrieverId()));

    return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
        .then(progress ->
            proxy.initiate("AWS-QBusiness-Retriever::Update", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToUpdateRequest)
                .backoffDelay(backOffStrategy)
                .makeServiceCall(this::callUpdateRetriever)
                .handleError((serviceRequest, error, client, model, context) -> handleError(
                    serviceRequest, model, error, context, logger, API_UPDATE_RETRIEVER
                ))
                .progress())
        .then(progress -> {
          if (!tagHelper.shouldUpdateTags(request)) {
            return progress;
          }

          Map<String, String> tagsToAdd = tagHelper.generateTagsToAdd(
              tagHelper.getPreviouslyAttachedTags(request),
              tagHelper.getNewDesiredTags(request)
          );

          if (tagsToAdd == null || tagsToAdd.isEmpty()) {
            return progress;
          }

          return proxy.initiate("AWS-QBusiness-Retriever::TagResource", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
              .translateToServiceRequest(model -> {
                return Translator.tagResourceRequest(request, model, tagsToAdd);
              })
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

  private UpdateRetrieverResponse callUpdateRetriever(UpdateRetrieverRequest request, ProxyClient<QBusinessClient> proxyClient) {
    return proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::updateRetriever);
  }

  private TagResourceResponse callTagResource(TagResourceRequest request, ProxyClient<QBusinessClient> proxyClient) {
    if (!request.hasTags()) {
      return TagResourceResponse.builder().build();
    }
    var client = proxyClient.client();
    return proxyClient.injectCredentialsAndInvokeV2(request, client::tagResource);
  }

  private UntagResourceResponse callUntagResource(UntagResourceRequest request, ProxyClient<QBusinessClient> proxyClient) {
    if (!request.hasTagKeys()) {
      return UntagResourceResponse.builder().build();
    }
    var client = proxyClient.client();
    return proxyClient.injectCredentialsAndInvokeV2(request, client::untagResource);
  }
}
