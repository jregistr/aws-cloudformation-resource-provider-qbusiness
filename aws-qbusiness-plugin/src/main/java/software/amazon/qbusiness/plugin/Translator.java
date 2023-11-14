package software.amazon.qbusiness.plugin;

import software.amazon.awssdk.awscore.AwsRequest;
import software.amazon.awssdk.services.qbusiness.model.CreateWebExperienceRequest;
import software.amazon.awssdk.services.qbusiness.model.DeleteWebExperienceRequest;
import software.amazon.awssdk.services.qbusiness.model.GetWebExperienceRequest;
import software.amazon.awssdk.services.qbusiness.model.GetWebExperienceResponse;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.qbusiness.model.ListWebExperiencesRequest;
import software.amazon.awssdk.services.qbusiness.model.ListWebExperiencesResponse;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is a centralized placeholder for
 *  - api request construction
 *  - object translation to/from aws sdk
 *  - resource model construction for read/list handlers
 */

public class Translator {

  /**
   * Request to create a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static CreateWebExperienceRequest translateToCreateRequest(final String idempotentToken, final ResourceModel model) {
    return CreateWebExperienceRequest.builder()
        .clientToken(idempotentToken)
        .applicationId(model.getApplicationId())
        .title(model.getTitle())
        .subtitle(model.getSubtitle())
        .authenticationConfiguration(toServiceAuthenticationConfiguration(model.getAuthenticationConfiguration()))
        .tags(TagHelper.serviceTagsFromCfnTags(model.getTags()))
        .build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static GetWebExperienceRequest translateToReadRequest(final ResourceModel model) {
    return GetWebExperienceRequest.builder()
        .applicationId(model.getApplicationId())
        .webExperienceId(model.getWebExperienceId())
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
    var webExperienceArn = Utils.buildWebExperienceArn(request, model);

    return ListTagsForResourceRequest.builder()
        .resourceARN(webExperienceArn)
        .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final GetWebExperienceResponse awsResponse) {
    return ResourceModel.builder()
        .applicationId(awsResponse.applicationId())
        .webExperienceId(awsResponse.webExperienceId())
        .status(awsResponse.statusAsString())
        .title(awsResponse.title())
        .subtitle(awsResponse.subtitle())
        .authenticationConfiguration(fromServiceAuthenticationConfiguration(awsResponse.authenticationConfiguration()))
        .endpoints(fromServiceEndpoints(awsResponse.endpoints()))
        .createdAt(instantToString(awsResponse.createdAt()))
        .updatedAt(instantToString(awsResponse.updatedAt()))
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
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteWebExperienceRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteWebExperienceRequest.builder()
        .applicationId(model.getApplicationId())
        .webExperienceId(model.getWebExperienceId())
        .build();
  }

  /**
   * Request to update properties of a previously created resource
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
   * @param model     resource model
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListWebExperiencesRequest translateToListRequest(final String nextToken, final ResourceModel model) {
    return ListWebExperiencesRequest.builder()
        .nextToken(nextToken)
        .applicationId(model.getApplicationId())
        .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   *
   * @param serviceResponse the aws service get resource response
   * @param applicationId   of the list of indices
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final ListWebExperiencesResponse serviceResponse, final String applicationId) {
    return serviceResponse.summaryItems()
        .stream()
        .map(summary -> ResourceModel.builder()
            .applicationId(applicationId)
            .webExperienceId(summary.id())
            .status(summary.statusAsString())
            .createdAt(instantToString(summary.createdAt()))
            .updatedAt(instantToString(summary.updatedAt()))
            .endpoints(fromServiceEndpoints(summary.endpoints()))
            .build())
        .toList();
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection)
        .map(Collection::stream)
        .orElseGet(Stream::empty);
  }

  /**
   * Request to add tags to a resource
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
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static AwsRequest untagResourceRequest(final ResourceModel model, final Set<String> removedTags) {
    final AwsRequest awsRequest = null;
    // TODO: construct a request
    // e.g. https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-logs/blob/2077c92299aeb9a68ae8f4418b5e932b12a8b186/aws-logs-loggroup/src/main/java/com/aws/logs/loggroup/Translator.java#L39-L43
    return awsRequest;
  }

  private static WebExperienceAuthConfiguration fromServiceAuthenticationConfiguration(
      final software.amazon.awssdk.services.qbusiness.model.WebExperienceAuthConfiguration webExperienceAuthConfiguration) {
    if (Objects.isNull(webExperienceAuthConfiguration)) {
      return null;
    }

    return WebExperienceAuthConfiguration.builder()
        .samlConfigurationOptions(fromServiceWebExperienceAuthConfiguration(webExperienceAuthConfiguration.samlConfigurationOptions()))
        .build();
  }

  private static SamlConfigurationOptions fromServiceWebExperienceAuthConfiguration(
      final software.amazon.awssdk.services.qbusiness.model.SamlConfigurationOptions samlConfigurationOptions) {
    return SamlConfigurationOptions.builder()
        .metadataXML(samlConfigurationOptions.metadataXML())
        .roleArn(samlConfigurationOptions.roleArn())
        .userAttribute(samlConfigurationOptions.userAttribute())
        .userGroupAttribute(samlConfigurationOptions.userGroupAttribute())
        .build();
  }

  private static List<WebExperienceEndpointConfig> fromServiceEndpoints(
      final List<software.amazon.awssdk.services.qbusiness.model.WebExperienceEndpointConfig> endpoints) {

    return endpoints.stream()
        .map(endpoint -> WebExperienceEndpointConfig.builder()
            .endpoint(endpoint.endpoint())
            .type(endpoint.typeAsString())
            .build())
        .collect(Collectors.toList());
  }

  private static software.amazon.awssdk.services.qbusiness.model.WebExperienceAuthConfiguration
  toServiceAuthenticationConfiguration(final WebExperienceAuthConfiguration authenticationConfiguration) {
    if (authenticationConfiguration == null || authenticationConfiguration.getSamlConfigurationOptions() == null) {
      return null;
    }

    return software.amazon.awssdk.services.qbusiness.model.WebExperienceAuthConfiguration.builder()
        .samlConfigurationOptions(toServiceSamlConfigurationOptions(authenticationConfiguration.getSamlConfigurationOptions()))
        .build();
  }

  private static software.amazon.awssdk.services.qbusiness.model.SamlConfigurationOptions
  toServiceSamlConfigurationOptions(final SamlConfigurationOptions samlConfigurationOptions) {
    if (samlConfigurationOptions == null) {
      return null;
    }

    return software.amazon.awssdk.services.qbusiness.model.SamlConfigurationOptions.builder()
        .metadataXML(samlConfigurationOptions.getMetadataXML())
        .roleArn(samlConfigurationOptions.getRoleArn())
        .userAttribute(samlConfigurationOptions.getUserAttribute())
        .userGroupAttribute(samlConfigurationOptions.getUserGroupAttribute())
        .build();
  }

  private static String instantToString(Instant instant) {
    return Optional.ofNullable(instant)
        .map(Instant::toString)
        .orElse(null);
  }
}
