package software.amazon.qbusiness.retriever;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.ListRetrieversRequest;
import software.amazon.awssdk.services.qbusiness.model.ListRetrieversResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.List;

public class ListHandler extends BaseHandlerStd {
    @Override
    public ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<QBusinessClient> proxyClient,
        final Logger logger) {

        final ListRetrieversRequest awsRequest = Translator.translateToListRequest(
            request.getDesiredResourceState(),
            request.getNextToken()
        );

        ListRetrieversResponse listRetrieversResponse = proxy.injectCredentialsAndInvokeV2(awsRequest, proxyClient.client()::listRetrievers);

        String nextToken = listRetrieversResponse.nextToken();

        List<ResourceModel> models = Translator.translateFromListResponse(listRetrieversResponse);
        return ProgressEvent.<ResourceModel, CallbackContext>builder()
            .resourceModels(models)
            .nextToken(nextToken)
            .status(OperationStatus.SUCCESS)
            .build();
    }
}
