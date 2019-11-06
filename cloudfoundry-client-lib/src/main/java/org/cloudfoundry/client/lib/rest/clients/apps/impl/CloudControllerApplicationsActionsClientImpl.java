package org.cloudfoundry.client.lib.rest.clients.apps.impl;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.DockerCredentials;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsActionsClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsBaseClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsServicesClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsStagingClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsUrisClient;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.applications.ListApplicationServiceBindingsRequest;
import org.cloudfoundry.client.v2.servicebindings.DeleteServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.ServiceBindingEntity;
import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.LifecycleType;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.applications.ApplicationRelationships;
import org.cloudfoundry.client.v3.applications.CreateApplicationRequest;
import org.cloudfoundry.client.v3.applications.CreateApplicationResponse;
import org.cloudfoundry.client.v3.applications.DeleteApplicationRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationProcessResponse;
import org.cloudfoundry.client.v3.applications.StartApplicationRequest;
import org.cloudfoundry.client.v3.applications.StopApplicationRequest;
import org.cloudfoundry.client.v3.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v3.processes.ScaleProcessRequest;
import org.cloudfoundry.util.PaginationUtils;
import org.springframework.util.CollectionUtils;

import reactor.core.publisher.Flux;

public class CloudControllerApplicationsActionsClientImpl extends CloudControllerApplicationsBaseClient
    implements CloudControllerApplicationsActionsClient {

    private CloudControllerApplicationsServicesClient servicesClient;
    private CloudControllerApplicationsUrisClient urisClient;
    private CloudControllerApplicationsStagingClient stagingClient;

    public CloudControllerApplicationsActionsClientImpl(CloudSpace target, CloudFoundryClient v3Client,
                                                        CloudControllerApplicationsClient applicationsClient,
                                                        CloudControllerApplicationsServicesClient servicesClient,
                                                        CloudControllerApplicationsUrisClient urisClient,
                                                        CloudControllerApplicationsStagingClient stagingClient) {
        super(target, v3Client, applicationsClient);
        this.servicesClient = servicesClient;
        this.urisClient = urisClient;
        this.stagingClient = stagingClient;
    }

    @Override
    public void rename(String applicationName, String newName) {
        UUID applicationGuid = getGuid(applicationsClient.findApplicationByName(applicationName, true));
        v3Client.applicationsV3()
                .update(UpdateApplicationRequest.builder()
                                                .applicationId(applicationGuid.toString())
                                                .name(newName)
                                                .build())
                .block();
    }

    @Override
    public StartingInfo restart(String applicationName) {
        stop(applicationName);
        return start(applicationName);
    }

    @Override
    public StartingInfo start(String applicationName) {
        CloudApplication application = applicationsClient.getApplication(applicationName);
        if (application.getState() == CloudApplication.State.STARTED) {
            return null;
        }
        UUID applicationGuid = application.getMetadata()
                                          .getGuid();

        v3Client.applicationsV3()
                .start(StartApplicationRequest.builder()
                                              .applicationId(applicationGuid.toString())
                                              .build())
                .block();
        return null;
    }

    @Override
    public void stop(String applicationName) {
        CloudApplication application = applicationsClient.getApplication(applicationName);
        if (application.getState() == CloudApplication.State.STOPPED) {
            return;
        }
        UUID applicationGuid = application.getMetadata()
                                          .getGuid();
        v3Client.applicationsV3()
                .stop(StopApplicationRequest.builder()
                                            .applicationId(applicationGuid.toString())
                                            .build())
                .block();
    }
    
    @Override
    public void create(String name, Staging staging, Integer diskQuota, Integer memory, List<String> uris,
                                  List<String> serviceNames, DockerInfo dockerInfo) {
        CreateApplicationRequest.Builder createApplicationRequestBuilder = CreateApplicationRequest.builder()
                                                                                                   .relationships(ApplicationRelationships.builder()
                                                                                                                                          .space(ToOneRelationship.builder()
                                                                                                                                                                  .data(Relationship.builder()
                                                                                                                                                                                    .id(getTargetSpaceGuid().toString())
                                                                                                                                                                                    .build())
                                                                                                                                                                  .build())
                                                                                                                                          .build())
                                                                                                   .name(name);
        if (dockerInfo != null) {
            DockerCredentials dockerCredentials = dockerInfo.getCredentials();
            if (dockerCredentials != null) {
                // createApplicationRequestBuilder.lifecycle(Lifecycle.builder()
                // .type(LifecycleType.DOCKER)
                // .data(DockerData.builder()
                // .build())
                // .build());
                // TODO: Think what to do with the docker credentials
                // .username(dockerCredentials.getUsername())
                // .password(dockerCredentials.getPassword())
            }
        }
        if (staging.getStack() != null) {
            createApplicationRequestBuilder.lifecycle(Lifecycle.builder()
                                                               .type(LifecycleType.BUILDPACK)
                                                               .data(BuildpackData.builder()
                                                                                  .stack(staging.getStack())
                                                                                  .build())
                                                               .build());
        }

        CreateApplicationResponse response = v3Client.applicationsV3()
                                                     .create(createApplicationRequestBuilder.build())
                                                     .block();

        UUID newAppGuid = UUID.fromString(response.getId());
        // Application process of type web

        // Update the 'web' process type with health-check data
        stagingClient.update(newAppGuid, staging);

        // Scale application
        GetApplicationProcessResponse applicationProcessResponse = getApplicationProcess(newAppGuid);
        v3Client.processes()
                .scale(ScaleProcessRequest.builder()
                                          .processId(applicationProcessResponse.getId())
                                          .memoryInMb(memory)
                                          .diskInMb(diskQuota)
                                          .build())
                .block();
        // v3Client.applicationsV3()
        // .scale(ScaleApplicationRequest.builder()
        // .applicationId(newAppGuid.toString())
        // .memoryInMb(memory)
        // .diskInMb(diskQuota)
        // .type("web")
        // .build())
        // .block();

        if (!CollectionUtils.isEmpty(serviceNames)) {
            servicesClient.update(name, Collections.emptyMap());
        }

        if (!CollectionUtils.isEmpty(uris)) {
            urisClient.add(newAppGuid, uris);
        }
    }

    @Override
    public void deleteAll() {
        List<CloudApplication> applications = applicationsClient.getApplications();
        for (CloudApplication application : applications) {
            delete(application.getName());
        }
    }

    @Override
    public void delete(String applicationName) {
        UUID applicationGuid = getGuid(applicationsClient.findApplicationByName(applicationName, true));
        List<UUID> serviceBindingGuids = getServiceBindingGuids(applicationGuid);
        for (UUID serviceBindingGuid : serviceBindingGuids) {
            doUnbindService(serviceBindingGuid);
        }
        v3Client.applicationsV3()
                .delete(DeleteApplicationRequest.builder()
                                                .applicationId(applicationGuid.toString())
                                                .build())
                .block();
    }
    
    
    
    private List<UUID> getServiceBindingGuids(UUID applicationGuid) {
        Flux<? extends Resource<ServiceBindingEntity>> bindings = getServiceBindingResourcesByApplicationGuid(applicationGuid);
        return getGuids(bindings);
    }

    private Flux<? extends Resource<ServiceBindingEntity>> getServiceBindingResourcesByApplicationGuid(UUID applicationGuid) {
        IntFunction<ListApplicationServiceBindingsRequest> pageRequestSupplier = page -> ListApplicationServiceBindingsRequest.builder()
                                                                                                                              .applicationId(applicationGuid.toString())
                                                                                                                              .page(page)
                                                                                                                              .build();
        return getApplicationServiceBindingResources(pageRequestSupplier);
    }

    private Flux<? extends Resource<ServiceBindingEntity>>
            getApplicationServiceBindingResources(IntFunction<ListApplicationServiceBindingsRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV2Resources(page -> v3Client.applicationsV2()
                                                                        .listServiceBindings(pageRequestSupplier.apply(page)));
    }

    private void doUnbindService(UUID serviceBindingGuid) {
        v3Client.serviceBindingsV2()
                .delete(DeleteServiceBindingRequest.builder()
                                                   .serviceBindingId(serviceBindingGuid.toString())
                                                   .build())
                .block();
    }

}
