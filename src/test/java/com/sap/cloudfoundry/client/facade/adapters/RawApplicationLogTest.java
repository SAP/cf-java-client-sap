package com.sap.cloudfoundry.client.facade.adapters;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.adapters.ImmutableApplicationLogEntity.ImmutableLogBody;
import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;
import com.sap.cloudfoundry.client.facade.domain.ImmutableApplicationLog;

class RawApplicationLogTest {

    private static final String APPLICATION_GUID = "9dda5446-34c9-11ed-a261-0242ac120002";
    private static final String LOG_MESSAGE_TEXT = "Some message here";
    private static final String LOG_MESSAGE_TYPE = "OUT";
    private static final String INDEX = "0";
    private static final long TIMESTAMP = 1397132691L;
    private static final String SOURCE_TYPE = "CELL";

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedApplicationLog(), buildActualApplicationLog());
    }

    private ApplicationLog buildExpectedApplicationLog() {
        return ImmutableApplicationLog.builder()
                                      .applicationGuid(APPLICATION_GUID)
                                      .message(LOG_MESSAGE_TEXT)
                                      .timestamp(new Date(TimeUnit.NANOSECONDS.toMillis(TIMESTAMP)))
                                      .messageType(ApplicationLog.MessageType.STDOUT)
                                      .sourceName(SOURCE_TYPE)
                                      .build();
    }

    private RawApplicationLog buildActualApplicationLog() {
        return ImmutableRawApplicationLog.builder()
                                         .log(ImmutableApplicationLogEntity.builder()
                                                                           .putTag("source_type", SOURCE_TYPE)
                                                                           .sourceId(APPLICATION_GUID)
                                                                           .instanceId(INDEX)
                                                                           .timestamp(Long.toString(TIMESTAMP))
                                                                           .logBody(ImmutableLogBody.builder()
                                                                                   .message(encodeBase64(LOG_MESSAGE_TEXT))
                                                                                   .messageType(LOG_MESSAGE_TYPE)
                                                                                   .build())
                                                                           .build())
                                         .build();
    }

    private static String encodeBase64(String origin) {
        return Base64.getEncoder()
                     .encodeToString(origin.getBytes(StandardCharsets.UTF_8));
    }

}
