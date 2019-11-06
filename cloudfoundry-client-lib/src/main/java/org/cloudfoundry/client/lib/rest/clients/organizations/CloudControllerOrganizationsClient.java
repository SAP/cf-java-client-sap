package org.cloudfoundry.client.lib.rest.clients.organizations;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudOrganization;

public interface CloudControllerOrganizationsClient {

    CloudOrganization getOrganization(String organizationName, boolean required);
    
    List<CloudOrganization> getOrganizations();
}
