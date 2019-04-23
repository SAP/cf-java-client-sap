package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceBroker.class)
@JsonDeserialize(as = ImmutableCloudServiceBroker.class)
public interface CloudServiceBroker extends CloudEntity {

    @Nullable
    String getUsername();

    @Nullable
    String getPassword();

    @Nullable
    String getUrl();

    @Nullable
    String getSpaceGuid();

}
