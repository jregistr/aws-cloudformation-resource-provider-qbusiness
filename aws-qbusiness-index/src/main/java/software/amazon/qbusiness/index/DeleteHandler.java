package software.amazon.qbusiness.datasource;

import static software.amazon.qbusiness.datasource.Constants.API_DELETE_DATASOURCE;

import java.time.Duration;
import java.util.List;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.qbusiness.QBusinessClient;
import software.amazon.awssdk.services.qbusiness.model.DataSourceStatus;
import software.amazon.awssdk.services.qbusiness.model.DataSourceSyncJobStatus;
import software.amazon.awssdk.services.qbusiness.model.DeleteDataSourceRequest;
import software.amazon.awssdk.services.qbusiness.model.DeleteDataSourceResponse;
import software.amazon.awssdk.services.qbusiness.model.GetDataSourceRequest;
import software.amazon.awssdk.services.qbusiness.model.GetDataSourceResponse;
import software.amazon.awssdk.services.qbusiness.model.ListDataSourceSyncJobsRequest;
import software.amazon.awssdk.services.qbusiness.model.ListDataSourceSyncJobsResponse;
import software.amazon.awssdk.services.qbusiness.model.ResourceNotFoundException;
import software.amazon.awssdk.services.qbusiness.model.StopDataSourceSyncJobRequest;
import software.amazon.awssdk.services.qbusiness.model.StopDataSourceSyncJobResponse;
import software.amazon.cloudformation.proxy.AmazonWebServicesClientProxy;
import software.amazon.cloudformation.proxy.Logger;
import software.amazon.cloudformation.proxy.ProgressEvent;
import software.amazon.cloudformation.proxy.ProxyClient;
import software.amazon.cloudformation.proxy.ResourceHandlerRequest;
import software.amazon.cloudformation.proxy.delay.Constant;

@RequiredArgsConstructor
public class DeleteHandler extends BaseHandlerStd {

  private static final List<DataSourceSyncJobStatus> SYNC_JOB_IN_PROGRESS_STATUSES = List.of(
      DataSourceSyncJobStatus.SYNCING,
      DataSourceSyncJobStatus.STOPPING
  );

  private static final Constant DEFAULT_DELETION_BACK_OFF_STRATEGY = Constant.of()
      .timeout(Duration.ofHours(4))
      // Deleting a datasource usually takes about 20 minutes. Let's check every 5 minutes
      .delay(Duration.ofMinutes(5))
      .build();

  private static final Constant DEFAULT_SYNCING_WAIT_BACKOFF_STRATEGY = Constant.of()
      .timeout(Duration.ofHours(24))
      .delay(Duration.ofMinutes(1))
      .build();

  private final Constant deletionBackOffStrategy;
  private final Constant syncingWaitBackOffStrategy;

  private Logger logger;

  public DeleteHandler() {
    this(DEFAULT_DELETION_BACK_OFF_STRATEGY, DEFAULT_SYNCING_WAIT_BACKOFF_STRATEGY);
  }

  protected ProgressEvent<ResourceModel, CallbackContext> handleRequest(
      final AmazonWebServicesClientProxy proxy,
      final ResourceHandlerRequest<ResourceModel> request,
      final CallbackContext callbackContext,
      final ProxyClient<QBusinessClient> proxyClient,
      final Logger logger) {

    this.logger = logger;

    var desiredResourceState = request.getDesiredResourceState();
    logger.log("[INFO] Initiating delete of %s in Stack: %s for ID: %s, application: %s, index: %s".formatted(
        ResourceModel.TYPE_NAME,
        request.getStackId(),
        desiredResourceState.getDataSourceId(),
        desiredResourceState.getApplicationId(),
        desiredResourceState.getIndexId()
    ));

    return ProgressEvent.progress(request.getDesiredResourceState(), callbackContext)
        .then(progress ->
            proxy.initiate("AWS-QBusiness-DataSource::GetDataSourceStatus", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToReadRequest)
                .makeServiceCall((getDataSourceRequest, clientProxyClient) -> callGetDataSourceStatus(
                    getDataSourceRequest, clientProxyClient, progress.getCallbackContext()
                ))
                .handleError((deleteReq, error, clientProxyClient, model, context) -> handleError(
                    deleteReq, model, error, context, logger, API_DELETE_DATASOURCE
                ))
                .progress()
        )
        .then(progress -> {
          if (progress.getCallbackContext().isFailedDataSource()) {
            return progress;
          }

          return proxy.initiate("AWS-QBusiness-DataSource::ListSyncJobs", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
              .translateToServiceRequest(Translator::translateToListSyncJobsRequest)
              .makeServiceCall((listSyncJobsReq, clientProxyClient) -> listSyncingJobsAndUpdateContext(listSyncJobsReq, clientProxyClient, progress.getCallbackContext()))
              .handleError((deleteReq, error, clientProxyClient, model, context) -> handleError(
                  deleteReq, model, error, context, logger, API_DELETE_DATASOURCE
              ))
              .progress();
        })
        .then(progress -> {
          if (progress.getCallbackContext().isFailedDataSource() || !progress.getCallbackContext().isCurrentlySyncing()) {
            // data source failed, or it is not currently syncing, nothing to do here.
            return progress;
          }

          return proxy.initiate("AWS-QBusiness-DataSource::StopSyncing", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
              .translateToServiceRequest(Translator::translateToStopSyncJobsRequest)
              .makeServiceCall(this::callStopSyncing)
              .handleError((deleteReq, error, clientProxyClient, model, context) -> handleError(
                  deleteReq, model, error, context, logger, API_DELETE_DATASOURCE
              ))
              .progress();
        })
        .then(progress -> {
          if (progress.getCallbackContext().isFailedDataSource()) {
            return progress;
          }

          if (!(progress.getCallbackContext().isCurrentlyStoppingSync() || progress.getCallbackContext().isCurrentlySyncing())) {
            // not currently stopping or syncing, nothing to do here
            return progress;
          }

          return proxy.initiate("AWS-QBusiness-DataSource::EnsureSyncingStopped", proxyClient, progress.getResourceModel(),
                  progress.getCallbackContext())
              .translateToServiceRequest(model -> ListDataSourceSyncJobsRequest.builder().build())
              .backoffDelay(syncingWaitBackOffStrategy)
              .makeServiceCall((model, clientProxyClient) -> ListDataSourceSyncJobsResponse.builder().build())
              .stabilize((noOpReq, noOpRes, clientProxyClient, model, context) -> isDoneSyncing(clientProxyClient, model))
              .handleError((deleteReq, error, clientProxyClient, model, context) -> handleError(
                  deleteReq, model, error, context, logger, API_DELETE_DATASOURCE
              ))
              .progress();
        })
        .then(progress ->
            proxy.initiate("AWS-QBusiness-DataSource::Delete", proxyClient, progress.getResourceModel(), progress.getCallbackContext())
                .translateToServiceRequest(Translator::translateToDeleteRequest)
                .backoffDelay(deletionBackOffStrategy)
                .makeServiceCall(this::callDeleteDataSource)
                .stabilize((deleteReq, deleteRes, client, model, context) -> isDoneDeleting(client, model))
                .handleError((deleteReq, error, clientProxyClient, model, context) -> handleError(
                    deleteReq, model, error, context, logger, API_DELETE_DATASOURCE
                ))
                .done(deleteResponse -> ProgressEvent.defaultSuccessHandler(null))
        );
  }

  private boolean isDoneDeleting(
      ProxyClient<QBusinessClient> proxyClient,
      ResourceModel model
  ) {
    try {
      getDataSource(model, proxyClient);
      logger.log("[INFO] Delete of %s still stabilizing for Resource id: %s, application: %s, index: %s"
          .formatted(ResourceModel.TYPE_NAME, model.getDataSourceId(), model.getApplicationId(), model.getIndexId()));
      return false;
    } catch (ResourceNotFoundException e) {
      logger.log("[INFO] Delete process of %s has stabilized for Resource id: %s, application: %s, index: %s"
          .formatted(ResourceModel.TYPE_NAME, model.getDataSourceId(), model.getApplicationId(), model.getIndexId()));
      return true;
    }
  }

  private boolean isDoneSyncing(
      ProxyClient<QBusinessClient> proxyClient,
      ResourceModel model
  ) {
    ListDataSourceSyncJobsRequest listSyncJobsReq = Translator.translateToListSyncJobsRequest(model);
    ListDataSourceSyncJobsResponse syncJobsResponse = proxyClient.injectCredentialsAndInvokeV2(
        listSyncJobsReq,
        proxyClient.client()::listDataSourceSyncJobs
    );

    if (!syncJobsResponse.hasHistory()) {
      return true;
    }

    return syncJobsResponse.history().stream().noneMatch(dataSourceSyncJob -> SYNC_JOB_IN_PROGRESS_STATUSES.contains(dataSourceSyncJob.status()));
  }

  private ListDataSourceSyncJobsResponse listSyncingJobsAndUpdateContext(
      ListDataSourceSyncJobsRequest listDataSourceSyncJobsRequest,
      ProxyClient<QBusinessClient> proxyClient,
      CallbackContext context
  ) {
    ListDataSourceSyncJobsResponse syncJobsResponse = proxyClient.injectCredentialsAndInvokeV2(
        listDataSourceSyncJobsRequest,
        proxyClient.client()::listDataSourceSyncJobs
    );

    if (!syncJobsResponse.hasHistory()) {
      return syncJobsResponse;
    }

    context.setCurrentlyStoppingSync(
        syncJobsResponse.history().stream().anyMatch(dataSourceSyncJob -> DataSourceSyncJobStatus.STOPPING.equals(dataSourceSyncJob.status()))
    );

    context.setCurrentlySyncing(
        syncJobsResponse.history().stream().anyMatch(dataSourceSyncJob -> DataSourceSyncJobStatus.SYNCING.equals(dataSourceSyncJob.status()))
    );

    return syncJobsResponse;
  }

  private StopDataSourceSyncJobResponse callStopSyncing(
      StopDataSourceSyncJobRequest stopSyncRequest,
      ProxyClient<QBusinessClient> proxyClient
  ) {
    return proxyClient.injectCredentialsAndInvokeV2(stopSyncRequest, proxyClient.client()::stopDataSourceSyncJob);
  }

  private DeleteDataSourceResponse callDeleteDataSource(
      final DeleteDataSourceRequest request,
      final ProxyClient<QBusinessClient> proxyClient
  ) {
    return proxyClient.injectCredentialsAndInvokeV2(request, proxyClient.client()::deleteDataSource);
  }

  private GetDataSourceResponse callGetDataSourceStatus(
      GetDataSourceRequest request,
      ProxyClient<QBusinessClient> proxyClient,
      CallbackContext callbackContext
  ) {
    var client = proxyClient.client();
    GetDataSourceResponse getDataSourceResponse = proxyClient.injectCredentialsAndInvokeV2(request, client::getDataSource);

    callbackContext.setFailedDataSource(DataSourceStatus.FAILED.equals(getDataSourceResponse.status()));

    return getDataSourceResponse;
  }
}
