package org.cloudfoundry.client.lib.domain;

import java.util.UUID;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableUploadToken.class)
@JsonDeserialize(as = ImmutableUploadToken.class)
public interface UploadToken {

    String getToken();

    UUID getPackageGuid();

}
