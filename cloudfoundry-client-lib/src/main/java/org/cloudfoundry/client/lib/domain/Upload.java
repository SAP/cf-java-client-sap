package org.cloudfoundry.client.lib.domain;

import org.cloudfoundry.client.lib.domain.CloudJob.ErrorDetails;
import org.cloudfoundry.client.lib.domain.CloudJob.Status;

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
