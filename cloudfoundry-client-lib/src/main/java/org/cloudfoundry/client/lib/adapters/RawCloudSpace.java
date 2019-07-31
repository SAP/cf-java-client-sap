package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.domain.ImmutableCloudSpace;
import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.spaces.SpaceEntity;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawCloudSpace extends RawCloudEntity<CloudSpace> {

    @Value.Parameter
    public abstract Resource<SpaceEntity> getResource();

    @Nullable
    public abstract Derivable<CloudOrganization> getOrganization();

    @Override
    public CloudSpace derive() {
        Resource<SpaceEntity> resource = getResource();
        SpaceEntity entity = resource.getEntity();
        return ImmutableCloudSpace.builder()
                                  .metadata(parseResourceMetadata(resource))
                                  .name(entity.getName())
                                  .organization(deriveFromNullable(getOrganization()))
                                  .build();
    }

}
