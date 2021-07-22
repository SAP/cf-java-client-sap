package com.sap.cloudfoundry.client.facade;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

import com.sap.cloudfoundry.client.facade.domain.CloudSpace;

abstract class CloudControllerClientIntegrationTest {

    private static final String DEFAULT_CLIENT_ID = "cf";
    private static final String DEFAULT_CLIENT_SECRET = "";

    private static CloudControllerClient clientWithoutTarget;
    protected static CloudControllerClient client;

    @BeforeAll
    static void login() throws MalformedURLException {
        assertAllRequiredVariablesAreDefined();
        CloudCredentials credentials = getCloudCredentials();
        URL apiUrl = URI.create(ITVariable.CF_API.getValue())
                        .toURL();
        clientWithoutTarget = new CloudControllerClientImpl(apiUrl, credentials);
        CloudSpace target = clientWithoutTarget.getSpace(ITVariable.ORG.getValue(), ITVariable.SPACE.getValue());
        client = new CloudControllerClientImpl(apiUrl, credentials, target, true);
    }

    @BeforeEach
    public void setUp(TestInfo testInfo) {
        System.out.println("================================");
        System.out.println(String.format("Test started: %s", testInfo.getDisplayName()));
    }

    @AfterEach
    public void tearDown(TestInfo testInfo) {
        System.out.println(String.format("Test finished: %s", testInfo.getDisplayName()));
    }

    private static void assertAllRequiredVariablesAreDefined() {
        for (ITVariable itVariable : ITVariable.values()) {
            if (!itVariable.isRequired()) {
                continue;
            }
            assertNotNull(itVariable.getValue(), String.format("Missing required value defined by env var %s or system property %s",
                                                               itVariable.getEnvVariable(), itVariable.getProperty()));
        }
    }

    private static CloudCredentials getCloudCredentials() {
        if (ITVariable.USER_ORIGIN.getValue() == null) {
            return new CloudCredentials(ITVariable.USER_EMAIL.getValue(), ITVariable.USER_PASSWORD.getValue());
        }
        return new CloudCredentials(ITVariable.USER_EMAIL.getValue(),
                                    ITVariable.USER_PASSWORD.getValue(),
                                    DEFAULT_CLIENT_ID,
                                    DEFAULT_CLIENT_SECRET,
                                    ITVariable.USER_ORIGIN.getValue());
    }

}
