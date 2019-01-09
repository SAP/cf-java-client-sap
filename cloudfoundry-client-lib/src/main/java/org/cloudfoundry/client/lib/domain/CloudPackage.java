package org.cloudfoundry.client.lib.domain;

public class CloudPackage extends CloudEntity {
    
    private Type type;

    private Data data;

    private Status status;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Data getData() {
        return data;
    }

    public void setData(Data data) {
        this.data = data;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status state) {
        this.status = state;
    }
    
    public static class Builder {
        private CloudPackage cloudPackage;
        
        public Builder() {
            cloudPackage = new CloudPackage();
        }
        
        public CloudPackage build() {
            return cloudPackage;
        }
        
        public Builder type(Type type) {
            cloudPackage.type = type;
            return this;
        }
        
        public Builder data(Data data) {
            cloudPackage.data = data;
            return this;
        }
        
        public Builder status(Status status) {
            cloudPackage.status = status;
            return this;
        }
        
        public Builder meta(Meta meta) {
            cloudPackage.setMeta(meta);
            return this;
        }
    }

    public static class Data {

        private Checksum checksum;
        private String error;

        public Data(Checksum checksum, String error) {
            this.checksum = checksum;
            this.error = error;
        }

        public Checksum getChecksum() {
            return checksum;
        }

        public void setChecksum(Checksum checksum) {
            this.checksum = checksum;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        public static class Checksum {

            private String type;

            private String value;

            public Checksum(String type, String value) {
                this.type = type;
                this.value = value;
            }

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }
        }
    }

    public static enum Type {
        BITS("bits"), DOCKER("docker");

        private final String type;

        Type(String status) {
            this.type = status;
        }

        public static Type getEnum(String status) {
            for (Type value : Type.values()) {
                if (value.type.equals(status)) {
                    return value;
                }
            }

            throw new IllegalArgumentException("Invalid Status value: " + status);
        }

        @Override
        public String toString() {
            return type;
        }
    }

}
