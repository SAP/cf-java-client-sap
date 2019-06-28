package org.cloudfoundry.client.lib.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudOrganization.class)
@JsonDeserialize(as = ImmutableCloudOrganization.class)
public interface CloudOrganization extends CloudEntity<CloudOrganization> {

}
