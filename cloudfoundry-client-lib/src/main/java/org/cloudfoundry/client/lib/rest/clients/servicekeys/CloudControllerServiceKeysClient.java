package org.cloudfoundry.client.lib.rest.clients.servicekeys;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudServiceKey;

public interface CloudControllerServiceKeysClient {

    CloudServiceKey create(String serviceName, String name, Map<String, Object> parameters);

    void delete(String serviceName, String serviceKeyName);

    List<CloudServiceKey> get(String serviceName);
}
