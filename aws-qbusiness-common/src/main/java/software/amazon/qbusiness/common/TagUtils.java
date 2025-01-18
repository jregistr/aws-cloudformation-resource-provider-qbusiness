package software.amazon.qbusiness.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.Tag;
import software.amazon.awssdk.services.qbusiness.model.TagResourceRequest;
import software.amazon.awssdk.services.qbusiness.model.UntagResourceRequest;
import software.amazon.cloudformation.exceptions.CfnGeneralServiceException;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.StdCallbackContext;

/**
 * Class containing common handler tag operations.
 */
public final class TagUtils {
  private static final String PROP_NAME_TAGS = "tags";
  private static final String PROP_NAME_KEY = "key";
  private static final String PROP_NAME_VALUE = "tags";

  private TagUtils() {
  }

  public static <T> List<Tag> mergeCreateHandlerTagsToSdkTags(
      final ResourceHandlerRequest<T> handlerRequest,
      final T model
  ) {
    Map<String, String> modelTags = getModelJsonTags(model);

    return mergeCreateHandlerTagsToSdkTags(modelTags, handlerRequest);
  }

  public static <RType, CtxType extends StdCallbackContext> ProgressEvent<RType, CtxType> makeUpdateTagsEvent(
      ProgressEvent<RType, CtxType> progressEvent,
      final ResourceHandlerRequest<RType> handlerRequest,
      BiFunction<RType, ResourceHandlerRequest<RType>, String> modelToArn,
//      Function<RType, String> modelToArn,
      AmazonWebServicesClientProxy proxy,
      ProxyClient<QBusinessClient> proxyClient,
      Logger logger
  ) {
    Map<String, String> previousTags = getPreviouslyAttachedTags(handlerRequest);
    Map<String, String> desiredTags = getNewDesiredTags(handlerRequest);
    if (!shouldUpdateTags(previousTags, desiredTags)) {
      return progressEvent;
    }

    Map<String, String> tagsToAdd = generateTagsToAdd(previousTags, desiredTags);
    if (tagsToAdd == null || tagsToAdd.isEmpty()) {
      return progressEvent;
    }

    return proxy.initiate("AWS-QBusiness-Application::TagResource", proxyClient, progressEvent.getResourceModel(), progressEvent.getCallbackContext())
        .translateToServiceRequest(modelT -> {
          String arn = modelToArn.apply(modelT, handlerRequest);
          List<Tag> toTags = tagsToAdd.entrySet()
              .stream()
              .map(entry -> Tag.builder()
                  .key(entry.getKey())
                  .value(entry.getValue())
                  .build())
              .toList();

          System.out.println("We tag");

          return TagResourceRequest.builder()
              .resourceARN(arn)
              .tags(toTags)
              .build();
        })
        .makeServiceCall((tagResourceRequest, clientProxyClient) -> {
          var client = proxyClient.client();
          System.out.println("Over here call tag");
          return proxyClient.injectCredentialsAndInvokeV2(tagResourceRequest, client::tagResource);
        })
        .progress()
        .then(nextProgress -> {
          Set<String> tagsToRemove = generateTagsToRemove(previousTags, desiredTags);

          if (CollectionUtils.isEmpty(tagsToRemove)) {
            return nextProgress;
          }

          System.out.println("Over here");
          return proxy.initiate("AWS-QBusiness-Application::UnTagResource", proxyClient, nextProgress.getResourceModel(),
                  nextProgress.getCallbackContext())
              .translateToServiceRequest(model -> {
                String arn = modelToArn.apply(model, handlerRequest);
                return UntagResourceRequest.builder()
                    .resourceARN(arn)
                    .tagKeys(tagsToRemove)
                    .build();
              })
              .makeServiceCall((untagResourceRequest, clientProxyClient) -> {
                var client = proxyClient.client();
                System.out.println("Over here call untag");
                return proxyClient.injectCredentialsAndInvokeV2(untagResourceRequest, client::untagResource);
              })
              .progress();
        });
  }

  public static <T> List<Tag> mergeCreateHandlerTagsToSdkTags(
      final Map<String, String> modelTags,
      final ResourceHandlerRequest<T> handlerRequest
  ) {
    return Stream.of(handlerRequest.getDesiredResourceTags(), modelTags, handlerRequest.getSystemTags())
        .filter(Objects::nonNull)
        .flatMap(map -> map.entrySet().stream())
        .map(entry -> Tag.builder()
            .key(entry.getKey())
            .value(entry.getValue())
            .build()
        )
        .collect(Collectors.toList());
  }

  public static boolean shouldUpdateTags(
      final Map<String, String> allPreviousTags,
      final Map<String, String> allDesiredTags
  ) {
    return ObjectUtils.notEqual(allPreviousTags, allDesiredTags);
  }

  public static <T> Map<String, String> getPreviouslyAttachedTags(
      final Map<String, String> previousModelTags,
      final ResourceHandlerRequest<T> handlerRequest
  ) {
    return mergedTags(
        previousModelTags,
        handlerRequest.getPreviousSystemTags(),
        handlerRequest.getPreviousResourceTags()
    );
  }

  public static <T> Map<String, String> getPreviouslyAttachedTags(
      final ResourceHandlerRequest<T> handlerRequest
  ) {
    var previousState = handlerRequest.getPreviousResourceState();
    Map<String, String> modelTags = getModelJsonTags(previousState);
    var systemTags = handlerRequest.getPreviousSystemTags();
    var desiredResourceTags = handlerRequest.getPreviousResourceTags();
    return mergedTags(modelTags, systemTags, desiredResourceTags);
  }

  private static <T> Map<String, String> getNewDesiredTags(
      final ResourceHandlerRequest<T> handlerRequest
  ) {
    var model = handlerRequest.getDesiredResourceState();
    Map<String, String> modelTags = getModelJsonTags(model);
    var systemTags = handlerRequest.getSystemTags();
    var desiredResourceTags = handlerRequest.getDesiredResourceTags();
    return mergedTags(modelTags, systemTags, desiredResourceTags);
  }

  public static <T> Map<String, String> getNewDesiredTags(
      final Map<String, String> modelTags,
      final ResourceHandlerRequest<T> handlerRequest
  ) {
    return mergedTags(
        modelTags,
        handlerRequest.getSystemTags(),
        handlerRequest.getDesiredResourceTags()
    );
  }

  public static Map<String, String> generateTagsToAdd(
      final Map<String, String> previousTags,
      final Map<String, String> desiredTags
  ) {
    return desiredTags.entrySet().stream()
        .filter(e -> !previousTags.containsKey(e.getKey()) || !Objects.equals(previousTags.get(e.getKey()), e.getValue()))
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue));
  }

  public static Set<String> generateTagsToRemove(
      final Map<String, String> previousTags,
      final Map<String, String> desiredTags
  ) {
    final Set<String> desiredTagNames = desiredTags.keySet();

    return previousTags.keySet().stream()
        .filter(tagName -> !desiredTagNames.contains(tagName))
        .collect(Collectors.toSet());
  }

  private static Map<String, String> getModelJsonTags(Object model) {
    var mapper = new ObjectMapper();
    JsonNode modelAsJson = mapper.valueToTree(model);
    JsonNode tags = modelAsJson.get(PROP_NAME_TAGS);

    if (tags == null) {
      return null;
    }

    if (!tags.isArray()) {
      throw new CfnGeneralServiceException("Error processing tags as a list");
    }

    return StreamSupport.stream(tags.spliterator(), false)
        .collect(Collectors.toMap(
            tag -> tag.get(PROP_NAME_KEY).asText(),
            tag -> tag.get(PROP_NAME_VALUE).asText()
        ));
  }

  private static Map<String, String> mergedTags(
      Map<String, String> modelTags,
      Map<String, String> systemTags,
      Map<String, String> resourceTags
  ) {
    var combined = new HashMap<String, String>();
    Stream.of(
            Optional.ofNullable(modelTags),
            Optional.ofNullable(systemTags),
            Optional.ofNullable(resourceTags)
        ).flatMap(Optional::stream)
        .forEach(combined::putAll);
    return combined;
  }

}
