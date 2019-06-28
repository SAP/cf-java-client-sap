package org.cloudfoundry.client.lib.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudQuota.class)
@JsonDeserialize(as = ImmutableCloudQuota.class)
public interface CloudQuota extends CloudEntity<CloudQuota> {

}
