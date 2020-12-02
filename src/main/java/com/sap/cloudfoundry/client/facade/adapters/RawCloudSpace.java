package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v3.spaces.Space;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.Nullable;
import com.sap.cloudfoundry.client.facade.domain.CloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.Derivable;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudSpace;

@Value.Immutable
public abstract class RawCloudSpace extends RawCloudEntity<CloudSpace> {

    public abstract Space getSpace();

    @Nullable
    public abstract Derivable<CloudOrganization> getOrganization();

    @Override
    public CloudSpace derive() {
        Space space = getSpace();
        return ImmutableCloudSpace.builder()
                                  .metadata(parseResourceMetadata(space))
                                  .name(space.getName())
                                  .organization(deriveFromNullable(getOrganization()))
                                  .build();
    }

}
