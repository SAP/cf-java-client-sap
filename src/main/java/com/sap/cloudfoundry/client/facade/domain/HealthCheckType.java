package com.sap.cloudfoundry.client.facade.domain;

public enum HealthCheckType {
    HTTP, PORT, PROCESS;

    @Override
    public String toString() {
        return this.name().toLowerCase();
    }
}
