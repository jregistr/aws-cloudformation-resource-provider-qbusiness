package software.amazon.qbusiness.retriever;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.ListWebExperiencesRequest;
import software.amazon.awssdk.services.qbusiness.model.ListWebExperiencesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

public class ListHandler extends BaseHandlerStd {

  private Logger logger;

  @Override
  public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      final AmazonWebServicesClientProxy proxy,
      final ResourceHandlerRequest<ResourceModel> request,
      final CallbackContext callbackContext,
      final ProxyClient<QBusinessClient> proxyClient,
      final Logger logger) {

    this.logger = logger;

    this.logger.log("[INFO] - [StackId: %s, ApplicationId: %s] Entering List Handler"
        .formatted(request.getStackId(), request.getDesiredResourceState().getApplicationId()));

    final ListWebExperiencesRequest awsRequest = Translator.translateToListRequest(request.getNextToken(), request.getDesiredResourceState());

    final ListWebExperiencesResponse listWebExperienceResponse = proxy.injectCredentialsAndInvokeV2(
        awsRequest, proxyClient.client()::listWebExperiences);

    final String nextToken = listWebExperienceResponse.nextToken();

    final List<ResourceModel> models = Translator.translateFromListResponse(
        listWebExperienceResponse, request.getDesiredResourceState().getApplicationId());

    return ProgressEvent.<ResourceModel, CallbackContext>builder()
        .resourceModels(models)
        .nextToken(nextToken)
        .status(OperationStatus.SUCCESS)
        .build();
  }
}
