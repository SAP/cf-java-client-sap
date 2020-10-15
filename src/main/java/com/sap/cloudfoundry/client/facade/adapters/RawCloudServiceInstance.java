package com.sap.cloudfoundry.client.facade.adapters;

import java.util.Optional;

import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.serviceinstances.UnionServiceInstanceEntity;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanEntity;
import org.cloudfoundry.client.v2.services.ServiceEntity;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ServiceInstanceType;
import com.sap.cloudfoundry.client.facade.domain.ServiceOperation;
import com.sap.cloudfoundry.client.facade.domain.annotation.Nullable;

@Value.Immutable
public abstract class RawCloudServiceInstance extends RawCloudEntity<CloudServiceInstance> {

    @Value.Parameter
    public abstract Resource<UnionServiceInstanceEntity> getResource();

    @Nullable
    public abstract Resource<ServicePlanEntity> getServicePlanResource();

    @Nullable
    public abstract Resource<ServiceEntity> getServiceResource();

    @Override
    public CloudServiceInstance derive() {
        Resource<UnionServiceInstanceEntity> resource = getResource();
        UnionServiceInstanceEntity entity = resource.getEntity();
        return ImmutableCloudServiceInstance.builder()
                                            .metadata(parseResourceMetadata(resource))
                                            .name(entity.getName())
                                            .plan(parsePlan(getServicePlanResource()))
                                            .label(parseLabel(getServiceResource()))
                                            .type(ServiceInstanceType.valueOfWithDefault(entity.getType()))
                                            .tags(entity.getTags())
                                            .credentials(entity.getCredentials())
                                            .lastOperation(ServiceOperation.fromLastOperation(entity.getLastOperation()))
                                            .build();
    }

    private static String parsePlan(Resource<ServicePlanEntity> resource) {
        return Optional.ofNullable(resource)
                       .map(Resource::getEntity)
                       .map(ServicePlanEntity::getName)
                       .orElse(null);
    }

    private static String parseLabel(Resource<ServiceEntity> serviceResource) {
        return Optional.ofNullable(serviceResource)
                       .map(Resource::getEntity)
                       .map(ServiceEntity::getLabel)
                       .orElse(null);
    }

}
