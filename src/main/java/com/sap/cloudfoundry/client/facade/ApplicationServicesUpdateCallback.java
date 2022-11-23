package com.sap.cloudfoundry.client.facade;

public interface ApplicationServicesUpdateCallback {

    void onError(CloudOperationException e, String applicationName, String serviceName);
}
