package org.cloudfoundry.client.lib.rest.clients.servicebrokers;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudServiceBroker;

public interface CloudControllerServiceBrokersClient {

    void create(CloudServiceBroker serviceBroker);
    
    void delete(String name);

    CloudServiceBroker get( String name, boolean required);
    
    List<CloudServiceBroker> getAll();
    
    void update(CloudServiceBroker serviceBroker);
    
    void updateServicePlanVisibility(String name, boolean visibility);
}
