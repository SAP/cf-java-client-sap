package org.cloudfoundry.client.lib.rest.clients.routes;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudRoute;

public interface CloudControllerRoutesClient {
    
    void add(String host, String domainName, String path);
    
    List<CloudRoute> deleteOrphaned();
    
    void delete(String host, String domainName, String path);
    
    List<CloudRoute> getAll(String domainName);
    
}
