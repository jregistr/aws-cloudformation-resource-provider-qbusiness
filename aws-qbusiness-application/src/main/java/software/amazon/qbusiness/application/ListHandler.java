package software.amazon.qbusiness.application;

import java.util.List;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.ListApplicationsRequest;
import software.amazon.awssdk.services.qbusiness.model.ListApplicationsResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandler extends BaseHandlerStd {

  @Override
  public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      final AmazonWebServicesClientProxy proxy,
      final ResourceHandlerRequest<ResourceModel> request,
      final CallbackContext callbackContext,
      final ProxyClient<QBusinessClient> proxyClient,
      final Logger logger) {

    final ListApplicationsRequest awsRequest = Translator.translateToListRequest(request.getNextToken());

    ListApplicationsResponse listApplicationsResponse = proxy.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::listApplications);

    String nextToken = listApplicationsResponse.nextToken();

    List<ResourceModel> models = Translator.translateFromListResponse(listApplicationsResponse);
    return ProgressEvent.<ResourceModel, CallbackContext>builder()
        .resourceModels(models)
        .nextToken(nextToken)
        .status(OperationStatus.SUCCESS)
        .build();
  }
}
