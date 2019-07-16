package org.cloudfoundry.client.lib.domain;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.AllowNulls;
import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudApplication.class)
@JsonDeserialize(as = ImmutableCloudApplication.class)
public interface CloudApplication extends CloudEntity, Derivable<CloudApplication> {

    enum State {
        UPDATING, STARTED, STOPPED
    }

    @Value.Default
    default int getMemory() {
        return 0;
    }

    @Value.Default
    default int getDiskQuota() {
        return 0;
    }

    @Value.Default
    default int getInstances() {
        return 1;
    }

    @Value.Default
    default int getRunningInstances() {
        return 0;
    }

    @Nullable
    State getState();

    @Nullable
    Staging getStaging();

    @Nullable
    PackageState getPackageState();

    @Nullable
    String getStagingError();

    List<String> getUris();

    List<String> getServices();

    @AllowNulls
    Map<String, String> getEnv();

    @Nullable
    CloudSpace getSpace();

    @Override
    default CloudApplication derive() {
        return this;
    }

}
