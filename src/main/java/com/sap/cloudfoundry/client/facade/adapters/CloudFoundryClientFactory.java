package com.sap.cloudfoundry.client.facade.adapters;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloudfoundry.client.facade.CloudException;
import com.sap.cloudfoundry.client.facade.Messages;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.rest.CloudSpaceClient;
import com.sap.cloudfoundry.client.facade.util.CloudUtil;
import com.sap.cloudfoundry.client.facade.util.JsonUtil;

import io.netty.channel.ChannelOption;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Value.Immutable
public abstract class CloudFoundryClientFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudFoundryClientFactory.class);

    private final Map<String, ConnectionContext> connectionContextCache = new ConcurrentHashMap<>();

    private static final ObjectMapper MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                                                 .configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);

    static final WebClient WEB_CLIENT = buildWebClient();

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
        String logCacheApi;
        try {
            var links = CloudUtil.executeWithRetry(() -> callCfRoot(controllerUrl, requestTags));
            @SuppressWarnings("unchecked")
            var logCache = (Map<String, Object>) links.get("log_cache");
            logCacheApi = (String) logCache.get("href");
        } catch (CloudException e) {
            LOGGER.warn(MessageFormat.format(Messages.CALL_TO_0_FAILED_WITH_1, controllerUrl.toString(), e.getMessage()), e);
            logCacheApi = controllerUrl.toString()
                                       .replace("api", "log-cache");
        }
        return new LogCacheClient(logCacheApi, oAuthClient, requestTags);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callCfRoot(URL controllerUrl, Map<String, String> requestTags) {
        LOGGER.info(MessageFormat.format(Messages.CALLING_CF_ROOT_0_TO_ACCESS_LOG_CACHE_URL, controllerUrl));
        String response = WEB_CLIENT.get()
                                    .uri(getControllerUri(controllerUrl))
                                    .headers(httpHeaders -> httpHeaders.addAll(getAdditionalRequestHeaders(requestTags)))
                                    .exchangeToMono(this::handleClientResponse)
                                    .block();
        LOGGER.info(Messages.CF_ROOT_REQUEST_FINISHED);
        var map = JsonUtil.convertJsonToMap(response);
        return (Map<String, Object>) map.get("links");
    }

    private URI getControllerUri(URL controllerUrl) {
        try {
            return controllerUrl.toURI();
        } catch (URISyntaxException e) {
            throw new CloudException(e.getMessage(), e);
        }
    }

    private LinkedMultiValueMap<String, String> getAdditionalRequestHeaders(Map<String, String> requestTags) {
        LinkedMultiValueMap<String, String> additionalHeaders = new LinkedMultiValueMap<>();
        requestTags.forEach(additionalHeaders::add);
        return additionalHeaders;
    }

    private Mono<String> handleClientResponse(ClientResponse clientResponse) {
        if (clientResponse.statusCode()
                          .is2xxSuccessful()) {
            return clientResponse.bodyToMono(String.class);
        }
        return clientResponse.createException()
                             .flatMap(Mono::error);
    }

    public CloudSpaceClient createSpaceClient(URL controllerUrl, OAuthClient oAuthClient, Map<String, String> requestTags) {
        String v3Api;
        try {
            var links = CloudUtil.executeWithRetry(() -> callCfRoot(controllerUrl, requestTags));
            @SuppressWarnings("unchecked")
            var ccv3 = (Map<String, Object>) links.get("cloud_controller_v3");
            v3Api = (String) ccv3.get("href");
        } catch (CloudException e) {
            LOGGER.warn(MessageFormat.format(Messages.CALL_TO_0_FAILED_WITH_1, controllerUrl.toString(), e.getMessage()), e);
            v3Api = controllerUrl + "/v3";
        }
        var spacesV3 = createV3SpacesClient(controllerUrl, v3Api, oAuthClient, requestTags);
        var orgsV3 = createV3OrgsClient(controllerUrl, v3Api, oAuthClient, requestTags);
        return new CloudSpaceClient(spacesV3, orgsV3);
    }

    private SpacesV3 createV3SpacesClient(URL controllerUrl, String v3Api, OAuthClient oAuthClient, Map<String, String> requestTags) {
        return new ReactorSpacesV3(getOrCreateConnectionContext(controllerUrl.getHost()),
                                   Mono.just(v3Api),
                                   oAuthClient.getTokenProvider(),
                                   requestTags);
    }

    private OrganizationsV3 createV3OrgsClient(URL controllerUrl, String v3Api, OAuthClient oAuthClient, Map<String, String> requestTags) {
        return new ReactorOrganizationsV3(getOrCreateConnectionContext(controllerUrl.getHost()),
                                          Mono.just(v3Api),
                                          oAuthClient.getTokenProvider(),
                                          requestTags);
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

    private reactor.netty.http.client.HttpClient getAdditionalHttpClientConfiguration(reactor.netty.http.client.HttpClient client) {
        var clientWithOptions = client;
        if (getResponseTimeout().isPresent()) {
            clientWithOptions = clientWithOptions.responseTimeout(getResponseTimeout().get());
        }
        clientWithOptions = clientWithOptions.metrics(true, Function.identity());
        return clientWithOptions;
    }

    private static WebClient buildWebClient() {
        HttpClient httpClient = HttpClient.create()
                                          .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) Duration.ofMinutes(10)
                                                                                                      .toMillis())
                                          .responseTimeout(Duration.ofMinutes(5));
        return WebClient.builder()
                        .clientConnector(new ReactorClientHttpConnector(httpClient))
                        .exchangeStrategies(ExchangeStrategies.builder()
                                                              .codecs(configurer -> configurer.defaultCodecs()
                                                                                              .jackson2JsonDecoder(new Jackson2JsonDecoder(MAPPER)))
                                                              .build())
                        .build();

    }

}
