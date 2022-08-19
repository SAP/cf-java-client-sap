package com.sap.cloudfoundry.client.facade.domain;

public enum LifecycleType {

    BUILDPACK, DOCKER, KPACK;

    public String toString() {
        return this.name().toLowerCase();
    }

}
