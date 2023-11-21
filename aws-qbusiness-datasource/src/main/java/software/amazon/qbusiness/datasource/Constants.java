package software.amazon.qbusiness.datasource;

import java.util.Locale;

public final class Constants {
  public static final String SERVICE_NAME = "QBusiness";
  public static final String API_GET_DATASOURCE = "GetDataSource";
  public static final String API_LIST_TAGS = "ListTagsForResource";
  public static final String API_CREATE_DATASOURCE = "CreateDataSource";
  public static final String API_DELETE_DATASOURCE = "DeleteDataSource";
  public static final String API_UPDATE_DATASOURCE = "UpdateDataSource";
  public static final String SERVICE_NAME_LOWER = SERVICE_NAME.toLowerCase(Locale.ENGLISH);
  public static final String ENV_AWS_REGION = "AWS_REGION";

  private Constants() {
  }
}
