package org.cloudfoundry.client.lib.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudDomain.class)
@JsonDeserialize(as = ImmutableCloudDomain.class)
public interface CloudDomain extends CloudEntity<CloudDomain> {

}
