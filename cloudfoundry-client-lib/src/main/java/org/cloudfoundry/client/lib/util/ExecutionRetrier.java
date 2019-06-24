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

package org.cloudfoundry.client.lib.util;

import org.cloudfoundry.client.lib.exception.ExceptionHandlerFactory;
import org.cloudfoundry.client.lib.exception.GenericExceptionHandler;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public class ExecutionRetrier {

    private static final long DEFAULT_RETRY_COUNT = 3;

    private static final long DEFAULT_WAIT_TIME_BETWEEN_RETRIES_IN_MILLIS = 5000;

    private boolean failSafe;

    private long retryCount = DEFAULT_RETRY_COUNT;

    private long waitTimeBetweenRetriesInMillis = DEFAULT_WAIT_TIME_BETWEEN_RETRIES_IN_MILLIS;

    public <T> T executeWithRetry(Supplier<T> supplier, HttpStatus... httpStatusesToIgnore) {
        Set<HttpStatus> httpStatuses = new HashSet<>();
        httpStatuses.addAll(Arrays.asList(httpStatusesToIgnore));
        for (int i = 1; i < retryCount; i++) {
            try {
                return supplier.get();
            } catch (Exception e) {
                new ExceptionHandlerFactory().getExceptionHandler(e, httpStatuses, failSafe)
                        .handleException(e);
            }
            sleep(waitTimeBetweenRetriesInMillis);
        }
        return executeWithGenericExceptionHandler(supplier);
    }

    public void executeWithRetry(Runnable runnable, HttpStatus... httpStatusesToIgnore) {
        executeWithRetry(() -> {
            runnable.run();
            return null;
        }, httpStatusesToIgnore);
    }

    public ExecutionRetrier failSafe() {
        this.failSafe = true;
        return this;
    }

    public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread()
                    .interrupt();
            throw new IllegalStateException("Interrupted!", e);
        }
    }

    public ExecutionRetrier withRetryCount(long retryCount) {
        this.retryCount = retryCount;
        return this;
    }

    public ExecutionRetrier withWaitTimeBetweenRetriesInMillis(long waitTimeBetweenRetriesInMillis) {
        this.waitTimeBetweenRetriesInMillis = waitTimeBetweenRetriesInMillis;
        return this;
    }

    private <T> T executeWithGenericExceptionHandler(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            new GenericExceptionHandler(failSafe).handleException(e);
        }
        return null;
    }

}
