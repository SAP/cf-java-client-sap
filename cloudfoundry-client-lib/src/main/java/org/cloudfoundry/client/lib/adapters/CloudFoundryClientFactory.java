package org.cloudfoundry.client.lib.adapters;

import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.ProxyConfiguration;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.immutables.value.Value;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;

@Value.Immutable
public abstract class CloudFoundryClientFactory {

    private final Map<String, ConnectionContext> connectionContextCache = new ConcurrentHashMap<>();

    public abstract Optional<Duration> getSslHandshakeTimeout();

    public abstract Optional<Duration> getConnectTimeout();

    public abstract Optional<Integer> getConnectionPoolSize();

    public abstract Optional<Integer> getThreadPoolSize();

    public CloudFoundryClient createClient(URL controllerUrl, OAuthClient oAuthClient) {
        return ReactorCloudFoundryClient.builder()
                                        .connectionContext(getOrCreateConnectionContext(controllerUrl.getHost()))
                                        .tokenProvider(oAuthClient.getTokenProvider())
                                        .build();
    }

    public DopplerClient createDopplerClient(URL controllerUrl, OAuthClient oAuthClient) {
        return ReactorDopplerClient.builder()
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
        getSslHandshakeTimeout().ifPresent(builder::sslHandshakeTimeout);
        getConnectTimeout().ifPresent(builder::connectTimeout);
        getConnectionPoolSize().ifPresent(builder::connectionPoolSize);
        getThreadPoolSize().ifPresent(builder::threadPoolSize);
        builder.additionalHttpClientConfiguration(client -> client.metrics(true));
        return builder.build();
    }

}
