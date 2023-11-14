package software.amazon.qbusiness.application;

import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.AccessDeniedException;
import software.amazon.awssdk.services.qbusiness.model.ConflictException;
import software.amazon.awssdk.services.qbusiness.model.QBusinessException;
import software.amazon.awssdk.services.qbusiness.model.GetPluginRequest;
import software.amazon.awssdk.services.qbusiness.model.GetPluginResponse;
import software.amazon.awssdk.services.qbusiness.model.InternalServerException;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.ResourceNotFoundException;
import software.amazon.awssdk.services.qbusiness.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.qbusiness.model.ThrottlingException;
import software.amazon.awssdk.services.qbusiness.model.UpdatePluginRequest;
import software.amazon.awssdk.services.qbusiness.model.UpdatePluginResponse;
import software.amazon.awssdk.services.qbusiness.model.ValidationException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

    private static final String APPLICATION_ID = "ApplicationId";
    private static final String PLUGIN_ID = "PluginId";
    private static final String PLUGIN_NAME = "PluginName";
    private static final String PLUGIN_TYPE = "JIRA";
    private static final String PLUGIN_STATE = "ACTIVE";
    private static final String SERVER_URL = "ServerUrl";
    private static final String ROLE_ARN = "role-1";
    private static final String SECRET_ARN = "secret-1";
    private static final String UPDATED_PLUGIN_NAME = "UpdatedPluginName";
    private static final String UPDATED_PLUGIN_STATE = "INACTIVE";
    private static final String UPDATED_SERVER_URL = "UpdatedServerUrl";
    private static final String UPDATED_ROLE_ARN = "updated-role-1";
    private static final String UPDATED_SECRET_ARN = "updated-secret-1";
    private static final String AWS_PARTITION = "aws";
    private static final String ACCOUNT_ID = "123456789012";
    private static final String REGION = "us-west-2";

    @Mock
    private AmazonWebServicesClientProxy proxy;

    @Mock
    private ProxyClient<QBusinessClient> proxyClient;

    @Mock
    QBusinessClient qbusinessClient;

    private UpdateHandler underTest;
    private PluginAuthConfiguration serviceAuthConfiguration;
    private PluginAuthConfiguration updatedServiceAuthConfiguration;
    private software.amazon.awssdk.services.qbusiness.model.PluginAuthConfiguration cfnAuthConfiguration;
    private software.amazon.awssdk.services.qbusiness.model.PluginAuthConfiguration updatedCfnAuthConfiguration;
    private ResourceModel model;
    private ResourceModel updatedModel;
    private ResourceHandlerRequest<ResourceModel> request;

    @BeforeEach
    public void setup() {
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        qbusinessClient = mock(QBusinessClient.class);
        proxyClient = MOCK_PROXY(proxy, qbusinessClient);
        this.underTest = new UpdateHandler();

        serviceAuthConfiguration = PluginAuthConfiguration.builder()
                .basicAuthConfiguration(BasicAuthConfiguration.builder()
                        .roleArn(ROLE_ARN)
                        .secretArn(SECRET_ARN)
                        .build())
                .build();

        updatedServiceAuthConfiguration = PluginAuthConfiguration.builder()
                .basicAuthConfiguration(BasicAuthConfiguration.builder()
                        .roleArn(UPDATED_ROLE_ARN)
                        .secretArn(UPDATED_SECRET_ARN)
                        .build())
                .build();

        cfnAuthConfiguration = software.amazon.awssdk.services.qbusiness.model.PluginAuthConfiguration.builder()
                .basicAuthConfiguration(software.amazon.awssdk.services.qbusiness.model.BasicAuthConfiguration.builder()
                        .roleArn(ROLE_ARN)
                        .secretArn(SECRET_ARN)
                        .build())
                .build();

        updatedCfnAuthConfiguration = software.amazon.awssdk.services.qbusiness.model.PluginAuthConfiguration.builder()
                .basicAuthConfiguration(software.amazon.awssdk.services.qbusiness.model.BasicAuthConfiguration.builder()
                        .roleArn(UPDATED_ROLE_ARN)
                        .secretArn(UPDATED_SECRET_ARN)
                        .build())
                .build();

        model = ResourceModel.builder()
                    .applicationId(APPLICATION_ID)
                    .pluginId(PLUGIN_ID)
                    .displayName(PLUGIN_NAME)
                    .type(PLUGIN_TYPE)
                    .state(PLUGIN_STATE)
                    .serverUrl(SERVER_URL)
                    .authConfiguration(serviceAuthConfiguration)
                .build();

        updatedModel = ResourceModel.builder()
                    .applicationId(APPLICATION_ID)
                    .pluginId(PLUGIN_ID)
                    .displayName(UPDATED_PLUGIN_NAME)
                    .type(PLUGIN_TYPE)
                    .state(UPDATED_PLUGIN_STATE)
                    .serverUrl(UPDATED_SERVER_URL)
                    .authConfiguration(updatedServiceAuthConfiguration)
                .build();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                    .awsPartition(AWS_PARTITION)
                    .region(REGION)
                    .awsAccountId(ACCOUNT_ID)
                    .desiredResourceState(model)
                .build();
    }

    @AfterEach
    public void tear_down() {
        verify(qbusinessClient, atLeastOnce()).serviceName();
        verifyNoMoreInteractions(qbusinessClient);
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        when(proxyClient.client().updatePlugin(any(UpdatePluginRequest.class)))
          .thenReturn(UpdatePluginResponse.builder()
              .build());

        when(proxyClient.client().getPlugin(any(GetPluginRequest.class)))
          .thenReturn(GetPluginResponse.builder()
                  .applicationId(APPLICATION_ID)
                  .pluginId(PLUGIN_ID)
                  .displayName(UPDATED_PLUGIN_NAME)
                  .type(PLUGIN_TYPE)
                  .state(UPDATED_PLUGIN_STATE)
                  .serverUrl(UPDATED_SERVER_URL)
                  .authConfiguration(updatedCfnAuthConfiguration)
              .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = underTest.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

        verify(qbusinessClient).updatePlugin(any(UpdatePluginRequest.class));
        verify(qbusinessClient).getPlugin(any(GetPluginRequest.class));
        verify(qbusinessClient).listTagsForResource(any(ListTagsForResourceRequest.class));

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
        assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
        assertThat(response.getResourceModel()).isEqualTo(updatedModel);
        assertThat(response.getResourceModels()).isNull();
        assertThat(response.getMessage()).isNull();
        assertThat(response.getErrorCode()).isNull();
    }

    private static Stream<Arguments> serviceErrorAndHandlerCodes() {
    return Stream.of(
            Arguments.of(ValidationException.builder().build(), HandlerErrorCode.InvalidRequest),
            Arguments.of(ConflictException.builder().build(), HandlerErrorCode.ResourceConflict),
            Arguments.of(ResourceNotFoundException.builder().build(), HandlerErrorCode.NotFound),
            Arguments.of(ServiceQuotaExceededException.builder().build(), HandlerErrorCode.ServiceLimitExceeded),
            Arguments.of(ThrottlingException.builder().build(), HandlerErrorCode.Throttling),
            Arguments.of(AccessDeniedException.builder().build(), HandlerErrorCode.AccessDenied),
            Arguments.of(InternalServerException.builder().build(), HandlerErrorCode.GeneralServiceException)
        );
    }

  @ParameterizedTest
  @MethodSource("serviceErrorAndHandlerCodes")
  public void testThatItReturnsExpectedErrorCode(QBusinessException serviceError, HandlerErrorCode cfnErrorCode) {

    // set up test
    when(proxyClient.client().updatePlugin(any(UpdatePluginRequest.class)))
        .thenThrow(serviceError);

    // call method under test
    final ProgressEvent<ResourceModel, CallbackContext> responseProgress = underTest.handleRequest(
        proxy, request, new CallbackContext(), proxyClient, logger
    );

    // verify
    assertThat(responseProgress.getStatus()).isEqualTo(OperationStatus.FAILED);
    assertThat(responseProgress.getErrorCode()).isEqualTo(cfnErrorCode);
    assertThat(responseProgress.getResourceModels()).isNull();
  }

}
