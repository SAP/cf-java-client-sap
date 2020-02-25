package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;
import java.util.Map;

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

    Map<String, Object> getCredentials();

    List<String> getTags();

    @Nullable
    ServiceInstanceType getType();

    default boolean isUserProvided() {
        return getType() != null && getType().equals(ServiceInstanceType.USER_PROVIDED);
    }

    @Override
    default CloudService derive() {
        return this;
    }

}
