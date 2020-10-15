package com.sap.cloudfoundry.client.facade;

import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;

public interface ApplicationLogListener {

    void onComplete();

    void onError(Throwable exception);

    void onMessage(ApplicationLog log);

}
