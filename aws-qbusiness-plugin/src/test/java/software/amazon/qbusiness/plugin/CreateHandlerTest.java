package software.amazon.qbusiness.plugin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.AccessDeniedException;
import software.amazon.awssdk.services.qbusiness.model.ConflictException;
import software.amazon.awssdk.services.qbusiness.model.CreateWebExperienceRequest;
import software.amazon.awssdk.services.qbusiness.model.CreateWebExperienceResponse;
import software.amazon.awssdk.services.qbusiness.model.QBusinessException;
import software.amazon.awssdk.services.qbusiness.model.GetWebExperienceRequest;
import software.amazon.awssdk.services.qbusiness.model.GetWebExperienceResponse;
import software.amazon.awssdk.services.qbusiness.model.WebExperienceStatus;
import software.amazon.awssdk.services.qbusiness.model.InternalServerException;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.qbusiness.model.ResourceNotFoundException;
import software.amazon.awssdk.services.qbusiness.model.ServiceQuotaExceededException;
import software.amazon.awssdk.services.qbusiness.model.ThrottlingException;
import software.amazon.awssdk.services.qbusiness.model.ValidationException;
import software.amazon.cloudformation.exceptions.CfnNotStabilizedException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CreateHandlerTest extends AbstractTestBase {

  private static final String APP_ID = "a197dafc-2158-4f93-ab0d-b1c361c39838";
  private static final String WEB_EXPERIENCE_ID = "22222222-1596-4f1a-a3c8-e5f4b33d9fe5";

  @Mock
  private AmazonWebServicesClientProxy proxy;

  @Mock
  private ProxyClient<QBusinessClient> proxyClient;

  @Mock
  QBusinessClient qbusinessClient;

  private Constant testBackOff;
  private CreateHandler underTest;

  private AutoCloseable testMocks;

  private ResourceHandlerRequest<ResourceModel> testRequest;
  private ResourceModel createModel;

  @BeforeEach
  public void setup() {
    testMocks = MockitoAnnotations.openMocks(this);
    proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    qbusinessClient = mock(QBusinessClient.class);
    proxyClient = MOCK_PROXY(proxy, qbusinessClient);

    testBackOff = Constant.of()
        .delay(Duration.ofSeconds(5))
        .timeout(Duration.ofSeconds(45))
        .build();

    underTest = new CreateHandler(testBackOff);

    createModel = ResourceModel.builder()
        .applicationId(APP_ID)
        .title("This is a title of the web experience.")
        .subtitle("This is a subtitle of the web experience.")
        .authenticationConfiguration(WebExperienceAuthConfiguration.builder()
            .samlConfigurationOptions(SamlConfigurationOptions.builder()
                .metadataXML("XML")
                .roleArn("RoleARN")
                .userAttribute("UserAttribute")
                .userGroupAttribute("UserGroupAttribute")
                .build())
            .build())
        .build();

    testRequest = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(createModel)
        .awsAccountId("123456")
        .awsPartition("aws")
        .region("us-east-1")
        .stackId("Stack1")
        .build();
  }

  @AfterEach
  public void tear_down() throws Exception {
    verify(qbusinessClient, atLeastOnce()).serviceName();
    verifyNoMoreInteractions(qbusinessClient);
    testMocks.close();
  }

  @Test
  public void handleRequest_SimpleSuccess() {
    // set up scenario
    when(qbusinessClient.createWebExperience(any(CreateWebExperienceRequest.class)))
        .thenReturn(CreateWebExperienceResponse.builder()
            .id(WEB_EXPERIENCE_ID)
            .build()
        );
    when(qbusinessClient.listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder()
        .tags(List.of())
        .build());
    when(qbusinessClient.getWebExperience(any(GetWebExperienceRequest.class)))
        .thenReturn(GetWebExperienceResponse.builder()
            .applicationId(APP_ID)
            .webExperienceId(WEB_EXPERIENCE_ID)
            .createdAt(Instant.ofEpochMilli(1697824935000L))
            .updatedAt(Instant.ofEpochMilli(1697839335000L))
            .title("This is a title of the web experience.")
            .subtitle("This is a subtitle of the web experience.")
            .status(WebExperienceStatus.ACTIVE)
            .authenticationConfiguration(software.amazon.awssdk.services.qbusiness.model.WebExperienceAuthConfiguration.builder()
                .samlConfigurationOptions(software.amazon.awssdk.services.qbusiness.model.SamlConfigurationOptions.builder()
                    .metadataXML("XML")
                    .roleArn("RoleARN")
                    .userAttribute("UserAttribute")
                    .userGroupAttribute("UserGroupAttribute")
                    .build())
                .build())
            .endpoints(List.of(software.amazon.awssdk.services.qbusiness.model.WebExperienceEndpointConfig.builder()
                .endpoint("Endpoint")
                .type("Type")
                .build()))
            .build());

    // call method under test
    final ProgressEvent<ResourceModel, CallbackContext> resultProgress = underTest.handleRequest(
        proxy, testRequest, new CallbackContext(), proxyClient, logger
    );

    // verify
    assertThat(resultProgress).isNotNull();
    assertThat(resultProgress.isSuccess()).isTrue();
    var resultModel = resultProgress.getResourceModel();
    assertThat(resultModel.getApplicationId()).isEqualTo(APP_ID);
    assertThat(resultModel.getWebExperienceId()).isEqualTo(WEB_EXPERIENCE_ID);
    assertThat(resultModel.getCreatedAt()).isEqualTo("2023-10-20T18:02:15Z");
    assertThat(resultModel.getUpdatedAt()).isEqualTo("2023-10-20T22:02:15Z");
    assertThat(resultModel.getTitle()).isEqualTo("This is a title of the web experience.");
    assertThat(resultModel.getSubtitle()).isEqualTo("This is a subtitle of the web experience.");
    assertThat(resultModel.getStatus()).isEqualTo(WebExperienceStatus.ACTIVE.toString());
    assertThat(resultModel.getAuthenticationConfiguration().getSamlConfigurationOptions().getMetadataXML()).isEqualTo("XML");
    assertThat(resultModel.getAuthenticationConfiguration().getSamlConfigurationOptions().getRoleArn()).isEqualTo("RoleARN");
    assertThat(resultModel.getAuthenticationConfiguration().getSamlConfigurationOptions().getUserAttribute()).isEqualTo("UserAttribute");
    assertThat(resultModel.getAuthenticationConfiguration().getSamlConfigurationOptions().getUserGroupAttribute())
        .isEqualTo("UserGroupAttribute");
    assertThat(resultModel.getEndpoints().size()).isEqualTo(1);
    assertThat(resultModel.getEndpoints().get(0).getEndpoint()).isEqualTo("Endpoint");
    assertThat(resultModel.getEndpoints().get(0).getType()).isEqualTo("Type");

    verify(qbusinessClient).createWebExperience(any(CreateWebExperienceRequest.class));
    verify(qbusinessClient, times(2)).getWebExperience(
        argThat((ArgumentMatcher<GetWebExperienceRequest>) t -> t.applicationId().equals(APP_ID) && t.webExperienceId().equals(WEB_EXPERIENCE_ID))
    );
    verify(qbusinessClient).listTagsForResource(any(ListTagsForResourceRequest.class));
  }

  @Test
  public void handleRequestFromProcessingStateToActive() {
    // set up scenario
    var getResponse = GetWebExperienceResponse.builder()
        .applicationId(APP_ID)
        .webExperienceId(WEB_EXPERIENCE_ID)
        .createdAt(Instant.ofEpochMilli(1697824935000L))
        .updatedAt(Instant.ofEpochMilli(1697839335000L))
        .title("This is a title of the web experience.")
        .subtitle("This is a subtitle of the web experience.")
        .authenticationConfiguration(software.amazon.awssdk.services.qbusiness.model.WebExperienceAuthConfiguration.builder()
            .samlConfigurationOptions(software.amazon.awssdk.services.qbusiness.model.SamlConfigurationOptions.builder()
                .metadataXML("XML")
                .roleArn("RoleARN")
                .userAttribute("UserAttribute")
                .userGroupAttribute("UserGroupAttribute")
                .build())
            .build())
        .endpoints(List.of(software.amazon.awssdk.services.qbusiness.model.WebExperienceEndpointConfig.builder()
            .endpoint("Endpoint")
            .type("Type")
            .build()))
        .build();

    when(qbusinessClient.createWebExperience(any(CreateWebExperienceRequest.class)))
        .thenReturn(CreateWebExperienceResponse.builder()
            .id(WEB_EXPERIENCE_ID)
            .build()
        );
    when(qbusinessClient.listTagsForResource(any(ListTagsForResourceRequest.class))).thenReturn(ListTagsForResourceResponse.builder()
        .tags(List.of())
        .build());

    when(qbusinessClient.getWebExperience(any(GetWebExperienceRequest.class)))
        .thenReturn(
            getResponse.toBuilder().status(WebExperienceStatus.CREATING).build(),
            getResponse.toBuilder().status(WebExperienceStatus.ACTIVE).build()
        );

    // call method under test
    final ProgressEvent<ResourceModel, CallbackContext> resultProgress = underTest.handleRequest(
        proxy, testRequest, new CallbackContext(), proxyClient, logger
    );

    // verify
    assertThat(resultProgress).isNotNull();
    assertThat(resultProgress.isSuccess()).isTrue();
    verify(qbusinessClient).createWebExperience(any(CreateWebExperienceRequest.class));
    verify(qbusinessClient, times(3)).getWebExperience(
        argThat((ArgumentMatcher<GetWebExperienceRequest>) t -> t.applicationId().equals(APP_ID) && t.webExperienceId().equals(WEB_EXPERIENCE_ID))
    );
    verify(qbusinessClient).listTagsForResource(any(ListTagsForResourceRequest.class));
  }

  @Test
  public void testItFailsWithErrorMessageWhenGetReturnsFailStatus() {
    // set up
    when(qbusinessClient.createWebExperience(any(CreateWebExperienceRequest.class)))
        .thenReturn(CreateWebExperienceResponse.builder()
            .id(WEB_EXPERIENCE_ID)
            .build()
        );
    when(qbusinessClient.getWebExperience(any(GetWebExperienceRequest.class)))
        .thenReturn(GetWebExperienceResponse.builder()
            .applicationId(APP_ID)
            .webExperienceId(WEB_EXPERIENCE_ID)
            .createdAt(Instant.ofEpochMilli(1697824935000L))
            .updatedAt(Instant.ofEpochMilli(1697839335000L))
            .title("This is a title of the web experience.")
            .subtitle("This is a subtitle of the web experience.")
            .errorMessage("There was a problem in get web experience.")
            .status(WebExperienceStatus.FAILED)
            .authenticationConfiguration(software.amazon.awssdk.services.qbusiness.model.WebExperienceAuthConfiguration.builder()
                .samlConfigurationOptions(software.amazon.awssdk.services.qbusiness.model.SamlConfigurationOptions.builder()
                    .metadataXML("XML")
                    .roleArn("RoleARN")
                    .userAttribute("UserAttribute")
                    .userGroupAttribute("UserGroupAttribute")
                    .build())
                .build())
            .endpoints(List.of(software.amazon.awssdk.services.qbusiness.model.WebExperienceEndpointConfig.builder()
                .endpoint("Endpoint")
                .type("Type")
                .build()))
            .build());

    // call method under test & verify
    assertThatThrownBy(() -> underTest.handleRequest(
        proxy, testRequest, new CallbackContext(), proxyClient, logger
    )).isInstanceOf(CfnNotStabilizedException.class);

    verify(qbusinessClient).createWebExperience(any(CreateWebExperienceRequest.class));
    verify(qbusinessClient).getWebExperience(any(GetWebExperienceRequest.class));
  }

  private static Stream<Arguments> createWebExperienceErrorsAndExpectedCodes() {
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
  @MethodSource("createWebExperienceErrorsAndExpectedCodes")
  public void testItReturnsExpectedCfnErrorWhenCreateWebExperienceFails(
      final QBusinessException serviceError,
      final HandlerErrorCode expectedHandlerErrorCode) {
    // set up
    when(qbusinessClient.createWebExperience(any(CreateWebExperienceRequest.class)))
        .thenThrow(serviceError);

    // call method under test
    final ProgressEvent<ResourceModel, CallbackContext> resultProgress = underTest.handleRequest(
        proxy, testRequest, new CallbackContext(), proxyClient, logger
    );

    // verify
    assertThat(resultProgress.getStatus()).isEqualTo(OperationStatus.FAILED);
    verify(qbusinessClient).createWebExperience(any(CreateWebExperienceRequest.class));
    assertThat(resultProgress.getErrorCode()).isEqualTo(expectedHandlerErrorCode);
  }
}
