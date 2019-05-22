package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableStaging.class)
@JsonDeserialize(as = ImmutableStaging.class)
public interface Staging {

    /**
     * @return The buildpack url, or null to use the default buildpack detected based on application content
     */
    @Nullable
    String getBuildpackUrl();

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
     * @return boolean value to see if ssh is enabled
     */
    @Nullable
    Boolean isSshEnabled();

    /**
     * @return the stack to use when staging the application, or null to use the default stack
     */
    @Nullable
    String getStack();

    @Nullable
    DockerInfo getDockerInfo();

}
