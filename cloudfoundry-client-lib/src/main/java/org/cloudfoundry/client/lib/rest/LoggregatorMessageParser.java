package org.cloudfoundry.client.lib.rest;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ImmutableApplicationLog;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.TextFormat;

import loggregator.LogMessages;

public class LoggregatorMessageParser {

    public ApplicationLog parseMessage(byte[] rawMessage) throws InvalidProtocolBufferException {
        LogMessages.Message message = LogMessages.Message.parseFrom(rawMessage);

        return createApplicationLog(message);
    }

    public ApplicationLog parseMessage(String messageString) throws TextFormat.ParseException {
        LogMessages.Message.Builder builder = LogMessages.Message.newBuilder();
        TextFormat.merge(messageString, builder);
        LogMessages.Message message = builder.build();

        return createApplicationLog(message);
    }

    private ApplicationLog createApplicationLog(LogMessages.Message message) {
        ApplicationLog.MessageType messageType = message.getMessageType() == LogMessages.Message.MessageType.OUT
            ? ApplicationLog.MessageType.STDOUT
            : ApplicationLog.MessageType.STDERR;
        Date timestamp = new Date(TimeUnit.NANOSECONDS.toMillis(message.getTimestamp()));

        return ImmutableApplicationLog.builder()
                                      .applicationGuid(message.getAppId())
                                      .message(message.getMessage()
                                                      .toStringUtf8())
                                      .timestamp(timestamp)
                                      .messageType(messageType)
                                      .sourceName(message.getSourceName())
                                      .sourceId(message.getSourceId())
                                      .build();
    }
}
