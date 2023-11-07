package software.amazon.qbusiness.application;

import java.util.Locale;

public final class Constants {
  public static final String SERVICE_NAME = "QBusiness";
  public static final String API_GET_APPLICATION = "GetApplication";
  public static final String API_CREATE_APPLICATION = "CreateApplication";
  public static final String API_DELETE_APPLICATION = "DeleteApplication";
  public static final String SERVICE_NAME_LOWER = SERVICE_NAME.toLowerCase(Locale.ENGLISH);
  private Constants() {}
}
