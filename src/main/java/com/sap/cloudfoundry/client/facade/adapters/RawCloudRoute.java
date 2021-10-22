package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v3.routes.Route;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.Derivable;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRoute;

@Value.Immutable
public abstract class RawCloudRoute extends RawCloudEntity<CloudRoute> {

    public abstract Route getRoute();

    public abstract Derivable<CloudDomain> getDomain();

    @Override
    public CloudRoute derive() {
        Route route = getRoute();
        return ImmutableCloudRoute.builder()
                                  .metadata(parseResourceMetadata(route))
                                  .appsUsingRoute(route.getDestinations()
                                                       .size())
                                  .host(route.getHost())
                                  .domain(getDomain().derive())
                                  .path(route.getPath())
                                  .build();
    }

}
