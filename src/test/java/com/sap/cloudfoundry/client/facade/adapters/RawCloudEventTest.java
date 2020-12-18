package com.sap.cloudfoundry.client.facade.adapters;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.v3.auditevents.AuditEventActor;
import org.cloudfoundry.client.v3.auditevents.AuditEventResource;
import org.cloudfoundry.client.v3.auditevents.AuditEventTarget;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudEvent;
import com.sap.cloudfoundry.client.facade.domain.CloudEvent.Participant;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudEvent;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudEvent.ImmutableParticipant;

public class RawCloudEventTest {

    private static final String TARGET_GUID_STRING = "b7c57058-afc0-43c9-afee-64019e850bef";
    private static final String TARGET_NAME = "foo";
    private static final String TARGET_TYPE = "app";
    private static final String ACTOR_GUID_STRING = "72c1e48a-9629-4cfe-a64c-f53851d81f61";
    private static final String ACTOR_NAME = "john";
    private static final String ACTOR_TYPE = "user";
    private static final String TIMESTAMP_STRING = "2019-07-03T20:00:46Z";
    private static final String TYPE = "audit.app.create";

    private static final UUID TARGET_GUID = UUID.fromString(TARGET_GUID_STRING);
    private static final UUID ACTOR_GUID = UUID.fromString(ACTOR_GUID_STRING);
    private static final Date TIMESTAMP = RawCloudEntityTest.fromZonedDateTime(ZonedDateTime.of(2019, 7, 3, 20, 0, 46, 0, ZoneId.of("Z")));

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
                                  .metadata(ImmutableCloudMetadata.builder()
                                                                  .guid(RawCloudEntityTest.GUID)
                                                                  .createdAt(TIMESTAMP)
                                                                  .updatedAt(RawCloudEntityTest.UPDATED_AT)
                                                                  .build())
                                  .target(buildExpectedTarget())
                                  .actor(buildExpectedActor())
                                  .type(TYPE)
                                  .build();
    }

    private Participant buildExpectedTarget() {
        return ImmutableParticipant.builder()
                                   .guid(TARGET_GUID)
                                   .name(TARGET_NAME)
                                   .type(TARGET_TYPE)
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
                                  .metadata(ImmutableCloudMetadata.builder()
                                                                  .guid(RawCloudEntityTest.GUID)
                                                                  .createdAt(TIMESTAMP)
                                                                  .updatedAt(RawCloudEntityTest.UPDATED_AT)
                                                                  .build())
                                  .target(buildEmptyParticipant())
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

    private AuditEventResource buildTestResource() {
        return AuditEventResource.builder()
                                 .id(RawCloudEntityTest.GUID_STRING)
                                 .createdAt(TIMESTAMP_STRING)
                                 .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                 .type(TYPE)
                                 .auditEventActor(AuditEventActor.builder()
                                                                .id(ACTOR_GUID_STRING)
                                                                .name(ACTOR_NAME)
                                                                .type(ACTOR_TYPE)
                                                                .build())
                                 .auditEventTarget(AuditEventTarget.builder()
                                                                   .id(TARGET_GUID_STRING)
                                                                   .type(TARGET_TYPE)
                                                                   .name(TARGET_NAME)
                                                                   .build())
                                 .build();
    }

    private RawCloudEvent buildEmptyRawEvent() {
        return ImmutableRawCloudEvent.of(buildEmptyTestResource());
    }

    private AuditEventResource buildEmptyTestResource() {
        return AuditEventResource.builder()
                                 .id(RawCloudEntityTest.GUID_STRING)
                                 .createdAt(TIMESTAMP_STRING)
                                 .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                 .build();
    }

}
