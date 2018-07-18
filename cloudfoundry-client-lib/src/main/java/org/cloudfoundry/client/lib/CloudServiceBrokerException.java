package org.cloudfoundry.client.lib;

import java.text.MessageFormat;

import org.springframework.http.HttpStatus;

public class CloudServiceBrokerException extends CloudOperationException {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_SERVICE_BROKER_ERROR_MESSAGE = "Service broker operation failed: {0}";

    public CloudServiceBrokerException(CloudOperationException cloudOperationException) {
        super(cloudOperationException.getStatusCode(), cloudOperationException.getStatusText(), cloudOperationException.getDescription(),
            cloudOperationException.getCloudFoundryErrorCode(), cloudOperationException.getCloudFoundryErrorName(), cloudOperationException);
    }

    public CloudServiceBrokerException(HttpStatus statusCode) {
        super(statusCode);
    }

    public CloudServiceBrokerException(HttpStatus statusCode, String statusText) {
        super(statusCode, statusText);
    }

    public CloudServiceBrokerException(HttpStatus statusCode, String statusText, String description) {
        super(statusCode, statusText, description);
    }

    public CloudServiceBrokerException(HttpStatus statusCode, String statusText, String description, int cloudFoundryErrorCode,
        String cloudFoundryErrorName) {
        super(statusCode, statusText, description, cloudFoundryErrorCode, cloudFoundryErrorName);
    }

    @Override
    public String getMessage() {
        return decorateExceptionMessage(super.getMessage());
    }

    private String decorateExceptionMessage(String exceptionMessage) {
        return MessageFormat.format(DEFAULT_SERVICE_BROKER_ERROR_MESSAGE, exceptionMessage);
    }
}
