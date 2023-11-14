package software.amazon.qbusiness.webexperience;

import java.util.Locale;

public final class Constants {
  public static final String SERVICE_NAME = "QBusiness";
  public static final String API_GET_WEB_EXPERIENCE = "GetWebExperience";
  public static final String API_CREATE_WEB_EXPERIENCE = "CreateWebExperience";
  public static final String API_UPDATE_WEB_EXPERIENCE = "UpdateWebExperience";
  public static final String API_DELETE_WEB_EXPERIENCE = "DeleteWebExperience";
  public static final String SERVICE_NAME_LOWER = SERVICE_NAME.toLowerCase(Locale.ENGLISH);
  public static final String ENV_AWS_REGION = "AWS_REGION";

  private Constants() {
  }
}
