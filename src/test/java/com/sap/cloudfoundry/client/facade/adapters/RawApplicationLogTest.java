package com.sap.cloudfoundry.client.facade.adapters;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.cloudfoundry.doppler.ContainerMetric;
import org.cloudfoundry.doppler.CounterEvent;
import org.cloudfoundry.doppler.Envelope;
import org.cloudfoundry.doppler.EventType;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.doppler.MessageType;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;
import com.sap.cloudfoundry.client.facade.domain.ImmutableApplicationLog;

class RawApplicationLogTest {

    private static final String ORIGIN = "application";
    private static final String APPLICATION_GUID = "9dda5446-34c9-11ed-a261-0242ac120002";
    private static final String COUNTER_EVENT_NAME = "counter";
    private static final String LOG_MESSAGE_TEXT = "Some message here";
    private static final String INDEX = "0";
    private static final String SOURCE_INSTANCE = "source-instance";
    private static final long TIMESTAMP = 1397132691L;
    private static final String SOURCE_TYPE = "source-type";
    private static final double CPU_PERCENTAGE = 54.54;
    private static final long DISK_BYTES = 123123123L;
    private static final int INSTANCE_INDEX = 0;
    private static final long MEMORY_BYTES = 123123123L;
    private static final long DELTA = 12123123L;

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
                                      .sourceId(SOURCE_INSTANCE)
                                      .sourceName(SOURCE_TYPE)
                                      .build();
    }

    private RawApplicationLog buildActualApplicationLog() {
        return ImmutableRawApplicationLog.builder()
                                         .envelope(Envelope.builder()
                                                           .index(INDEX)
                                                           .containerMetric(ContainerMetric.builder()
                                                                                           .applicationId(APPLICATION_GUID)
                                                                                           .cpuPercentage(CPU_PERCENTAGE)
                                                                                           .diskBytes(DISK_BYTES)
                                                                                           .instanceIndex(INSTANCE_INDEX)
                                                                                           .memoryBytes(MEMORY_BYTES)
                                                                                           .build())
                                                           .counterEvent(CounterEvent.builder()
                                                                                     .name(COUNTER_EVENT_NAME)
                                                                                     .delta(DELTA)
                                                                                     .build())
                                                           .eventType(EventType.LOG_MESSAGE)
                                                           .logMessage(LogMessage.builder()
                                                                                 .message(LOG_MESSAGE_TEXT)
                                                                                 .applicationId(APPLICATION_GUID)
                                                                                 .sourceInstance(SOURCE_INSTANCE)
                                                                                 .timestamp(TIMESTAMP)
                                                                                 .sourceType(SOURCE_TYPE)
                                                                                 .messageType(MessageType.OUT)
                                                                                 .build())
                                                           .origin(ORIGIN)
                                                           .build())
                                         .build();
    }

}
