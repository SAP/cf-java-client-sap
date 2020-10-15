package com.sap.cloudfoundry.client.facade;

public interface ApplicationServicesUpdateCallback {

    void onError(CloudOperationException e, String applicationName, String serviceName);

    ApplicationServicesUpdateCallback DEFAULT_APPLICATION_SERVICES_UPDATE_CALLBACK = (e, applicationName, serviceName) -> {
    };
}
