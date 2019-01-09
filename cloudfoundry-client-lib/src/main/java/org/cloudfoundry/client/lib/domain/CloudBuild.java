package org.cloudfoundry.client.lib.domain;

import java.util.List;
import java.util.UUID;

public class CloudBuild extends CloudEntity {

    private CreatedBy createdBy;
    
    private Droplet droplet;
    
    private String error;
    
    private Lifecycle lifecycle;
    
    private Package packageInfo;
    
    private BuildState state;
    
    public CreatedBy getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(CreatedBy createdBy) {
        this.createdBy = createdBy;
    }

    public Droplet getDroplet() {
        return droplet;
    }

    public void setDroplet(Droplet droplet) {
        this.droplet = droplet;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public Package getPackageInfo() {
        return packageInfo;
    }

    public void setPackageInfo(Package packageInfo) {
        this.packageInfo = packageInfo;
    }

    public BuildState getState() {
        return state;
    }

    public void setState(BuildState state) {
        this.state = state;
    }
    
    public static class Builder {
        
        private CloudBuild cloudBuild;
        
        public Builder() {
            cloudBuild = new CloudBuild();
        }
        
        public Builder meta(Meta meta) {
            cloudBuild.setMeta(meta);
            return this;
        }
        
        public Builder createdBy(CreatedBy createdBy) {
            cloudBuild.createdBy = createdBy;
            return this;
        }
        
        public Builder droplet(Droplet droplet) {
            cloudBuild.droplet = droplet;
            return this;
        }
        
        public Builder error(String error) {
            cloudBuild.error = error;
            return this;
        }
        
        public Builder lifecycle(Lifecycle lifecycle) {
            cloudBuild.lifecycle = lifecycle;
            return this;
        }
        
        public Builder packageInfo(Package packageInfo) {
            cloudBuild.packageInfo = packageInfo;
            return this;
        }
        
        public Builder buildState(BuildState state) {
            cloudBuild.state = state;
            return this;
        }
        
        public CloudBuild build() {
            return cloudBuild;
        }
    }

    public static enum BuildState {
        FAILED("FAILED"), STAGED("STAGED"), STAGING("STAGING");

        private String status;

        BuildState(String status) {
            this.status = status;
        }

        public static BuildState getEnum(String status) {
            for (BuildState value : BuildState.values()) {
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

    public static class Package {
        private String guid;

        public Package(String guid) {
            this.guid = guid;
        }

        public String getGuid() {
            return guid;
        }

        public void setGuid(String guid) {
            this.guid = guid;
        }
    }

    public static class Lifecycle {
        private String type;
        private Data data;

        public Lifecycle(String type, Data data) {
            this.type = type;
            this.data = data;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Data getData() {
            return data;
        }

        public void setData(Data data) {
            this.data = data;
        }

        public static class Data {
            private List<String> buildpacks;
            private String stack;

            public Data(List<String> buildpacks, String stack) {
                this.buildpacks = buildpacks;
                this.stack = stack;
            }

            public List<String> getBuildpacks() {
                return buildpacks;
            }

            public void setBuildpacks(List<String> buildpacks) {
                this.buildpacks = buildpacks;
            }

            public String getStack() {
                return stack;
            }

            public void setStack(String stack) {
                this.stack = stack;
            }
        }
    }

    public static class Droplet {
        private UUID guid;
        private String href;

        public Droplet(UUID guid, String href) {
            this.guid = guid;
            this.href = href;
        }

        public UUID getGuid() {
            return guid;
        }

        public void setGuid(UUID guid) {
            this.guid = guid;
        }

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }
    }

    public static class CreatedBy {

        private String email;
        private UUID guid;
        private String name;

        public CreatedBy(String email, UUID guid, String name) {
            this.email = email;
            this.guid = guid;
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public UUID getGuid() {
            return guid;
        }

        public void setGuid(UUID guid) {
            this.guid = guid;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
