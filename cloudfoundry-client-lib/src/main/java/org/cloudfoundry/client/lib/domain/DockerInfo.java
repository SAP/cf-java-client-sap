package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableDockerInfo.class)
@JsonDeserialize(as = ImmutableDockerInfo.class)
public interface DockerInfo {

    String getImage();

    @Nullable
    DockerCredentials getCredentials();

}
