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
public interface CloudPackage extends CloudEntity {

    @Nullable
    Type getType();

    @Nullable
    Data getData();

    @Nullable
    Status getStatus();

    enum Type {
        BITS, DOCKER,
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableData.class)
    @JsonDeserialize(as = ImmutableData.class)
    interface Data {

        @Nullable
        Checksum getChecksum();

        @Nullable
        String getError();

    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableChecksum.class)
    @JsonDeserialize(as = ImmutableChecksum.class)
    interface Checksum {

        @Nullable
        String getAlgorithm();

        @Nullable
        String getValue();

    }

}
