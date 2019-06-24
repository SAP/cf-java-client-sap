package org.cloudfoundry.client.lib;

import org.cloudfoundry.client.lib.exception.CloudOperationException;

public interface ApplicationServicesUpdateCallback {

    void onError(CloudOperationException e, String applicationName, String serviceName);

    public static final ApplicationServicesUpdateCallback DEFAULT_APPLICATION_SERVICES_UPDATE_CALLBACK = new ApplicationServicesUpdateCallback() {

        @Override
        public void onError(CloudOperationException e, String applicationName, String serviceName) {
        }
    };
}
