package org.cloudfoundry.client.lib.domain;

import java.util.List;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceOffering.class)
@JsonDeserialize(as = ImmutableCloudServiceOffering.class)
public interface CloudServiceOffering extends CloudEntity<CloudServiceOffering> {

    @Nullable
    Boolean isActive();

    @Nullable
    Boolean isBindable();

    List<CloudServicePlan> getServicePlans();

    @Nullable
    String getDescription();

    @Nullable
    String getDocUrl();

    @Nullable
    String getExtra();

    @Nullable
    String getInfoUrl();

    @Nullable
    String getProvider();

    @Nullable
    String getUniqueId();

    @Nullable
    String getUrl();

    @Nullable
    String getVersion();

}
