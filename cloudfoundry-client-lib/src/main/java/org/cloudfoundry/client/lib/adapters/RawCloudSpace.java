package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.domain.ImmutableCloudSpace;
import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.cloudfoundry.client.v3.spaces.Space;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawCloudSpace extends RawCloudEntity<CloudSpace> {

    @Value.Parameter
    public abstract Space getResource();

    @Nullable
    public abstract Derivable<CloudOrganization> getOrganization();

    @Override
    public CloudSpace derive() {
        return ImmutableCloudSpace.builder()
                                  .metadata(parseResourceMetadata(getResource()))
                                  .name(getResource().getName())
                                  .organization(deriveFromNullable(getOrganization()))
                                  .build();
    }

}
