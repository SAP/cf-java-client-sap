package org.cloudfoundry.client.lib.rest.clients.apps;

import java.util.List;
import java.util.Map;

public interface CloudControllerApplicationsServicesClient {

    List<String> update(String applicationName, Map<String, Map<String, Object>> serviceNamesWithBindingParameters);

}
