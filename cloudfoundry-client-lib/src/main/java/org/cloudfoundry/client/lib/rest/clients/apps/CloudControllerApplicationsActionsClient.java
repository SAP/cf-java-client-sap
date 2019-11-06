package org.cloudfoundry.client.lib.rest.clients.apps;

import java.util.List;

import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.Staging;

public interface CloudControllerApplicationsActionsClient {

    void rename(String applicationName, String newName);

    StartingInfo restart(String applicationName);

    StartingInfo start(String applicationName);

    void stop(String applicationName);
    
    void create(String name, Staging staging, Integer diskQuota, Integer memory, List<String> uris,
                           List<String> serviceNames, DockerInfo dockerInfo);
    
    void deleteAll();
    
    void delete(String applicationName);

}
