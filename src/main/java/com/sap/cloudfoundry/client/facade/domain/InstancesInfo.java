package com.sap.cloudfoundry.client.facade.domain;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableInstancesInfo.class)
@JsonDeserialize(as = ImmutableInstancesInfo.class)
public interface InstancesInfo {

    List<InstanceInfo> getInstances();

}
