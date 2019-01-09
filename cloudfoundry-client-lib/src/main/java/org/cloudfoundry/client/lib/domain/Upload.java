package org.cloudfoundry.client.lib.domain;

public class Upload {

    private final Status status;
    private final ErrorDetails errorDetails;

    public Upload(Status status, ErrorDetails errorDetails) {
        this.status = status;
        this.errorDetails = errorDetails;
    }

    public Status getStatus() {
        return status;
    }

    public ErrorDetails getErrorDetails() {
        return errorDetails;
    }

}
