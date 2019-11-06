package org.cloudfoundry.client.lib.rest.clients;

import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsClient;
import org.cloudfoundry.client.lib.rest.clients.builds.CloudControllerBuildsClient;
import org.cloudfoundry.client.lib.rest.clients.domains.CloudControllerDomainsClient;
import org.cloudfoundry.client.lib.rest.clients.droplets.CloudControllerDropletsClient;
import org.cloudfoundry.client.lib.rest.clients.events.CloudControllerEventsClient;
import org.cloudfoundry.client.lib.rest.clients.logs.CloudControllerLogsClient;
import org.cloudfoundry.client.lib.rest.clients.organizations.CloudControllerOrganizationsClient;
import org.cloudfoundry.client.lib.rest.clients.packages.CloudControllerPackagesClient;
import org.cloudfoundry.client.lib.rest.clients.routes.CloudControllerRoutesClient;
import org.cloudfoundry.client.lib.rest.clients.servicebrokers.CloudControllerServiceBrokersClient;
import org.cloudfoundry.client.lib.rest.clients.servicekeys.CloudControllerServiceKeysClient;
import org.cloudfoundry.client.lib.rest.clients.services.CloudControllerServicesClient;
import org.cloudfoundry.client.lib.rest.clients.spaces.CloudControllerSpacesClient;
import org.cloudfoundry.client.lib.rest.clients.stacks.CloudControllerStacksClient;
import org.cloudfoundry.client.lib.rest.clients.tasks.CloudControllerTasksClient;
import org.immutables.value.Value;

@Value.Immutable
public interface CloudControllerCompositeClient {

    CloudControllerRoutesClient routes();

    CloudControllerDomainsClient domains();

    CloudControllerApplicationsClient applications();

    CloudControllerServicesClient services();

    CloudControllerTasksClient tasks();

    CloudControllerServiceBrokersClient serviceBrokers();

    CloudControllerServiceKeysClient serviceKeys();

    CloudControllerOrganizationsClient organizations();

    CloudControllerSpacesClient spaces();

    CloudControllerLogsClient logs();

    CloudControllerBuildsClient builds();

    CloudControllerStacksClient stacks();

    CloudControllerEventsClient events();

    CloudControllerPackagesClient packages();

    CloudControllerDropletsClient droplets();
}
