package com.sap.cloudfoundry.client.facade.adapters;

import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v3.organizations.OrganizationsV3;
import org.cloudfoundry.client.v3.spaces.SpacesV3;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.client.v3.organizations.ReactorOrganizationsV3;
import org.cloudfoundry.reactor.client.v3.spaces.ReactorSpacesV3;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.rest.CloudSpaceClient;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.util.JsonUtil;

import reactor.core.publisher.Mono;
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

    public LogCacheClient createLogCacheClient(URL controllerUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        String logCacheApi = controllerUrl.toString()
                                          .replace("api", "log-cache");
        return new LogCacheClient(logCacheApi, oAuthClient, requestTags);
    }

    @SuppressWarnings("unchecked")
    public CloudSpaceClient createSpaceClient(URL controllerUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        String v3Api;
        var httpClient = java.net.http.HttpClient.newHttpClient();
        try {
            var response = httpClient.send(HttpRequest.newBuilder()
                                                      .GET()
                                                      .uri(controllerUrl.toURI())
                                                      .build(), HttpResponse.BodyHandlers.ofString());
            var map = JsonUtil.convertJsonToMap(response.body());
            var links = (Map<String, Object>) map.get("links");
            var ccv3 = (Map<String, Object>) links.get("cloud_controller_v3");
            v3Api = (String) ccv3.get("href");
        } catch (Exception ignored) {
            v3Api = controllerUrl.toString() + "/v3";
        }

        var spacesV3 = createV3SpacesClient(controllerUrl, v3Api, oAuthClient, requestTags);
        var orgsV3 = createV3OrgsClient(controllerUrl, v3Api, oAuthClient, requestTags);
        return new CloudSpaceClient(spacesV3, orgsV3);
    }

    private SpacesV3 createV3SpacesClient(URL controllerUrl, String v3Api, OAuthClient oAuthClient,
                                          Map<String, String> requestTags) {
        return new ReactorSpacesV3(getOrCreateConnectionContext(controllerUrl.getHost()), Mono.just(v3Api),
                                   oAuthClient.getTokenProvider(), requestTags);
    }

    private OrganizationsV3 createV3OrgsClient(URL controllerUrl, String v3Api, OAuthClient oAuthClient,
                                               Map<String, String> requestTags) {
        return new ReactorOrganizationsV3(getOrCreateConnectionContext(controllerUrl.getHost()), Mono.just(v3Api),
                                          oAuthClient.getTokenProvider(), requestTags);
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
