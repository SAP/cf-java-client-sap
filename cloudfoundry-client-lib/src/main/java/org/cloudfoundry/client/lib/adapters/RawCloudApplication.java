package org.cloudfoundry.client.lib.adapters;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.domain.DockerCredentials;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableDockerCredentials;
import org.cloudfoundry.client.lib.domain.ImmutableDockerInfo;
import org.cloudfoundry.client.lib.domain.ImmutableStaging;
import org.cloudfoundry.client.lib.domain.PackageState;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.applications.ApplicationEntity;
import org.cloudfoundry.client.v2.applications.SummaryApplicationResponse;
import org.cloudfoundry.client.v2.domains.Domain;
import org.cloudfoundry.client.v2.routes.Route;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstance;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawCloudApplication extends RawCloudEntity<CloudApplication> {

    private static final String HOST_SEPARATOR = ".";
    private static final String PORT_SEPARATOR = ":";

    public abstract Resource<ApplicationEntity> getResource();

    public abstract SummaryApplicationResponse getSummary();

    public abstract Derivable<CloudStack> getStack();

    public abstract Derivable<CloudSpace> getSpace();

    @Override
    public CloudApplication derive() {
        SummaryApplicationResponse summary = getSummary();
        return ImmutableCloudApplication.builder()
                                        .metadata(parseResourceMetadata(getResource()))
                                        .name(summary.getName())
                                        .memory(summary.getMemory())
                                        .uris(toUrlStrings(summary.getRoutes()))
                                        .diskQuota(summary.getDiskQuota())
                                        .instances(summary.getInstances())
                                        .runningInstances(summary.getRunningInstances())
                                        .state(parseState(summary.getState()))
                                        .staging(parseStaging(summary, getStack()))
                                        .packageState(parsePackageState(summary.getPackageState()))
                                        .stagingError(summary.getStagingFailedDescription())
                                        .services(getNames(summary.getServices()))
                                        .env(parseEnv(summary.getEnvironmentJsons()))
                                        .space(getSpace().derive())
                                        .build();
    }

    private static CloudApplication.State parseState(String state) {
        return CloudApplication.State.valueOf(state);
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

    private static List<String> toUrlStrings(List<org.cloudfoundry.client.v2.routes.Route> routes) {
        return routes.stream()
                     .map(RawCloudApplication::toUrlString)
                     .collect(Collectors.toList());
    }

    private static String toUrlString(org.cloudfoundry.client.v2.routes.Route route) {
        StringBuilder url = new StringBuilder();
        appendHost(url, route);
        appendDomain(url, route);
        appendPort(url, route);
        appendPath(url, route);
        return url.toString();
    }

    private static void appendHost(StringBuilder url, Route route) {
        String host = route.getHost();
        if (host != null && !host.isEmpty()) {
            url.append(host);
            url.append(HOST_SEPARATOR);
        }
    }

    private static void appendDomain(StringBuilder url, Route route) {
        Optional.ofNullable(route.getDomain())
                .map(Domain::getName)
                .ifPresent(url::append);
    }

    private static void appendPort(StringBuilder url, Route route) {
        Integer port = route.getPort();
        if (port != null) {
            url.append(PORT_SEPARATOR);
            url.append(port);
        }
    }

    private static void appendPath(StringBuilder url, Route route) {
        String path = route.getPath();
        if (path != null) {
            // Paths always start with a forward slash, so we don't need to append one.
            url.append(path);
        }
    }

    private static List<String> getNames(List<ServiceInstance> services) {
        return services.stream()
                       .map(ServiceInstance::getName)
                       .collect(Collectors.toList());
    }

    private static Map<String, String> parseEnv(Map<String, Object> env) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> envEntry : env.entrySet()) {
            result.put(envEntry.getKey(), convertValueToString(envEntry.getValue()));
        }
        return result;
    }

    private static String convertValueToString(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof String ? (String) value : JsonUtil.convertToJson(value);
    }

}
