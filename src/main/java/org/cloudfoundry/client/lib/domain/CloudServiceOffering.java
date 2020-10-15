package org.cloudfoundry.client.lib.domain;

import java.util.List;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceOffering.class)
@JsonDeserialize(as = ImmutableCloudServiceOffering.class)
public abstract class CloudServiceOffering extends CloudEntity implements Derivable<CloudServiceOffering> {

    @Nullable
    public abstract Boolean isActive();

    @Nullable
    public abstract Boolean isBindable();

    public abstract List<CloudServicePlan> getServicePlans();

    @Nullable
    public abstract String getDescription();

    @Nullable
    public abstract String getDocUrl();

    @Nullable
    public abstract String getExtra();

    @Nullable
    public abstract String getInfoUrl();

    @Nullable
    public abstract String getProvider();
    
    @Nullable
    public abstract String getBrokerName();

    @Nullable
    public abstract String getUniqueId();

    @Nullable
    public abstract String getUrl();

    @Nullable
    public abstract String getVersion();

    @Override
    public CloudServiceOffering derive() {
        return this;
    }

}
