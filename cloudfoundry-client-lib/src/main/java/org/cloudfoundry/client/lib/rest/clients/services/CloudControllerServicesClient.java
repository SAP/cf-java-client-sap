package org.cloudfoundry.client.lib.rest.clients.services;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;

public interface CloudControllerServicesClient {

    void create(CloudService service);

    void createUserProvidedService(CloudService service, Map<String, Object> credentials);

    void createUserProvidedService(CloudService service, Map<String, Object> credentials, String syslogDrainUrl);

    void deleteAll();

    void delete(String service);

    CloudService getService(String service);

    CloudService getService(String service, boolean required);
    
    List<CloudService> getServices();

    CloudServiceInstance getServiceInstance(String serviceName);

    CloudServiceInstance getServiceInstance(String serviceName, boolean required);

    Map<String, Object> getServiceParameters(UUID guid);
    
    void bindService(String applicationName, String serviceName, Map<String, Object> parameters);
    
    void unbindService(String applicationName, String serviceName);
    
    List<CloudServiceOffering> getServiceOfferings();

}
