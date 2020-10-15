package com.sap.cloudfoundry.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;

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
