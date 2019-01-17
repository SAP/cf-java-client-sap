/*
 * Copyright 2009-2012 the original author or authors.
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

package org.cloudfoundry.client.lib.domain;

/**
 * @author Scott Frederick
 */
public class CloudJob extends CloudEntity {

    private final ErrorDetails errorDetails;

    private final Status status;

    public CloudJob(Meta meta, Status status) {
        this(meta, status, null);
    }

    public CloudJob(Meta meta, Status status, ErrorDetails errorDetails) {
        super(meta);
        this.status = status;
        this.errorDetails = errorDetails;
    }

    public ErrorDetails getErrorDetails() {
        return errorDetails;
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        FAILED("failed"), FINISHED("finished"), QUEUED("queued"), RUNNING("running");

        private final String status;

        Status(String status) {
            this.status = status;
        }

        public static Status getEnum(String status) {
            for (Status value : Status.values()) {
                if (value.status.equals(status)) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Invalid Status value: " + status);
        }

        @Override
        public String toString() {
            return status;
        }
    }
}
