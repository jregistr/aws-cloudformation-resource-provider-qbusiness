package software.amazon.qbusiness.index;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import software.amazon.awssdk.services.qbusiness.model.Tag;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class TagHelper {
  /**
   * convertToMap
   * Converts a collection of Tag objects to a tag-name -> tag-value map.
   * Note: Tag objects with null tag values will not be included in the output
   * map.
   *
   * @param tags Collection of tags to convert
   * @return Converted Map of tags
   */
  public static Map<String, String> convertToMap(final Collection<software.amazon.qbusiness.index.Tag> tags) {
    if (CollectionUtils.isEmpty(tags)) {
      return Collections.emptyMap();
    }
    return tags.stream()
        .filter(tag -> tag.getValue() != null)
        .collect(Collectors.toMap(
            software.amazon.qbusiness.index.Tag::getKey,
            software.amazon.qbusiness.index.Tag::getValue,
            (oldValue, newValue) -> newValue));
  }

  /**
   * convertToSet
   * Converts a tag map to a set of Tag objects.
   * Note: Like convertToMap, convertToSet filters out value-less tag entries.
   *
   * @param tagMap Map of tags to convert
   * @return Set of Tag objects
   */
  public static Set<Tag> convertToSet(final Map<String, String> tagMap) {
    if (MapUtils.isEmpty(tagMap)) {
      return Collections.emptySet();
    }
    return tagMap.entrySet().stream()
        .filter(tag -> tag.getValue() != null)
        .map(tag -> Tag.builder()
            .key(tag.getKey())
            .value(tag.getValue())
            .build())
        .collect(Collectors.toSet());
  }

  /**
   * Converts model tags to a list of Tag objects.
   *
   * @param modelTags Collection of model tags
   * @return List of Tag objects
   */
  public static List<Tag> serviceTagsFromCfnTags(
      final Collection<software.amazon.qbusiness.index.Tag> modelTags) {
    if (modelTags == null) {
      return null;
    }
    return modelTags.stream()
        .map(tag -> Tag.builder()
            .key(tag.getKey())
            .value(tag.getValue())
            .build()
        ).toList();
  }

  /**
   * shouldUpdateTags
   * Determines whether user defined tags have been changed during update.
   */
  public final boolean shouldUpdateTags(final ResourceHandlerRequest<ResourceModel> handlerRequest) {
    final Map<String, String> previousTags = getPreviouslyAttachedTags(handlerRequest);
    final Map<String, String> desiredTags = getNewDesiredTags(handlerRequest);
    return ObjectUtils.notEqual(previousTags, desiredTags);
  }

  /**
   * getPreviouslyAttachedTags
   * If stack tags and resource tags are not merged together in Configuration class,
   * we will get previously attached system (with `aws:cloudformation` prefix) and user defined tags from
   * handlerRequest.getPreviousSystemTags() (system tags),
   * handlerRequest.getPreviousResourceTags() (stack tags),
   * handlerRequest.getPreviousResourceState().getTags() (resource tags).
   * System tags are an optional feature. Merge them to your tags if you have enabled them for your resource.
   * System tags can change on resource update if the resource is imported to the stack.
   */
  public Map<String, String> getPreviouslyAttachedTags(final ResourceHandlerRequest<ResourceModel> handlerRequest) {
    final Map<String, String> previousTags = new HashMap<>();

    if (handlerRequest.getPreviousSystemTags() != null) {
      previousTags.putAll(handlerRequest.getPreviousSystemTags());
    }

    // get previous stack level tags from handlerRequest
    if (handlerRequest.getPreviousResourceTags() != null) {
      previousTags.putAll(handlerRequest.getPreviousResourceTags());
    }

    if (handlerRequest.getPreviousResourceState() != null && handlerRequest.getPreviousResourceState().getTags() != null) {
      previousTags.putAll(convertToMap(handlerRequest.getPreviousResourceState().getTags()));
    }
    return previousTags;
  }

  /**
   * getNewDesiredTags
   * If stack tags and resource tags are not merged together in Configuration class,
   * we will get new desired system (with `aws:cloudformation` prefix) and user defined tags from
   * handlerRequest.getSystemTags() (system tags),
   * handlerRequest.getDesiredResourceTags() (stack tags),
   * handlerRequest.getDesiredResourceState().getTags() (resource tags).
   * System tags are an optional feature. Merge them to your tags if you have enabled them for your resource.
   * System tags can change on resource update if the resource is imported to the stack.
   */
  public Map<String, String> getNewDesiredTags(final ResourceHandlerRequest<ResourceModel> handlerRequest) {
    final Map<String, String> desiredTags = new HashMap<>();

    if (handlerRequest.getSystemTags() != null) {
      desiredTags.putAll(handlerRequest.getSystemTags());
    }

    // get desired stack level tags from handlerRequest
    if (handlerRequest.getDesiredResourceTags() != null) {
      desiredTags.putAll(handlerRequest.getDesiredResourceTags());
    }

    desiredTags.putAll(convertToMap(handlerRequest.getDesiredResourceState().getTags())); // if tags are not null
    return desiredTags;
  }

  /**
   * generateTagsToAdd
   * Determines the tags the customer desired to define or redefine.
   */
  public Map<String, String> generateTagsToAdd(final Map<String, String> previousTags, final Map<String, String> desiredTags) {
    return desiredTags.entrySet().stream()
        .filter(e -> !previousTags.containsKey(e.getKey()) || !Objects.equals(previousTags.get(e.getKey()), e.getValue()))
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue));
  }

  /**
   * getTagsToRemove
   * Determines the tags the customer desired to remove from the function.
   */
  public Set<String> generateTagsToRemove(final Map<String, String> previousTags, final Map<String, String> desiredTags) {
    final Set<String> desiredTagNames = desiredTags.keySet();

    return previousTags.keySet().stream()
        .filter(tagName -> !desiredTagNames.contains(tagName))
        .collect(Collectors.toSet());
  }

  /**
   * Get model tags from service tags.
   *
   * @param serviceTags List of service tags
   * @return List of model tags
   */
  public static List<software.amazon.qbusiness.index.Tag> modelTagsFromServiceTags(
      List<Tag> serviceTags
  ) {
    return serviceTags.stream()
        .map(serviceTag -> new software.amazon.qbusiness.index.Tag(serviceTag.key(), serviceTag.value())
        )
        .toList();
  }

}
