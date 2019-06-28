package org.cloudfoundry.client.lib.domain;

import java.util.Map;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceKey.class)
@JsonDeserialize(as = ImmutableCloudServiceKey.class)
public interface CloudServiceKey extends CloudEntity<CloudServiceKey> {

    Map<String, Object> getCredentials();

    @Nullable
    CloudService getService();

}
