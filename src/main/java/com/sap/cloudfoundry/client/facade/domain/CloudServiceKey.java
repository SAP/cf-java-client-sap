package com.sap.cloudfoundry.client.facade.domain;

import java.util.Map;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.domain.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceKey.class)
@JsonDeserialize(as = ImmutableCloudServiceKey.class)
public abstract class CloudServiceKey extends CloudEntity implements Derivable<CloudServiceKey> {

    public abstract Map<String, Object> getCredentials();

    @Nullable
    public abstract CloudServiceInstance getServiceInstance();

    @Override
    public CloudServiceKey derive() {
        return this;
    }

}
