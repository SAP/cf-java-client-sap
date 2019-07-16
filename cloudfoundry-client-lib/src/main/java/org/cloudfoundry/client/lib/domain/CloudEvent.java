package org.cloudfoundry.client.lib.domain;

import java.util.Date;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent.ImmutableParticipant;
import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Enclosing
@Value.Immutable
@JsonSerialize(as = ImmutableCloudEvent.class)
@JsonDeserialize(as = ImmutableCloudEvent.class)
public interface CloudEvent extends CloudEntity, Derivable<CloudEvent> {

    @Nullable
    String getType();

    @Nullable
    Participant getActor();

    @Nullable
    Participant getActee();

    @Nullable
    Date getTimestamp();

    @Override
    default CloudEvent derive() {
        return this;
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableParticipant.class)
    @JsonDeserialize(as = ImmutableParticipant.class)
    interface Participant {

        @Nullable
        UUID getGuid();

        @Nullable
        String getName();

        @Nullable
        String getType();

    }

}
