package org.cloudfoundry.client.lib.rest.clients.apps.impl;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsBaseClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsStagingClient;
import org.cloudfoundry.client.v2.featureflags.SetFeatureFlagRequest;
import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.LifecycleType;
import org.cloudfoundry.client.v3.applications.GetApplicationProcessResponse;
import org.cloudfoundry.client.v3.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v3.processes.Data;
import org.cloudfoundry.client.v3.processes.HealthCheck;
import org.cloudfoundry.client.v3.processes.HealthCheckType;
import org.cloudfoundry.client.v3.processes.UpdateProcessRequest;

public class CloudControllerApplicationsStagingClientImpl extends CloudControllerApplicationsBaseClient
    implements CloudControllerApplicationsStagingClient {

    protected CloudControllerApplicationsStagingClientImpl(CloudSpace target, CloudFoundryClient v3Client,
                                                           CloudControllerApplicationsClient applicationsClient) {
        super(target, v3Client, applicationsClient);
    }

    @Override
    public void update(String applicationName, Staging staging) {
        UUID applicationGuid = getGuid(applicationsClient.findApplicationByName(applicationName, true));
        UpdateApplicationRequest.Builder requestBuilder = UpdateApplicationRequest.builder();
        requestBuilder.applicationId(applicationGuid.toString());
        if (staging != null) {
            if (staging.getBuildpack() != null) {
                BuildpackData.Builder buildpackDataBuilder = BuildpackData.builder();
                buildpackDataBuilder.buildpack(staging.getBuildpack());
                String stackName = staging.getStack();
                if (stackName != null) {
                    buildpackDataBuilder.stack(stackName);
                }
                requestBuilder.lifecycle(Lifecycle.builder()
                                                  .type(LifecycleType.BUILDPACK)
                                                  .data(buildpackDataBuilder.build())
                                                  .build());
            }
        }
        update(applicationGuid, staging);

        if (shouldUpdateWithV3Buildpacks(staging)) {
            updateBuildpacks(applicationGuid, staging.getBuildpacks());
        }
    }

    @Override
    public void update(UUID applicationGuid, Staging staging) {
        if (staging == null) {
            return;
        }

        if (staging.isSshEnabled() != null) {
            v3Client.featureFlags()
                    .set(SetFeatureFlagRequest.builder()
                                              .name("ssh")
                                              .enabled(staging.isSshEnabled())
                                              .build())
                    .block();
        }
        GetApplicationProcessResponse applicationProcessResponse = getApplicationProcess(applicationGuid);
        UpdateProcessRequest.Builder updateProcessRequestBuilder = UpdateProcessRequest.builder()
                                                                                       .processId(applicationProcessResponse.getId())
                                                                                       .command(staging.getCommand());
        if (staging.getHealthCheckType() != null) {
            updateProcessRequestBuilder.healthCheck(HealthCheck.builder()
                                                               .type(HealthCheckType.from(staging.getHealthCheckType()))
                                                               .data(Data.builder()
                                                                         .endpoint(staging.getHealthCheckHttpEndpoint())
                                                                         .timeout(staging.getHealthCheckTimeout())
                                                                         .build())
                                                               .build());
        }
        v3Client.processes()
                .update(updateProcessRequestBuilder.build())
                .block();

        if (shouldUpdateWithV3Buildpacks(staging)) {
            updateBuildpacks(applicationGuid, staging.getBuildpacks());
        }
    }

    private boolean shouldUpdateWithV3Buildpacks(Staging staging) {
        return staging.getBuildpacks()
                      .size() > 1;
    }

    private void updateBuildpacks(UUID appGuid, List<String> buildpacks) {
        v3Client.applicationsV3()
                .update(org.cloudfoundry.client.v3.applications.UpdateApplicationRequest.builder()
                                                                                        .applicationId(appGuid.toString())
                                                                                        .lifecycle(Lifecycle.builder()
                                                                                                            .type(LifecycleType.BUILDPACK)
                                                                                                            .data(BuildpackData.builder()
                                                                                                                               .addAllBuildpacks(buildpacks)
                                                                                                                               .build())
                                                                                                            .build())
                                                                                        .build())
                .block();
    }

}
