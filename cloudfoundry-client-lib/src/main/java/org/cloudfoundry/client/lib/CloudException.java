package org.cloudfoundry.client.lib;

@SuppressWarnings("serial")
public class CloudException extends RuntimeException {

    public CloudException(Throwable cause) {
        super(cause);
    }

    public CloudException(String message) {
        super(message);
    }
}
