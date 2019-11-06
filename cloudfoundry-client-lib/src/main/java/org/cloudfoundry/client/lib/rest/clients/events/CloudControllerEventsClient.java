package org.cloudfoundry.client.lib.rest.clients.events;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudEvent;

public interface CloudControllerEventsClient {

    List<CloudEvent> getEvents();
    
}
