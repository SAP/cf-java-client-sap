package org.cloudfoundry.client.lib.domain;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceInstance.class)
@JsonDeserialize(as = ImmutableCloudServiceInstance.class)
public interface CloudServiceInstance extends CloudEntity {

    List<CloudServiceBinding> getBindings();

    Map<String, Object> getCredentials();

    @Nullable
    String getDashboardUrl();

    @Nullable
    CloudService getService();

    @Nullable
    CloudServicePlan getServicePlan();

    @Nullable
    String getType();

}
