package com.sap.cloudfoundry.client.facade.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;
import com.sap.cloudfoundry.client.facade.SkipNulls;
import org.immutables.value.Value;

import java.util.List;

@Value.Immutable
@JsonSerialize(as = ImmutableStaging.class)
@JsonDeserialize(as = ImmutableStaging.class)
public interface Staging {
    /**
     * @return The buildpacks, or empty to use the default buildpack detected based on application content
     */
    @SkipNulls
    List<String> getBuildpacks();

    /**
     * @return The start command to use
     */
    @Nullable
    String getCommand();

    /**
     * @return Raw, free-form information regarding a detected buildpack, or null if no detected buildpack was resolved. For example, if the
     *         application is stopped, the detected buildpack may be null.
     */
    @Nullable
    String getDetectedBuildpack();

    /**
     * @return the health check timeout value
     */
    @Nullable
    Integer getHealthCheckTimeout();

    /**
     * @return health check type
     */
    @Nullable
    String getHealthCheckType();

    /**
     * @return health check http endpoint value
     */
    @Nullable
    String getHealthCheckHttpEndpoint();

    /**
     * @return readiness health check interval
     */
    @Nullable
    Integer getReadinessHealthCheckInterval();

    /**
     * @return readiness health check timeout
     */
    @Nullable
    Integer getReadinessHealthCheckTimeout();

    /**
     * @return readiness health check type
     */
    @Nullable
    String getReadinessHealthCheckType();

    /**
     * @return readiness health check http endpoint
     */
    @Nullable
    String getReadinessHealthCheckHttpEndpoint();

    /**
     * @return boolean value to see if ssh is enabled
     */
    @Nullable
    Boolean isSshEnabled();

    /**
     * @return the stack to use when staging the application, or null to use the default stack
     */
    @Nullable
    String getStackName();

    @Nullable
    DockerInfo getDockerInfo();

    @Nullable
    Integer getInvocationTimeout();

    default String getBuildpack() {
        return getBuildpacks().isEmpty() ? null : getBuildpacks().get(0);
    }

}
