package software.amazon.qbusiness.retriever;

import java.util.Locale;

public final class Constants {
  public static final String SERVICE_NAME = "QBusiness";
  public static final String API_GET_RETRIEVER = "GetRetriever";
  public static final String API_CREATE_RETRIEVER = "CreateRetriever";
  public static final String API_DELETE_RETRIEVER = "DeleteRetriever";
  public static final String SERVICE_NAME_LOWER = SERVICE_NAME.toLowerCase(Locale.ENGLISH);
  private Constants() {}
}
