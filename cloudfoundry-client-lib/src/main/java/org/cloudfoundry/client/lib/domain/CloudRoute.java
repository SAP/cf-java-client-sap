package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudRoute.class)
@JsonDeserialize(as = ImmutableCloudRoute.class)
public interface CloudRoute extends CloudEntity, Derivable<CloudRoute> {

    @Value.Default
    default int getAppsUsingRoute() {
        return 0;
    }

    @Nullable
    CloudDomain getDomain();

    @Nullable
    String getHost();

    @Override
    default String getName() {
        return getHost() + "." + getDomain();
    }

    default boolean isUsed() {
        return getAppsUsingRoute() > 0;
    }

    @Override
    default CloudRoute derive() {
        return this;
    }

}
