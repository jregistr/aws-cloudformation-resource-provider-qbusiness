package software.amazon.qbusiness.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.DataSource;
import software.amazon.awssdk.services.qbusiness.model.ListDataSourcesRequest;
import software.amazon.awssdk.services.qbusiness.model.ListDataSourcesResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandlerTest extends AbstractTestBase {

  private static final String TEST_NEXT_TOKEN = "next-token";
  private static final String APP_ID = "5d31a0e5-2d19-4ac3-90da-34534fa1d2df";
  private static final String INDEX_ID = "9a2515e0-5760-4414-9fe2-c17e95406e5f";

  private AmazonWebServicesClientProxy proxy;
  private ProxyClient<QBusinessClient> proxyClient;

  @Mock
  private QBusinessClient sdkClient;

  private AutoCloseable testMocks;
  private ListHandler underTest;

  private ResourceHandlerRequest<ResourceModel> testRequest;
  private ResourceModel model;

  @BeforeEach
  public void setup() {
    testMocks = MockitoAnnotations.openMocks(this);
    proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    proxyClient = MOCK_PROXY(proxy, sdkClient);

    underTest = new ListHandler();

    model = ResourceModel.builder()
        .applicationId(APP_ID)
        .indexId(INDEX_ID)
        .build();

    testRequest = ResourceHandlerRequest.<ResourceModel>builder()
        .desiredResourceState(model)
        .nextToken(TEST_NEXT_TOKEN)
        .build();
  }

  @AfterEach
  public void tearDown() throws Exception {
    verifyNoMoreInteractions(sdkClient);
    testMocks.close();
  }

  @Test
  public void handleRequest_SimpleSuccess() {
    // set up
    List<String> dataSourceIds = List.of(
        "52868e5a-6cda-4380-a2fd-a50df21b66ea",
        "406eb804-ccb5-49f3-a9b3-bd348b6e568f",
        "6b77da76-3dd8-4614-ba6f-0956f6981a49"
    );
    when(sdkClient.listDataSources(any(ListDataSourcesRequest.class)))
        .thenReturn(ListDataSourcesResponse.builder()
            .nextToken("some-other-token")
            .dataSources(dataSourceIds.stream()
                .map(id -> DataSource.builder()
                    .dataSourceId(id)
                    .build()
                )
                .toList())
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
    verify(sdkClient).listDataSources(
        argThat((ArgumentMatcher<ListDataSourcesRequest>) t -> t.nextToken().equals(TEST_NEXT_TOKEN) &&
            t.applicationId().equals(APP_ID) && t.indexId().equals(INDEX_ID)
        )
    );

    var modelIds = resultProgress.getResourceModels().stream()
        .map(ResourceModel::getDataSourceId)
        .toList();
    assertThat(modelIds).isEqualTo(dataSourceIds);
  }
}
