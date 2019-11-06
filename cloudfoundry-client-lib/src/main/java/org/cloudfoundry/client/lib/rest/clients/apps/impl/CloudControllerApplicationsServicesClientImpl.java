package org.cloudfoundry.client.lib.rest.clients.apps.impl;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsServicesClient;

public class CloudControllerApplicationsServicesClientImpl extends CloudControllerBaseClient
    implements CloudControllerApplicationsServicesClient {

    private static final String MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED = "Feature is not yet implemented.";
    
    protected CloudControllerApplicationsServicesClientImpl(CloudSpace target, CloudFoundryClient v3Client) {
        super(target, v3Client);
    }

    @Override
    public List<String> update(String applicationName, Map<String, Map<String, Object>> serviceNamesWithBindingParameters) {
        // No implementation here is needed because the logic is moved in ApplicationServicesUpdater in order to be used in other
        // implementations of the client. Currently, the ApplicationServicesUpdater is used only in CloudControllerClientImpl. Check
        // CloudControllerClientImpl.updateApplicationServices
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

}
