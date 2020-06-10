package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServicePlan.class)
@JsonDeserialize(as = ImmutableCloudServicePlan.class)
public abstract class CloudServicePlan extends CloudEntity implements Derivable<CloudServicePlan> {

    @Nullable
    public abstract String getDescription();

    @Nullable
    public abstract String getExtra();

    @Nullable
    public abstract String getUniqueId();

    @Nullable
    public abstract Boolean isFree();

    @Nullable
    public abstract Boolean isPublic();

    @Override
    public CloudServicePlan derive() {
        return this;
    }

}
