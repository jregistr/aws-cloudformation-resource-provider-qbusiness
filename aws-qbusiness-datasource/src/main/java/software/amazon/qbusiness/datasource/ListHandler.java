package software.amazon.qbusiness.datasource;

import java.util.List;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.ListDataSourcesRequest;
import software.amazon.awssdk.services.qbusiness.model.ListDataSourcesResponse;
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

    var resourceModel = request.getDesiredResourceState();
    final ListDataSourcesRequest serviceRequest = Translator.translateToListRequest(
        resourceModel,
        request.getNextToken()
    );
    ListDataSourcesResponse listDataSourcesResponse = proxy.injectCredentialsAndInvokeV2(serviceRequest, proxyClient.client()::listDataSources);

    final String nextToken = listDataSourcesResponse.nextToken();

    List<ResourceModel> models = Translator.translateFromListResponse(
        resourceModel.getApplicationId(),
        resourceModel.getIndexId(),
        listDataSourcesResponse
    );

    return ProgressEvent.<ResourceModel, CallbackContext>builder()
        .resourceModels(models)
        .nextToken(nextToken)
        .status(OperationStatus.SUCCESS)
        .build();
  }
}
