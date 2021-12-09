package com.sap.cloudfoundry.client.facade.domain;

import org.cloudfoundry.client.v3.jobs.JobState;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudAsyncJob.class)
@JsonDeserialize(as = ImmutableCloudAsyncJob.class)
public abstract class CloudAsyncJob extends CloudEntity implements Derivable<CloudAsyncJob> {

    public abstract JobState getState();

    @Nullable
    public abstract String getOperation();

    @Nullable
    public abstract String getWarnings();

    @Nullable
    public abstract String getErrors();

    @Override
    public CloudAsyncJob derive() {
        return this;
    }

}
