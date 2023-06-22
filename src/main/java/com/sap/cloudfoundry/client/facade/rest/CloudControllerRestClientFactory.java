package com.sap.cloudfoundry.client.facade.rest;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.adapters.CloudFoundryClientFactory;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableCloudFoundryClientFactory;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.util.RestUtil;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.reactor.ConnectionContext;
import org.immutables.value.Value;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

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
                                                  String spaceName, OAuthClient oAuthClient, Map<String, String> requestTags) {
        oAuthClient.init(credentials);
        CloudSpaceClient spaceGetter = getCloudFoundryClientFactory().createSpaceClient(controllerUrl, oAuthClient, requestTags);
        CloudSpace target = spaceGetter.getSpace(organizationName, spaceName);
        return createClient(controllerUrl, credentials, target, oAuthClient, requestTags);
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, CloudSpace target) {
        return createClient(controllerUrl, credentials, target, createOAuthClient(controllerUrl, credentials.getOrigin()),
                            Collections.emptyMap());
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, CloudSpace target,
                                                  OAuthClient oAuthClient, Map<String, String> requestTags) {
        oAuthClient.init(credentials);
        CloudFoundryClient delegate = getCloudFoundryClientFactory().createClient(controllerUrl, oAuthClient, requestTags);
        return new CloudControllerRestClientImpl(delegate, target);
    }

    private OAuthClient createOAuthClient(URL controllerUrl, String origin) {
        if (!StringUtils.hasLength(origin)) {
            return restUtil.createOAuthClientByControllerUrl(controllerUrl, shouldTrustSelfSignedCertificates());
        }
        ConnectionContext connectionContext = getCloudFoundryClientFactory().getOrCreateConnectionContext(controllerUrl.getHost());
        return restUtil.createOAuthClient(controllerUrl, connectionContext, origin, shouldTrustSelfSignedCertificates());
    }

}
