package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.cloudfoundry.client.v3.Metadata;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudService.class)
@JsonDeserialize(as = ImmutableCloudService.class)
public interface CloudService extends CloudEntity, Derivable<CloudService> {

    @Nullable
    String getLabel();

    @Nullable
    String getPlan();

    @Nullable
    String getProvider();

    @Nullable
    String getVersion();

    @Nullable
    Metadata getV3Metadata();

    default boolean isUserProvided() {
        return getPlan() == null && getProvider() == null && getVersion() == null;
    }

    @Override
    default CloudService derive() {
        return this;
    }

}
