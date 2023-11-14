package software.amazon.qbusiness.webexperience;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.cloudformation.LambdaWrapper;

import java.net.URI;

import static software.amazon.qbusiness.webexperience.Constants.ENV_AWS_REGION;
import static software.amazon.qbusiness.webexperience.Constants.SERVICE_NAME_LOWER;

public class ClientBuilder {

  // https://{service}.{region}.api.aws
  private static final String URL_PATTERN = "https://%s.%s.api.aws";

  public static QBusinessClient getClient() {
    String region = System.getenv(ENV_AWS_REGION);
    var urlString = URL_PATTERN.formatted(SERVICE_NAME_LOWER, region);

    return QBusinessClient.builder()
        .httpClient(LambdaWrapper.HTTP_CLIENT)
        .endpointOverride(URI.create(urlString))
        .build();
  }
}
