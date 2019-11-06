package org.cloudfoundry.client.lib.rest.clients.builds;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudBuild;

public interface CloudControllerBuildsClient {
    
    CloudBuild getBuild(UUID buildGuid);

    List<CloudBuild> getBuildsForApplication(UUID applicationGuid);

    List<CloudBuild> getBuildsForPackage(UUID packageGuid);

    CloudBuild createBuild(UUID packageGuid);
}
