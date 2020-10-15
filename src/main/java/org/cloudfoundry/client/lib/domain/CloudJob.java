package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudJob.class)
@JsonDeserialize(as = ImmutableCloudJob.class)
public abstract class CloudJob extends CloudEntity implements Derivable<CloudJob> {

    @Nullable
    public abstract Status getStatus();

    @Nullable
    public abstract ErrorDetails getErrorDetails();

    @Override
    public CloudJob derive() {
        return this;
    }

    public enum Status {

        FAILED("failed"), FINISHED("finished"), QUEUED("queued"), RUNNING("running");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public static Status fromString(String value) {
            for (Status status : Status.values()) {
                if (status.value.equals(value)) {
                    return status;
                }
            }
            throw new IllegalArgumentException("Invalid job status: " + value);
        }

        @Override
        public String toString() {
            return value;
        }

    }

}
