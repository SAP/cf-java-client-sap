package com.sap.cloudfoundry.client.facade.domain;

public enum ServicePlanVisibility {

    PUBLIC, ADMIN, ORGANIZATION;

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
