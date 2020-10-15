package com.sap.cloudfoundry.client.facade.adapters;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v2.applications.ApplicationInstanceInfo;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesResponse;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.ImmutableInstanceInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableInstancesInfo;
import com.sap.cloudfoundry.client.facade.domain.InstanceInfo;
import com.sap.cloudfoundry.client.facade.domain.InstanceState;
import com.sap.cloudfoundry.client.facade.domain.InstancesInfo;

@Value.Immutable
public abstract class RawInstancesInfo extends RawCloudEntity<InstancesInfo> {

    @Value.Parameter
    public abstract ApplicationInstancesResponse getInstancesResponse();

    @Override
    public InstancesInfo derive() {
        ApplicationInstancesResponse instancesResponse = getInstancesResponse();
        return ImmutableInstancesInfo.builder()
                                     .instances(parseInstancesMap(instancesResponse.getInstances()))
                                     .build();
    }

    private static List<InstanceInfo> parseInstancesMap(Map<String, ApplicationInstanceInfo> instances) {
        return instances.entrySet()
                        .stream()
                        .map(RawInstancesInfo::parseInstance)
                        .collect(Collectors.toList());
    }

    private static InstanceInfo parseInstance(Map.Entry<String, ApplicationInstanceInfo> instance) {
        return ImmutableInstanceInfo.builder()
                                    .index(parseIndex(instance))
                                    .state(parseState(instance))
                                    .build();
    }

    private static int parseIndex(Entry<String, ApplicationInstanceInfo> instance) {
        return Integer.parseInt(instance.getKey());
    }

    private static InstanceState parseState(Entry<String, ApplicationInstanceInfo> instance) {
        return InstanceState.valueOfWithDefault(instance.getValue()
                                                        .getState());
    }

}
