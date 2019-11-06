package org.cloudfoundry.client.lib.rest.clients.apps;

import java.util.List;
import java.util.UUID;

public interface CloudControllerApplicationsUrisClient {

    void add(UUID applicationGuid, List<String> uris);

    void update(String applicationName, List<String> uris);
    
    void remove(UUID applicationGuid, List<String> uris);
    
    
}
