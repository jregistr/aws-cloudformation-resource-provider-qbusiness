package software.amazon.qbusiness.webexperience;

import static software.amazon.qbusiness.datasource.translators.DataSourceConfigurationTranslator.fromServiceDataSourceConfiguration;
import static software.amazon.qbusiness.datasource.translators.DataSourceConfigurationTranslator.toServiceDataSourceConfiguration;
import static software.amazon.qbusiness.datasource.translators.DocumentEnrichmentTranslator.fromServiceCustomEnrichmentConf;
import static software.amazon.qbusiness.datasource.translators.DocumentEnrichmentTranslator.toServiceCustomDocumentEnrichmentConf;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.qbusiness.model.CreateDataSourceRequest;
import software.amazon.awssdk.services.qbusiness.model.DeleteDataSourceRequest;
import software.amazon.awssdk.services.qbusiness.model.GetDataSourceRequest;
import software.amazon.awssdk.services.qbusiness.model.GetDataSourceResponse;
import software.amazon.awssdk.services.qbusiness.model.ListDataSourcesRequest;
import software.amazon.awssdk.services.qbusiness.model.ListDataSourcesResponse;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

public class Translator {

  /**
   * Request to create a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateDataSourceRequest translateToCreateRequest(final String idempotencyToken, final ResourceModel model) {
    return CreateDataSourceRequest.builder()
        .clientToken(idempotencyToken)
        .applicationId(model.getApplicationId())
        .indexId(model.getIndexId())
        .name(model.getName())
        .description(model.getDescription())
        .roleArn(model.getRoleArn())
        .schedule(model.getSchedule())
        .tags(TagHelper.serviceTagsFromCfnTags(model.getTags()))
        .vpcConfiguration(toServiceDataSourceVpcConfiguration(model.getVpcConfiguration()))
        .configuration(toServiceDataSourceConfiguration(model.getConfiguration()))
        .customDocumentEnrichmentConfiguration(toServiceCustomDocumentEnrichmentConf(model.getCustomDocumentEnrichmentConfiguration()))
        .build();
  }

  /**
   * Request to read a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static GetDataSourceRequest translateToReadRequest(final ResourceModel model) {
    return GetDataSourceRequest.builder()
        .applicationId(model.getApplicationId())
        .indexId(model.getIndexId())
        .dataSourceId(model.getDataSourceId())
        .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   *
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final GetDataSourceResponse awsResponse) {
    return ResourceModel.builder()
        .applicationId(awsResponse.applicationId())
        .indexId(awsResponse.indexId())
        .dataSourceId(awsResponse.dataSourceId())
        .name(awsResponse.name())
        .description(awsResponse.description())
        .createdAt(instantToString(awsResponse.createdAt()))
        .updatedAt(instantToString(awsResponse.updatedAt()))
        .roleArn(awsResponse.roleArn())
        .schedule(awsResponse.schedule())
        .type(awsResponse.typeAsString())
        .status(awsResponse.statusAsString())
        .vpcConfiguration(fromServiceDataSourceVpcConfiguration(awsResponse.vpcConfiguration()))
        .configuration(fromServiceDataSourceConfiguration(awsResponse.configuration()))
        .customDocumentEnrichmentConfiguration(fromServiceCustomEnrichmentConf(awsResponse.customDocumentEnrichmentConfiguration()))
        .build();
  }

  static DataSourceVpcConfiguration fromServiceDataSourceVpcConfiguration(
      software.amazon.awssdk.services.qbusiness.model.DataSourceVpcConfiguration maybeServiceConf
  ) {
    return Optional.ofNullable(maybeServiceConf)
        .map(serviceConf -> DataSourceVpcConfiguration.builder()
            .subnetIds(serviceConf.subnetIds())
            .securityGroupIds(serviceConf.securityGroupIds())
            .build())
        .orElse(null);
  }

  static software.amazon.awssdk.services.qbusiness.model.DataSourceVpcConfiguration toServiceDataSourceVpcConfiguration(
      DataSourceVpcConfiguration modelData
  ) {
    if (modelData == null) {
      return null;
    }

    return software.amazon.awssdk.services.qbusiness.model.DataSourceVpcConfiguration.builder()
        .subnetIds(modelData.getSubnetIds())
        .securityGroupIds(modelData.getSecurityGroupIds())
        .build();
  }

  public static String instantToString(Instant instant) {
    return Optional.ofNullable(instant)
        .map(Instant::toString)
        .orElse(null);
  }

  static ResourceModel translateFromReadResponseWithTags(final ListTagsForResourceResponse listTagsResponse, final ResourceModel model) {
    if (listTagsResponse == null || !listTagsResponse.hasTags()) {
      return model;
    }

    return model.toBuilder()
        .tags(TagHelper.cfnTagsFromServiceTags(listTagsResponse.tags()))
        .build();
  }

  static ListTagsForResourceRequest translateToListTagsRequest(final ResourceHandlerRequest<ResourceModel> request, final ResourceModel model) {
    var dataSourceArn = Utils.buildDataSourceArn(request, model);
    return ListTagsForResourceRequest.builder()
        .resourceARN(dataSourceArn)
        .build();
  }

  /**
   * Request to delete a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteDataSourceRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteDataSourceRequest.builder()
        .applicationId(model.getApplicationId())
        .indexId(model.getIndexId())
        .dataSourceId(model.getDataSourceId())
        .build();
  }

  /**
   * Request to update properties of a previously created resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static AwsRequest translateToFirstUpdateRequest(final ResourceModel model) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L45-L50
    return awsRequest;
  }

  /**
   * Request to update some other properties that could not be provisioned through first update request
   *
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static AwsRequest translateToSecondUpdateRequest(final ResourceModel model) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    return awsRequest;
  }

  /**
   * Request to list resources
   *
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListDataSourcesRequest translateToListRequest(
      ResourceModel resourceModel,
      final String nextToken
  ) {
    return ListDataSourcesRequest.builder()
        .applicationId(resourceModel.getApplicationId())
        .indexId(resourceModel.getIndexId())
        .nextToken(nextToken)
        .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   *
   * @param applicationId   - The id of the application to list data sources.
   * @param indexId         - The parent index id.
   * @param serviceResponse - the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(
      final String applicationId,
      final String indexId,
      final ListDataSourcesResponse serviceResponse) {
    return streamOfOrEmpty(serviceResponse.summaryItems())
        .map(resource -> ResourceModel.builder()
            .applicationId(applicationId)
            .indexId(indexId)
            .dataSourceId(resource.dataSourceId())
            .build()
        )
        .toList();
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection).stream()
        .flatMap(Collection::stream);
  }

  /**
   * Request to add tags to a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static AwsRequest tagResourceRequest(final ResourceModel model, final Map<String, String> addedTags) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L39-L43
    return awsRequest;
  }

  /**
   * Request to add tags to a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static AwsRequest untagResourceRequest(final ResourceModel model, final Set<String> removedTags) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L39-L43
    return awsRequest;
  }
}
