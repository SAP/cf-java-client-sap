package com.sap.cloudfoundry.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.domain.annotation.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableErrorDetails.class)
@JsonDeserialize(as = ImmutableErrorDetails.class)
public interface ErrorDetails {

    @Value.Default
    default long getCode() {
        return 0;
    }

    @Nullable
    String getDescription();

    @Nullable
    String getErrorCode();

}