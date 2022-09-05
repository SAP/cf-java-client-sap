package com.sap.cloudfoundry.client.facade.domain;

import java.util.Map;

import org.cloudfoundry.AllowNulls;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceKey.class)
@JsonDeserialize(as = ImmutableCloudServiceKey.class)
public abstract class CloudServiceKey extends CloudEntity implements Derivable<CloudServiceKey> {

    @Nullable
    @AllowNulls
    public abstract Map<String, Object> getCredentials();

    @Nullable
    public abstract CloudServiceInstance getServiceInstance();

    @Override
    public CloudServiceKey derive() {
        return this;
    }

}
