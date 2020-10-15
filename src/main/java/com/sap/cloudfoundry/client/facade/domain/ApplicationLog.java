package com.sap.cloudfoundry.client.facade.domain;

import java.util.Date;

import org.immutables.value.Value;

@Value.Immutable
public abstract class ApplicationLog implements Comparable<ApplicationLog> {

    public enum MessageType {
        STDOUT, STDERR
    }

    public abstract String getApplicationGuid();

    public abstract String getMessage();

    public abstract Date getTimestamp();

    public abstract MessageType getMessageType();

    public abstract String getSourceId();

    public abstract String getSourceName();

    @Override
    public int compareTo(ApplicationLog other) {
        return getTimestamp().compareTo(other.getTimestamp());
    }

    @Override
    public String toString() {
        return String.format("%s [%s] %s (%s, %s)", getApplicationGuid(), getTimestamp(), getMessage(), getMessageType(),
                getSourceName());
    }
}
