package org.cloudfoundry.client.lib.adapters;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudEvent.Participant;
import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent;
import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent.ImmutableParticipant;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.events.EventEntity;
import org.cloudfoundry.client.v2.events.EventResource;
import org.junit.jupiter.api.Test;

public class RawCloudEventTest {

    private static final String ACTEE_GUID_STRING = "b7c57058-afc0-43c9-afee-64019e850bef";
    private static final String ACTEE_NAME = "foo";
    private static final String ACTEE_TYPE = "app";
    private static final String ACTOR_GUID_STRING = "72c1e48a-9629-4cfe-a64c-f53851d81f61";
    private static final String ACTOR_NAME = "john";
    private static final String ACTOR_TYPE = "user";
    private static final String TIMESTAMP_STRING = "2019-07-03T20:00:46Z";
    private static final String TYPE = "audit.app.create";

    private static final UUID ACTEE_GUID = UUID.fromString(ACTEE_GUID_STRING);
    private static final UUID ACTOR_GUID = UUID.fromString(ACTOR_GUID_STRING);
    private static final Date TIMESTAMP = RawCloudEntityTest.fromZonedDateTime(ZonedDateTime.of(2019, 7, 3, 20, 00, 46, 0, ZoneId.of("Z")));

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedEvent(), buildRawEvent());
    }

    @Test
    public void testDeriveWithEmptyResponse() {
        RawCloudEntityTest.testDerive(buildEmptyExpectedEvent(), buildEmptyRawEvent());
    }

    private CloudEvent buildExpectedEvent() {
        return ImmutableCloudEvent.builder()
            .metadata(RawCloudEntityTest.EXPECTED_METADATA)
            .actee(buildExpectedActee())
            .actor(buildExpectedActor())
            .timestamp(TIMESTAMP)
            .type(TYPE)
            .build();
    }

    private Participant buildExpectedActee() {
        return ImmutableParticipant.builder()
            .guid(ACTEE_GUID)
            .name(ACTEE_NAME)
            .type(ACTEE_TYPE)
            .build();
    }

    private Participant buildExpectedActor() {
        return ImmutableParticipant.builder()
            .guid(ACTOR_GUID)
            .name(ACTOR_NAME)
            .type(ACTOR_TYPE)
            .build();
    }

    private CloudEvent buildEmptyExpectedEvent() {
        return ImmutableCloudEvent.builder()
            .metadata(RawCloudEntityTest.EXPECTED_METADATA)
            .actee(buildEmptyParticipant())
            .actor(buildEmptyParticipant())
            .build();
    }

    private Participant buildEmptyParticipant() {
        return ImmutableParticipant.builder()
            .build();
    }

    private RawCloudEvent buildRawEvent() {
        return ImmutableRawCloudEvent.of(buildTestResource());
    }

    private Resource<EventEntity> buildTestResource() {
        return EventResource.builder()
            .metadata(RawCloudEntityTest.METADATA)
            .entity(buildTestEntity())
            .build();
    }

    private EventEntity buildTestEntity() {
        return EventEntity.builder()
            .actee(ACTEE_GUID_STRING)
            .acteeName(ACTEE_NAME)
            .acteeType(ACTEE_TYPE)
            .actor(ACTOR_GUID_STRING)
            .actorName(ACTOR_NAME)
            .actorType(ACTOR_TYPE)
            .timestamp(TIMESTAMP_STRING)
            .type(TYPE)
            .build();
    }

    private RawCloudEvent buildEmptyRawEvent() {
        return ImmutableRawCloudEvent.of(buildEmptyTestResource());
    }

    private Resource<EventEntity> buildEmptyTestResource() {
        return EventResource.builder()
            .metadata(RawCloudEntityTest.METADATA)
            .entity(buildEmptyTestEntity())
            .build();
    }

    private EventEntity buildEmptyTestEntity() {
        return EventEntity.builder()
            .build();
    }

}
