package com.sap.cloudfoundry.client.facade.adapters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@Value.Enclosing
@JsonDeserialize(as = ImmutableApplicationLogEntity.class)
public interface ApplicationLogEntity {

    String getTimestamp();

    @JsonProperty("source_id")
    String getSourceId();

    @JsonProperty("instance_id")
    String getInstanceId();

    Map<String, String> getTags();

    @JsonProperty("log")
    LogBody getLogBody();

    @Value.Immutable
    @JsonDeserialize(as = ImmutableApplicationLogEntity.ImmutableLogBody.class)
    interface LogBody {

        @JsonProperty("payload")
        String getMessage();

        @JsonProperty("type")
        String getMessageType();
    }
}
