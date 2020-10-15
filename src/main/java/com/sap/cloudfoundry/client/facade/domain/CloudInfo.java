package com.sap.cloudfoundry.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudInfo.class)
@JsonDeserialize(as = ImmutableCloudInfo.class)
public interface CloudInfo {

    @Nullable
    String getAuthorizationEndpoint();

    @Nullable
    String getLoggingEndpoint();

    @Nullable
    String getBuild();

    @Nullable
    String getDescription();

    @Nullable
    String getName();

    @Nullable
    String getUser();

    @Nullable
    String getSupport();

    @Nullable
    String getVersion();

}
