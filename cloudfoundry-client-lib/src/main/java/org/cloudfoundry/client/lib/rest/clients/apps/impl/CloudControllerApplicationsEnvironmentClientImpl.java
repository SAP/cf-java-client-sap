package org.cloudfoundry.client.lib.rest.clients.apps.impl;

import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsEnvironmentClient;
import org.cloudfoundry.client.v3.applications.UpdateApplicationEnvironmentVariablesRequest;

public class CloudControllerApplicationsEnvironmentClientImpl extends CloudControllerBaseClient implements CloudControllerApplicationsEnvironmentClient {

    private CloudControllerApplicationsClient applicationsClient;
    
    protected CloudControllerApplicationsEnvironmentClientImpl(CloudSpace target, CloudFoundryClient v3Client, CloudControllerApplicationsClient applicationsClient) {
        super(target, v3Client);
        this.applicationsClient = applicationsClient;
    }

    @Override
    public Map<String, String> get(String applicationName) {
        return applicationsClient.getApplication(applicationName).getEnv();
    }

    @Override
    public Map<String, String> get(UUID applicationGuid) {
        return applicationsClient.getApplication(applicationGuid).getEnv();
    }

    @Override
    public void update(String applicationName, Map<String, String> env) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);

        v3Client.applicationsV3()
                .updateEnvironmentVariables(UpdateApplicationEnvironmentVariablesRequest.builder()
                                                                                        .applicationId(applicationGuid.toString())
                                                                                        .putAllVars(env)
                                                                                        .build())
                .block();
    }
    
    // TODO: code dup
    private UUID getRequiredApplicationGuid(String name) {
        return getGuid(applicationsClient.findApplicationByName(name, true));
    }

}
