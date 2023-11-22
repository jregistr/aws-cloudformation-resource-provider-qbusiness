package software.amazon.qbusiness.plugin;

import java.util.List;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.ListPluginsRequest;
import software.amazon.awssdk.services.qbusiness.model.ListPluginsResponse;
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

    var applicationId = request.getDesiredResourceState().getApplicationId();
    final ListPluginsRequest listPluginsRequest = Translator.translateToListRequest(applicationId, request.getNextToken());

    ListPluginsResponse listPluginsResponse = proxy.injectCredentialsAndInvokeV2(listPluginsRequest, proxyClient.client()::listPlugins);

    String nextToken = listPluginsResponse.nextToken();

    List<ResourceModel> models = Translator.translateFromListResponse(applicationId, listPluginsResponse);
    return ProgressEvent.<ResourceModel, CallbackContext>builder()
        .resourceModels(models)
        .nextToken(nextToken)
        .status(OperationStatus.SUCCESS)
        .build();
  }

}
