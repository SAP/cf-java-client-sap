package org.cloudfoundry.client.lib.rest.clients.apps;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudEvent;

public interface CloudControllerApplicationsEventsClient {

    List<CloudEvent> get(String applicationName);

    List<CloudEvent> get(UUID applicationGuid);
    
}
