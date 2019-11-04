package org.cloudfoundry.client.lib.adapters;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.domain.ImmutableCloudRoute;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.routemappings.RouteMappingEntity;
import org.cloudfoundry.client.v3.routes.Route;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawCloudRoute extends RawCloudEntity<CloudRoute> {

    public abstract Route getResource();

    public abstract List<Resource<RouteMappingEntity>> getRouteMappingResources();

    public abstract Derivable<CloudDomain> getDomain();

    @Override
    public CloudRoute derive() {
        Route route = getResource();
        return ImmutableCloudRoute.builder()
                                  .metadata(parseResourceMetadata(route))
                                  .appsUsingRoute(getRouteMappingResources().size())
                                  .host(route.getHost())
                                  .domain(getDomain().derive())
                                  .build();
    }

}
