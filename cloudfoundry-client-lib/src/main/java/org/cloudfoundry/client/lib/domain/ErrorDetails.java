package org.cloudfoundry.client.lib.domain;

public class ErrorDetails {

    private long code;
    private String description;
    private String errorCode;

    // Required by Jackson.
    public ErrorDetails() {
    }

    public ErrorDetails(long code, String description, String errorCode) {
        this.code = code;
        this.description = description;
        this.errorCode = errorCode;
    }

    public long getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getErrorCode() {
        return errorCode;
    }
}