package software.amazon.qbusiness.application;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import software.amazon.awssdk.services.qbusiness.model.AppliedChatConfiguration;
import software.amazon.awssdk.services.qbusiness.model.CreateApplicationRequest;
import software.amazon.awssdk.services.qbusiness.model.DeleteApplicationRequest;
import software.amazon.awssdk.services.qbusiness.model.DescribeApplicationRequest;
import software.amazon.awssdk.services.qbusiness.model.DescribeApplicationResponse;
import software.amazon.awssdk.services.qbusiness.model.ListApplicationsRequest;
import software.amazon.awssdk.services.qbusiness.model.ListApplicationsResponse;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.qbusiness.model.Tag;
import software.amazon.awssdk.services.qbusiness.model.TagResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.UntagResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.UpdateApplicationRequest;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

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
  static CreateApplicationRequest translateToCreateRequest(final String idempotentToken, final ResourceModel model) {
    return CreateApplicationRequest.builder()
        .clientToken(idempotentToken)
        .name(model.getName())
        .description(model.getDescription())
        .roleArn(model.getRoleArn())
        .capacityUnitConfiguration(toServiceChatCapacityConfiguration(model.getCapacityUnitConfiguration()))
        .chatConfiguration(toServiceChatConfiguration(model.getChatConfiguration()))
        .serverSideEncryptionConfiguration(toServiceServerSideEncryptionConfig(model.getServerSideEncryptionConfiguration()))
        .tags(TagHelper.serviceTagsFromCfnTags(model.getTags()))
        .build();
  }

  /**
   * Request to read a resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static DescribeApplicationRequest translateToReadRequest(final ResourceModel model) {
    return DescribeApplicationRequest.builder()
        .applicationId(model.getApplicationId())
        .build();
  }

  static ListTagsForResourceRequest translateToListTagsRequest(final ResourceHandlerRequest<ResourceModel> request, final ResourceModel model) {
    var applicationArn = Utils.buildApplicationArn(request, model);

    return ListTagsForResourceRequest.builder()
        .resourceARN(applicationArn)
        .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   *
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final DescribeApplicationResponse awsResponse) {
    return ResourceModel.builder()
        .name(awsResponse.name())
        .applicationId(awsResponse.applicationId())
        .roleArn(awsResponse.roleArn())
        .status(awsResponse.statusAsString())
        .description(awsResponse.description())
        .createdAt(instantToString(awsResponse.createdAt()))
        .updatedAt(instantToString(awsResponse.updatedAt()))
        .capacityUnitConfiguration(fromServiceChatCapacityConfiguration(awsResponse.capacityUnitConfiguration()))
        .chatConfiguration(fromServiceChatConfiguration(awsResponse.chatConfiguration()))
        .serverSideEncryptionConfiguration(fromServiceServerSideEncryptionConfig(awsResponse.serverSideEncryptionConfiguration()))
        .build();
  }

  static String instantToString(Instant instant) {
    return Optional.ofNullable(instant)
        .map(Instant::toString)
        .orElse(null);
  }

  static ChatConfiguration fromServiceChatConfiguration(AppliedChatConfiguration chatConfiguration) {
    if (chatConfiguration == null) {
      return null;
    }

    software.amazon.awssdk.services.qbusiness.model.ResponseConfiguration serviceResponseConfig = chatConfiguration.responseConfiguration();

    if (serviceResponseConfig == null) {
      return null;
    }

    return ChatConfiguration.builder()
        .responseConfiguration(ResponseConfiguration.builder()
            .blockedPhrases(serviceResponseConfig.blockedPhrases())
            .blockedTopicsPrompt(serviceResponseConfig.blockedTopicsPrompt())
            .defaultMessage(serviceResponseConfig.defaultMessage())
            .nonRetrievalResponseControlStatus(serviceResponseConfig.nonRetrievalResponseControlStatusAsString())
            .retrievalResponseControlStatus(serviceResponseConfig.retrievalResponseControlStatusAsString())
            .build())
        .build();
  }

  static software.amazon.awssdk.services.qbusiness.model.ChatConfiguration toServiceChatConfiguration(
      ChatConfiguration modelChatConfig
  ) {
    if (modelChatConfig == null || modelChatConfig.getResponseConfiguration() == null) {
      return null;
    }

    ResponseConfiguration modelResponseConfig = modelChatConfig.getResponseConfiguration();

    return software.amazon.awssdk.services.qbusiness.model.ChatConfiguration.builder()
        .responseConfiguration(software.amazon.awssdk.services.qbusiness.model.ResponseConfiguration.builder()
            .blockedPhrases(modelResponseConfig.getBlockedPhrases())
            .blockedTopicsPrompt(modelResponseConfig.getBlockedTopicsPrompt())
            .defaultMessage(modelResponseConfig.getDefaultMessage())
            .nonRetrievalResponseControlStatus(modelResponseConfig.getNonRetrievalResponseControlStatus())
            .retrievalResponseControlStatus(modelResponseConfig.getRetrievalResponseControlStatus())
            .build())
        .build();
  }

  static ServerSideEncryptionConfiguration fromServiceServerSideEncryptionConfig(
      software.amazon.awssdk.services.qbusiness.model.ServerSideEncryptionConfiguration serviceConfig
  ) {
    if (serviceConfig == null) {
      return null;
    }

    return ServerSideEncryptionConfiguration.builder()
        .kmsKeyId(serviceConfig.kmsKeyId())
        .build();
  }

  static software.amazon.awssdk.services.qbusiness.model.ServerSideEncryptionConfiguration toServiceServerSideEncryptionConfig(
      ServerSideEncryptionConfiguration modelServerSideConfig
  ) {
    if (modelServerSideConfig == null) {
      return null;
    }

    return software.amazon.awssdk.services.qbusiness.model.ServerSideEncryptionConfiguration.builder()
        .kmsKeyId(modelServerSideConfig.getKmsKeyId())
        .build();
  }

  static ChatCapacityUnitConfiguration fromServiceChatCapacityConfiguration(
      software.amazon.awssdk.services.qbusiness.model.ChatCapacityUnitConfiguration responseChatCapacityUnitsConf
  ) {

    if (responseChatCapacityUnitsConf == null) {
      return null;
    }

    return ChatCapacityUnitConfiguration.builder()
        .users(Double.valueOf(responseChatCapacityUnitsConf.users()))
        .build();
  }

  static software.amazon.awssdk.services.qbusiness.model.ChatCapacityUnitConfiguration toServiceChatCapacityConfiguration(
      ChatCapacityUnitConfiguration modelChatCapacityConfig
  ) {
    if (modelChatCapacityConfig == null) {
      return null;
    }

    if (modelChatCapacityConfig.getUsers() == null) {
      return null;
    }

    return software.amazon.awssdk.services.qbusiness.model.ChatCapacityUnitConfiguration.builder()
        .users(modelChatCapacityConfig.getUsers().intValue())
        .build();
  }

  static ResourceModel translateFromReadResponseWithTags(final ListTagsForResourceResponse listTagsResponse, final ResourceModel model) {
    if (listTagsResponse == null || !listTagsResponse.hasTags()) {
      return model;
    }

    return model.toBuilder()
        .tags(TagHelper.cfnTagsFromServiceTags(listTagsResponse.tags()))
        .build();
  }

  /**
   * Request to delete a resource
   *
   * @param model resource model
   * @return DeleteApplicationRequest - the aws service request to delete a resource.
   */
  static DeleteApplicationRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteApplicationRequest.builder()
        .applicationId(model.getApplicationId())
        .build();
  }

  /**
   * Request to update properties of a previously created resource
   *
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static UpdateApplicationRequest translateToUpdateRequest(final ResourceModel model) {
    return UpdateApplicationRequest.builder()
        .applicationId(model.getApplicationId())
        .name(model.getName())
        .description(model.getDescription())
        .roleArn(model.getRoleArn())
        .chatConfiguration(toServiceChatConfiguration(model.getChatConfiguration()))
        .capacityUnitConfiguration(toServiceChatCapacityConfiguration(model.getCapacityUnitConfiguration()))
        .build();
  }

  /**
   * Request to list resources
   *
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListApplicationsRequest translateToListRequest(final String nextToken) {
    return ListApplicationsRequest.builder()
        .nextToken(nextToken)
        .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   *
   * @param serviceResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final ListApplicationsResponse serviceResponse) {
    return serviceResponse.items()
        .stream()
        .map(summary -> ResourceModel.builder()
            .applicationId(summary.applicationId())
            .build()
        )
        .toList();
  }

  private static <T> Stream<T> streamOfOrEmpty(final Collection<T> collection) {
    return Optional.ofNullable(collection).stream().flatMap(Collection::stream);
  }

  /**
   * Request to add tags to a resource
   *
   * @param model resource model
   * @return TagResourceRequest the aws service request to create a resource
   */
  static TagResourceRequest tagResourceRequest(
      final ResourceHandlerRequest<ResourceModel> request,
      final ResourceModel model,
      final Map<String, String> addedTags) {
    var applicationArn = Utils.buildApplicationArn(request, model);

    List<Tag> toTags = Optional.ofNullable(addedTags)
        .map(Map::entrySet)
        .map(pairs -> pairs.stream()
            .map(pair -> Tag.builder()
                .key(pair.getKey())
                .value(pair.getValue())
                .build()
            )
            .toList()
        )
        .filter(list -> !list.isEmpty())
        .orElse(null);

    return TagResourceRequest.builder()
        .resourceARN(applicationArn)
        .tags(toTags)
        .build();
  }

  /**
   * Request to add tags to a resource
   *
   * @param model resource model
   * @return UntagResourceRequest the aws service request to create a resource
   */
  static UntagResourceRequest untagResourceRequest(
      final ResourceHandlerRequest<ResourceModel> request,
      final ResourceModel model,
      final Set<String> removedTags) {
    var applicationArn = Utils.buildApplicationArn(request, model);
    var tagsToRemove = Optional.ofNullable(removedTags)
        .filter(set -> !set.isEmpty())
        .orElse(null);

    return UntagResourceRequest.builder()
        .resourceARN(applicationArn)
        .tagKeys(tagsToRemove)
        .build();
  }
}
