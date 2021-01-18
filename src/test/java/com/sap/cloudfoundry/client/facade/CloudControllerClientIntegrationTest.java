package com.sap.cloudfoundry.client.facade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;

class CloudControllerClientIntegrationTest {

    private static final String DEFAULT_CLIENT_ID = "cf";
    private static final String DEFAULT_CLIENT_SECRET = "";
    private static CloudControllerClient clientWithoutTarget;
    private static CloudControllerClient client;

    // Make sure tests are not running in parallel
    private static Lock sequential = new ReentrantLock();

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
        sequential.lock();
    }

    @AfterEach
    public void tearDown(TestInfo testInfo) {
        sequential.unlock();
        System.out.println(String.format("Test finished: %s", testInfo.getDisplayName()));
    }

    @Test
    @DisplayName("Add missing domain")
    void addDomain() throws IOException {
        String domainName = ITVariable.DOMAIN_NAME.getValue();
        assertDomainExists(domainName, false);
        try {
            client.addDomain(domainName);
            assertDomainExists(domainName, true);
        } finally {
            client.deleteDomain(domainName);
        }
    }

    @Test
    @DisplayName("Add existing domain and verify addition does not fail")
    void addDomainAlreadyExists() throws IOException {
        String domainName = ITVariable.DOMAIN_NAME.getValue();
        try {
            client.addDomain(domainName);
            assertDomainExists(domainName, true);
            client.addDomain(domainName);
        } finally {
            client.deleteDomain(domainName);
        }
    }

    @Test
    @DisplayName("Delete existing domain")
    void deleteDomain() throws IOException {
        String domainName = ITVariable.DOMAIN_NAME.getValue();
        client.addDomain(domainName);
        assertDomainExists(domainName, true);
        client.deleteDomain(domainName);
        assertDomainExists(domainName, false);
    }

    @Test
    @DisplayName("Delete missing domain and verify deletion fails")
    void deleteDomainMissing() throws IOException {
        String domainName = ITVariable.DOMAIN_NAME.getValue();
        assertDomainExists(domainName, false);
        try {
            client.deleteDomain(domainName);
            fail();
        } catch (CloudOperationException e) {
            assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode());
        }

    }

    private static void assertAllRequiredVariablesAreDefined() {
        for (ITVariable itVariable : ITVariable.values()) {
            if (!itVariable.isRequired()) {
                continue;
            }
            Assertions.assertNotNull(itVariable.getValue(),
                                     String.format("Missing required value defined by env var %s or system property %s",
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

    private static void assertDomainExists(String domainName, boolean domainExists) {
        boolean actualDomainExists = client.getDomainsForOrganization()
                                           .stream()
                                           .map(CloudDomain::getName)
                                           .anyMatch(domainName::equals);
        Assertions.assertEquals(domainExists, actualDomainExists);
    }

}
