package software.amazon.qbusiness.plugin;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.CreatePluginRequest;
import software.amazon.awssdk.services.qbusiness.model.CreatePluginResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

import java.time.Duration;

import static software.amazon.qbusiness.plugin.Constants.API_CREATE_PLUGIN;


public class CreateHandler extends BaseHandlerStd {
    private Logger logger;

    private static final Constant DEFAULT_BACK_OFF_STRATEGY = Constant.of()
     .timeout(Duration.ofHours(4))
     .delay(Duration.ofMinutes(2))
     .build();
    private final Constant backOffStrategy;
    public CreateHandler() {
      this(DEFAULT_BACK_OFF_STRATEGY);
    }
    public CreateHandler(Constant backOffStrategy) {
      this.backOffStrategy = backOffStrategy;
    }

    protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
        final AmazonWebServicesClientProxy proxy,
        final ResourceHandlerRequest<ResourceModel> request,
        final CallbackContext callbackContext,
        final ProxyClient<QBusinessClient> proxyClient,
        final Logger logger) {

        this.logger = logger;

        this.logger.log("[INFO] - [StackId: %s, ApplicationId: %s, PluginId: %s] Entering Create Handler"
        .formatted(request.getStackId(), request.getDesiredResourceState().getApplicationId(), request.getDesiredResourceState().getPluginId()));

        return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
        .then(progress ->
            proxy.initiate("AWS-QBusiness-Plugin::Create", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(model -> Translator.translateToCreateRequest(model, request.getClientRequestToken()))
                .backoffDelay(backOffStrategy)
                .makeServiceCall((awsRequest, clientProxyClient) -> callCreatePlugin(awsRequest, clientProxyClient, progress.getResourceModel()))
                .handleError((describeApplicationRequest, error, client, model, context) ->
                    handleError(describeApplicationRequest, model, error, context, logger, API_CREATE_PLUGIN))
                .progress()
        )
        .then(progress ->
            new ReadHandler().handleRequest(proxy, request, callbackContext, proxyClient, logger)
        );
    }

    private CreatePluginResponse callCreatePlugin(CreatePluginRequest request,
                                                  ProxyClient<QBusinessClient> client,
                                                  ResourceModel model) {
      CreatePluginResponse response = client.injectCredentialsAndInvokeV2(request, client.client()::createPlugin);
      model.setPluginId(response.pluginId());
      return response;
  }
}
