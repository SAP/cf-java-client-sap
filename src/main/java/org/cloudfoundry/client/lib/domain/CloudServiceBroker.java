package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceBroker.class)
@JsonDeserialize(as = ImmutableCloudServiceBroker.class)
public abstract class CloudServiceBroker extends CloudEntity implements Derivable<CloudServiceBroker> {

    @Nullable
    public abstract String getUsername();

    @Nullable
    public abstract String getPassword();

    @Nullable
    public abstract String getUrl();

    @Nullable
    public abstract String getSpaceGuid();

    @Override
    public CloudServiceBroker derive() {
        return this;
    }

}
