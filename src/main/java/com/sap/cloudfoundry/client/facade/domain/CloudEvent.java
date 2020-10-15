package com.sap.cloudfoundry.client.facade.domain;

import java.util.Date;
import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudEvent.ImmutableParticipant;
import com.sap.cloudfoundry.client.facade.domain.annotation.Nullable;

@Value.Enclosing
@Value.Immutable
@JsonSerialize(as = ImmutableCloudEvent.class)
@JsonDeserialize(as = ImmutableCloudEvent.class)
public abstract class CloudEvent extends CloudEntity implements Derivable<CloudEvent> {

    @Nullable
    public abstract String getType();

    @Nullable
    public abstract Participant getActor();

    @Nullable
    public abstract Participant getActee();

    @Nullable
    public abstract Date getTimestamp();

    @Override
    public CloudEvent derive() {
        return this;
    }

    @Value.Immutable
    @JsonSerialize(as = ImmutableParticipant.class)
    @JsonDeserialize(as = ImmutableParticipant.class)
    public interface Participant {

        @Nullable
        UUID getGuid();

        @Nullable
        String getName();

        @Nullable
        String getType();

    }

}
