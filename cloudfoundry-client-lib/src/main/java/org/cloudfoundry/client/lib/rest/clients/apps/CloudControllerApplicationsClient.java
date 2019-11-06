package org.cloudfoundry.client.lib.rest.clients.apps;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudApplication;

public interface CloudControllerApplicationsClient {

    CloudApplication findApplicationByName(String name, boolean required);

    CloudApplication getApplication(String applicationName);

    CloudApplication getApplication(String applicationName, boolean required);

    CloudApplication getApplication(UUID applicationGuid);

    List<CloudApplication> getApplications();

    // Application Upload
    CloudControllerApplicationsUploadClient upload();

    // Environment
    CloudControllerApplicationsEnvironmentClient environment();

    // Application Events
    CloudControllerApplicationsEventsClient events();

    // Application Instances
    CloudControllerApplicationsInstancesClient instances();

    // Application Actions
    CloudControllerApplicationsActionsClient actions();

    // Application Disk Quota
    CloudControllerApplicationsDiskClient diskQuota();

    // Application Memory
    CloudControllerApplicationsMemoryClient memory();

    // Application Services
    CloudControllerApplicationsServicesClient services();

    // Application Staging
    CloudControllerApplicationsStagingClient staging();

    // Application Uris
    CloudControllerApplicationsUrisClient uris();

}
