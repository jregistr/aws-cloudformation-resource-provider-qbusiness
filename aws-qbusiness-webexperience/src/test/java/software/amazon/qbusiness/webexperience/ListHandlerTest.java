package software.amazon.qbusiness.webexperience;

import org.junit.jupiter.api.AfterEach;
import org.mockito.ArgumentMatcher;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.ListPluginsRequest;
import software.amazon.awssdk.services.qbusiness.model.ListPluginsResponse;
import software.amazon.awssdk.services.qbusiness.model.PluginSummary;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ListHandlerTest extends AbstractTestBase {

    private static final String APPLICATION_ID = "ApplicationId";
    private static final String PLUGIN_ID = "PluginId";
    private static final String PLUGIN_NAME = "PluginName";
    private static final String PLUGIN_TYPE = "JIRA";
    private static final String PLUGIN_STATE = "ACTIVE";
    private static final String SERVER_URL = "ServerUrl";
    private static final String NEXT_TOKEN = "next-token";
    private static final Long CREATED_TIME = 1697824935000L;
    private static final Long UPDATED_TIME = 1697839335000L;


    @Mock
    private AmazonWebServicesClientProxy proxy;
    @Mock
    private ProxyClient<QBusinessClient> proxyClient;
    @Mock
    private QBusinessClient sdkClient;

    private AutoCloseable testMocks;
    private ListHandler underTest;

    private ResourceHandlerRequest<ResourceModel> request;

    @BeforeEach
    public void setup() {
        testMocks = MockitoAnnotations.openMocks(this);
        proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
        proxyClient = MOCK_PROXY(proxy, sdkClient);

        underTest = new ListHandler();

        request = ResourceHandlerRequest.<ResourceModel>builder()
                .nextToken(NEXT_TOKEN)
                .desiredResourceState(ResourceModel.builder()
                        .applicationId(APPLICATION_ID)
                        .build())
            .build();
    }

    @AfterEach
    public void tear_down() throws Exception {
        verifyNoMoreInteractions(sdkClient);
        testMocks.close();
    }

    @Test
    public void handleRequest_SimpleSuccess() {

        List<PluginSummary> pluginSummaries = List.of(PluginSummary.builder()
                    .pluginId(PLUGIN_ID)
                    .displayName(PLUGIN_NAME)
                    .type(PLUGIN_TYPE)
                    .state(PLUGIN_STATE)
                    .serverUrl(SERVER_URL)
                    .createdAt(Instant.ofEpochMilli(CREATED_TIME))
                    .lastUpdatedAt(Instant.ofEpochMilli(UPDATED_TIME))
                .build());

        when(proxyClient.client().listPlugins(any(ListPluginsRequest.class)))
                .thenReturn(ListPluginsResponse.builder()
                        .plugins(pluginSummaries)
                        .nextToken(NEXT_TOKEN)
                        .build());

        final ProgressEvent<ResourceModel, CallbackContext> response = underTest.handleRequest(proxy, request, null,
                proxyClient, logger);

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getResourceModel()).isNull();
        assertThat(response.getResourceModels()).isNotEmpty();

        verify(sdkClient).listPlugins(
            argThat((ArgumentMatcher<ListPluginsRequest>) t -> t.nextToken().equals(NEXT_TOKEN))
        );

       ResourceModel model = response.getResourceModels().stream().toList().get(0);

       assertThat(model.getPluginId()).isEqualTo(PLUGIN_ID);
       assertThat(model.getType()).isEqualTo(PLUGIN_TYPE);
       assertThat(model.getState()).isEqualTo(PLUGIN_STATE);
       assertThat(model.getDisplayName()).isEqualTo(PLUGIN_NAME);
       assertThat(model.getCreatedAt()).isEqualTo(Instant.ofEpochMilli(CREATED_TIME).toString());
       assertThat(model.getLastUpdatedAt()).isEqualTo(Instant.ofEpochMilli(UPDATED_TIME).toString());

    }
}
