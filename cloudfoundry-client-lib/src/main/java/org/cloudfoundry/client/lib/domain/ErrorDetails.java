package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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