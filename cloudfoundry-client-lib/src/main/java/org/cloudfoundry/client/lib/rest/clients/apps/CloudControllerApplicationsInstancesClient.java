package org.cloudfoundry.client.lib.rest.clients.apps;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.InstancesInfo;

public interface CloudControllerApplicationsInstancesClient {
    
    InstancesInfo get(String applicationName);

    InstancesInfo get(CloudApplication application);
    
    void update(String applicationName, int instances);
}
