package org.cloudfoundry.client.lib.domain;

import java.util.Date;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudMetadata.class)
@JsonDeserialize(as = ImmutableCloudMetadata.class)
public interface CloudMetadata {

    @Nullable
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
