package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.stacks.StackEntity;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudStack;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudStack;

@Value.Immutable
public abstract class RawCloudStack extends RawCloudEntity<CloudStack> {

    @Value.Parameter
    public abstract Resource<StackEntity> getResource();

    @Override
    public CloudStack derive() {
        Resource<StackEntity> resource = getResource();
        StackEntity entity = resource.getEntity();
        return ImmutableCloudStack.builder()
                                  .metadata(parseResourceMetadata(resource))
                                  .name(entity.getName())
                                  .description(entity.getDescription())
                                  .build();
    }

}
