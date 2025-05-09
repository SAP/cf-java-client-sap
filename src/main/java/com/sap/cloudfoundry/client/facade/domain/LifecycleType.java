package com.sap.cloudfoundry.client.facade.domain;

public enum LifecycleType {

    BUILDPACK, DOCKER, KPACK, CNB;

    public String toString() {
        return this.name()
                   .toLowerCase();
    }

}
