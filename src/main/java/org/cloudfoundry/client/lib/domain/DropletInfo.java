package org.cloudfoundry.client.lib.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import java.util.UUID;

@Value.Immutable
@JsonSerialize(as = ImmutableDropletInfo.class)
@JsonDeserialize(as = ImmutableDropletInfo.class)
public interface DropletInfo {

    @Nullable
    @Value.Parameter
    UUID getGuid();

    @Nullable
    @Value.Parameter
    UUID getPackageGuid();
}