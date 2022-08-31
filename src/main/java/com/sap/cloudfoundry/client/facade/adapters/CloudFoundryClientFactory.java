package com.sap.cloudfoundry.client.facade.adapters;

import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.doppler.ReactorDopplerClient;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;

import reactor.netty.http.client.HttpClient;

@Value.Immutable
public abstract class CloudFoundryClientFactory {

    private final Map<String, ConnectionContext> connectionContextCache = new ConcurrentHashMap<>();

    public abstract Optional<Duration> getSslHandshakeTimeout();

    public abstract Optional<Duration> getConnectTimeout();

    public abstract Optional<Integer> getConnectionPoolSize();

    public abstract Optional<Integer> getThreadPoolSize();

    public abstract Optional<Duration> getResponseTimeout();

    public CloudFoundryClient createClient(URL controllerUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        return ReactorCloudFoundryClient.builder()
                                        .connectionContext(getOrCreateConnectionContext(controllerUrl.getHost()))
                                        .tokenProvider(oAuthClient.getTokenProvider())
                                        .requestTags(requestTags)
                                        .build();
    }

    public DopplerClient createDopplerClient(URL controllerUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        return ReactorDopplerClient.builder()
                                   .connectionContext(getOrCreateConnectionContext(controllerUrl.getHost()))
                                   .tokenProvider(oAuthClient.getTokenProvider())
                                   .requestTags(requestTags)
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
        builder.additionalHttpClientConfiguration(this::getAdditionalHttpClientConfiguration);
        return builder.build();
    }

    private HttpClient getAdditionalHttpClientConfiguration(HttpClient client) {
        HttpClient clientWithOptions = client;
        if (getResponseTimeout().isPresent()) {
            clientWithOptions = clientWithOptions.responseTimeout(getResponseTimeout().get());
        }
        clientWithOptions = clientWithOptions.metrics(true, Function.identity());
        return clientWithOptions;
    }

}
