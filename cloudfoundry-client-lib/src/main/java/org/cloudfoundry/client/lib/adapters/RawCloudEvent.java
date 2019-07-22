package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudEvent.Participant;
import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent;
import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent.ImmutableParticipant;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.events.EventEntity;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawCloudEvent extends RawCloudEntity<CloudEvent> {

    @Value.Parameter
    public abstract Resource<EventEntity> getResource();

    @Override
    public CloudEvent derive() {
        Resource<EventEntity> resource = getResource();
        EventEntity entity = resource.getEntity();
        return ImmutableCloudEvent.builder()
            .metadata(parseResourceMetadata(resource))
            .actee(parseActee(entity))
            .actor(parseActor(entity))
            .timestamp(parseNullableDate(entity.getTimestamp()))
            .type(entity.getType())
            .build();
    }

    private static Participant parseActee(EventEntity entity) {
        return ImmutableParticipant.builder()
            .guid(parseNullableGuid(entity.getActee()))
            .name(entity.getActeeName())
            .type(entity.getActeeType())
            .build();
    }

    private static Participant parseActor(EventEntity entity) {
        return ImmutableParticipant.builder()
            .guid(parseNullableGuid(entity.getActor()))
            .name(entity.getActorName())
            .type(entity.getActorType())
            .build();
    }

}
