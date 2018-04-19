package org.cloudfoundry.client.lib;

import java.text.MessageFormat;

import org.springframework.http.HttpStatus;

public class ServiceBrokerException extends CloudFoundryException {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_SERVICE_BROKER_ERROR_MESSAGE = "Service broker operation failed: {0}";

    public ServiceBrokerException(CloudFoundryException cloudFoundryException) {
        super(cloudFoundryException.getStatusCode(), decorateExceptionMessage(cloudFoundryException.getStatusText()),
            cloudFoundryException.getDescription(), cloudFoundryException.getCloudFoundryErrorCode(),
            cloudFoundryException.getCloudFoundryErrorName());
    }

    public ServiceBrokerException(HttpStatus statusCode) {
        super(statusCode, statusCode.getReasonPhrase());
    }

    public ServiceBrokerException(HttpStatus statusCode, String statusText) {
        super(statusCode, decorateExceptionMessage(statusText), null);
    }

    public ServiceBrokerException(HttpStatus statusCode, String statusText, String description) {
        super(statusCode, decorateExceptionMessage(statusText), description, DEFAULT_CF_ERROR_CODE, null);
    }

    public ServiceBrokerException(HttpStatus statusCode, String statusText, String description, int cloudFoundryErrorCode,
        String cloudFoundryErrorName) {
        super(statusCode, decorateExceptionMessage(statusText), description, cloudFoundryErrorCode, cloudFoundryErrorName);
    }

    private static String decorateExceptionMessage(String exceptionMessage) {
        return MessageFormat.format(DEFAULT_SERVICE_BROKER_ERROR_MESSAGE, exceptionMessage);
    }
}
