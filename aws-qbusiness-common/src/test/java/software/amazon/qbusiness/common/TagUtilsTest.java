package software.amazon.qbusiness.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import software.amazon.awssdk.services.qbusiness.model.Tag;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

class TagUtilsTest {

  private ResourceHandlerRequest<Object> testRequest;
  private Map<String, String> resourceModelTags;

  private AutoCloseable testAutoCloseable;

  @BeforeEach
  void setUp() {
    testAutoCloseable = MockitoAnnotations.openMocks(this);
    resourceModelTags = Map.of(
        "tagA", "ValueValue"
    );

    testRequest = ResourceHandlerRequest.builder()
        .desiredResourceTags(Map.of(
            "stackTagA", "valueA"
        ))
        .systemTags(Map.of(
            "aws:cloudformation:stack-id", "superstack"
        ))
        .build();
  }

  @AfterEach
  void tearDown() throws Exception {
    testAutoCloseable.close();
  }

}
