package software.amazon.qbusiness.index;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Duration;

import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class UpdateHandlerTest extends AbstractTestBase {

  @Mock
  private AmazonWebServicesClientProxy proxy;

  @Mock
  private ProxyClient<QBusinessClient> proxyClient;

  @Mock
  QBusinessClient qbusinessClient;

  @BeforeEach
  public void setup() {
    proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    qbusinessClient = mock(QBusinessClient.class);
    proxyClient = MOCK_PROXY(proxy, qbusinessClient);
  }

  @AfterEach
  public void tear_down() {
//    TODO: Fix when UpdateHandler is complete.
//    verify(qbusinessClient, atLeastOnce()).serviceName();
//    verifyNoMoreInteractions(qbusinessClient);
  }

  @Test
  public void handleRequest_SimpleSuccess() {
    final UpdateHandler handler = new UpdateHandler();

    final ResourceModel model = ResourceModel.builder().build();

    final ResourceHandlerRequest<ResourceModel> request = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .build();

    final ProgressEvent<ResourceModel, CallbackContext> response = handler.handleRequest(proxy, request, new CallbackContext(), proxyClient, logger);

//    TODO: Fix when UpdateHandler is complete.
//    assertThat(response).isNotNull();
//    assertThat(response.getStatus()).isEqualTo(OperationStatus.SUCCESS);
//    assertThat(response.getCallbackDelaySeconds()).isEqualTo(0);
//    assertThat(response.getResourceModel()).isEqualTo(request.getDesiredResourceState());
//    assertThat(response.getResourceModels()).isNull();
//    assertThat(response.getMessage()).isNull();
//    assertThat(response.getErrorCode()).isNull();
  }
}
