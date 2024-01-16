package com.sap.cloudfoundry.client.facade.domain;

public enum HealthCheckType {
    HTTP, PORT, PROCESS, @Deprecated NONE;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
