package org.cloudfoundry.client.lib.rest.clients.apps;

public interface CloudControllerApplicationsDiskClient {

    void update(String applicationName, int diskQuota);
    
}
