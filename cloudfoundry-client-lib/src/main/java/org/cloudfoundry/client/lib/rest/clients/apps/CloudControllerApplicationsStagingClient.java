package org.cloudfoundry.client.lib.rest.clients.apps;

import java.util.UUID;

import org.cloudfoundry.client.lib.domain.Staging;

public interface CloudControllerApplicationsStagingClient {

    void update(String applicationName, Staging staging);
    
    void update(UUID applicationGuid, Staging staging);
    
}
