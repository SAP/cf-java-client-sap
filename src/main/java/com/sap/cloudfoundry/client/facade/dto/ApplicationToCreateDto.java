package com.sap.cloudfoundry.client.facade.dto;

import java.util.Map;
import java.util.Set;

import org.cloudfoundry.client.v3.Metadata;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.Staging;

@Value.Immutable
@JsonSerialize(as = ImmutableApplicationToCreateDto.class)
@JsonDeserialize(as = ImmutableApplicationToCreateDto.class)
public interface ApplicationToCreateDto {

    String getName();

    @Nullable
    Staging getStaging();

    @Nullable
    Integer getDiskQuotaInMb();

    @Nullable
    Integer getMemoryInMb();

    @Nullable
    Metadata getMetadata();

    @Nullable
    Set<CloudRoute> getRoutes();

    @Nullable
    Map<String, String> getEnv();

}
