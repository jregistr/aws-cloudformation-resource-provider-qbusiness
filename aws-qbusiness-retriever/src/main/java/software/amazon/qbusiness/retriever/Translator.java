package software.amazon.qbusiness.retriever;

import software.amazon.awssdk.services.qbusiness.model.AddRetrieverRequest;
import software.amazon.awssdk.services.qbusiness.model.DeleteRetrieverRequest;
import software.amazon.awssdk.services.qbusiness.model.GetRetrieverRequest;
import software.amazon.awssdk.services.qbusiness.model.GetRetrieverResponse;
import software.amazon.awssdk.services.qbusiness.model.ListRetrieversRequest;
import software.amazon.awssdk.services.qbusiness.model.ListRetrieversResponse;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.ListTagsForResourceResponse;
import software.amazon.awssdk.services.qbusiness.model.Tag;
import software.amazon.awssdk.services.qbusiness.model.TagResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.UntagResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.UpdateRetrieverRequest;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

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
  static AddRetrieverRequest translateToCreateRequest(final String idempotentToken, final ResourceModel model) {
    return AddRetrieverRequest.builder()
        .applicationId(model.getApplicationId())
        .retrieverType(model.getRetrieverType())
        .retrieverName(model.getRetrieverName())
        .retrieverConfiguration(toServiceRetrieverConfiguration(model.getRetrieverConfiguration()))
        .roleArn(model.getRoleArn())
        .clientToken(idempotentToken)
        .tags(TagHelper.serviceTagsFromCfnTags(model.getTags()))
        .build();
  }

  /**
   * Request to read a resource
   * @param model resource model
   * @return awsRequest the aws service request to describe a resource
   */
  static GetRetrieverRequest translateToReadRequest(final ResourceModel model) {
    return GetRetrieverRequest.builder()
        .applicationId(model.getApplicationId())
        .retrieverId(model.getRetrieverId())
        .build();
  }

  /**
   * Translates resource object from sdk into a resource model
   * @param awsResponse the aws service describe resource response
   * @return model resource model
   */
  static ResourceModel translateFromReadResponse(final GetRetrieverResponse awsResponse) {
    return ResourceModel.builder()
        .applicationId(awsResponse.applicationId())
        .retrieverId(awsResponse.retrieverId())
        .retrieverType(awsResponse.retrieverTypeAsString())
        .retrieverState(awsResponse.retrieverStateAsString())
        .retrieverName(awsResponse.retrieverName())
        .retrieverConfiguration(fromServiceRetrieverConfiguration(awsResponse.retrieverConfiguration()))
        .roleArn(awsResponse.roleArn())
        .createdAt(instantToString(awsResponse.createdAt()))
        .updatedAt(instantToString(awsResponse.updatedAt()))
        .build();
  }

  /**
   * Request to delete a resource
   * @param model resource model
   * @return awsRequest the aws service request to delete a resource
   */
  static DeleteRetrieverRequest translateToDeleteRequest(final ResourceModel model) {
    return DeleteRetrieverRequest.builder()
        .applicationId(model.getApplicationId())
        .retrieverId(model.getRetrieverId())
        .build();
  }

  /**
   * Request to update properties of a previously created resource
   * @param model resource model
   * @return awsRequest the aws service request to modify a resource
   */
  static UpdateRetrieverRequest translateToUpdateRequest(final ResourceModel model) {
    return UpdateRetrieverRequest.builder()
        .applicationId(model.getApplicationId())
        .retrieverId(model.getRetrieverId())
        .retrieverConfiguration(toServiceRetrieverConfiguration(model.getRetrieverConfiguration()))
        .retrieverName(model.getRetrieverName())
        .roleArn(model.getRoleArn())
        .build();
  }

  /**
   * Request to list resources
   * @param nextToken token passed to the aws service list resources request
   * @return awsRequest the aws service request to list resources within aws account
   */
  static ListRetrieversRequest translateToListRequest(final String nextToken) {
    return ListRetrieversRequest.builder()
        .nextToken(nextToken)
        .build();
  }

  /**
   * Translates resource objects from sdk into a resource model (primary identifier only)
   * @param serviceResponse the aws service describe resource response
   * @return list of resource models
   */
  static List<ResourceModel> translateFromListRequest(final ListRetrieversResponse serviceResponse) {
    return serviceResponse.summaryItems()
        .stream()
        .map(summary -> ResourceModel.builder()
            .applicationId(summary.applicationId())
            .retrieverId(summary.retrieverId())
            .build()
        )
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
  static TagResourceRequest tagResourceRequest(
      final ResourceHandlerRequest<ResourceModel> request,
      final ResourceModel model,
      final Map<String, String> addedTags) {
    var retrieverArn = Utils.buildRetrieverArn(request, model);

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
        .resourceARN(retrieverArn)
        .tags(toTags)
        .build();
  }

  /**
   * Request to add tags to a resource
   * @param model resource model
   * @return awsRequest the aws service request to create a resource
   */
  static UntagResourceRequest untagResourceRequest(
      final ResourceHandlerRequest<ResourceModel> request,
      final ResourceModel model,
      final Set<String> removedTags) {
    var retrieverArn = Utils.buildRetrieverArn(request, model);
    var tagsToRemove = Optional.ofNullable(removedTags)
        .filter(set -> !set.isEmpty())
        .orElse(null);

    return UntagResourceRequest.builder()
        .resourceARN(retrieverArn)
        .tagKeys(tagsToRemove)
        .build();
  }

  static ListTagsForResourceRequest translateToListTagsRequest(final ResourceHandlerRequest<ResourceModel> request, final ResourceModel model) {
    var retrieverArn = Utils.buildRetrieverArn(request, model);

    return ListTagsForResourceRequest.builder()
        .resourceARN(retrieverArn)
        .build();
  }

  static ResourceModel translateFromReadResponseWithTags(final ListTagsForResourceResponse listTagsResponse, final ResourceModel model) {
    if (listTagsResponse == null || !listTagsResponse.hasTags())  {
      return model;
    }

    return model.toBuilder()
        .tags(TagHelper.cfnTagsFromServiceTags(listTagsResponse.tags()))
        .build();
  }

  static software.amazon.awssdk.services.qbusiness.model.RetrieverConfiguration toServiceRetrieverConfiguration(
      RetrieverConfiguration modelRetrieverConfiguration
  ) {
    if (modelRetrieverConfiguration == null) {
      return null;
    }

    return software.amazon.awssdk.services.qbusiness.model.RetrieverConfiguration.builder()
        .kendraIndexConfiguration(toServiceKendraIndexConfiguration(modelRetrieverConfiguration.getKendraIndexConfiguration()))
        .build();
  }

  static software.amazon.awssdk.services.qbusiness.model.KendraIndexConfiguration toServiceKendraIndexConfiguration(
      KendraIndexConfiguration modelKendraIndexConfiguration
  ) {
    if (modelKendraIndexConfiguration == null) {
      return null;
    }

    return software.amazon.awssdk.services.qbusiness.model.KendraIndexConfiguration.builder()
        .indexId(modelKendraIndexConfiguration.getIndexId())
        .documentRelevanceOverrideConfigurations(toServiceDocumentRelevanceOverrideConfigurations(modelKendraIndexConfiguration.getDocumentRelevanceOverrideConfigurations()))
        .build();
  }

  static List<software.amazon.awssdk.services.qbusiness.model.DocumentRelevanceOverrideConfiguration> toServiceDocumentRelevanceOverrideConfigurations(
      List<DocumentRelevanceOverrideConfiguration> modelDocumentRelevanceOverrideConfigurations
  ) {
    if (modelDocumentRelevanceOverrideConfigurations == null) {
      return null;
    }

    return modelDocumentRelevanceOverrideConfigurations.stream().map(config ->
      software.amazon.awssdk.services.qbusiness.model.DocumentRelevanceOverrideConfiguration.builder()
          .name(config.getName())
          .relevance(toServiceRelevance(config.getRelevance()))
          .build()
    ).collect(toList());
  }

  static software.amazon.awssdk.services.qbusiness.model.Relevance toServiceRelevance(
      Relevance modelRelevance
  ) {
    if (modelRelevance == null) {
      return null;
    }

    return software.amazon.awssdk.services.qbusiness.model.Relevance.builder()
        .freshness(modelRelevance.getFreshness())
        .duration(modelRelevance.getDuration())
        .importance(modelRelevance.getImportance().intValue())
        .rankOrder(modelRelevance.getRankOrder())
        .valueImportanceMap(toServiceValueImportanceMap(modelRelevance.getValueImportanceMap()))
        .build();
  }

  static Map<String, Integer> toServiceValueImportanceMap(
      Map<String, Double> modelValueImportanceMap
  ) {
    if (modelValueImportanceMap == null) {
      return null;
    }
    Map<String, Integer> map = new HashMap<>();

    for (Map.Entry<String, Double> entry : modelValueImportanceMap.entrySet()) {
      map.put(entry.getKey(), entry.getValue().intValue());
    }

    return map;
  }

  static RetrieverConfiguration fromServiceRetrieverConfiguration(
      software.amazon.awssdk.services.qbusiness.model.RetrieverConfiguration serviceRetrieverConfiguration
  ) {
    if (serviceRetrieverConfiguration == null) {
      return null;
    }

    return RetrieverConfiguration.builder()
        .kendraIndexConfiguration(fromServiceKendraIndexConfiguration(serviceRetrieverConfiguration.kendraIndexConfiguration()))
        .build();
  }

  static KendraIndexConfiguration fromServiceKendraIndexConfiguration(
      software.amazon.awssdk.services.qbusiness.model.KendraIndexConfiguration serviceKendraIndexConfiguration
  ) {
    if (serviceKendraIndexConfiguration == null) {
      return null;
    }

    return KendraIndexConfiguration.builder()
        .indexId(serviceKendraIndexConfiguration.indexId())
        .documentRelevanceOverrideConfigurations(fromServiceDocumentRelevanceOverrideConfigurations(serviceKendraIndexConfiguration.documentRelevanceOverrideConfigurations()))
        .build();
  }

  static List<DocumentRelevanceOverrideConfiguration> fromServiceDocumentRelevanceOverrideConfigurations(
      List<software.amazon.awssdk.services.qbusiness.model.DocumentRelevanceOverrideConfiguration> serviceDocumentRelevanceOverrideConfigurations
  ) {
    if (serviceDocumentRelevanceOverrideConfigurations == null) {
      return null;
    }

    return serviceDocumentRelevanceOverrideConfigurations.stream().map(config ->
        DocumentRelevanceOverrideConfiguration.builder()
            .name(config.name())
            .relevance(fromServiceRelevance(config.relevance()))
            .build()
    ).collect(toList());
  }

  static Relevance fromServiceRelevance(
      software.amazon.awssdk.services.qbusiness.model.Relevance serviceRelevance
  ) {
    if (serviceRelevance == null) {
      return null;
    }

    return Relevance.builder()
        .freshness(serviceRelevance.freshness())
        .duration(serviceRelevance.duration())
        .importance(serviceRelevance.importance().doubleValue())
        .rankOrder(serviceRelevance.rankOrderAsString())
        .valueImportanceMap(fromServiceValueImportanceMap(serviceRelevance.valueImportanceMap()))
        .build();
  }

  static Map<String, Double> fromServiceValueImportanceMap(
      Map<String, Integer> serviceValueImportanceMap
  ) {
    if (serviceValueImportanceMap == null) {
      return null;
    }
    Map<String, Double> map = new HashMap<>();

    for (Map.Entry<String, Integer> entry : serviceValueImportanceMap.entrySet()) {
      map.put(entry.getKey(), entry.getValue().doubleValue());
    }

    return map;
  }

  static String instantToString(Instant instant) {
    return Optional.ofNullable(instant)
        .map(Instant::toString)
        .orElse(null);
  }
}
