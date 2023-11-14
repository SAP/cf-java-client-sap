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
 * The staging information related to an application. Used for creating the application
 *
 * @author Jennifer Hickey
 * @author Ramnivas Laddad
 * @author Scott Frederick
 */
public class Staging {

    private String buildpackUrl;

    private String command;

    private String detectedBuildpack;

    private String stack;

    private Integer healthCheckTimeout;
    
    private Integer invocationTimeout;

    private String healthCheckType;

    private String healthCheckHttpEndpoint;

    private Boolean sshEnabled;
    
    private DockerInfo dockerInfo;

    /**
     * Default staging: No command, default buildpack
     */
    public Staging() {

    }

    private Staging(StagingBuilder builder) {
        this.command = builder.command;
        this.buildpackUrl = builder.buildpackUrl;
        this.stack = builder.stack;
        this.detectedBuildpack = builder.detectedBuildpack;
        this.healthCheckTimeout = builder.healthCheckTimeout;
        this.invocationTimeout = builder.invocationTimeout;
        this.healthCheckType = builder.healthCheckType;
        this.healthCheckHttpEndpoint = builder.healthCheckHttpEndpoint;
        this.sshEnabled = builder.sshEnabled;
        this.dockerInfo = builder.dockerInfo;
    }

    public static class StagingBuilder {
        private String command;
        private String buildpackUrl;
        private String stack;
        private String detectedBuildpack;
        private Integer healthCheckTimeout;
        private Integer invocationTimeout;
        private String healthCheckType;
        private String healthCheckHttpEndpoint;
        private Boolean sshEnabled;
        private DockerInfo dockerInfo;
        
        // @param command the application command; may be null
        public StagingBuilder command(String command) {
            this.command = command;
            return this;
        }

        // @param buildpackUrl a custom buildpack url (e.g. https://github.com/cloudfoundry/java-buildpack.git); may be null
        public StagingBuilder buildpackUrl(String buildpackUrl) {
            this.buildpackUrl = buildpackUrl;
            return this;
        }

        /*
         * @param detectedBuildpack raw, free-form information regarding a detected buildpack. It is a read-only property, and should not be
         * set except when parsing a response. May be null.
         */
        public StagingBuilder detectedBuildpack(String detectedBuildpack) {
            this.detectedBuildpack = detectedBuildpack;
            return this;
        }

        // @param stack the stack to use when staging the application; may be null
        public StagingBuilder stack(String stack) {
            this.stack = stack;
            return this;
        }

        // @param healthCheckTimeout the amount of time the platform should wait when verifying that an app started; may be null
        public StagingBuilder healthCheckTimeout(Integer healthCheckTimeout) {
            this.healthCheckTimeout = healthCheckTimeout;
            return this;
        }

        // @param healthCheckType; may be null
        public StagingBuilder healthCheckType(String healthCheckType) {
            this.healthCheckType = healthCheckType;
            return this;
        }
        
        // @param invocationTimeout; default value will be 1 second only if health check type is HTTP
        public StagingBuilder invocationTimeout(Integer invocationTimeout) {
            this.invocationTimeout = invocationTimeout;
            return this;
        }

        // @param healthCheckHttpEndpoint; may be null
        public StagingBuilder healthCheckHttpEndpoint(String healthCheckHttpEndpoint) {
            this.healthCheckHttpEndpoint = healthCheckHttpEndpoint;
            return this;
        }

        // @param sshEnabled boolean value which shows if the ssh for the app is enabled or disabled; may be null
        public StagingBuilder sshEnabled(Boolean sshEnabled) {
            this.sshEnabled = sshEnabled;
            return this;
        }
        
        // @param dockerInfo; may be null if a docker image is not used
        public StagingBuilder dockerInfo(DockerInfo dockerInfo) {
            this.dockerInfo = dockerInfo;
            return this;
        }

        public Staging build() {
            return new Staging(this);
        }

    }

    /**
     * @return The buildpack url, or null to use the default buildpack detected based on application content
     */
    public String getBuildpackUrl() {
        return buildpackUrl;
    }

    /**
     * @return The start command to use
     */
    public String getCommand() {
        return command;
    }

    /**
     * @return Raw, free-form information regarding a detected buildpack, or null if no detected buildpack was resolved. For example, if the
     *         application is stopped, the detected buildpack may be null.
     */
    public String getDetectedBuildpack() {
        return detectedBuildpack;
    }

    /**
     * @return the health check timeout value
     */
    public Integer getHealthCheckTimeout() {
        return healthCheckTimeout;
    }
    
    /**
     * @return the invocation timeout value measured in seconds
     */
    public Integer getInvocationTimeout() {
        return invocationTimeout;
    }

    /**
     * @return health check type
     */
    public String getHealthCheckType() {
        return healthCheckType;
    }

    /**
     * @return health check http endpoint value
     */
    public String getHealthCheckHttpEndpoint() {
        return healthCheckHttpEndpoint;
    }

    /**
     * @return boolean value to see if ssh is enabled
     */
    public Boolean isSshEnabled() {
        return sshEnabled;
    }

    /**
     * @return the stack to use when staging the application, or null to use the default stack
     */
    public String getStack() {
        return stack;
    }
    
    /**
     * @return dockerInfo
     */
    public DockerInfo getDockerInfo() {
        return dockerInfo;
    }

    @Override
    public String toString() {
        return "Staging [command=" + getCommand() + " buildpack=" + getBuildpackUrl() + " stack=" + getStack() + " healthCheckTimeout="
            + getHealthCheckTimeout() + " healthCheckType" + getHealthCheckType() + " healthCheckHttpEndpoint="
            + getHealthCheckHttpEndpoint() + " sshEnabled=" + isSshEnabled() + " invocationTimeout=" + getInvocationTimeout() + "]";
    }
}
