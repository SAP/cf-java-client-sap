package org.cloudfoundry.client.lib.rest.clients.apps.impl;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchListWithAuxiliaryContent;
import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchWithAuxiliaryContent;

import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudApplication;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudStack;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsActionsClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsBaseClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsDiskClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsEnvironmentClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsEventsClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsInstancesClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsMemoryClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsServicesClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsStagingClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsUploadClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsUrisClient;
import org.cloudfoundry.client.lib.rest.clients.domains.CloudControllerDomainsClient;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.applications.SummaryApplicationRequest;
import org.cloudfoundry.client.v2.applications.SummaryApplicationResponse;
import org.cloudfoundry.client.v2.stacks.GetStackRequest;
import org.cloudfoundry.client.v2.stacks.StackEntity;
import org.cloudfoundry.client.v3.applications.Application;
import org.cloudfoundry.client.v3.applications.GetApplicationRequest;
import org.cloudfoundry.client.v3.applications.ListApplicationsRequest;
import org.cloudfoundry.util.PaginationUtils;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CloudControllerApplicationsClientImpl extends CloudControllerApplicationsBaseClient
    implements CloudControllerApplicationsClient {

    private CloudControllerApplicationsUploadClient uploadClient;
    private CloudControllerApplicationsEnvironmentClient environmentClient;
    private CloudControllerApplicationsEventsClient eventsClient;
    private CloudControllerApplicationsActionsClient actionsClient;
    private CloudControllerApplicationsInstancesClient instancesClient;
    private CloudControllerApplicationsDiskClient diskQuotaClient;
    private CloudControllerApplicationsMemoryClient memoryClient;
    private CloudControllerApplicationsServicesClient servicesClient;
    private CloudControllerApplicationsStagingClient stagingClient;
    private CloudControllerApplicationsUrisClient urisClient;

    public CloudControllerApplicationsClientImpl(CloudSpace target, CloudFoundryClient v3Client,
                                                    CloudControllerDomainsClient domainsClient) {
        super(target, v3Client, null);
        this.uploadClient = new CloudControllerApplicationsUploadClientImpl(target, v3Client, this);
        this.environmentClient = new CloudControllerApplicationsEnvironmentClientImpl(target, v3Client, this);
        this.eventsClient = new CloudControllerApplicationsEventsClientImpl(target, v3Client, this);
        this.instancesClient = new CloudControllerApplicationsInstancesClientImpl(target, v3Client, this);
        this.diskQuotaClient = new CloudControllerApplicationsDiskClientImpl(target, v3Client, this);
        this.memoryClient = new CloudControllerApplicationsMemoryClientImpl(target, v3Client, this);
        this.servicesClient = new CloudControllerApplicationsServicesClientImpl(target, v3Client);
        this.stagingClient = new CloudControllerApplicationsStagingClientImpl(target, v3Client, this);
        this.urisClient = new CloudControllerApplicationsUrisClientImpl(target, v3Client, this, domainsClient);
        this.actionsClient = new CloudControllerApplicationsActionsClientImpl(target,
                                                                              v3Client,
                                                                              this,
                                                                              servicesClient,
                                                                              urisClient,
                                                                              stagingClient);
    }

    @Override
    public CloudApplication getApplication(String applicationName) {
        return getApplication(applicationName, true);
    }

    @Override
    public CloudApplication getApplication(String applicationName, boolean required) {
        return findApplicationByName(applicationName, required);
    }

    @Override
    public CloudApplication getApplication(UUID applicationGuid) {
        return findApplication(applicationGuid);
    }

    @Override
    public List<CloudApplication> getApplications() {
        return fetchListWithAuxiliaryContent(this::getApplicationResources, this::zipWithAuxiliaryApplicationContent);
    }

    @Override
    public CloudControllerApplicationsUploadClient upload() {
        return uploadClient;
    }

    @Override
    public CloudControllerApplicationsEnvironmentClient environment() {
        return environmentClient;
    }

    @Override
    public CloudControllerApplicationsEventsClient events() {
        return eventsClient;
    }

    @Override
    public CloudControllerApplicationsInstancesClient instances() {
        return instancesClient;
    }

    @Override
    public CloudControllerApplicationsActionsClient actions() {
        return actionsClient;
    }

    @Override
    public CloudControllerApplicationsDiskClient diskQuota() {
        return diskQuotaClient;
    }

    @Override
    public CloudControllerApplicationsMemoryClient memory() {
        return memoryClient;
    }

    @Override
    public CloudControllerApplicationsServicesClient services() {
        return servicesClient;
    }

    @Override
    public CloudControllerApplicationsStagingClient staging() {
        return stagingClient;
    }

    @Override
    public CloudControllerApplicationsUrisClient uris() {
        return urisClient;
    }

    @Override
    public CloudApplication findApplicationByName(String name, boolean required) {
        CloudApplication application = findApplicationByName(name);
        if (application == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application " + name + " not found.");
        }
        return application;
    }

    private CloudApplication findApplicationByName(String name) {
        return fetchWithAuxiliaryContent(() -> getApplicationResourceByName(name), this::zipWithAuxiliaryApplicationContent);
    }

    private CloudApplication findApplication(UUID guid) {
        return fetchWithAuxiliaryContent(() -> getApplicationResource(guid), this::zipWithAuxiliaryApplicationContent);
    }

    private Flux<? extends Application> getApplicationResources() {
        IntFunction<ListApplicationsRequest> pageRequestSupplier = page -> ListApplicationsRequest.builder()
                                                                                                  .spaceId(getTargetSpaceGuid().toString())
                                                                                                  .page(page)
                                                                                                  .build();
        return PaginationUtils.requestClientV3Resources(page -> v3Client.applicationsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Mono<? extends Application> getApplicationResource(UUID guid) {
        GetApplicationRequest request = GetApplicationRequest.builder()
                                                             .applicationId(guid.toString())
                                                             .build();
        return v3Client.applicationsV3()
                       .get(request);
    }

    private Mono<? extends Application> getApplicationResourceByName(String name) {
        IntFunction<ListApplicationsRequest> pageRequestSupplier = page -> ListApplicationsRequest.builder()
                                                                                                  .spaceId(getTargetSpaceGuid().toString())
                                                                                                  .name(name)
                                                                                                  .page(page)
                                                                                                  .build();
        return PaginationUtils.requestClientV3Resources(page -> v3Client.applicationsV3()
                                                                        .list(pageRequestSupplier.apply(page)))
                              .singleOrEmpty();
    }

    private Mono<Derivable<CloudApplication>> zipWithAuxiliaryApplicationContent(Application applicationResource) {
        UUID applicationGuid = getGuid(applicationResource);
        return getApplicationSummary(applicationGuid).zipWhen(this::getApplicationStackResource)
                                                     .map(tuple -> ImmutableRawCloudApplication.builder()
                                                                                               .resource(applicationResource)
                                                                                               .summary(tuple.getT1())
                                                                                               .stack(ImmutableRawCloudStack.of(tuple.getT2()))
                                                                                               .space(target)
                                                                                               .build());
    }

    private Mono<SummaryApplicationResponse> getApplicationSummary(UUID guid) {
        SummaryApplicationRequest request = SummaryApplicationRequest.builder()
                                                                     .applicationId(guid.toString())
                                                                     .build();
        return v3Client.applicationsV2()
                       .summary(request);
    }

    private Mono<? extends Resource<StackEntity>> getApplicationStackResource(SummaryApplicationResponse summary) {
        UUID stackGuid = UUID.fromString(summary.getStackId());
        return getStackResource(stackGuid);
    }

    private Mono<? extends Resource<StackEntity>> getStackResource(UUID guid) {
        GetStackRequest request = GetStackRequest.builder()
                                                 .stackId(guid.toString())
                                                 .build();
        return v3Client.stacks()
                       .get(request);
    }
}
