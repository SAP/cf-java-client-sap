package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v3.routes.Route;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRoute;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;

import java.util.UUID;

@Value.Immutable
public abstract class RawCloudRoute extends RawCloudEntity<CloudRoute> {

    @Value.Parameter
    public abstract Route getRoute();

    @Override
    public CloudRoute derive() {
        Route route = getRoute();
        String domainGuid = route.getRelationships()
                                 .getDomain()
                                 .getData()
                                 .getId();
        return ImmutableCloudRoute.builder()
                                  .metadata(parseResourceMetadata(route))
                                  .appsUsingRoute(route.getDestinations()
                                                       .size())
                                  .host(route.getHost())
                                  .port(route.getPort())
                                  .domain(ImmutableCloudDomain.builder()
                                                              .name(computeDomain(route))
                                                              .metadata(ImmutableCloudMetadata.of(UUID.fromString(domainGuid)))
                                                              .build())
                                  .path(route.getPath())
                                  .url(route.getUrl())
                                  .build();
    }

    private static String computeDomain(Route route) {
        String domain = route.getUrl();
        if (!route.getHost()
                  .isEmpty()) {
            domain = domain.substring(route.getHost()
                                           .length() + 1);
        }
        if (!route.getPath()
                  .isEmpty()) {
            domain = domain.substring(0, domain.indexOf('/'));
        }
        if (route.getPort() != null) {
            domain = domain.substring(0, domain.indexOf(':'));
        }
        return domain;
    }

}
