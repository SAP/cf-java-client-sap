package com.sap.cloudfoundry.client.facade.domain;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.AllowNulls;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.domain.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceInstance.class)
@JsonDeserialize(as = ImmutableCloudServiceInstance.class)
public abstract class CloudServiceInstance extends CloudEntity implements Derivable<CloudServiceInstance> {

    @Nullable
    public abstract String getLabel();

    @Nullable
    public abstract String getPlan();

    @Nullable
    public abstract String getProvider();

    @Nullable
    public abstract String getBroker();

    @Nullable
    public abstract String getVersion();

    @AllowNulls
    public abstract Map<String, Object> getCredentials();

    public abstract List<String> getTags();

    @Nullable
    public abstract ServiceInstanceType getType();
    
    @Nullable
    public abstract ServiceOperation getLastOperation();

    public boolean isUserProvided() {
        return getType() != null && getType().equals(ServiceInstanceType.USER_PROVIDED);
    }

    @Override
    public CloudServiceInstance derive() {
        return this;
    }

}
