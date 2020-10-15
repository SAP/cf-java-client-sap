package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudStack.class)
@JsonDeserialize(as = ImmutableCloudStack.class)
public abstract class CloudStack extends CloudEntity implements Derivable<CloudStack> {

    @Nullable
    public abstract String getDescription();

    @Override
    public CloudStack derive() {
        return this;
    }

}
