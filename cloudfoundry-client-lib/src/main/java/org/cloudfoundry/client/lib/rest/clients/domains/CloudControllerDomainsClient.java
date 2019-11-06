package org.cloudfoundry.client.lib.rest.clients.domains;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudDomain;

public interface CloudControllerDomainsClient {
    
    void addDomain(String domainName);
    
    void deleteDomain(String domainName);
    
    CloudDomain getDefaultDomain();
    
    List<CloudDomain> getSharedDomains();
    
     List<CloudDomain> getDomains();
    
     List<CloudDomain> getPrivateDomains();
    
    UUID getRequiredDomainGuid(String name);
    
    List<CloudDomain> getDomainsForOrganization();
    
    CloudDomain findDomainByName(String name, boolean required);

    CloudDomain findDomainByName(String name);
}
