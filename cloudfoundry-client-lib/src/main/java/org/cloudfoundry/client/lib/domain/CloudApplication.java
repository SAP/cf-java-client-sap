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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;

@JsonAutoDetect(fieldVisibility = Visibility.ANY, getterVisibility = Visibility.NONE, creatorVisibility = Visibility.NONE)
public class CloudApplication extends CloudEntity {

    private int memory;
    private int diskQuota;
    private int instances;
    private int runningInstances;
    private State state;
    private Staging staging;
    private PackageState packageState;
    private String stagingError;
    private List<String> uris;
    private List<String> services;
    private Map<String, String> env = new LinkedHashMap<>();
    private CloudSpace space;

    // Required by Jackson.
    public CloudApplication() {
    }

    public CloudApplication(Meta meta, String name) {
        super(meta, name);
    }

    public CloudApplication(String name, String command, String buildpackUrl, int memory, int instances, List<String> uris,
        List<String> serviceNames, State state) {
        super(CloudEntity.Meta.defaultMeta(), name);
        this.staging = new Staging.StagingBuilder().command(command)
            .buildpackUrl(buildpackUrl)
            .build();
        this.memory = memory;
        this.instances = instances;
        this.uris = uris;
        this.services = serviceNames;
        this.state = state;
    }

    public int getDiskQuota() {
        return diskQuota;
    }

    public void setDiskQuota(int diskQuota) {
        this.diskQuota = diskQuota;
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public int getInstances() {
        return instances;
    }

    public void setInstances(int instances) {
        this.instances = instances;
    }

    public int getMemory() {
        return memory;
    }

    public void setMemory(int memory) {
        this.memory = memory;
    }

    public int getRunningInstances() {
        return runningInstances;
    }

    public void setRunningInstances(int runningInstances) {
        this.runningInstances = runningInstances;
    }

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public CloudSpace getSpace() {
        return space;
    }

    public void setSpace(CloudSpace space) {
        this.space = space;
    }

    public Staging getStaging() {
        return staging;
    }

    public void setStaging(Staging staging) {
        this.staging = staging;
    }

    public String getStagingError() {
        return stagingError;
    }

    public void setStagingError(String stagingError) {
        this.stagingError = stagingError;
    }

    public PackageState getPackageState() {
        return packageState;
    }

    public void setPackageState(PackageState packageState) {
        this.packageState = packageState;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public List<String> getUris() {
        return uris;
    }

    public void setUris(List<String> uris) {
        this.uris = uris;
    }

    @Override
    public String toString() {
        return "CloudApplication [staging=" + staging + ", instances=" + instances + ", name=" + getName() + ", memory=" + memory
            + ", diskQuota=" + diskQuota + ", state=" + state + ", uris=" + uris + ", services=" + services + ", env=" + env + ", space="
            + space.getName() + "]";
    }

    public enum State {
        UPDATING, STARTED, STOPPED
    }

}
