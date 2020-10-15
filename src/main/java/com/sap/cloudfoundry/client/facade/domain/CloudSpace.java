package com.sap.cloudfoundry.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudSpace.class)
@JsonDeserialize(as = ImmutableCloudSpace.class)
public abstract class CloudSpace extends CloudEntity implements Derivable<CloudSpace> {

    @Nullable
    public abstract CloudOrganization getOrganization();

    @Override
    public CloudSpace derive() {
        return this;
    }

}
