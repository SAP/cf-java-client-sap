package com.sap.cloudfoundry.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableUpload.class)
@JsonDeserialize(as = ImmutableUpload.class)
public interface Upload {

    Status getStatus();

    @Nullable
    ErrorDetails getErrorDetails();

}
