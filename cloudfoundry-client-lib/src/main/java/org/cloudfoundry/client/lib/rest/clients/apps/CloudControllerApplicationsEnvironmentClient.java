package org.cloudfoundry.client.lib.rest.clients.apps;

import java.util.Map;
import java.util.UUID;

public interface CloudControllerApplicationsEnvironmentClient {

    Map<String, String> get(String applicationName);

    Map<String, String> get(UUID applicationGuid);
    
    void update(String applicationName, Map<String, String> env);
}
