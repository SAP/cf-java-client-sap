package com.sap.cloudfoundry.client.facade.domain;

public enum InstanceState {
    CRASHED, DOWN, RUNNING, STARTING, UNKNOWN;

    public static InstanceState valueOfWithDefault(String s) {
        try {
            return InstanceState.valueOf(s);
        } catch (IllegalArgumentException e) {
            return InstanceState.UNKNOWN;
        }
    }
}
