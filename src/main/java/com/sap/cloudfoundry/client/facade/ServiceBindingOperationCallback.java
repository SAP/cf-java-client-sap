package com.sap.cloudfoundry.client.facade;

import java.util.UUID;

public interface ServiceBindingOperationCallback {

    void onError(CloudOperationException e, UUID serviceBindingGuid);
}
