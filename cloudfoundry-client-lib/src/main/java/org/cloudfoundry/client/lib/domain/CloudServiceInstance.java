package org.cloudfoundry.client.lib.domain;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.cloudfoundry.client.v3.Metadata;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceInstance.class)
@JsonDeserialize(as = ImmutableCloudServiceInstance.class)
public interface CloudServiceInstance extends CloudEntity, Derivable<CloudServiceInstance> {

    List<CloudServiceBinding> getBindings();

    Map<String, Object> getCredentials();

    @Nullable
    String getDashboardUrl();

    @Nullable
    CloudService getService();

    @Nullable
    String getType();

    @Nullable
    Metadata getV3Metadata();

    @Override
    default CloudServiceInstance derive() {
        return this;
    }

}
