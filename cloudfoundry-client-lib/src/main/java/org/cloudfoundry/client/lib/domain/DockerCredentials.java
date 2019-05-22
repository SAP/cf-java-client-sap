package org.cloudfoundry.client.lib.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableDockerCredentials.class)
@JsonDeserialize(as = ImmutableDockerCredentials.class)
public interface DockerCredentials {

    String getUsername();

    String getPassword();

}
