package org.cloudfoundry.client.lib.rest.clients.apps;

public interface CloudControllerApplicationsMemoryClient {
    
    void update(String applicationName, int memory);
    
}
