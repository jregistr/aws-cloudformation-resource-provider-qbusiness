package software.amazon.qbusiness.datasource.translators;

import software.amazon.qbusiness.datasource.DataSourceConfiguration;
import software.amazon.qbusiness.datasource.TemplateConfiguration;

public final class DataSourceConfigurationTranslator {

  private DataSourceConfigurationTranslator() {
  }

  public static DataSourceConfiguration fromServiceDataSourceConfiguration(
      software.amazon.awssdk.services.qbusiness.model.DataSourceConfiguration serviceConf
  ) {
    if (serviceConf == null || serviceConf.templateConfiguration() == null) {
      return null;
    }

    return DataSourceConfiguration.builder()
        .templateConfiguration(TemplateConfiguration.builder()
            .template(DocumentConverter.convertDocumentToMap(serviceConf.templateConfiguration().template()))
            .build())
        .build();
  }

  public static software.amazon.awssdk.services.qbusiness.model.DataSourceConfiguration toServiceDataSourceConfiguration(
      DataSourceConfiguration modelData
  ) {
    if (modelData == null || modelData.getTemplateConfiguration() == null) {
      return null;
    }

    var templateConfiguration = modelData.getTemplateConfiguration();

    return software.amazon.awssdk.services.qbusiness.model.DataSourceConfiguration.builder()
        .templateConfiguration(software.amazon.awssdk.services.qbusiness.model.TemplateConfiguration.builder()
            .template(DocumentConverter.convertToMapToDocument(templateConfiguration.getTemplate()))
            .build())
        .build();
  }
}
