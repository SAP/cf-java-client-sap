package org.cloudfoundry.client.lib.rest.clients.droplets;

import java.util.UUID;

public interface CloudControllerDropletsClient {
    void bindDropletToApp(UUID dropletGuid, UUID applicationGuid);
}
