package com.sap.cloudfoundry.client.facade.domain;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;

import java.util.Objects;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudRoute.class)
@JsonDeserialize(as = ImmutableCloudRoute.class)
public abstract class CloudRoute extends CloudEntity implements Derivable<CloudRoute> {

    @Value.Default
    public int getAppsUsingRoute() {
        return 0;
    }

    public abstract CloudDomain getDomain();

    @Nullable
    public abstract String getHost();

    @Nullable
    public abstract String getPath();

    @Nullable
    public abstract Integer getPort();

    public abstract String getUrl();

    @Override
    public CloudRoute derive() {
        return this;
    }

    @Override
    public String toString() {
        return getUrl();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDomain().getName(), getHost(), getPath(), getPort());
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof CloudRoute)) {
            return false;
        }
        var otherRoute = (CloudRoute) object;
        var thisDomain = getDomain().getName();
        var otherDomain = otherRoute.getDomain()
                                    .getName();
        return thisDomain.equals(otherDomain)
                && areEmptyOrEqual(getHost(), otherRoute.getHost())
                && areEmptyOrEqual(getPath(), otherRoute.getPath())
                && Objects.equals(getPort(), otherRoute.getPort());
    }

    private static boolean areEmptyOrEqual(String lhs, String rhs) {
        if (lhs == null || lhs.isEmpty()) {
            return rhs == null || rhs.isEmpty();
        }
        return lhs.equals(rhs);
    }

}
