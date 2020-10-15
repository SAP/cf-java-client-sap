package com.sap.cloudfoundry.client.facade.domain;

import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.domain.annotation.Nullable;

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