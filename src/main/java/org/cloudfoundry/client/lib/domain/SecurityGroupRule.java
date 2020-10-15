package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableSecurityGroupRule.class)
@JsonDeserialize(as = ImmutableSecurityGroupRule.class)
public interface SecurityGroupRule {

    String getProtocol();

    String getPorts();

    String getDestination();

    @Nullable
    Boolean getLog();

    @Nullable
    Integer getType();

    @Nullable
    Integer getCode();

}
