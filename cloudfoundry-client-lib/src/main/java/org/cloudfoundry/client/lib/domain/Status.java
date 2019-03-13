package org.cloudfoundry.client.lib.domain;

public enum Status {
    AWAITING_UPLOAD("awaiting_upload"), COPYING("copying"), EXPIRED("expired"), PROCESSING_UPLOAD("processing_upload"), READY("ready"), FAILED("failed");

    private final String status;

    Status(String status) {
        this.status = status;
    }

    public static Status getEnum(String status) {
        for (Status value : Status.values()) {
            if (value.status.equals(status)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid Status value: " + status);
    }

    @Override
    public String toString() {
        return status;
    }
}