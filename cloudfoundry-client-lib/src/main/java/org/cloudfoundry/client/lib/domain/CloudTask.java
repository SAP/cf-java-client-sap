package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.ImmutableCloudTask.ImmutableLimits;
import org.cloudfoundry.client.lib.domain.ImmutableCloudTask.ImmutableResult;
import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Enclosing
@Value.Immutable
@JsonSerialize(as = ImmutableCloudTask.class)
@JsonDeserialize(as = ImmutableCloudTask.class)
public interface CloudTask extends CloudEntity, Derivable<CloudTask> {

    @Nullable
    String getCommand();

    @Nullable
    Limits getLimits();

    @Nullable
    Result getResult();

    @Nullable
    State getState();

    @Override
    default CloudTask derive() {
        return this;
    }

    enum State {
        PENDING, RUNNING, SUCCEEDED, CANCELING, FAILED
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableResult.class)
    @JsonDeserialize(as = ImmutableResult.class)
    interface Result {

        @Nullable
        @Value.Parameter
        String getFailureReason();

    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableLimits.class)
    @JsonDeserialize(as = ImmutableLimits.class)
    interface Limits {

        @Nullable
        Integer getDisk();

        @Nullable
        Integer getMemory();

    }

}
