package org.cloudfoundry.client.lib;

public interface ApplicationServicesUpdateCallback {

    void onError(CloudOperationException e, String applicationName, String serviceName);

    ApplicationServicesUpdateCallback DEFAULT_APPLICATION_SERVICES_UPDATE_CALLBACK = new ApplicationServicesUpdateCallback() {
        @Override
        public void onError(CloudOperationException e, String applicationName, String serviceName) {
        }
    };
}
