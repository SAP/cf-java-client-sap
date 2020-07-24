package org.cloudfoundry.client.lib.domain;

import java.util.UUID;

import org.cloudfoundry.client.lib.domain.ImmutableCloudBuild.ImmutableCreatedBy;
import org.cloudfoundry.client.lib.domain.ImmutableCloudBuild.ImmutablePackageInfo;
import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Enclosing
@Value.Immutable
@JsonSerialize(as = ImmutableCloudBuild.class)
@JsonDeserialize(as = ImmutableCloudBuild.class)
public abstract class CloudBuild extends CloudEntity implements Derivable<CloudBuild> {

    @Nullable
    public abstract State getState();

    @Nullable
    public abstract CreatedBy getCreatedBy();

    @Nullable
    public abstract DropletInfo getDropletInfo();

    @Nullable
    public abstract PackageInfo getPackageInfo();

    @Nullable
    public abstract String getError();

    @Override
    public CloudBuild derive() {
        return this;
    }

    public enum State {

        FAILED("FAILED"), STAGED("STAGED"), STAGING("STAGING");

        private final String value;

        State(String value) {
            this.value = value;
        }

        public static State fromString(String value) {
            for (State state : State.values()) {
                if (state.value.equals(value)) {
                    return state;
                }
            }
            throw new IllegalArgumentException("Invalid build state: " + value);
        }

        @Override
        public String toString() {
            return value;
        }

    }

    @Value.Immutable
    @JsonSerialize(as = ImmutablePackageInfo.class)
    @JsonDeserialize(as = ImmutablePackageInfo.class)
    public interface PackageInfo {

        @Nullable
        @Value.Parameter
        UUID getGuid();

    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableCreatedBy.class)
    @JsonDeserialize(as = ImmutableCreatedBy.class)
    public interface CreatedBy {

        @Nullable
        UUID getGuid();

        @Nullable
        String getName();

    }

}
