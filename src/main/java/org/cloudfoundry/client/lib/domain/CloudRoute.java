package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudRoute.class)
@JsonDeserialize(as = ImmutableCloudRoute.class)
public abstract class CloudRoute extends CloudEntity implements Derivable<CloudRoute> {

    @Value.Default
    public int getAppsUsingRoute() {
        return 0;
    }

    @Value.Default
    public boolean hasServiceUsingRoute() {
        return false;
    }

    @Nullable
    public abstract CloudDomain getDomain();

    @Nullable
    public abstract String getHost();

    @Nullable
    public abstract String getPath();

    @Override
    public String getName() {
        return getHost() + "." + getDomain();
    }

    public boolean isUsed() {
        return getAppsUsingRoute() > 0 || hasServiceUsingRoute();
    }

    @Override
    public CloudRoute derive() {
        return this;
    }

}
