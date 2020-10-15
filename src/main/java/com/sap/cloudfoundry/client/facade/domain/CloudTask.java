package com.sap.cloudfoundry.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudTask.ImmutableLimits;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudTask.ImmutableResult;
import com.sap.cloudfoundry.client.facade.domain.annotation.Nullable;

@Value.Enclosing
@Value.Immutable
@JsonSerialize(as = ImmutableCloudTask.class)
@JsonDeserialize(as = ImmutableCloudTask.class)
public abstract class CloudTask extends CloudEntity implements Derivable<CloudTask> {

    @Nullable
    public abstract String getCommand();

    @Nullable
    public abstract Limits getLimits();

    @Nullable
    public abstract Result getResult();

    @Nullable
    public abstract State getState();

    @Override
    public CloudTask derive() {
        return this;
    }

    public enum State {
        PENDING, RUNNING, SUCCEEDED, CANCELING, FAILED
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableResult.class)
    @JsonDeserialize(as = ImmutableResult.class)
    public interface Result {

        @Nullable
        @Value.Parameter
        String getFailureReason();

    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableLimits.class)
    @JsonDeserialize(as = ImmutableLimits.class)
    public interface Limits {

        @Nullable
        Integer getDisk();

        @Nullable
        Integer getMemory();

    }

}
