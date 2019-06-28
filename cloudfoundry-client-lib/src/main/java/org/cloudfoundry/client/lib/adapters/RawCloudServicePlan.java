package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServicePlan;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanEntity;
import org.immutables.value.Value;

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
