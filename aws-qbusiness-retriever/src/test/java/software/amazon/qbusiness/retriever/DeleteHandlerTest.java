package software.amazon.qbusiness.retriever;

import java.time.Duration;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.AccessDeniedException;
import software.amazon.awssdk.services.qbusiness.model.ConflictException;
import software.amazon.awssdk.services.qbusiness.model.DeleteRetrieverRequest;
import software.amazon.awssdk.services.qbusiness.model.DeleteRetrieverResponse;
import software.amazon.awssdk.services.qbusiness.model.QBusinessException;
import software.amazon.awssdk.services.qbusiness.model.InternalServerException;
import software.amazon.awssdk.services.qbusiness.model.ResourceNotFoundException;
import software.amazon.awssdk.services.qbusiness.model.ThrottlingException;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeleteHandlerTest extends AbstractTestBase {
  private static final String APP_ID = "ApplicationId";
  private static final String RETRIEVER_TYPE = "KENDRA_INDEX";
  private static final String CLIENT_TOKEN = "client-token";
  @Mock
  private AmazonWebServicesClientProxy proxy;

  @Mock
  private ProxyClient<QBusinessClient> proxyClient;

  @Mock
  QBusinessClient sdkClient;

  private DeleteHandler underTest;
  private ResourceModel model;
  private ResourceHandlerRequest<ResourceModel> request;

  @BeforeEach
  public void setup() {
    proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    sdkClient = mock(QBusinessClient.class);
    proxyClient = MOCK_PROXY(proxy, sdkClient);
    this.underTest = new DeleteHandler();

    model = ResourceModel.builder()
        .applicationId(APP_ID)
        .retrieverType(RETRIEVER_TYPE)
        .build();
    request = ResourceHandlerRequest.<ResourceModel>builder()
        .awsPartition("aws")
        .region("us-west-2")
        .awsAccountId("123412341234")
        .desiredResourceState(model)
        .clientRequestToken(CLIENT_TOKEN)
        .build();
  }

  @AfterEach
  public void tear_down() {
    verify(sdkClient, atLeastOnce()).serviceName();
    verifyNoMoreInteractions(sdkClient);
  }

  @Test
  public void handleRequest_SimpleSuccess() {
    when(proxyClient.client().deleteRetriever(any(DeleteRetrieverRequest.class)))
        .thenReturn(DeleteRetrieverResponse.builder().build());

    final ProgressEvent<ResourceModel, CallbackContext> response = underTest.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

    verify(sdkClient).deleteRetriever(any(DeleteRetrieverRequest.class));

    assertThat(response).isNotNull();
    assertThat(response.isSuccess()).isTrue();
    assertThat(response.getResourceModel()).isNull();
    assertThat(response.getResourceModels()).isNull();
    assertThat(response.getErrorCode()).isNull();

    verify(sdkClient).deleteRetriever(
        argThat((ArgumentMatcher<DeleteRetrieverRequest>) t -> t.applicationId().equals(APP_ID))
    );
  }

  private static Stream<Arguments> serviceErrorAndHandlerCodes() {
    return Stream.of(
        Arguments.of(ConflictException.builder().build(), HandlerErrorCode.ResourceConflict),
        Arguments.of(ResourceNotFoundException.builder().build(), HandlerErrorCode.NotFound),
        Arguments.of(ThrottlingException.builder().build(), HandlerErrorCode.Throttling),
        Arguments.of(AccessDeniedException.builder().build(), HandlerErrorCode.AccessDenied),
        Arguments.of(InternalServerException.builder().build(), HandlerErrorCode.GeneralServiceException)
    );
  }

  @ParameterizedTest
  @MethodSource("serviceErrorAndHandlerCodes")
  public void testThatItReturnsExpectedHandlerErrorCodeForServiceError(QBusinessException serviceError, HandlerErrorCode expectedErrorCode) {
    // set up test
    when(sdkClient.deleteRetriever(any(DeleteRetrieverRequest.class))).thenThrow(serviceError);

    // call method under test
    final ProgressEvent<ResourceModel, CallbackContext> responseProgress = underTest.handleRequest(
        proxy, request, new CallbackContext(), proxyClient, logger
    );

    // verify
    assertThat(responseProgress).isNotNull();
    assertThat(responseProgress.isSuccess()).isFalse();
    assertThat(responseProgress.getStatus()).isEqualTo(OperationStatus.FAILED);
    verify(sdkClient).deleteRetriever(any(DeleteRetrieverRequest.class));
    assertThat(responseProgress.getErrorCode()).isEqualTo(expectedErrorCode);
    assertThat(responseProgress.getResourceModels()).isNull();
  }
}
