package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v3.processes.Data;
import org.cloudfoundry.client.v3.processes.HealthCheck;
import org.cloudfoundry.client.v3.processes.ReadinessHealthCheck;
import org.cloudfoundry.client.v3.processes.Process;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudProcess;
import com.sap.cloudfoundry.client.facade.domain.HealthCheckType;
import com.sap.cloudfoundry.client.facade.domain.ReadinessHealthCheckType;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudProcess;

@Value.Immutable
public abstract class RawCloudProcess extends RawCloudEntity<CloudProcess> {

    @Value.Parameter
    public abstract Process getProcess();

    @Override
    public CloudProcess derive() {
        Process process = getProcess();
        HealthCheck healthCheck = process.getHealthCheck();
        Integer healthCheckTimeout = null;
        String healthCheckHttpEndpoint = null;
        Integer healthCheckInvocationTimeout = null;
        if (healthCheck.getData() != null) {
            Data healthCheckData = healthCheck.getData();
            healthCheckTimeout = healthCheckData.getTimeout();
            healthCheckInvocationTimeout = healthCheckData.getInvocationTimeout();
            healthCheckHttpEndpoint = healthCheckData.getEndpoint();
        }
        ReadinessHealthCheck readinessHealthCheck = process.getReadinessHealthCheck();
        Integer readinessHealthCheckInvocationTimeout = null;
        String readinessHealthCheckHttpEndpoint = null;
        Integer readinessHealthCheckInterval = null;
        if (readinessHealthCheck.getData() != null) {
            Data readinessHealthCheckData = readinessHealthCheck.getData();
            readinessHealthCheckInvocationTimeout = readinessHealthCheckData.getInvocationTimeout();
            readinessHealthCheckInterval = readinessHealthCheckData.getInterval();
            readinessHealthCheckHttpEndpoint = readinessHealthCheckData.getEndpoint();
        }
        return ImmutableCloudProcess.builder()
                                    .command(process.getCommand())
                                    .instances(process.getInstances())
                                    .memoryInMb(process.getMemoryInMb())
                                    .diskInMb(process.getDiskInMb())
                                    .healthCheckType(HealthCheckType.valueOf(healthCheck.getType()
                                                                                        .getValue()
                                                                                        .toUpperCase()))
                                    .healthCheckHttpEndpoint(healthCheckHttpEndpoint)
                                    .healthCheckTimeout(healthCheckTimeout)
                                    .healthCheckInvocationTimeout(healthCheckInvocationTimeout)
                                    .readinessHealthCheckType(ReadinessHealthCheckType.valueOf(readinessHealthCheck.getType()
                                                                                                                   .getValue()
                                                                                                                   .toUpperCase()))
                                    .readinessHealthCheckHttpEndpoint(readinessHealthCheckHttpEndpoint)
                                    .readinessHealthCheckInvocationTimeout(readinessHealthCheckInvocationTimeout)
                                    .readinessHealthCheckInterval(readinessHealthCheckInterval)
                                    .build();
    }
}
