package software.amazon.qbusiness.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.Index;
import software.amazon.awssdk.services.qbusiness.model.IndexStatus;
import software.amazon.awssdk.services.qbusiness.model.ListIndicesRequest;
import software.amazon.awssdk.services.qbusiness.model.ListIndicesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandlerTest extends AbstractTestBase {

  private static final String APP_ID = "63451660-1596-4f1a-a3c8-e5f4b33d9fe5";
  private static final String TEST_NEXT_TOKEN = "this-is-next-token";

  private AmazonWebServicesClientProxy proxy;
  private ProxyClient<QBusinessClient> proxyClient;

  @Mock
  private QBusinessClient sdkClient;

  private AutoCloseable testMocks;
  private ListHandler underTest;
  private ResourceModel model;

  private ResourceHandlerRequest<ResourceModel> testRequest;

  @BeforeEach
  public void setup() {
    testMocks = MockitoAnnotations.openMocks(this);
    proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    proxyClient = MOCK_PROXY(proxy, sdkClient);

    underTest = new ListHandler();
    model = ResourceModel.builder()
        .applicationId(APP_ID)
        .build();
    testRequest = ResourceHandlerRequest.<ResourceModel>builder()
        .nextToken(TEST_NEXT_TOKEN)
        .desiredResourceState(model)
        .awsAccountId("123456")
        .awsPartition("aws")
        .region("us-east-1")
        .stackId("Stack1")
        .build();
  }

  @AfterEach
  public void tearDown() throws Exception {
    verifyNoMoreInteractions(sdkClient);
    testMocks.close();
  }

  @Test
  public void handleRequest_SimpleSuccess() {
    // set up scenario
    List<String> ids = List.of(
        "a98163cb-407b-492c-85d7-a96ebc514eac",
        "db6a3cc2-3de5-4ede-b802-80f107d63ad8",
        "25e148e0-777d-4f30-b523-1f895c36cf55"
    );
    var listIndices = ids.stream()
        .map(id -> Index.builder()
            .indexId(id)
            .createdAt(Instant.ofEpochMilli(1697824935000L))
            .updatedAt(Instant.ofEpochMilli(1697839335000L))
            .displayName("Index name")
            .status(IndexStatus.ACTIVE)
            .build()
        ).toList();
    when(sdkClient.listIndices(any(ListIndicesRequest.class)))
        .thenReturn(ListIndicesResponse.builder()
            .indices(listIndices)
            .build()
        );

    // call method under test
    final ProgressEvent<ResourceModel, CallbackContext> resultProgress = underTest.handleRequest(
        proxy, testRequest, new CallbackContext(), proxyClient, logger
    );

    // verify
    assertThat(resultProgress).isNotNull();
    assertThat(resultProgress.isSuccess()).isTrue();
    assertThat(resultProgress.getResourceModel()).isNull();
    assertThat(resultProgress.getResourceModels()).isNotEmpty();

    var modelIds = resultProgress.getResourceModels().stream()
        .map((resourceModel) -> {
          assertThat(resourceModel.getApplicationId()).isEqualTo(APP_ID);
          return resourceModel.getIndexId();
        })
        .toList();
    assertThat(modelIds).isEqualTo(ids);

    verify(sdkClient).listIndices(
        argThat((ArgumentMatcher<ListIndicesRequest>) t -> t.nextToken().equals(TEST_NEXT_TOKEN))
    );
  }
}
