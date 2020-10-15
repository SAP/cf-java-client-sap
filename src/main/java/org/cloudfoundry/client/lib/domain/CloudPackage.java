package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.ImmutableCloudPackage.ImmutableChecksum;
import org.cloudfoundry.client.lib.domain.ImmutableCloudPackage.ImmutableData;
import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Enclosing
@Value.Immutable
@JsonSerialize(as = ImmutableCloudPackage.class)
@JsonDeserialize(as = ImmutableCloudPackage.class)
public abstract class CloudPackage extends CloudEntity implements Derivable<CloudPackage> {

    @Nullable
    public abstract Type getType();

    @Nullable
    public abstract Data getData();

    @Nullable
    public abstract Status getStatus();

    @Override
    public CloudPackage derive() {
        return this;
    }

    public enum Type {
        BITS, DOCKER,
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableData.class)
    @JsonDeserialize(as = ImmutableData.class)
    public interface Data {

        @Nullable
        Checksum getChecksum();

        @Nullable
        String getError();

    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableChecksum.class)
    @JsonDeserialize(as = ImmutableChecksum.class)
    public interface Checksum {

        @Nullable
        String getAlgorithm();

        @Nullable
        String getValue();

    }

}
