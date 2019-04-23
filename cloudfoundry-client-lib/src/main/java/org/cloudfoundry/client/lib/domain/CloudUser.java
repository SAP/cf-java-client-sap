package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudUser.class)
@JsonDeserialize(as = ImmutableCloudUser.class)
public interface CloudUser extends CloudEntity {

    @Nullable
    String getDefaultSpaceGuid();

    @Nullable
    Boolean isActive();

    @Nullable
    Boolean isAdmin();

}
