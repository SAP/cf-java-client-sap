package com.sap.cloudfoundry.client.facade.adapters;

import java.util.List;

import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.routemappings.RouteMappingEntity;
import org.cloudfoundry.client.v2.routes.RouteEntity;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.Derivable;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRoute;

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
                                  .hasServiceUsingRoute(entity.getServiceInstanceId() != null)
                                  .build();
    }

}
