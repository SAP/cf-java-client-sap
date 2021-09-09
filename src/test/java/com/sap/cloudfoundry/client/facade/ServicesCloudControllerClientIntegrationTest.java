package com.sap.cloudfoundry.client.facade;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceInstance;

class ServicesCloudControllerClientIntegrationTest extends CloudControllerClientIntegrationTest {

    private static final String SYSLOG_DRAIN_URL = "syslogDrain";
    private static final Map<String, Object> USER_SERVICE_CREDENTIALS = Map.of("testCredentialsKey", "testCredentialsValue");

    @Test
    @DisplayName("Create a user provided service and verify its parameters")
    void createUserProvidedServiceTest() {
        String serviceName = "test-service-1";
        try {
            client.createUserProvidedServiceInstance(buildUserProvidedService(serviceName), USER_SERVICE_CREDENTIALS, SYSLOG_DRAIN_URL);
            CloudServiceInstance service = client.getServiceInstance(serviceName);
            assertEquals(SYSLOG_DRAIN_URL, service.getSyslogDrainUrl());
            assertEquals(USER_SERVICE_CREDENTIALS, service.getCredentials());
            assertTrue(service.isUserProvided());
        } finally {
            client.deleteServiceInstance(serviceName);
        }
    }

    @Test
    @DisplayName("Create a user provided service and update its parameters")
    void updateUserProvidedServiceTest() {
        String serviceName = "test-service-2";
        Map<String, Object> updatedServiceCredentials = Map.of("newTestCredentialsKey", "newTestCredentialsValue");
        String updatedSyslogDrainUrl = "newSyslogDrain";
        try {
            client.createUserProvidedServiceInstance(buildUserProvidedService(serviceName), USER_SERVICE_CREDENTIALS, SYSLOG_DRAIN_URL);
            client.updateServiceParameters(serviceName, updatedServiceCredentials);
            client.updateServiceSyslogDrainUrl(serviceName, updatedSyslogDrainUrl);
            CloudServiceInstance service = client.getServiceInstance(serviceName);
            assertEquals(updatedSyslogDrainUrl, service.getSyslogDrainUrl());
            assertEquals(updatedServiceCredentials, service.getCredentials());
        } finally {
            client.deleteServiceInstance(serviceName);
        }
    }

    private CloudServiceInstance buildUserProvidedService(String serviceName) {
        return ImmutableCloudServiceInstance.builder()
                                            .name(serviceName)
                                            .type(ServiceInstanceType.USER_PROVIDED)
                                            .build();
    }

}
