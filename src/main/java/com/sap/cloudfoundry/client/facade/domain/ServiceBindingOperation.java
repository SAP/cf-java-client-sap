package com.sap.cloudfoundry.client.facade.domain;

import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.Arrays;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableServiceBindingOperation.class)
@JsonDeserialize(as = ImmutableServiceBindingOperation.class)
public abstract class ServiceBindingOperation {

    public abstract Type getType();

    public abstract State getState();

    @Nullable
    public abstract String getDescription();

    @Nullable
    public abstract LocalDateTime getCreatedAt();

    @Nullable
    public abstract LocalDateTime getUpdatedAt();

    public enum Type {
        CREATE, DELETE;

        public static Type fromString(String value) {
            return Arrays.stream(values())
                         .filter(type -> type.toString()
                                             .equals(value))
                         .findFirst()
                         .orElseThrow(() -> new IllegalArgumentException(MessageFormat.format("Illegal service binding operation type: \"{0}\"",
                                                                                              value)));
        }

        @Override
        public String toString() {
            return name().toLowerCase();
        }
    }

    public enum State {
        INITIAL("initial"), IN_PROGRESS("in progress"), SUCCEEDED("succeeded"), FAILED("failed");

        private final String name;

        State(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        public static State fromString(String value) {
            return Arrays.stream(values())
                         .filter(state -> state.toString()
                                               .equals(value))
                         .findFirst()
                         .orElseThrow(() -> new IllegalArgumentException(MessageFormat.format("Illegal service binding state: \"{0}\"",
                                                                                              value)));
        }
    }

}
