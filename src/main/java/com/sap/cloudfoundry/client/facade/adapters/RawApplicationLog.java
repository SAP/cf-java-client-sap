package com.sap.cloudfoundry.client.facade.adapters;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;
import com.sap.cloudfoundry.client.facade.domain.Derivable;
import com.sap.cloudfoundry.client.facade.domain.ImmutableApplicationLog;

@Value.Immutable
public abstract class RawApplicationLog implements Derivable<ApplicationLog> {

    @Value.Parameter
    public abstract ApplicationLogEntity getLog();

    @Override
    public ApplicationLog derive() {
        ApplicationLogEntity log = getLog();
        return ImmutableApplicationLog.builder()
                                      .applicationGuid(log.getSourceId())
                                      .message(decodeLogPayload(log.getLogBody()
                                                                   .getMessage()))
                                      .timestamp(fromLogTimestamp(log.getTimestamp()))
                                      .messageType(fromLogMessageType(log.getLogBody()
                                                                         .getMessageType()))
                                      .sourceName(log.getTags()
                                                     .get("source_type"))
                                      .build();
    }

    private static String decodeLogPayload(String base64Encoded) {
        var result = Base64.getDecoder()
                           .decode(base64Encoded.getBytes(StandardCharsets.UTF_8));
        return new String(result, StandardCharsets.UTF_8);
    }

    private static Date fromLogTimestamp(String timestamp) {
        return new Date(TimeUnit.NANOSECONDS.toMillis(Long.parseLong(timestamp)));
    }

    private static ApplicationLog.MessageType fromLogMessageType(String messageType) {
        return "OUT".equals(messageType) ? ApplicationLog.MessageType.STDOUT : ApplicationLog.MessageType.STDERR;
    }
}
