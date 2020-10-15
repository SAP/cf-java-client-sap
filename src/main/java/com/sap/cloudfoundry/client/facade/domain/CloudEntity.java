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

package com.sap.cloudfoundry.client.facade.domain;

import java.util.UUID;

import org.cloudfoundry.client.v3.Metadata;

import com.sap.cloudfoundry.client.facade.domain.annotation.Nullable;

/**
 * Do not extend {@code Derivable<T>} in this interface. It is tempting, because all of its children have the same implementation, but
 * implementing the {@code derive()} method here leads to this bug: https://github.com/immutables/immutables/issues/1045
 *
 */
public abstract class CloudEntity {

    @Nullable
    public abstract String getName();

    @Nullable
    public abstract CloudMetadata getMetadata();

    @Nullable
    public abstract Metadata getV3Metadata();

    public UUID getGuid() {
        return getMetadata().getGuid();
    }

}
