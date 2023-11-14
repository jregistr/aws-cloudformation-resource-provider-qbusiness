package software.amazon.qbusiness.webexperience;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.AccessDeniedException;
import software.amazon.awssdk.services.qbusiness.model.QBusinessException;
import software.amazon.awssdk.services.qbusiness.model.GetWebExperienceRequest;
import software.amazon.awssdk.services.qbusiness.model.GetWebExperienceResponse;
import software.amazon.awssdk.services.qbusiness.model.InternalServerException;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.qbusiness.model.ResourceNotFoundException;
import software.amazon.awssdk.services.qbusiness.model.ThrottlingException;
import software.amazon.awssdk.services.qbusiness.model.ValidationException;
import software.amazon.awssdk.services.qbusiness.model.WebExperienceStatus;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.HandlerErrorCode;
import software.amazon.cloudformation.proxy.OperationStatus;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ReadHandlerTest extends AbstractTestBase {

  private static final String APP_ID = "63451660-1596-4f1a-a3c8-e5f4b33d9fe5";
  private static final String WEB_EXPERIENCE_ID = "11111111-1596-4f1a-a3c8-e5f4b33d9fe5";

  @Mock
  private AmazonWebServicesClientProxy proxy;

  @Mock
  private ProxyClient<QBusinessClient> proxyClient;

  @Mock
  private QBusinessClient sdkClient;

  private ReadHandler underTest;

  ResourceHandlerRequest<ResourceModel> testRequest;
  ResourceModel model;

  @BeforeEach
  public void setup() {
    proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    sdkClient = mock(QBusinessClient.class);
    proxyClient = MOCK_PROXY(proxy, sdkClient);
    underTest = new ReadHandler();

    model = ResourceModel.builder()
        .applicationId(APP_ID)
        .webExperienceId(WEB_EXPERIENCE_ID)
        .build();
    testRequest = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .awsAccountId("123456")
        .awsPartition("aws")
        .region("us-east-1")
        .stackId("Stack1")
        .build();
  }

  @AfterEach
  public void tear_down() {
    verify(sdkClient, atLeastOnce()).serviceName();
    verifyNoMoreInteractions(sdkClient);
  }

  @Test
  public void handleRequest_SimpleSuccess() {
    // set up test scenario
    when(proxyClient.client().getWebExperience(any(GetWebExperienceRequest.class)))
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
    when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
        .thenReturn(ListTagsForResourceResponse.builder()
            .tags(List.of(software.amazon.awssdk.services.qbusiness.model.Tag.builder()
                .key("Category")
                .value("Chat Stuff")
                .build()))
            .build());

    // call method under test
    final ProgressEvent<ResourceModel, CallbackContext> responseProgress = underTest.handleRequest(
        proxy, testRequest, new CallbackContext(), proxyClient, logger
    );

    // verify result
    verify(sdkClient).getWebExperience(any(GetWebExperienceRequest.class));
    verify(sdkClient).listTagsForResource(any(ListTagsForResourceRequest.class));

    assertThat(responseProgress).isNotNull();
    assertThat(responseProgress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(responseProgress.getResourceModels()).isNull();
    assertThat(responseProgress.getMessage()).isNull();
    assertThat(responseProgress.getErrorCode()).isNull();
    ResourceModel resultModel = responseProgress.getResourceModel();
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

    var tags = resultModel.getTags().stream().map(tag -> Map.entry(tag.getKey(), tag.getValue())).toList();
    assertThat(tags).isEqualTo(List.of(
        Map.entry("Category", "Chat Stuff")
    ));
  }

  @Test
  public void handleRequest_SimpleSuccess_withMissingProperties() {
    // set up test scenario
    when(proxyClient.client().getWebExperience(any(GetWebExperienceRequest.class)))
        .thenReturn(GetWebExperienceResponse.builder()
            .applicationId(APP_ID)
            .webExperienceId(WEB_EXPERIENCE_ID)
            .title("This is a title of the web experience.")
            .status(WebExperienceStatus.ACTIVE)
            .authenticationConfiguration(software.amazon.awssdk.services.qbusiness.model.WebExperienceAuthConfiguration.builder()
                .samlConfigurationOptions(software.amazon.awssdk.services.qbusiness.model.SamlConfigurationOptions.builder()
                    .metadataXML("XML")
                    .roleArn("RoleARN")
                    .userAttribute("UserAttribute")
                    .userGroupAttribute("UserGroupAttribute")
                    .build())
                .build())
            .build());

    // call method under test
    final ProgressEvent<ResourceModel, CallbackContext> responseProgress = underTest.handleRequest(
        proxy, testRequest, new CallbackContext(), proxyClient, logger
    );

    // verify result
    verify(sdkClient).getWebExperience(any(GetWebExperienceRequest.class));
    verify(sdkClient).listTagsForResource(any(ListTagsForResourceRequest.class));
    assertThat(responseProgress).isNotNull();
    assertThat(responseProgress.getStatus()).isEqualTo(OperationStatus.SUCCESS);
    assertThat(responseProgress.getResourceModels()).isNull();
    assertThat(responseProgress.getMessage()).isNull();
    assertThat(responseProgress.getErrorCode()).isNull();

    ResourceModel resultModel = responseProgress.getResourceModel();
    assertThat(resultModel.getCreatedAt()).isNull();
    assertThat(resultModel.getUpdatedAt()).isNull();
    assertThat(resultModel.getSubtitle()).isNull();
    assertThat(resultModel.getEndpoints()).isEmpty();
    assertThat(resultModel.getTitle()).isEqualTo("This is a title of the web experience.");
    assertThat(resultModel.getApplicationId()).isEqualTo(APP_ID);
    assertThat(resultModel.getWebExperienceId()).isEqualTo(WEB_EXPERIENCE_ID);
    assertThat(resultModel.getAuthenticationConfiguration().getSamlConfigurationOptions().getMetadataXML()).isEqualTo("XML");
    assertThat(resultModel.getAuthenticationConfiguration().getSamlConfigurationOptions().getRoleArn()).isEqualTo("RoleARN");
    assertThat(resultModel.getAuthenticationConfiguration().getSamlConfigurationOptions().getUserAttribute()).isEqualTo("UserAttribute");
    assertThat(resultModel.getAuthenticationConfiguration().getSamlConfigurationOptions().getUserGroupAttribute())
        .isEqualTo("UserGroupAttribute");
  }

  private static Stream<Arguments> serviceErrorAndExpectedCfnCode() {
    return Stream.of(
        Arguments.of(ValidationException.builder().message("nopes").build(), HandlerErrorCode.InvalidRequest),
        Arguments.of(ResourceNotFoundException.builder().message("404").build(), HandlerErrorCode.NotFound),
        Arguments.of(ThrottlingException.builder().message("too much").build(), HandlerErrorCode.Throttling),
        Arguments.of(AccessDeniedException.builder().message("denied!").build(), HandlerErrorCode.AccessDenied),
        Arguments.of(InternalServerException.builder().message("something happened").build(), HandlerErrorCode.GeneralServiceException)
    );
  }

  @ParameterizedTest
  @MethodSource("serviceErrorAndExpectedCfnCode")
  public void testThatItReturnsExpectedErrorCode(QBusinessException serviceError, HandlerErrorCode cfnErrorCode) {
    when(proxyClient.client().getWebExperience(any(GetWebExperienceRequest.class)))
        .thenThrow(serviceError);

    // call method under test
    final ProgressEvent<ResourceModel, CallbackContext> responseProgress = underTest.handleRequest(
        proxy, testRequest, new CallbackContext(), proxyClient, logger
    );

    // verify
    assertThat(responseProgress.getStatus()).isEqualTo(OperationStatus.FAILED);
    verify(sdkClient).getWebExperience(any(GetWebExperienceRequest.class));
    assertThat(responseProgress.getErrorCode()).isEqualTo(cfnErrorCode);
    assertThat(responseProgress.getResourceModels()).isNull();
  }

  @ParameterizedTest
  @MethodSource("serviceErrorAndExpectedCfnCode")
  public void testThatItReturnsExpectedErrorCodeWhenListTagsForResourceFails(
      QBusinessException serviceError,
      HandlerErrorCode cfnErrorCode) {
    // set up test scenario
    when(proxyClient.client().getWebExperience(any(GetWebExperienceRequest.class)))
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

    when(proxyClient.client().listTagsForResource(any(ListTagsForResourceRequest.class)))
        .thenThrow(serviceError);

    // call method under test
    final ProgressEvent<ResourceModel, CallbackContext> responseProgress = underTest.handleRequest(
        proxy, testRequest, new CallbackContext(), proxyClient, logger
    );

    // verify
    assertThat(responseProgress.getStatus()).isEqualTo(OperationStatus.FAILED);
    verify(sdkClient).getWebExperience(any(GetWebExperienceRequest.class));
    assertThat(responseProgress.getErrorCode()).isEqualTo(cfnErrorCode);
    assertThat(responseProgress.getResourceModels()).isNull();
  }
}
