package com.sap.cloudfoundry.client.facade.adapters;

import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v3.applications.GetApplicationProcessStatisticsResponse;
import org.cloudfoundry.client.v3.processes.ProcessState;
import org.cloudfoundry.client.v3.processes.ProcessStatisticsResource;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.ImmutableInstanceInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableInstancesInfo;
import com.sap.cloudfoundry.client.facade.domain.InstanceInfo;
import com.sap.cloudfoundry.client.facade.domain.InstanceState;
import com.sap.cloudfoundry.client.facade.domain.InstancesInfo;

@Value.Immutable
public abstract class RawInstancesInfo extends RawCloudEntity<InstancesInfo> {

    @Value.Parameter
    public abstract GetApplicationProcessStatisticsResponse getApplicationProcessStatisticsResponse();

    @Override
    public InstancesInfo derive() {
        GetApplicationProcessStatisticsResponse applicationProcessStatisticsResponse = getApplicationProcessStatisticsResponse();
        return ImmutableInstancesInfo.builder()
                                     .instances(parseInstanceResources(applicationProcessStatisticsResponse.getResources()))
                                     .build();
    }

    private static List<InstanceInfo> parseInstanceResources(List<ProcessStatisticsResource> processStatisticsResources) {
        return processStatisticsResources.stream()
                                         .map(RawInstancesInfo::parseInstance)
                                         .collect(Collectors.toList());
    }

    private static InstanceInfo parseInstance(ProcessStatisticsResource processStatisticsResource) {
        return ImmutableInstanceInfo.builder()
                                    .index(processStatisticsResource.getIndex())
                                    .state(parseState(processStatisticsResource.getState()))
                                    .build();
    }

    private static InstanceState parseState(ProcessState processState) {
        return InstanceState.valueOfWithDefault(processState.getValue());
    }

}
