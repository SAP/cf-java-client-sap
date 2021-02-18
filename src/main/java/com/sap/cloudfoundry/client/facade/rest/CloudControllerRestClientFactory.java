package com.sap.cloudfoundry.client.facade.rest;

import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.reactor.ConnectionContext;
import org.immutables.value.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.adapters.CloudFoundryClientFactory;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableCloudFoundryClientFactory;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.util.RestUtil;

@Value.Immutable
public abstract class CloudControllerRestClientFactory {

    private final RestUtil restUtil = new RestUtil();

    public abstract Optional<Duration> getSslHandshakeTimeout();

    public abstract Optional<Duration> getConnectTimeout();

    public abstract Optional<Integer> getConnectionPoolSize();

    public abstract Optional<Integer> getThreadPoolSize();

    public abstract Optional<Duration> getResponseTimeout();

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
        getResponseTimeout().ifPresent(builder::responseTimeout);
        return builder.build();
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
        if (StringUtils.isEmpty(origin)) {
            return restUtil.createOAuthClientByControllerUrl(controllerUrl, shouldTrustSelfSignedCertificates());
        }
        ConnectionContext connectionContext = getCloudFoundryClientFactory().getOrCreateConnectionContext(controllerUrl.getHost());
        return restUtil.createOAuthClient(controllerUrl, connectionContext, origin, shouldTrustSelfSignedCertificates());
    }

    private WebClient createWebClient(CloudCredentials credentials, OAuthClient oAuthClient, List<ExchangeFilterFunction> exchangeFilters) {
        Builder webClientBuilder = restUtil.createWebClient(shouldTrustSelfSignedCertificates())
                                           .mutate();
        oAuthClient.init(credentials);
        addExchangeFilters(webClientBuilder, List.of(new CloudControllerRestClientRequestFilterFunction(oAuthClient)));
        addExchangeFilters(webClientBuilder, exchangeFilters);
        return webClientBuilder.build();
    }

    private void addExchangeFilters(Builder webClientBuilder, List<ExchangeFilterFunction> exchangeFilters) {
        for (ExchangeFilterFunction exchangeFilter : exchangeFilters) {
            webClientBuilder.filter(exchangeFilter);
        }
    }
}
