package org.cloudfoundry.client.lib.adapters;

import java.net.URL;

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
            .connectionContext(createConnectionContext(controllerUrl))
            .tokenProvider(createTokenProvider(oAuthClient))
            .build();
    }

    private ConnectionContext createConnectionContext(URL controllerUrl) {
        return DefaultConnectionContext.builder()
            .apiHost(controllerUrl.getHost())
            .build();
    }

    private TokenProvider createTokenProvider(OAuthClient oAuthClient) {
        return new OAuthTokenProvider(oAuthClient);
    }

}
