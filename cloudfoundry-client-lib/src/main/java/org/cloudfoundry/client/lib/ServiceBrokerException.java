package org.cloudfoundry.client.lib;

import java.text.MessageFormat;

import org.springframework.http.HttpStatus;

public class ServiceBrokerException extends CloudFoundryException {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_SERVICE_BROKER_ERROR_MESSAGE = "Service broker operation failed: {0}";

    public ServiceBrokerException(CloudFoundryException cloudFoundryException) {
        super(cloudFoundryException.getStatusCode(), cloudFoundryException.getStatusText(), cloudFoundryException.getDescription(),
            cloudFoundryException.getCloudFoundryErrorCode(), cloudFoundryException.getCloudFoundryErrorName(), cloudFoundryException);
    }

    public ServiceBrokerException(HttpStatus statusCode) {
        super(statusCode);
    }

    public ServiceBrokerException(HttpStatus statusCode, String statusText) {
        super(statusCode, statusText);
    }

    public ServiceBrokerException(HttpStatus statusCode, String statusText, String description) {
        super(statusCode, statusText, description);
    }

    public ServiceBrokerException(HttpStatus statusCode, String statusText, String description, int cloudFoundryErrorCode,
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
