package com.sap.cloudfoundry.client.facade.domain;

import java.util.Date;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.domain.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudMetadata.class)
@JsonDeserialize(as = ImmutableCloudMetadata.class)
public interface CloudMetadata {

    @Nullable
    @Value.Parameter
    UUID getGuid();

    @Nullable
    Date getCreatedAt();

    @Nullable
    Date getUpdatedAt();

    @Nullable
    String getUrl();

    static CloudMetadata defaultMetadata() {
        return ImmutableCloudMetadata.builder()
                                     .build();
    }

}
