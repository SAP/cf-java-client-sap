package com.sap.cloudfoundry.client.facade.adapters;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v2.applications.SummaryApplicationResponse;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstance;
import org.cloudfoundry.client.v3.applications.Application;
import org.cloudfoundry.client.v3.applications.ApplicationState;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.CloudStack;
import com.sap.cloudfoundry.client.facade.domain.Derivable;
import com.sap.cloudfoundry.client.facade.domain.DockerCredentials;
import com.sap.cloudfoundry.client.facade.domain.DockerInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRouteSummary;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerCredentials;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableStaging;
import com.sap.cloudfoundry.client.facade.domain.PackageState;
import com.sap.cloudfoundry.client.facade.domain.Staging;
import com.sap.cloudfoundry.client.facade.util.EnvironmentUtil;

@Value.Immutable
public abstract class RawCloudApplication extends RawCloudEntity<CloudApplication> {

    public abstract Application getApplication();

    public abstract SummaryApplicationResponse getSummary();

    public abstract Derivable<CloudStack> getStack();

    public abstract Derivable<CloudSpace> getSpace();

    @Override
    public CloudApplication derive() {
        SummaryApplicationResponse summary = getSummary();
        return ImmutableCloudApplication.builder()
                                        .metadata(parseResourceMetadata(getApplication()))
                                        .name(summary.getName())
                                        .memory(summary.getMemory())
                                        .routes(parseRoutes(summary.getRoutes()))
                                        .diskQuota(summary.getDiskQuota())
                                        .instances(summary.getInstances())
                                        .runningInstances(summary.getRunningInstances())
                                        .state(parseState(getApplication().getState()))
                                        .staging(parseStaging(summary, getStack()))
                                        .packageState(parsePackageState(summary.getPackageState()))
                                        .stagingError(summary.getStagingFailedDescription())
                                        .services(getNames(summary.getServices()))
                                        .env(EnvironmentUtil.parse(summary.getEnvironmentJsons()))
                                        .space(getSpace().derive())
                                        .build();
    }

    private static CloudApplication.State parseState(ApplicationState state) {
        return CloudApplication.State.valueOf(state.getValue());
    }

    private static Staging parseStaging(SummaryApplicationResponse summary, Derivable<CloudStack> stack) {
        return ImmutableStaging.builder()
                               .addBuildpack(summary.getBuildpack())
                               .command(summary.getCommand())
                               .detectedBuildpack(summary.getDetectedBuildpack())
                               .healthCheckHttpEndpoint(summary.getHealthCheckHttpEndpoint())
                               .healthCheckTimeout(summary.getHealthCheckTimeout())
                               .healthCheckType(summary.getHealthCheckType())
                               .isSshEnabled(summary.getEnableSsh())
                               .dockerInfo(parseDockerInfo(summary))
                               .stack(parseStackName(stack))
                               .build();
    }

    private static DockerInfo parseDockerInfo(SummaryApplicationResponse summary) {
        String image = summary.getDockerImage();
        if (image == null) {
            return null;
        }
        return ImmutableDockerInfo.builder()
                                  .image(image)
                                  .credentials(parseDockerCredentials(summary))
                                  .build();
    }

    private static DockerCredentials parseDockerCredentials(SummaryApplicationResponse summary) {
        org.cloudfoundry.client.v2.applications.DockerCredentials credentials = summary.getDockerCredentials();
        if (credentials == null) {
            return null;
        }
        return ImmutableDockerCredentials.builder()
                                         .username(credentials.getUsername())
                                         .password(credentials.getPassword())
                                         .build();
    }

    private static String parseStackName(Derivable<CloudStack> derivableStack) {
        return derivableStack.derive()
                             .getName();
    }

    private static PackageState parsePackageState(String state) {
        return PackageState.valueOf(state);
    }

    private static Set<CloudRouteSummary> parseRoutes(List<org.cloudfoundry.client.v2.routes.Route> routes) {
        return routes.stream()
                     .map(RawCloudApplication::parseRoute)
                     .collect(Collectors.toSet());
    }

    private static CloudRouteSummary parseRoute(org.cloudfoundry.client.v2.routes.Route route) {
        return ImmutableCloudRouteSummary.builder()
                                         .host(route.getHost())
                                         .port(route.getPort())
                                         .path(route.getPath())
                                         .domain(route.getDomain()
                                                      .getName())
                                         .domainGuid(parseNullableGuid(route.getDomain()
                                                                            .getId()))
                                         .guid(parseNullableGuid(route.getId()))
                                         .build();
    }

    private static List<String> getNames(List<ServiceInstance> services) {
        return services.stream()
                       .map(ServiceInstance::getName)
                       .collect(Collectors.toList());
    }

}
