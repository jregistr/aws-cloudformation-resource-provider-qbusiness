package software.amazon.qbusiness.datasource.translators;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import software.amazon.qbusiness.datasource.CustomDocumentEnrichmentConfiguration;
import software.amazon.qbusiness.datasource.DocumentAttributeCondition;
import software.amazon.qbusiness.datasource.DocumentAttributeTarget;
import software.amazon.qbusiness.datasource.DocumentAttributeValue;
import software.amazon.qbusiness.datasource.HookConfiguration;
import software.amazon.qbusiness.datasource.InlineCustomDocumentEnrichmentConfiguration;
import software.amazon.qbusiness.datasource.Translator;

public final class DocumentEnrichmentTranslator {

  private DocumentEnrichmentTranslator() {
  }

  public static CustomDocumentEnrichmentConfiguration fromServiceCustomEnrichmentConf(
      software.amazon.awssdk.services.qbusiness.model.CustomDocumentEnrichmentConfiguration maybeServiceConf
  ) {
    return Optional.ofNullable(maybeServiceConf)
        .map(serviceConf -> CustomDocumentEnrichmentConfiguration.builder()
            .roleArn(maybeServiceConf.roleArn())
            .inlineConfigurations(fromServiceInlineEnrichmentConfs(maybeServiceConf.inlineConfigurations()))
            .preExtractionHookConfiguration(fromServiceHookConfiguration(serviceConf.preExtractionHookConfiguration()))
            .postExtractionHookConfiguration(fromServiceHookConfiguration(serviceConf.postExtractionHookConfiguration()))
            .build())
        .orElse(null);
  }

  public static software.amazon.awssdk.services.qbusiness.model.CustomDocumentEnrichmentConfiguration toServiceCustomDocumentEnrichmentConf(
      CustomDocumentEnrichmentConfiguration modelData
  ) {
    if (modelData == null) {
      return null;
    }

    return software.amazon.awssdk.services.qbusiness.model.CustomDocumentEnrichmentConfiguration.builder()
        .roleArn(modelData.getRoleArn())
        .inlineConfigurations(toServiceInlineEnrichmentConfs(modelData.getInlineConfigurations()))
        .preExtractionHookConfiguration(toServiceHookConfiguration(modelData.getPreExtractionHookConfiguration()))
        .postExtractionHookConfiguration(toServiceHookConfiguration(modelData.getPostExtractionHookConfiguration()))
        .build();
  }

  static HookConfiguration fromServiceHookConfiguration(
      software.amazon.awssdk.services.qbusiness.model.HookConfiguration serviceHookConfiguration
  ) {
    if (serviceHookConfiguration == null) {
      return null;
    }

    return HookConfiguration.builder()
        .lambdaArn(serviceHookConfiguration.lambdaArn())
        .s3Bucket(serviceHookConfiguration.s3Bucket())
        .invocationCondition(fromServiceDocumentAttributeCondition(serviceHookConfiguration.invocationCondition()))
        .build();
  }

  static software.amazon.awssdk.services.qbusiness.model.HookConfiguration toServiceHookConfiguration(
      HookConfiguration modelData
  ) {
    if (modelData == null) {
      return null;
    }

    return software.amazon.awssdk.services.qbusiness.model.HookConfiguration.builder()
        .lambdaArn(modelData.getLambdaArn())
        .s3Bucket(modelData.getS3Bucket())
        .invocationCondition(toServiceDocumentAttributeCondition(modelData.getInvocationCondition()))
        .build();
  }

  static List<InlineCustomDocumentEnrichmentConfiguration> fromServiceInlineEnrichmentConfs(
      List<software.amazon.awssdk.services.qbusiness.model.InlineCustomDocumentEnrichmentConfiguration> serviceInlineConfs
  ) {
    if (serviceInlineConfs == null) {
      return null;
    }

    return serviceInlineConfs.stream()
        .map(inlineConf -> InlineCustomDocumentEnrichmentConfiguration.builder()
            .documentContentDeletion(inlineConf.documentContentDeletion())
            .target(fromServiceDocAttributeTarget(inlineConf.target()))
            .condition(fromServiceDocumentAttributeCondition(inlineConf.condition()))
            .build())
        .toList();
  }

  static List<software.amazon.awssdk.services.qbusiness.model.InlineCustomDocumentEnrichmentConfiguration> toServiceInlineEnrichmentConfs(
      List<InlineCustomDocumentEnrichmentConfiguration> modelData
  ) {
    if (modelData == null) {
      return null;
    }

    return modelData.stream()
        .map(modelInlineConf -> software.amazon.awssdk.services.qbusiness.model.InlineCustomDocumentEnrichmentConfiguration.builder()
            .documentContentDeletion(modelInlineConf.getDocumentContentDeletion())
            .target(toServiceDocAttributeTarget(modelInlineConf.getTarget()))
            .condition(toServiceDocumentAttributeCondition(modelInlineConf.getCondition()))
            .build()
        )
        .toList();
  }

  static DocumentAttributeCondition fromServiceDocumentAttributeCondition(
      software.amazon.awssdk.services.qbusiness.model.DocumentAttributeCondition serviceDocumentAttributeCondition
  ) {
    if (serviceDocumentAttributeCondition == null) {
      return null;
    }

    return DocumentAttributeCondition.builder()
        .operator(serviceDocumentAttributeCondition.operatorAsString())
        .conditionOnValue(fromServiceTargetDocumentAttributeValue(serviceDocumentAttributeCondition.conditionOnValue()))
        .conditionDocumentAttributeKey(serviceDocumentAttributeCondition.conditionDocumentAttributeKey())
        .build();
  }

  static software.amazon.awssdk.services.qbusiness.model.DocumentAttributeCondition toServiceDocumentAttributeCondition(
      DocumentAttributeCondition modelData
  ) {
    if (modelData == null) {
      return null;
    }

    return software.amazon.awssdk.services.qbusiness.model.DocumentAttributeCondition.builder()
        .operator(modelData.getOperator())
        .conditionOnValue(toServiceTargetDocumentAttributeValue(modelData.getConditionOnValue()))
        .conditionDocumentAttributeKey(modelData.getConditionDocumentAttributeKey())
        .build();
  }

  static DocumentAttributeTarget fromServiceDocAttributeTarget(
      software.amazon.awssdk.services.qbusiness.model.DocumentAttributeTarget serviceData
  ) {
    if (serviceData == null) {
      return null;
    }

    return DocumentAttributeTarget.builder()
        .targetDocumentAttributeKey(serviceData.targetDocumentAttributeKey())
        .targetDocumentAttributeValueDeletion(serviceData.targetDocumentAttributeValueDeletion())
        .targetDocumentAttributeValue(fromServiceTargetDocumentAttributeValue(serviceData.targetDocumentAttributeValue()))
        .build();
  }

  static software.amazon.awssdk.services.qbusiness.model.DocumentAttributeTarget toServiceDocAttributeTarget(
      DocumentAttributeTarget modelData
  ) {
    if (modelData == null) {
      return null;
    }

    return software.amazon.awssdk.services.qbusiness.model.DocumentAttributeTarget.builder()
        .targetDocumentAttributeKey(modelData.getTargetDocumentAttributeKey())
        .targetDocumentAttributeValueDeletion(modelData.getTargetDocumentAttributeValueDeletion())
        .targetDocumentAttributeValue(toServiceTargetDocumentAttributeValue(modelData.getTargetDocumentAttributeValue()))
        .build();
  }

  static DocumentAttributeValue fromServiceTargetDocumentAttributeValue(
      software.amazon.awssdk.services.qbusiness.model.DocumentAttributeValue serviceData
  ) {
    if (serviceData == null) {
      return null;
    }

    Double longAsDouble = Optional.ofNullable(serviceData.longValue()).map(Long::doubleValue).orElse(null);

    return DocumentAttributeValue.builder()
        .dateValue(Translator.instantToString(serviceData.dateValue()))
        .longValue(longAsDouble)
        .stringValue(serviceData.stringValue())
        .stringListValue(serviceData.stringListValue())
        .build();
  }

  static software.amazon.awssdk.services.qbusiness.model.DocumentAttributeValue toServiceTargetDocumentAttributeValue(
      DocumentAttributeValue modelData
  ) {
    if (modelData == null) {
      return null;
    }

    Long longValue = Optional.ofNullable(modelData.getLongValue()).map(Double::longValue).orElse(null);
    Instant dateValue = Optional.ofNullable(modelData.getDateValue()).map(Instant::parse).orElse(null);

    return software.amazon.awssdk.services.qbusiness.model.DocumentAttributeValue.builder()
        .dateValue(dateValue)
        .longValue(longValue)
        .stringValue(modelData.getStringValue())
        .stringListValue(modelData.getStringListValue())
        .build();
  }
}
