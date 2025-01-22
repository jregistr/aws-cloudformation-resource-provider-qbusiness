package software.amazon.qbusiness.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.ApplicationStatus;
import software.amazon.awssdk.services.qbusiness.model.AttachmentsControlMode;
import software.amazon.awssdk.services.qbusiness.model.GetApplicationRequest;
import software.amazon.awssdk.services.qbusiness.model.GetApplicationResponse;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.qbusiness.model.TagResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.TagResourceResponse;
import software.amazon.awssdk.services.qbusiness.model.UntagResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.UntagResourceResponse;
import software.amazon.awssdk.services.qbusiness.model.UpdateApplicationRequest;
import software.amazon.awssdk.services.qbusiness.model.UpdateApplicationResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

public class UpdateHandlerTest extends AbstractTestBase {

  private static final String APP_ID = "a256a36f-7ae8-47c9-b794-5bb67b78a580";

  private AmazonWebServicesClientProxy proxy;

  private ProxyClient<QBusinessClient> proxyClient;

  @Mock
  private QBusinessClient sdkClient;
  private AutoCloseable testMocks;
  private ResourceHandlerRequest<ResourceModel> testRequest;
  private ResourceModel previousModel;
  private ResourceModel updateModel;
  private Constant backOffStrategy;
  private UpdateHandler underTest;

  @BeforeEach
  public void setup() {
    testMocks = MockitoAnnotations.openMocks(this);
    proxy = new AmazonWebServicesClientProxy(logger, MOCK_CREDENTIALS, () -> Duration.ofSeconds(600).toMillis());
    proxyClient = MOCK_PROXY(proxy, sdkClient);

    backOffStrategy = Constant.of()
        .delay(Duration.ofSeconds(5))
        .timeout(Duration.ofSeconds(45))
        .build();

    underTest = new UpdateHandler(backOffStrategy);

    previousModel = ResourceModel.builder()
        .displayName("Name of Me")
        .applicationId(APP_ID)
        .description("This is a description")
        .createdAt("2023-10-20T18:02:15Z")
        .updatedAt("2023-10-20T22:02:15Z")
        .roleArn("what-a-role")
        .status(ApplicationStatus.ACTIVE.name())
        .attachmentsConfiguration(AttachmentsConfiguration.builder()
            .attachmentsControlMode(AttachmentsControlMode.DISABLED.toString())
            .build()
        )
        .identityCenterInstanceArn("arn:aws:sso:::instance/before")
        .tags(List.of(
            Tag.builder().key("remain").value("thesame").build(),
            Tag.builder().key("toremove").value("nolongerthere").build(),
            Tag.builder().key("iwillchange").value("oldvalue").build()
        ))
        .build();

    updateModel = ResourceModel.builder()
        .applicationId(APP_ID)
        .displayName("New Phone Who dis")
        .description("It's a new description")
        .roleArn("now-better-role")
        .attachmentsConfiguration(AttachmentsConfiguration.builder()
            .attachmentsControlMode(AttachmentsControlMode.ENABLED.toString())
            .build()
        )
        .identityCenterInstanceArn("arn:aws:sso:::instance/after")
        .tags(List.of(
            Tag.builder().key("remain").value("thesame").build(),
            Tag.builder().key("iwillchange").value("nowanewvalue").build(),
            Tag.builder().key("iamnew").value("overhere").build()
        ))
        .build();

    testRequest = ResourceHandlerRequest.<ResourceModel>builder()
        .awsAccountId("123456")
        .awsPartition("aws")
        .region("us-east-1")
        .stackId("Stack1")
        .systemTags(Map.of(
            "aws::cloudformation::created", "onthisday"
        ))
        .previousSystemTags(Map.of(
            "aws::cloudformation::created", "onthisday"
        ))
        .previousResourceTags(Map.of(
            "stacksame", "value",
            "stackchange", "whowho",
            "stack-i-remove", "hello"
        ))
        .desiredResourceTags(Map.of(
            "stacksame", "value",
            "stackchange", "whatwhenwhere",
            "stacknewaddition", "newvalue"
        ))
        .previousResourceState(previousModel)
        .desiredResourceState(updateModel)
        .build();

    when(sdkClient.updateApplication(any(UpdateApplicationRequest.class)))
        .thenReturn(UpdateApplicationResponse.builder().build());
    when(sdkClient.tagResource(any(TagResourceRequest.class)))
        .thenReturn(TagResourceResponse.builder().build());
    when(sdkClient.untagResource(any(UntagResourceRequest.class)))
        .thenReturn(UntagResourceResponse.builder().build());
    when(sdkClient.getApplication(any(GetApplicationRequest.class)))
        .thenReturn(GetApplicationResponse.builder()
            .applicationId(APP_ID)
            .status(ApplicationStatus.ACTIVE)
            .build());
    when(sdkClient.listTagsForResource(any(ListTagsForResourceRequest.class)))
        .thenReturn(ListTagsForResourceResponse.builder().tags(List.of()).build());
  }

  @AfterEach
  public void tear_down() throws Exception {
    verify(sdkClient, atLeastOnce()).serviceName();
    verifyNoMoreInteractions(sdkClient);
    testMocks.close();
  }

 /* @Test
  public void handleRequest_SimpleSuccess() {
    // call method under test
    final ProgressEvent<ResourceModel, CallbackContext> resultProgress = underTest.handleRequest(
        proxy, testRequest, new CallbackContext(), proxyClient, logger
    );

    // verify
    assertThat(resultProgress).isNotNull();
    assertThat(resultProgress.isSuccess()).isTrue();
    ArgumentCaptor<UpdateApplicationRequest> updateAppReqCaptor = ArgumentCaptor.forClass(UpdateApplicationRequest.class);
    verify(sdkClient).updateApplication(updateAppReqCaptor.capture());
    var updateAppRequest = updateAppReqCaptor.getValue();
    assertThat(updateAppRequest.applicationId()).isEqualTo(APP_ID);
    assertThat(updateAppRequest.displayName()).isEqualTo("New Phone Who dis");
    assertThat(updateAppRequest.description()).isEqualTo("It's a new description");
    assertThat(updateAppRequest.roleArn()).isEqualTo("now-better-role");
    assertThat(updateAppRequest.identityCenterInstanceArn()).isEqualTo("arn:aws:sso:::instance/after");
    assertThat(updateAppRequest.attachmentsConfiguration()).isEqualTo(software.amazon.awssdk.services.qbusiness.model.AttachmentsConfiguration.builder()
        .attachmentsControlMode(AttachmentsControlMode.ENABLED)
        .build());

    verify(sdkClient, times(2)).getApplication(
        argThat((ArgumentMatcher<GetApplicationRequest>) t -> t.applicationId().equals(APP_ID))
    );
    verify(sdkClient).listTagsForResource(any(ListTagsForResourceRequest.class));

    var tagReqCaptor = ArgumentCaptor.forClass(TagResourceRequest.class);
    var untagReqCaptor = ArgumentCaptor.forClass(UntagResourceRequest.class);
    verify(sdkClient).tagResource(tagReqCaptor.capture());
    verify(sdkClient).untagResource(untagReqCaptor.capture());

    var tagResourceRequest = tagReqCaptor.getValue();
    Map<String, String> tagsInTagResourceReq = tagResourceRequest.tags().stream()
        .collect(Collectors.toMap(tag -> tag.key(), tag -> tag.value()));
    assertThat(tagsInTagResourceReq).containsOnly(
        Map.entry("iwillchange", "nowanewvalue"),
        Map.entry("iamnew", "overhere"),
        Map.entry("stackchange", "whatwhenwhere"),
        Map.entry("stacknewaddition", "newvalue")
    );

    var untagResourceReq = untagReqCaptor.getValue();
    assertThat(untagResourceReq.tagKeys()).containsOnlyOnceElementsOf(List.of(
        "toremove",
        "stack-i-remove"
    ));
  }*/

  @Test
  public void testThatItDoesntTagAndUnTag() {
    // set up scenario
    previousModel.setTags(List.of(
        Tag.builder().key("remain").value("thesame").build()
    ));
    updateModel.setTags(List.of(
        Tag.builder().key("remain").value("thesame").build()
    ));

    testRequest.setPreviousResourceTags(Map.of(
        "stacksame", "value"
    ));
    testRequest.setDesiredResourceTags(Map.of(
        "stacksame", "value"
    ));

    // call method under test
    final ProgressEvent<ResourceModel, CallbackContext> resultProgress = underTest.handleRequest(
        proxy, testRequest, new CallbackContext(), proxyClient, logger
    );

    // verify
    assertThat(resultProgress).isNotNull();
    assertThat(resultProgress.isSuccess()).isTrue();
    verify(sdkClient).updateApplication(any(UpdateApplicationRequest.class));
    verify(sdkClient, times(2)).getApplication(
        argThat((ArgumentMatcher<GetApplicationRequest>) t -> t.applicationId().equals(APP_ID))
    );
    verify(sdkClient).listTagsForResource(any(ListTagsForResourceRequest.class));
  }

  private static Stream<Arguments> tagAndUntagArguments() {
    return Stream.of(
        Arguments.of(
            null,
            Map.of("datTag", "suchValue"),
            Map.of("stacksame", "newValue")
        ),
        Arguments.of(
            List.of(Tag.builder().key("datTag").value("valuea").build()),
            null,
            Map.of("stacksame", "newValue")
        ),
        Arguments.of(
            List.of(Tag.builder().key("datTag").value("valuea").build()),
            Map.of("stacksame", "newValue"),
            null
        ),
        Arguments.of(
            null,
            null,
            null
        )
    );
  }

  @ParameterizedTest
  @MethodSource("tagAndUntagArguments")
  public void testThatItDoesntTagAndUnTagWhenTagFieldsAreNull(
      List<Tag> modelTags,
      Map<String, String> reqTags,
      Map<String, String> sysTags
  ) {
    // set up test scenario
    previousModel.setTags(modelTags);
    updateModel.setTags(modelTags);
    testRequest.setPreviousResourceTags(reqTags);
    testRequest.setDesiredResourceTags(reqTags);
    testRequest.setPreviousSystemTags(sysTags);
    testRequest.setSystemTags(sysTags);

    // call method under test
    final ProgressEvent<ResourceModel, CallbackContext> resultProgress = underTest.handleRequest(
        proxy, testRequest, new CallbackContext(), proxyClient, logger
    );

    // verify
    assertThat(resultProgress).isNotNull();
    assertThat(resultProgress.isSuccess()).isTrue();
    verify(sdkClient).updateApplication(any(UpdateApplicationRequest.class));
    verify(sdkClient, times(2)).getApplication(
        argThat((ArgumentMatcher<GetApplicationRequest>) t -> t.applicationId().equals(APP_ID))
    );
    verify(sdkClient).listTagsForResource(any(ListTagsForResourceRequest.class));
  }

 /* @Test
  public void testThatItCallsTagButSkipsCallingUntag() {
    // set up
    testRequest.setPreviousResourceTags(Map.of(
        "stacksame", "value"
    ));
    testRequest.setDesiredResourceTags(Map.of(
        "stacksame", "newValue"
    ));
    previousModel.setTags(List.of(
        Tag.builder().key("datTag").value("valuea").build()
    ));
    updateModel.setTags(List.of(
        Tag.builder().key("datTag").value("valueb").build()
    ));

    // call method under test
    final ProgressEvent<ResourceModel, CallbackContext> resultProgress = underTest.handleRequest(
        proxy, testRequest, new CallbackContext(), proxyClient, logger
    );

    // verify
    assertThat(resultProgress).isNotNull();
    assertThat(resultProgress.isSuccess()).isTrue();
    verify(sdkClient).updateApplication(any(UpdateApplicationRequest.class));

    verify(sdkClient, times(2)).getApplication(
        argThat((ArgumentMatcher<GetApplicationRequest>) t -> t.applicationId().equals(APP_ID))
    );
    verify(sdkClient).listTagsForResource(any(ListTagsForResourceRequest.class));

    var tagReqCaptor = ArgumentCaptor.forClass(TagResourceRequest.class);
    verify(sdkClient).tagResource(tagReqCaptor.capture());

    var tagRequest = tagReqCaptor.getValue();
    Map<String, String> tagsAddedInReq = tagRequest.tags().stream()
        .collect(Collectors.toMap(tag -> tag.key(), tag -> tag.value()));

    assertThat(tagsAddedInReq.entrySet()).isEqualTo(Set.of(
        Map.entry("stacksame", "newValue"),
        Map.entry("datTag", "valueb")
    ));
  }*/

 /* @Test
  public void testThatItCallsUnTagButSkipsCallingTag() {
    // set up scenario
    previousModel.setTags(List.of(
        Tag.builder().key("remain").value("thesame").build(),
        Tag.builder().key("toBeRemove").value("theValue").build()
    ));
    updateModel.setTags(List.of(
        Tag.builder().key("remain").value("thesame").build()
    ));
    testRequest.setPreviousResourceTags(Map.of(
        "stacksame", "value"
    ));
    testRequest.setDesiredResourceTags(Map.of(
        "stacksame", "value"
    ));

    // call method under test
    final ProgressEvent<ResourceModel, CallbackContext> resultProgress = underTest.handleRequest(
        proxy, testRequest, new CallbackContext(), proxyClient, logger
    );

    // verify
    assertThat(resultProgress).isNotNull();
    assertThat(resultProgress.isSuccess()).isTrue();
    verify(sdkClient).updateApplication(any(UpdateApplicationRequest.class));
    verify(sdkClient, times(2)).getApplication(
        argThat((ArgumentMatcher<GetApplicationRequest>) t -> t.applicationId().equals(APP_ID))
    );
    verify(sdkClient).listTagsForResource(any(ListTagsForResourceRequest.class));

    var untagReqCaptor = ArgumentCaptor.forClass(UntagResourceRequest.class);
    verify(sdkClient).untagResource(untagReqCaptor.capture());

    verify(sdkClient, times(0)).tagResource(any(TagResourceRequest.class));

    var untagReq = untagReqCaptor.getValue();
    assertThat(untagReq.tagKeys()).isEqualTo(List.of("toBeRemove"));
  }*/

}
