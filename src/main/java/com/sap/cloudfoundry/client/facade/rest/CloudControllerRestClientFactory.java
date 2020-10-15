package com.sap.cloudfoundry.client.facade.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.reactor.ConnectionContext;
import org.immutables.value.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.adapters.CloudFoundryClientFactory;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableCloudFoundryClientFactory;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.util.RestUtil;

@Value.Immutable
public abstract class CloudControllerRestClientFactory {

    private final Map<URL, Map<String, Object>> infoCache = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestUtil restUtil = new RestUtil();

    public abstract Optional<Duration> getSslHandshakeTimeout();

    public abstract Optional<Duration> getConnectTimeout();

    public abstract Optional<Integer> getConnectionPoolSize();

    public abstract Optional<Integer> getThreadPoolSize();

    @Value.Default
    public boolean shouldTrustSelfSignedCertificates() {
        return false;
    }

    @Value.Derived
    public CloudFoundryClientFactory getCloudFoundryClientFactory() {
        ImmutableCloudFoundryClientFactory.Builder builder = ImmutableCloudFoundryClientFactory.builder();
        getSslHandshakeTimeout().ifPresent(builder::sslHandshakeTimeout);
        getConnectTimeout().ifPresent(builder::connectTimeout);
        getConnectionPoolSize().ifPresent(builder::connectionPoolSize);
        getThreadPoolSize().ifPresent(builder::threadPoolSize);
        return builder.build();
    }

    public WebClient getGeneralPurposeWebClient() {
        return restUtil.createWebClient(shouldTrustSelfSignedCertificates());
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, String organizationName,
                                                  String spaceName, OAuthClient oAuthClient, List<ExchangeFilterFunction> exchangeFilters) {
        CloudControllerRestClient clientWithoutTarget = createClient(controllerUrl, credentials, oAuthClient);
        CloudSpace target = clientWithoutTarget.getSpace(organizationName, spaceName);

        return createClient(controllerUrl, credentials, target, oAuthClient, exchangeFilters);
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, OAuthClient oAuthClient) {
        return createClient(controllerUrl, credentials, null, oAuthClient, Collections.emptyList());
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, CloudSpace target) {
        return createClient(controllerUrl, credentials, target, createOAuthClient(controllerUrl, credentials.getOrigin()),
                            Collections.emptyList());
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, CloudSpace target,
                                                  OAuthClient oAuthClient, List<ExchangeFilterFunction> exchangeFilters) {
        WebClient webClient = createWebClient(credentials, oAuthClient, exchangeFilters);
        CloudFoundryClient delegate = getCloudFoundryClientFactory().createClient(controllerUrl, oAuthClient);
        DopplerClient dopplerClient = getCloudFoundryClientFactory().createDopplerClient(controllerUrl, oAuthClient);

        return new CloudControllerRestClientImpl(controllerUrl, credentials, webClient, oAuthClient, delegate, dopplerClient, target);
    }

    private OAuthClient createOAuthClient(URL controllerUrl, String origin) {
        Map<String, Object> infoMap = getInfoMap(controllerUrl);
        URL authorizationEndpoint = getAuthorizationEndpoint(infoMap);
        if (StringUtils.isEmpty(origin)) {
            return restUtil.createOAuthClient(authorizationEndpoint, shouldTrustSelfSignedCertificates());
        }
        ConnectionContext connectionContext = getCloudFoundryClientFactory().getOrCreateConnectionContext(controllerUrl.getHost());
        return restUtil.createOAuthClient(authorizationEndpoint, shouldTrustSelfSignedCertificates(), connectionContext, origin);
    }

    private Map<String, Object> getInfoMap(URL controllerUrl) {
        if (infoCache.containsKey(controllerUrl)) {
            return infoCache.get(controllerUrl);
        }

        String infoResponse = getGeneralPurposeWebClient().get()
                                                          .uri(controllerUrl + "/v2/info")
                                                          .retrieve()
                                                          .bodyToMono(String.class)
                                                          .block();
        try {
            return objectMapper.readValue(infoResponse, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new IllegalArgumentException("Error getting /v2/info from cloud controller.", e);
        }
    }

    private URL getAuthorizationEndpoint(Map<String, Object> infoMap) {
        String authorizationEndpoint = (String) infoMap.get("authorization_endpoint");
        try {
            return new URL(authorizationEndpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(MessageFormat.format("Error creating authorization endpoint URL for endpoint {0}.",
                                                                    authorizationEndpoint),
                                               e);
        }
    }

    private WebClient createWebClient(CloudCredentials credentials, OAuthClient oAuthClient, List<ExchangeFilterFunction> exchangeFilters) {
        Builder webClientBuilder = restUtil.createWebClient(shouldTrustSelfSignedCertificates())
                                           .mutate();
        oAuthClient.init(credentials);
        addExchangeFilters(webClientBuilder, Arrays.asList(new CloudControllerRestClientRequestFilterFunction(oAuthClient)));
        addExchangeFilters(webClientBuilder, exchangeFilters);
        return webClientBuilder.build();
    }

    private void addExchangeFilters(Builder webClientBuilder, List<ExchangeFilterFunction> exchangeFilters) {
        for (ExchangeFilterFunction exchangeFilter : exchangeFilters) {
            webClientBuilder.filter(exchangeFilter);
        }
    }
}
