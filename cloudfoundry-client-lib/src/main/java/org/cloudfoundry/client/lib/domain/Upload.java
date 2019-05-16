package org.cloudfoundry.client.lib.domain;

public class Upload {

    private Status status;
    private ErrorDetails errorDetails;

    // Required by Jackson.
    public Upload() {
    }

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
