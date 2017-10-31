package org.cloudfoundry.client.lib.domain;

import java.util.Map;

public class ServiceKey extends CloudEntity {

    private Map<String, Object> parameters;
    private Map<String, Object> credentials;
    private CloudService service;

    public ServiceKey(Meta meta, String name) {
        super(meta, name);
    }

    public ServiceKey(String name, Map<String, Object> parameters, Map<String, Object> credentials, CloudService service) {
        this(CloudEntity.Meta.defaultMeta(), name, parameters, credentials, service);
    }

    public ServiceKey(Meta meta, String name, Map<String, Object> parameters, Map<String, Object> credentials, CloudService service) {
        super(meta, name);
        this.parameters = parameters;
        this.credentials = credentials;
        this.service = service;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Map<String, Object> getCredentials() {
        return credentials;
    }

    public CloudService getService() {
        return service;
    }

    public void setService(CloudService service) {
        this.service = service;
    }

}
