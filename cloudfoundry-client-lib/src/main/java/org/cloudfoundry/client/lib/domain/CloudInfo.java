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
 * @author Ramnivas Laddad
 * @author Dave Syer
 * @author Thomas Risberg
 */
public class CloudInfo {

    private boolean allowDebug;
    private String authorizationEndpoint;
    private String build;
    private String description;
    private Limits limits;
    private String loggingEndpoint;
    private String name;
    private String support;
    private Usage usage;
    private String user;
    private String version;

    // Required by Jackson.
    public CloudInfo() {
    }

    public CloudInfo(String name, String support, String authorizationEndpoint, String build, String version, String user,
        String description, Limits limits, Usage usage, boolean allowDebug, String loggingEndpoint) {
        this.name = name;
        this.support = support;
        this.authorizationEndpoint = authorizationEndpoint;
        this.loggingEndpoint = loggingEndpoint;
        this.build = build;
        this.version = version;
        this.user = user;
        this.description = description;
        this.limits = limits;
        this.usage = usage;
        this.allowDebug = allowDebug;
    }

    public boolean getAllowDebug() {
        return allowDebug;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public String getBuild() {
        return build;
    }

    public String getDescription() {
        return description;
    }

    public Limits getLimits() {
        return limits;
    }

    public String getLoggingEndpoint() {
        return loggingEndpoint;
    }

    public String getName() {
        return name;
    }

    public String getSupport() {
        return support;
    }

    public Usage getUsage() {
        return usage;
    }

    public String getUser() {
        return user;
    }

    public String getVersion() {
        return version;
    }

    public static class Limits {

        private int maxApps;

        private int maxServices;

        private int maxTotalMemory;

        private int maxUrisPerApp;

        public int getMaxApps() {
            return maxApps;
        }

        public int getMaxServices() {
            return maxServices;
        }

        public int getMaxTotalMemory() {
            return maxTotalMemory;
        }

        public int getMaxUrisPerApp() {
            return maxUrisPerApp;
        }

        public void setMaxApps(int maxApps) {
            this.maxApps = maxApps;
        }

        public void setMaxServices(int maxServices) {
            this.maxServices = maxServices;
        }

        public void setMaxTotalMemory(int maxTotalMemory) {
            this.maxTotalMemory = maxTotalMemory;
        }

        public void setMaxUrisPerApp(int maxUrisPerApp) {
            this.maxUrisPerApp = maxUrisPerApp;
        }

    }

    public static class Usage {

        private int apps;
        private int services;
        private int totalMemory;
        private int urisPerApp;

        public int getApps() {
            return apps;
        }

        public int getServices() {
            return services;
        }

        public int getTotalMemory() {
            return totalMemory;
        }

        public int getUrisPerApp() {
            return urisPerApp;
        }

        public void setApps(int apps) {
            this.apps = apps;
        }

        public void setServices(int services) {
            this.services = services;
        }

        public void setTotalMemory(int totalMemory) {
            this.totalMemory = totalMemory;
        }

        public void setUrisPerApp(int urisPerApp) {
            this.urisPerApp = urisPerApp;
        }

    }
}
