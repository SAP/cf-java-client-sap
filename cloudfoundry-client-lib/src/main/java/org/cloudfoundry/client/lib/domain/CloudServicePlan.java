package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServicePlan.class)
@JsonDeserialize(as = ImmutableCloudServicePlan.class)
public interface CloudServicePlan extends CloudEntity, Derivable<CloudServicePlan> {

    @Nullable
    String getDescription();

    @Nullable
    String getExtra();

    @Nullable
    String getUniqueId();

    @Nullable
    Boolean isFree();

    @Nullable
    Boolean isPublic();

    @Override
    default CloudServicePlan derive() {
        return this;
    }

}
