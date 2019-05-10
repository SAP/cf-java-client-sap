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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.util.Assert;

/**
 * JSON Object that contains details of a list of {@link org.cloudfoundry.client.lib.domain.CloudResource}s.
 *
 * @author Phillip Webb
 */
@JsonSerialize(using = CloudResources.Serializer.class)
@JsonDeserialize(using = CloudResources.Deserializer.class)
public class CloudResources {

    private static final String HEX_CHARS = "0123456789ABCDEF";

    private List<CloudResource> resources;

    public CloudResources() {
        this.resources = new ArrayList<>();
    }

    /**
     * Create a new {@link CloudResources} instance for the specified resources.
     *
     * @param resources the resources
     */
    public CloudResources(Collection<? extends CloudResource> resources) {
        Assert.notNull(resources, "Resources must not be null");
        this.resources = new ArrayList<>(resources);
    }

    /**
     * Create a new {@link CloudResources} instance for the specified resources.
     *
     * @param resources the resources
     */
    public CloudResources(Iterator<? extends CloudResource> resources) {
        Assert.notNull(resources, "Resources must not be null");
        this.resources = new ArrayList<>();
        while (resources.hasNext()) {
            this.resources.add(resources.next());
        }
    }

    /**
     * Returns the list of {@link CloudResource}s.
     *
     * @return the resources as a list
     */
    public List<CloudResource> asList() {
        return Collections.unmodifiableList(resources);
    }

    /**
     * Returns a set of all resource {@link CloudResource#getFilename() filenames}.
     *
     * @return the filenames.
     */
    public Set<String> getFileNames() {
        Set<String> filenames = new LinkedHashSet<>();
        for (CloudResource resource : resources) {
            filenames.add(resource.getFilename());
        }
        return filenames;
    }

    /**
     * Internal JSON Deserializer.
     */
    public static class Deserializer extends JsonDeserializer<CloudResources> {

        @SuppressWarnings("unchecked")
        @Override
        public CloudResources deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            TypeReference<List<CloudResource>> ref = new TypeReference<List<CloudResource>>() {
            };
            return new CloudResources((Collection<? extends CloudResource>) jp.readValueAs(ref));
        }
    }

    /**
     * Internal JSON Serializer.
     */
    public static class Serializer extends JsonSerializer<CloudResources> {

        @Override
        public void serialize(CloudResources value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeObject(value.asList());
        }
    }
}
