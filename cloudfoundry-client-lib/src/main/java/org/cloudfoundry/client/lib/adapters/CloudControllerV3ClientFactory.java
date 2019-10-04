package org.cloudfoundry.client.lib.adapters;

import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.immutables.value.Value;
import org.springframework.util.Assert;

@Value.Immutable
public abstract class CloudControllerV3ClientFactory {

    private final Map<String, ConnectionContext> connectionContextCache = new ConcurrentHashMap<>();

    public abstract Optional<Duration> getClientConnectTimeout();

    public abstract Optional<Integer> getClientConnectionPoolSize();

    public abstract Optional<Integer> getClientThreadPoolSize();

    public CloudFoundryOperations createOperationsClient(URL controllerUrl, CloudFoundryClient cloudControllerClient, CloudSpace target) {
        DefaultCloudFoundryOperations.Builder builder = DefaultCloudFoundryOperations.builder()
                                                                                     .cloudFoundryClient(cloudControllerClient);
        if (target != null) {
            String organization = getOrganizationName(target);
            String space = getSpaceName(target);
            builder.organization(organization)
                   .space(space);
        }
        return builder.build();
    }

    private String getOrganizationName(CloudSpace target) {
        CloudOrganization organization = target.getOrganization();
        Assert.notNull(organization, "Target organization cannot be null.");
        String name = organization.getName();
        Assert.notNull(name, "Target organization name cannot be null.");
        return name;
    }

    private String getSpaceName(CloudSpace target) {
        String name = target.getName();
        Assert.notNull(name, "Target space name cannot be null.");
        return name;
    }

    public CloudFoundryClient createClient(URL controllerUrl, OAuthClient oAuthClient) {
        return ReactorCloudFoundryClient.builder()
                                        .connectionContext(getOrCreateConnectionContext(controllerUrl.getHost()))
                                        .tokenProvider(oAuthClient.getTokenProvider())
                                        .build();
    }

    public ConnectionContext getOrCreateConnectionContext(String controllerApiHost) {
        return connectionContextCache.computeIfAbsent(controllerApiHost, this::createConnectionContext);
    }

    private ConnectionContext createConnectionContext(String controllerApiHost) {
        DefaultConnectionContext.Builder builder = DefaultConnectionContext.builder()
                                                                           .apiHost(controllerApiHost);
        getClientConnectTimeout().ifPresent(builder::connectTimeout);
        getClientConnectionPoolSize().ifPresent(builder::connectionPoolSize);
        getClientThreadPoolSize().ifPresent(builder::threadPoolSize);
        return builder.build();
    }

}
