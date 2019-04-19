package org.cloudfoundry.client.lib.adapters;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.springframework.util.Assert;

public class CloudControllerV3ClientFactory {

    private static final int DEFAULT_CLIENT_CONNECTION_POOL_SIZE = 75;
    private static final int DEFAULT_CLIENT_THREAD_POOL_SIZE = 75;

    private final Map<String, ConnectionContext> connectionContextCache = new HashMap<>();
    private final int clientCoonectionPoolSize;
    private final int clientThreadPoolSize;

    public CloudControllerV3ClientFactory() {
        this(DEFAULT_CLIENT_CONNECTION_POOL_SIZE, DEFAULT_CLIENT_THREAD_POOL_SIZE);
    }

    public CloudControllerV3ClientFactory(int clientCoonectionPoolSize, int clientThreadPoolSize) {
        this.clientCoonectionPoolSize = clientCoonectionPoolSize;
        this.clientThreadPoolSize = clientThreadPoolSize;
    }

    public CloudFoundryOperations createOperationsClient(URL controllerUrl, OAuthClient oAuthClient, CloudSpace target) {
        DefaultCloudFoundryOperations.Builder builder = DefaultCloudFoundryOperations.builder()
            .cloudFoundryClient(createClient(controllerUrl, oAuthClient));
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
            .tokenProvider(createTokenProvider(oAuthClient))
            .build();
    }

    private ConnectionContext getOrCreateConnectionContext(String controllerApiHost) {
        return connectionContextCache.computeIfAbsent(controllerApiHost, this::createConnectionContext);
    }

    private ConnectionContext createConnectionContext(String controllerApiHost) {
        return DefaultConnectionContext.builder()
            .apiHost(controllerApiHost)
            .threadPoolSize(clientThreadPoolSize)
            .connectionPoolSize(clientCoonectionPoolSize)
            .build();
    }

    private TokenProvider createTokenProvider(OAuthClient oAuthClient) {
        return new OAuthTokenProvider(oAuthClient);
    }

}
