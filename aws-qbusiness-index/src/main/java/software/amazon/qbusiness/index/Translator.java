package software.amazon.qbusiness.index;

import com.google.common.collect.Lists;
import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.services.qbusiness.model.CreateIndexRequest;
import software.amazon.awssdk.services.qbusiness.model.GetIndexRequest;
import software.amazon.awssdk.services.qbusiness.model.GetIndexResponse;
import software.amazon.awssdk.services.qbusiness.model.ListIndicesRequest;
import software.amazon.awssdk.services.qbusiness.model.ListIndicesResponse;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceResponse;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 * - api request construction
 * - object translation to/from aws sdk
 * - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateIndexRequest translateToCreateRequest(final String idempotentToken, final ResourceModel model) {
    return CreateIndexRequest.builder()
        .clientToken(idempotentToken)
        .name(model.getName())
        .applicationId(model.getApplicationId())
        .description(model.getDescription())
        .capacityUnitConfiguration(toServiceCapacityUnitConfiguration(model.getCapacityUnitConfiguration()))
        .tags(TagHelper.serviceTagsFromCfnTags(model.getTags()))
        .build();
  }

  /**
   * Request to read a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static GetIndexRequest translateToReadRequest(final ResourceModel model) {
    return GetIndexRequest.builder()
        .applicationId(model.getApplicationId())
        .indexId(model.getIndexId())
        .build();
  }

  /**
   * Request to list tags for a resource
   *
   * @param request resource handler request for the resource model
   * @param model   resource model
   * @return awsRequest the aws service request to list tags for a resource
   */
  static ListTagsForResourceRequest translateToListTagsRequest(final ResourceHandlerRequest<ResourceModel> request, final ResourceModel model) {
    var indexArn = Utils.buildIndexArn(request, model);

    return ListTagsForResourceRequest.builder()
        .resourceARN(indexArn)
        .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   *
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final GetIndexResponse awsResponse) {
    return ResourceModel.builder()
        .name(awsResponse.name())
        .applicationId(awsResponse.applicationId())
        .indexId(awsResponse.indexId())
        .indexStatistics(fromServiceIndexStatistics(awsResponse.indexStatistics()))
        .status(awsResponse.statusAsString())
        .description(awsResponse.description())
        .documentAttributeConfigurations(fromServiceDocumentAttributeConfigurations(awsResponse.documentAttributeConfigurations()))
        .createdAt(awsResponse.createdAt().toString())
        .updatedAt(awsResponse.updatedAt().toString())
        .capacityUnitConfiguration(fromServiceCapacityUnitConfiguration(awsResponse.capacityUnitConfiguration()))
        .build();
  }

  /**
   * Request to add tags to resource model
   *
   * @param listTagsResponse response from list tags
   * @param model            resource model
   * @return model with tags added if they exist
   */
  static ResourceModel translateFromReadResponseWithTags(final ListTagsForResourceResponse listTagsResponse, final ResourceModel model) {
    if (listTagsResponse == null || !listTagsResponse.hasTags()) {
      return model;
    }

    return model.toBuilder()
        .tags(TagHelper.modelTagsFromServiceTags(listTagsResponse.tags()))
        .build();
  }

  /**
   * Request to delete a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static AwsRequest translateToDeleteRequest(final ResourceModel model) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L33-L37
    return awsRequest;
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
   * @param nextToken     token passed to the aws service list resources request
   * @param model         resource model
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListIndicesRequest translateToListRequest(final String nextToken, final ResourceModel model) {
    return ListIndicesRequest.builder()
        .nextToken(nextToken)
        .applicationId(model.getApplicationId())
        .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   *
   * @param serviceResponse the aws service get resource response
   * @param applicationId of the list of indices
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final ListIndicesResponse serviceResponse, final String applicationId) {
    return serviceResponse.items()
        .stream()
        .map(summary -> ResourceModel.builder()
            .indexId(summary.indexId())
            .applicationId(applicationId)
            .createdAt(summary.createdAt().toString())
            .updatedAt(summary.updatedAt().toString())
            .name(summary.name())
            .status(summary.statusAsString())
            .build())
        .toList();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   *
   * @param awsResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final AwsResponse awsResponse) {
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L75-L82
    return streamOfOrEmpty(Lists.newArrayList())
        .map(resource -> ResourceModel.builder()
            // include only primary identifier
            .build())
        .collect(Collectors.toList());
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
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

  private static StorageCapacityUnitConfiguration fromServiceCapacityUnitConfiguration(
      final software.amazon.awssdk.services.qbusiness.model.StorageCapacityUnitConfiguration storageCapacityUnitConfiguration) {

    if (storageCapacityUnitConfiguration == null) {
      return null;
    }

    return StorageCapacityUnitConfiguration.builder()
        .units(Double.valueOf(storageCapacityUnitConfiguration.units()))
        .build();
  }

  private static List<DocumentAttributeConfiguration> fromServiceDocumentAttributeConfigurations(
      final List<software.amazon.awssdk.services.qbusiness.model.DocumentAttributeConfiguration> documentAttributeConfigurations) {

    if (documentAttributeConfigurations == null) {
      return null;
    }

    return documentAttributeConfigurations.stream()
        .map(documentAttributeConfiguration -> DocumentAttributeConfiguration.builder()
            .name(documentAttributeConfiguration.name())
            .search(documentAttributeConfiguration.searchAsString())
            .type(documentAttributeConfiguration.typeAsString())
            .build())
        .collect(Collectors.toList());
  }

  private static IndexStatistics fromServiceIndexStatistics(
      final software.amazon.awssdk.services.qbusiness.model.IndexStatistics sdkIndexStatistics) {

    if (sdkIndexStatistics == null) {
      return null;
    }

    return IndexStatistics.builder()
        .textDocumentStatistics(fromServiceTextDocumentStatistics(sdkIndexStatistics.textDocumentStatistics()))
        .build();
  }

  private static TextDocumentStatistics fromServiceTextDocumentStatistics(
      final software.amazon.awssdk.services.qbusiness.model.TextDocumentStatistics sdkTextDocumentStatistics) {

    if (sdkTextDocumentStatistics == null) {
      return null;
    }

    return TextDocumentStatistics.builder()
        .indexedTextBytes(Double.valueOf(sdkTextDocumentStatistics.indexedTextBytes()))
        .indexedTextDocumentCount(Double.valueOf(sdkTextDocumentStatistics.indexedTextDocumentCount()))
        .build();
  }

  private static software.amazon.awssdk.services.qbusiness.model.StorageCapacityUnitConfiguration toServiceCapacityUnitConfiguration(
      final StorageCapacityUnitConfiguration storageCapacityUnitConfiguration) {
    if (storageCapacityUnitConfiguration == null || storageCapacityUnitConfiguration.getUnits() == null) {
      return null;
    }

    return software.amazon.awssdk.services.qbusiness.model.StorageCapacityUnitConfiguration.builder()
        .units(storageCapacityUnitConfiguration.getUnits().intValue())
        .build();
  }
}
