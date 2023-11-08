package software.amazon.qbusiness.index;

import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.cloudformation.LambdaWrapper;

public class ClientBuilder {
  public static QBusinessClient getClient() {
    return QBusinessClient.builder()
        .httpClient(LambdaWrapper.HTTP_CLIENT)
        .build();
  }

}
