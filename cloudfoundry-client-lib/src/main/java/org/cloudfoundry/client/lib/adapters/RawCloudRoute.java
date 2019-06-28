package org.cloudfoundry.client.lib.adapters;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.domain.ImmutableCloudRoute;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.routemappings.RouteMappingEntity;
import org.cloudfoundry.client.v2.routes.RouteEntity;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawCloudRoute extends RawCloudEntity<CloudRoute> {

    public abstract Resource<RouteEntity> getResource();

    public abstract List<Resource<RouteMappingEntity>> getRouteMappingResources();

    public abstract Derivable<CloudDomain> getDomain();

    @Override
    public CloudRoute derive() {
        Resource<RouteEntity> resource = getResource();
        RouteEntity entity = resource.getEntity();
        return ImmutableCloudRoute.builder()
            .metadata(parseResourceMetadata(resource))
            .appsUsingRoute(getRouteMappingResources().size())
            .host(entity.getHost())
            .domain(getDomain().derive())
            .build();
    }

}
