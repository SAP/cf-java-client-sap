package org.cloudfoundry.client.lib.rest.clients.apps.impl;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetch;

import java.util.UUID;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.adapters.ImmutableRawInstancesInfo;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsInstancesClient;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesResponse;
import org.cloudfoundry.client.v3.applications.GetApplicationProcessRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationProcessResponse;
import org.cloudfoundry.client.v3.processes.ScaleProcessRequest;

import reactor.core.publisher.Mono;

public class CloudControllerApplicationsInstancesClientImpl extends CloudControllerBaseClient
    implements CloudControllerApplicationsInstancesClient {

    private CloudControllerApplicationsClient applicationsClient;

    protected CloudControllerApplicationsInstancesClientImpl(CloudSpace target, CloudFoundryClient v3Client,
                                                             CloudControllerApplicationsClientImpl applicationsClient) {
        super(target, v3Client);
        this.applicationsClient = applicationsClient;
    }

    @Override
    public InstancesInfo get(String applicationName) {
        CloudApplication application = applicationsClient.getApplication(applicationName);
        return get(application);
    }

    @Override
    public InstancesInfo get(CloudApplication application) {
        if (application.getState()
                       .equals(CloudApplication.State.STARTED)) {
            return findApplicationInstances(getGuid(application));
        }
        return null;
    }

    private InstancesInfo findApplicationInstances(UUID applicationGuid) {
        return fetch(() -> getApplicationInstances(applicationGuid), ImmutableRawInstancesInfo::of);
    }

    private Mono<ApplicationInstancesResponse> getApplicationInstances(UUID applicationGuid) {
        ApplicationInstancesRequest request = ApplicationInstancesRequest.builder()
                                                                         .applicationId(applicationGuid.toString())
                                                                         .build();
        return v3Client.applicationsV2()
                       .instances(request);
    }

    @Override
    public void update(String applicationName, int instances) {
        UUID applicationGuid = getGuid(applicationsClient.findApplicationByName(applicationName, true));
        GetApplicationProcessResponse applicationProcessResponse = getApplicationProcess(applicationGuid);
        v3Client.processes()
                .scale(ScaleProcessRequest.builder()
                                          .processId(applicationProcessResponse.getId())
                                          .instances(instances)
                                          .build())
                .block();
        // Change to the below code after fixing the issue {issue}:
        // v3Client.applicationsV3()
        // .scale(ScaleApplicationRequest.builder()
        // .instances(instances)
        // .applicationId(applicationGuid.toString())
        // .type("web")
        // .build())
        // .block();
    }

    private GetApplicationProcessResponse getApplicationProcess(UUID applicationGuid) {
        return v3Client.applicationsV3()
                       .getProcess(GetApplicationProcessRequest.builder()
                                                               .applicationId(applicationGuid.toString())
                                                               .type("web")
                                                               .build())
                       .block();
    }

}
