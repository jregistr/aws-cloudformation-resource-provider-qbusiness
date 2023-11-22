package software.amazon.qbusiness.plugin;

import java.util.Locale;

public final class Constants {

  public static final String SERVICE_NAME = "QBusiness";
  public static final String SERVICE_NAME_LOWER = SERVICE_NAME.toLowerCase(Locale.ENGLISH);
  public static final String API_CREATE_PLUGIN = "CreatePlugin";
  public static final String API_GET_PLUGIN = "GetPlugin";
  public static final String API_UPDATE_PLUGIN = "UpdatePlugin";
  public static final String API_DELETE_PLUGIN = "DeletePlugin";

  public static final String ENV_AWS_REGION = "AWS_REGION";

  private Constants() {
  }
}
