package software.amazon.qbusiness.retriever;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.amazon.qbusiness.retriever.AbstractTestBase.MOCK_CREDENTIALS;
import static software.amazon.qbusiness.retriever.AbstractTestBase.MOCK_PROXY;
import static software.amazon.qbusiness.retriever.AbstractTestBase.logger;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.ListRetrieversRequest;
import software.amazon.awssdk.services.qbusiness.model.ListRetrieversResponse;
import software.amazon.awssdk.services.qbusiness.model.Retriever;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class ListHandlerTest {
    private static final String APP_ID = "ApplicationId";
    private static final String TEST_NEXT_TOKEN = "this-is-next-token";

    @Mock
    private AmazonWebServicesClientProxy proxy;
    private ProxyClient<QBusinessClient> proxyClient;

    @Mock
    private QBusinessClient sdkClient;

    private AutoCloseable testMocks;
    private ListHandler underTest;

    private ResourceHandlerRequest<ResourceModel> testRequest;

    @BeforeEach
    public void setup() {
        testMocks = MockitoAnnotations.openMocks(this);
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        proxyClient = MOCK_PROXY(proxy, sdkClient);

        underTest = new ListHandler();
        testRequest = ResourceHandlerRequest.<ResourceModel>builder()
            .desiredResourceState(ResourceModel.builder()
                .applicationId(APP_ID)
                .build())
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
        List<String> ids = List.of(
            "a98163cb-407b-492c-85d7-a96ebc514eac",
            "db6a3cc2-3de5-4ede-b802-80f107d63ad8",
            "25e148e0-777d-4f30-b523-1f895c36cf55"
        );
        var listRetrieverSummaries = ids.stream()
            .map(id -> Retriever.builder()
                .applicationId(APP_ID)
                .retrieverId(id)
                .build()
            ).toList();
        when(sdkClient.listRetrievers(any(ListRetrieversRequest.class)))
            .thenReturn(ListRetrieversResponse.builder()
                .retrievers(listRetrieverSummaries)
                .build()
            );

        final ProgressEvent<ResourceModel, CallbackContext> resultProgress = underTest.handleRequest(
            proxy, testRequest, new CallbackContext(), proxyClient, logger
        );

        // verify
        assertThat(resultProgress).isNotNull();
        assertThat(resultProgress.isSuccess()).isTrue();
        assertThat(resultProgress.getResourceModel()).isNull();
        assertThat(resultProgress.getResourceModels()).isNotEmpty();

        var modelRetrieverIds = resultProgress.getResourceModels().stream()
            .map(model -> {
                assertThat(model.getApplicationId()).isEqualTo(APP_ID);
                return model.getRetrieverId();
            })
            .toList();
        assertThat(modelRetrieverIds).isEqualTo(ids);

        verify(sdkClient).listRetrievers(
            argThat((ArgumentMatcher<ListRetrieversRequest>) t -> t.nextToken().equals(TEST_NEXT_TOKEN) &&
                t.applicationId().equals(APP_ID))
        );
    }
}
