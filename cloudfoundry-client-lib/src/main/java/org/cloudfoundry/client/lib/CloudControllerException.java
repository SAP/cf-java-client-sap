package org.cloudfoundry.client.lib;

import java.text.MessageFormat;

import org.springframework.http.HttpStatus;

public class CloudControllerException extends CloudFoundryException {

    private static final long serialVersionUID = 1L;
    private static final String DEFAULT_CLOUD_CONTROLLER_ERROR_MESSAGE = "Controller operation failed: {0}";

    public CloudControllerException(CloudFoundryException cloudFoundryException) {
        super(cloudFoundryException.getStatusCode(), decorateExceptionMessage(cloudFoundryException.getStatusText()),
            cloudFoundryException.getDescription(), cloudFoundryException.getCloudFoundryErrorCode(),
            cloudFoundryException.getCloudFoundryErrorName());
    }

    public CloudControllerException(HttpStatus statusCode) {
        super(statusCode, statusCode.getReasonPhrase());
    }

    public CloudControllerException(HttpStatus statusCode, String statusText) {
        super(statusCode, decorateExceptionMessage(statusText), null);
    }

    public CloudControllerException(HttpStatus statusCode, String statusText, String description) {
        super(statusCode, decorateExceptionMessage(statusText), description, DEFAULT_CF_ERROR_CODE, null);
    }

    public CloudControllerException(HttpStatus statusCode, String statusText, String description, int cloudFoundryErrorCode,
        String cloudFoundryErrorName) {
        super(statusCode, decorateExceptionMessage(statusText), description, cloudFoundryErrorCode, cloudFoundryErrorName);
    }

    private static String decorateExceptionMessage(String exceptionMessage) {
        return MessageFormat.format(DEFAULT_CLOUD_CONTROLLER_ERROR_MESSAGE, exceptionMessage);
    }

}
