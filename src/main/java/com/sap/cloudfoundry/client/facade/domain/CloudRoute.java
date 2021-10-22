package com.sap.cloudfoundry.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudRoute.class)
@JsonDeserialize(as = ImmutableCloudRoute.class)
public abstract class CloudRoute extends CloudEntity implements Derivable<CloudRoute> {

    @Value.Default
    public int getAppsUsingRoute() {
        return 0;
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

    @Override
    public CloudRoute derive() {
        return this;
    }

}
