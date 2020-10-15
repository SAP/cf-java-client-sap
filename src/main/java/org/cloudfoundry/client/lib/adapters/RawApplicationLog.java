package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.domain.ImmutableApplicationLog;
import org.cloudfoundry.doppler.Envelope;
import org.cloudfoundry.doppler.LogMessage;
import org.cloudfoundry.doppler.MessageType;
import org.immutables.value.Value;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Value.Immutable
public abstract class RawApplicationLog implements Derivable<ApplicationLog> {

    @Value.Parameter
    public abstract Envelope getEnvelope();

    @Override
    public ApplicationLog derive() {
        Envelope envelope = getEnvelope();
        LogMessage logMessage = envelope.getLogMessage();
        return ImmutableApplicationLog.builder()
                                      .applicationGuid(logMessage.getApplicationId())
                                      .message(logMessage.getMessage())
                                      .timestamp(fromLogTimestamp(logMessage))
                                      .messageType(fromLogMessageType(logMessage))
                                      .sourceId(logMessage.getSourceInstance())
                                      .sourceName(logMessage.getSourceType())
                                      .build();
    }

    private static Date fromLogTimestamp(LogMessage logMessage) {
        return new Date(TimeUnit.NANOSECONDS.toMillis(logMessage.getTimestamp()));
    }

    private static ApplicationLog.MessageType fromLogMessageType(LogMessage logMessage) {
        return logMessage.getMessageType() == MessageType.OUT ? ApplicationLog.MessageType.STDOUT :
                ApplicationLog.MessageType.STDERR;
    }
}
