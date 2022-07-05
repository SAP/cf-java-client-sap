package com.sap.cloudfoundry.client.facade.adapters;

import java.util.List;
import java.util.stream.Collectors;

import org.cloudfoundry.client.v3.applications.GetApplicationProcessStatisticsResponse;
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
    public abstract GetApplicationProcessStatisticsResponse getProcessStatisticsResponse();

    @Override
    public InstancesInfo derive() {
        var processStats = getProcessStatisticsResponse();
        return ImmutableInstancesInfo.builder()
                                     .instances(parseProcessStatistics(processStats.getResources()))
                                     .build();
    }

    private static List<InstanceInfo> parseProcessStatistics(List<ProcessStatisticsResource> stats) {
        return stats.stream()
                    .map(RawInstancesInfo::parseProcessStatistic)
                    .collect(Collectors.toList());
    }

    private static InstanceInfo parseProcessStatistic(ProcessStatisticsResource statsResource) {
        return ImmutableInstanceInfo.builder()
                                    .index(statsResource.getIndex())
                                    .state(InstanceState.valueOfWithDefault(statsResource.getState()))
                                    .build();
    }

}
