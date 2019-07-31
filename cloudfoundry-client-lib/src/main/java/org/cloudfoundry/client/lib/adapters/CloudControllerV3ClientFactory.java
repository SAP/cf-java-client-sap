package org.cloudfoundry.client.lib.adapters;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;

public class CloudControllerV3ClientFactory {

    private static final int DEFAULT_CLIENT_CONNECTION_POOL_SIZE = 75;
    private static final int DEFAULT_CLIENT_THREAD_POOL_SIZE = 75;

    private final Map<String, ConnectionContext> connectionContextCache = new ConcurrentHashMap<>();
    private final int clientCoonectionPoolSize;
    private final int clientThreadPoolSize;

    public CloudControllerV3ClientFactory() {
        this(DEFAULT_CLIENT_CONNECTION_POOL_SIZE, DEFAULT_CLIENT_THREAD_POOL_SIZE);
    }

    public CloudControllerV3ClientFactory(int clientCoonectionPoolSize, int clientThreadPoolSize) {
        this.clientCoonectionPoolSize = clientCoonectionPoolSize;
        this.clientThreadPoolSize = clientThreadPoolSize;
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
        return DefaultConnectionContext.builder()
                                       .apiHost(controllerApiHost)
                                       .threadPoolSize(clientThreadPoolSize)
                                       .connectionPoolSize(clientCoonectionPoolSize)
                                       .build();
    }

}
