/*
 * Copyright 2009-2013 the original author or authors.
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

package org.cloudfoundry.client.lib;

import org.springframework.http.HttpStatus;

@SuppressWarnings("serial")
public class CloudOperationException extends RuntimeException {

    private HttpStatus statusCode;
    private final String statusText;
    private final String description;

    public CloudOperationException(HttpStatus statusCode) {
        this(statusCode, statusCode.getReasonPhrase());
    }

    public CloudOperationException(HttpStatus statusCode, String statusText) {
        this(statusCode, statusText, null);
    }

    public CloudOperationException(HttpStatus statusCode, String statusText, String description) {
        this(statusCode, statusText, description, null);
    }

    public CloudOperationException(HttpStatus statusCode, String statusText, String description, Throwable cause) {
        super(getExceptionMessage(statusCode, statusText, description), cause);
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.description = description;
    }

    private static String getExceptionMessage(HttpStatus statusCode, String statusText, String description) {
        if (description != null) {
            return String.format("%d %s: %s", statusCode.value(), statusText, description);
        }
        return String.format("%d %s", statusCode.value(), statusText);
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public String getStatusText() {
        return statusText;
    }

    public String getDescription() {
        return description;
    }

}
