package org.cloudfoundry.client.lib.rest.clients.apps;

import java.util.UUID;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.v3.applications.GetApplicationProcessRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationProcessResponse;

public abstract class CloudControllerApplicationsBaseClient extends CloudControllerBaseClient {

    protected final CloudControllerApplicationsClient applicationsClient;

    protected CloudControllerApplicationsBaseClient(CloudSpace target, CloudFoundryClient v3Client,
                                                    CloudControllerApplicationsClient applicationsClient) {
        super(target, v3Client);
        this.applicationsClient = applicationsClient;
    }

    protected GetApplicationProcessResponse getApplicationProcess(UUID applicationGuid) {
        return v3Client.applicationsV3()
                       .getProcess(GetApplicationProcessRequest.builder()
                                                               .applicationId(applicationGuid.toString())
                                                               .type("web")
                                                               .build())
                       .block();
    }
}