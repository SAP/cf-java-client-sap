package com.sap.cloudfoundry.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.domain.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableDockerInfo.class)
@JsonDeserialize(as = ImmutableDockerInfo.class)
public interface DockerInfo {

    String getImage();

    @Nullable
    DockerCredentials getCredentials();

}
