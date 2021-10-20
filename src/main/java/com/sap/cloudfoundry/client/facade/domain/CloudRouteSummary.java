package com.sap.cloudfoundry.client.facade.domain;

import java.util.Objects;
import java.util.UUID;

import org.immutables.value.Value;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudRouteSummary.class)
@JsonDeserialize(as = ImmutableCloudRouteSummary.class)
public abstract class CloudRouteSummary implements Derivable<CloudRouteSummary> {

    @Nullable
    public abstract UUID getGuid();

    public abstract String getDomain();

    @Nullable
    public abstract UUID getDomainGuid();

    @Nullable
    public abstract String getHost();

    @Nullable
    public abstract String getPath();

    @Nullable
    public abstract Integer getPort();

    @Override
    public CloudRouteSummary derive() {
        return this;
    }

    public String toUriString() {
        StringBuilder uriBuilder = new StringBuilder("");
        if (StringUtils.hasLength(getHost())) {
            uriBuilder.append(getHost())
                      .append(".");
        }
        uriBuilder.append(getDomain());
        if (Objects.nonNull(getPort())) {
            uriBuilder.append(":")
                      .append(getPort());
        }
        if (StringUtils.hasLength(getPath())) {
            uriBuilder.append(getPath());
        }
        return uriBuilder.toString();
    }

    @Override
    public String toString() {
        return toUriString();
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (!(object instanceof CloudRouteSummary)) {
            return false;
        }
        return this.describesTheSameUri((CloudRouteSummary) object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.getDomain(), this.getHost(), this.getPath(), this.getPort());
    }

    public boolean containsFullSummary() {
        return getGuid() != null && getDomainGuid() != null;
    }

    public boolean describesTheSameUri(CloudRouteSummary another) {
        return areEmptyOrEqual(this.getHost(), another.getHost()) && Objects.equals(this.getDomain(), another.getDomain())
            && areEmptyOrEqual(this.getPath(), another.getPath()) && Objects.equals(this.getPort(), another.getPort());
    }

    public boolean describesTheSameUri(CloudRoute route) {
        return Objects.equals(route.getDomain()
                                   .getName(),
                              this.getDomain())
            && areEmptyOrEqual(route.getHost(), this.getHost()) && areEmptyOrEqual(route.getPath(), this.getPath());
    }

    private boolean areEmptyOrEqual(String value, String otherValue) {
        if (StringUtils.isEmpty(value) && StringUtils.isEmpty(otherValue)) {
            return true;
        }

        return Objects.equals(value, otherValue);
    }
}
