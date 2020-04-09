package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;
import java.util.Map;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceInstance.class)
@JsonDeserialize(as = ImmutableCloudServiceInstance.class)
public interface CloudServiceInstance extends CloudEntity, Derivable<CloudServiceInstance> {

    @Nullable
    String getLabel();

    @Nullable
    String getPlan();

    @Nullable
    String getProvider();

    @Nullable
    String getVersion();

    Map<String, Object> getCredentials();

    List<String> getTags();

    @Nullable
    ServiceInstanceType getType();

    default boolean isUserProvided() {
        return getType() != null && getType().equals(ServiceInstanceType.USER_PROVIDED);
    }

    @Override
    default CloudServiceInstance derive() {
        return this;
    }

}
