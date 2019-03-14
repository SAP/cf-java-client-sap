package org.cloudfoundry.client.lib.domain;

import java.util.Map;
import java.util.UUID;

/**
 * Class representing the binding of a service instance.
 *
 * @author Scott Frederick
 */
public class CloudServiceBinding extends CloudEntity {

    private UUID applicationGuid;

    private Map<String, Object> bindingOptions;

    private Map<String, Object> credentials;

    private String syslogDrainUrl;

    public CloudServiceBinding() {
        super();
    }

    public CloudServiceBinding(Meta meta, String name) {
        super(meta, name);
    }

    public UUID getApplicationGuid() {
        return applicationGuid;
    }

    public void setApplicationGuid(UUID applicationGuid) {
        this.applicationGuid = applicationGuid;
    }

    public Map<String, Object> getBindingOptions() {
        return bindingOptions;
    }

    public void setBindingOptions(Map<String, Object> bindingOptions) {
        this.bindingOptions = bindingOptions;
    }

    public Map<String, Object> getCredentials() {
        return credentials;
    }

    public void setCredentials(Map<String, Object> credentials) {
        this.credentials = credentials;
    }

    public String getSyslogDrainUrl() {
        return syslogDrainUrl;
    }

    public void setSyslogDrainUrl(String syslogDrainUrl) {
        this.syslogDrainUrl = syslogDrainUrl;
    }
}
