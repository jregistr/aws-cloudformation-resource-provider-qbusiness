package software.amazon.qbusiness.plugin;

public class AuthConfigHelper {

  public static PluginAuthConfiguration convertFromServiceAuthConfig(
          software.amazon.awssdk.services.qbusiness.model.PluginAuthConfiguration serviceAuthConfig
  ) {
    if (serviceAuthConfig.oAuth2ClientCredentialConfiguration() != null) {
       return software.amazon.qbusiness.plugin.PluginAuthConfiguration.builder()
               .oAuth2ClientCredentialConfiguration(
                       convertFromServiceOAuth(serviceAuthConfig.oAuth2ClientCredentialConfiguration()))
            .build();
    }

    return software.amazon.qbusiness.plugin.PluginAuthConfiguration.builder()
            .basicAuthConfiguration(
                    convertFromServiceBasicAuth(serviceAuthConfig.basicAuthConfiguration()))
            .build();
  }

  public static software.amazon.awssdk.services.qbusiness.model.PluginAuthConfiguration convertToServiceAuthConfig(
          PluginAuthConfiguration cfnAuthConfig
  ) {
    if (cfnAuthConfig.getOAuth2ClientCredentialConfiguration() != null) {
      return software.amazon.awssdk.services.qbusiness.model.PluginAuthConfiguration.builder()
              .oAuth2ClientCredentialConfiguration(convertToServiceOath(cfnAuthConfig.getOAuth2ClientCredentialConfiguration()))
              .build();
    }

    return software.amazon.awssdk.services.qbusiness.model.PluginAuthConfiguration.builder()
            .basicAuthConfiguration(convertToServiceBasicAuth(cfnAuthConfig.getBasicAuthConfiguration()))
            .build();
  }

  private static BasicAuthConfiguration convertFromServiceBasicAuth(
          software.amazon.awssdk.services.qbusiness.model.BasicAuthConfiguration serviceBasicAuth
  ) {
    return BasicAuthConfiguration.builder()
            .roleArn(serviceBasicAuth.roleArn())
            .secretArn(serviceBasicAuth.secretArn())
            .build();
  }

  private static OAuth2ClientCredentialConfiguration convertFromServiceOAuth(
          software.amazon.awssdk.services.qbusiness.model.OAuth2ClientCredentialConfiguration serviceOath
  ) {
    return OAuth2ClientCredentialConfiguration.builder()
            .roleArn(serviceOath.roleArn())
            .secretArn(serviceOath.secretArn())
            .build();
  }

  private static software.amazon.awssdk.services.qbusiness.model.BasicAuthConfiguration convertToServiceBasicAuth(
          BasicAuthConfiguration cfnBasicAuth
  ) {
    return software.amazon.awssdk.services.qbusiness.model.BasicAuthConfiguration.builder()
            .roleArn(cfnBasicAuth.getRoleArn())
            .secretArn(cfnBasicAuth.getSecretArn())
            .build();
  }

  private static software.amazon.awssdk.services.qbusiness.model.OAuth2ClientCredentialConfiguration convertToServiceOath(
          OAuth2ClientCredentialConfiguration cfnOath
  ) {
    return software.amazon.awssdk.services.qbusiness.model.OAuth2ClientCredentialConfiguration.builder()
            .roleArn(cfnOath.getRoleArn())
            .secretArn(cfnOath.getSecretArn())
            .build();
  }

}
