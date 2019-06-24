/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.client.lib.exception;

import org.springframework.http.HttpStatus;

import java.text.MessageFormat;

public class CloudServiceBrokerException extends CloudOperationException {

    private static final String DEFAULT_SERVICE_BROKER_ERROR_MESSAGE = "Service broker operation failed: {0}";

    private static final long serialVersionUID = 1L;

    public CloudServiceBrokerException(CloudOperationException cloudOperationException) {
        super(cloudOperationException.getStatusCode(), cloudOperationException.getStatusText(),
                cloudOperationException.getDescription(),
                cloudOperationException);
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

    @Override
    public String getMessage() {
        return decorateExceptionMessage(super.getMessage());
    }

    private String decorateExceptionMessage(String exceptionMessage) {
        return MessageFormat.format(DEFAULT_SERVICE_BROKER_ERROR_MESSAGE, exceptionMessage);
    }
}
