package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanEntity;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudServicePlan;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServicePlan;

@Value.Immutable
public abstract class RawCloudServicePlan extends RawCloudEntity<CloudServicePlan> {

    @Value.Parameter
    public abstract Resource<ServicePlanEntity> getResource();

    @Override
    public CloudServicePlan derive() {
        Resource<ServicePlanEntity> resource = getResource();
        ServicePlanEntity entity = resource.getEntity();
        return ImmutableCloudServicePlan.builder()
                                        .metadata(parseResourceMetadata(resource))
                                        .name(entity.getName())
                                        .description(entity.getDescription())
                                        .extra(entity.getExtra())
                                        .uniqueId(entity.getUniqueId())
                                        .isPublic(entity.getPubliclyVisible())
                                        .isFree(entity.getFree())
                                        .build();
    }

}
