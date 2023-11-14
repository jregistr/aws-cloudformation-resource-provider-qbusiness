package software.amazon.qbusiness.datasource.translators;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import software.amazon.qbusiness.datasource.DocumentAttributeValue;

class DocumentEnrichmentTranslatorTest {

  @Test
  void toServiceTargetDocumentAttributeValue() {
    var result1 = DocumentEnrichmentTranslator.toServiceTargetDocumentAttributeValue(
        DocumentAttributeValue.builder()
            .longValue(30D)
            .build()
    );

    var result2 = DocumentEnrichmentTranslator.toServiceTargetDocumentAttributeValue(
        DocumentAttributeValue.builder()
            .dateValue(Instant.ofEpochMilli(1697824935000L).toString())
            .build()
    );

    assertThat(result1.longValue()).isEqualTo(30);
    assertThat(result1.dateValue()).isNull();
    assertThat(result1.stringValue()).isNull();
    assertThat(result1.stringListValue()).isEmpty();

    assertThat(result2.dateValue()).isEqualTo("2023-10-20T18:02:15Z");
  }
}
