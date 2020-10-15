package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableUpload.class)
@JsonDeserialize(as = ImmutableUpload.class)
public interface Upload {

    Status getStatus();

    @Nullable
    ErrorDetails getErrorDetails();

}
