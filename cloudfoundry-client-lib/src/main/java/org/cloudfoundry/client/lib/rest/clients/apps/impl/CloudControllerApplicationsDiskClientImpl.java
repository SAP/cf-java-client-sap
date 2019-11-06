package org.cloudfoundry.client.lib.rest.clients.apps.impl;

import java.util.UUID;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsBaseClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsDiskClient;
import org.cloudfoundry.client.v3.applications.GetApplicationProcessResponse;
import org.cloudfoundry.client.v3.processes.ScaleProcessRequest;

public class CloudControllerApplicationsDiskClientImpl extends CloudControllerApplicationsBaseClient
    implements CloudControllerApplicationsDiskClient {

    protected CloudControllerApplicationsDiskClientImpl(CloudSpace target, CloudFoundryClient v3Client,
                                                        CloudControllerApplicationsClient applicationsClient) {
        super(target, v3Client, applicationsClient);
    }

    @Override
    public void update(String applicationName, int diskQuota) {
        UUID applicationGuid = getGuid(applicationsClient.findApplicationByName(applicationName, true));
        GetApplicationProcessResponse applicationProcessResponse = getApplicationProcess(applicationGuid);
        v3Client.processes()
                .scale(ScaleProcessRequest.builder()
                                          .processId(applicationProcessResponse.getId())
                                          .diskInMb(diskQuota)
                                          .build())
                .block();
        // Change to the below code after fixing the issue {issue}:
        // v3Client.applicationsV3()
        // .scale(ScaleApplicationRequest.builder()
        // .applicationId(applicationGuid.toString())
        // .diskInMb(diskQuota)
        // .type("web")
        // .build())
        // .block();
    }

}
