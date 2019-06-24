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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;
import java.util.Set;

public class CloudOperationExceptionHandler implements ExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudOperationExceptionHandler.class);

    private Set<HttpStatus> httpStatuses;

    private boolean isFailSafe;

    public CloudOperationExceptionHandler(boolean isFailSafe, Set<HttpStatus> httpStatuses) {
        this.httpStatuses = httpStatuses;
        this.isFailSafe = isFailSafe;
    }

    @Override
    public void handleException(Exception e) {
        CloudOperationException cloudOperationException = (CloudOperationException) e;
        if (!shouldIgnoreException(cloudOperationException, httpStatuses)) {
            throw cloudOperationException;
        }
        LOGGER.warn(MessageFormat.format("Retrying failed request with status: {0} and message: {1}",
                cloudOperationException.getStatusCode(), cloudOperationException.getMessage()));
    }

    private boolean shouldIgnoreException(CloudOperationException e, Set<HttpStatus> httpStatusesToIgnore) {
        if (isFailSafe) {
            return true;
        }
        for (HttpStatus status : httpStatusesToIgnore) {
            if (e.getStatusCode()
                    .equals(status)) {
                return true;
            }
        }
        return e.getStatusCode()
                .equals(HttpStatus.INTERNAL_SERVER_ERROR)
                || e.getStatusCode()
                .equals(HttpStatus.BAD_GATEWAY)
                || e.getStatusCode()
                .equals(HttpStatus.SERVICE_UNAVAILABLE)
                || e.getStatusCode()
                .equals(HttpStatus.GATEWAY_TIMEOUT)
                || e.getStatusCode()
                .equals(HttpStatus.REQUEST_TIMEOUT);
    }
}
