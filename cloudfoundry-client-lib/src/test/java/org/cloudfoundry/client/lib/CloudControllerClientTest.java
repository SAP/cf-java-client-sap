package org.cloudfoundry.client.lib;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudQuota;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudSecurityGroup;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.domain.CloudUser;
import org.cloudfoundry.client.lib.domain.CrashInfo;
import org.cloudfoundry.client.lib.domain.CrashesInfo;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudSecurityGroup;
import org.cloudfoundry.client.lib.domain.ImmutableCloudService;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBroker;
import org.cloudfoundry.client.lib.domain.ImmutableSecurityGroupRule;
import org.cloudfoundry.client.lib.domain.ImmutableStaging;
import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.client.lib.domain.InstanceState;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.SecurityGroupRule;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.client.lib.rest.CloudControllerRestClient;
import org.cloudfoundry.client.lib.rest.CloudControllerRestClientFactory;
import org.cloudfoundry.client.lib.rest.ImmutableCloudControllerRestClientFactory;
import org.cloudfoundry.client.lib.util.RestUtil;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ConnectHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

/**
 * Note that this integration tests rely on other methods working correctly, so these tests aren't independent unit tests for all methods,
 * they are intended to test the completeness of the functionality of each API version implementation.
 *
 * @author Ramnivas Laddad
 * @author A.B.Srinivasan
 * @author Jennifer Hickey
 * @author Thomas Risberg
 */
@RunWith(BMUnitRunner.class)
@BMScript(value = "trace", dir = "target/test-classes")
public class CloudControllerClientTest {

    public static final int STARTUP_TIMEOUT = Integer.getInteger("ccng.startup.timeout", 60000);

    private static final String CCNG_API_PROXY_HOST = System.getProperty("http.proxyHost", null);

    private static final String CCNG_API_PROXY_PASSWD = System.getProperty("http.proxyPassword", null);

    private static final int CCNG_API_PROXY_PORT = Integer.getInteger("http.proxyPort", 80);

    private static final String CCNG_API_PROXY_USER = System.getProperty("http.proxyUsername", null);

    private static final boolean CCNG_API_SSL = Boolean.getBoolean("ccng.ssl");

    // Pass -Dccng.target=http://api.cloudfoundry.com, vcap.me, or your own cloud -- must point to a v2 cloud controller
    private static final String CCNG_API_URL = System.getProperty("ccng.target", "http://api.run.pivotal.io");

    private static final String CCNG_SECURITY_GROUP_NAME_TEST = System.getProperty("ccng.securityGroup", "test_security_group");

    private static final String CCNG_USER_EMAIL = System.getProperty("ccng.email", "java-authenticatedClient-test-user@vmware.com");

    private static final boolean CCNG_USER_IS_ADMIN = Boolean.getBoolean("ccng.admin");

    private static final String CCNG_USER_ORG = System.getProperty("ccng.org", "gopivotal.com");

    private static final String CCNG_USER_PASS = System.getProperty("ccng.passwd");

    private static final String CCNG_USER_SPACE = System.getProperty("ccng.space", "test");

    private static final int DEFAULT_DISK = 1024; // MB

    private static final int DEFAULT_MEMORY = 512; // MB

    private static final String DEFAULT_STACK_NAME = "lucid64";

    private static final int FIVE_MINUTES = 300 * 1000;

    private static final String MYSQL_SERVICE_LABEL = System.getProperty("vcap.mysql.label", "p-mysql");

    private static final String MYSQL_SERVICE_PLAN = System.getProperty("vcap.mysql.plan", "100mb-dev");

    private static final boolean SILENT_TEST_TIMINGS = Boolean.getBoolean("silent.testTimings");

    private static final boolean SKIP_INJVM_PROXY = Boolean.getBoolean("http.skipInJvmProxy");

    private static final String TEST_DOMAIN = System.getProperty("vcap.test.domain", defaultNamespace(CCNG_USER_EMAIL) + ".com");

    private static final String TEST_NAMESPACE = System.getProperty("vcap.test.namespace", defaultNamespace(CCNG_USER_EMAIL));

    private static String defaultDomainName = null;

    private static HttpProxyConfiguration httpProxyConfiguration;

    private static int inJvmProxyPort;

    private static Server inJvmProxyServer;

    private static AtomicInteger nbInJvmProxyRcvReqs;

    private static boolean tearDownComplete = false;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public TestRule watcher = new TestWatcher() {
        private long startTime;

        @Override
        protected void finished(Description description) {
            if (!SILENT_TEST_TIMINGS) {
                System.out.println("Test " + description.getMethodName() + " took " + (System.currentTimeMillis() - startTime) + " ms");
            }
        }

        @Override
        protected void starting(Description description) {
            if (!SILENT_TEST_TIMINGS) {
                System.out.println("Starting test " + description.getMethodName());
            }
            startTime = System.currentTimeMillis();
        }
    };

    private CloudControllerClient connectedClient;

    @AfterClass
    public static void afterClass() throws Exception {
        if (inJvmProxyServer != null) {
            inJvmProxyServer.stop();
            nbInJvmProxyRcvReqs.set(0);
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.out.println("Running tests on " + CCNG_API_URL + " on behalf of " + CCNG_USER_EMAIL);
        System.out.println("Using space " + CCNG_USER_SPACE + " of organization " + CCNG_USER_ORG);
        if (CCNG_USER_PASS == null) {
            fail("System property ccng.passwd must be specified, supply -Dccng.passwd=<password>");
        }

        if (CCNG_API_PROXY_HOST != null) {
            if (CCNG_API_PROXY_USER != null) {
                httpProxyConfiguration = new HttpProxyConfiguration(CCNG_API_PROXY_HOST,
                                                                    CCNG_API_PROXY_PORT,
                                                                    true,
                                                                    CCNG_API_PROXY_USER,
                                                                    CCNG_API_PROXY_PASSWD);
            } else {
                httpProxyConfiguration = new HttpProxyConfiguration(CCNG_API_PROXY_HOST, CCNG_API_PROXY_PORT);
            }
        }
        if (!SKIP_INJVM_PROXY) {
            startInJvmProxy();
            httpProxyConfiguration = new HttpProxyConfiguration("127.0.0.1", inJvmProxyPort);
        }
    }

    private static String defaultNamespace(String email) {
        String s;
        if (email.contains("@")) {
            s = email.substring(0, email.indexOf('@'));
        } else {
            s = email;
        }
        return s.replaceAll("\\.", "-")
                .replaceAll("\\+", "-");
    }

    private static int getNextAvailablePort(int initial) {
        int current = initial;
        while (!PortAvailability.available(current)) {
            current++;
            if (current - initial > 100) {
                throw new RuntimeException("did not find an available port from " + initial + " up to:" + current);
            }
        }
        return current;
    }

    /**
     * To test that the CF client is able to go through a proxy, we point the CC client to a broken url that can only be resolved by going
     * through an inJVM proxy which rewrites the URI. This method starts this inJvm proxy.
     *
     * @throws Exception
     */
    private static void startInJvmProxy() throws Exception {
        inJvmProxyPort = getNextAvailablePort(8080);
        inJvmProxyServer = new Server(new InetSocketAddress("127.0.0.1", inJvmProxyPort)); // forcing use of loopback
        // that will be used both for Httpclient proxy and SocketDestHelper
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMinThreads(1);
        inJvmProxyServer.setThreadPool(threadPool);

        HandlerCollection handlers = new HandlerCollection();
        inJvmProxyServer.setHandler(handlers);

        ServletHandler servletHandler = new ServletHandler();
        handlers.addHandler(servletHandler);
        nbInJvmProxyRcvReqs = new AtomicInteger();
        ChainedProxyServlet chainedProxyServlet = new ChainedProxyServlet(httpProxyConfiguration, nbInJvmProxyRcvReqs);
        servletHandler.addServletWithMapping(new ServletHolder(chainedProxyServlet), "/*");

        // Setup proxy handler to handle CONNECT methods
        ConnectHandler proxyHandler;
        proxyHandler = new ChainedProxyConnectHandler(httpProxyConfiguration, nbInJvmProxyRcvReqs);
        handlers.addHandler(proxyHandler);

        inJvmProxyServer.start();
    }

    @Test
    public void accessRandomApplicationUrl() throws Exception {
        String applicationName = UUID.randomUUID()
                                     .toString();
        CloudApplication application = createAndUploadAndStartSimpleSpringApp(applicationName);
        connectedClient.startApplication(applicationName);
        assertEquals(1, application.getInstances());
        for (int i = 0; i < 100 && application.getRunningInstances() < 1; i++) {
            Thread.sleep(1000);
            application = connectedClient.getApplication(applicationName);
        }
        assertEquals(1, application.getRunningInstances());
        RestUtil restUtil = new RestUtil();
        RestTemplate rest = restUtil.createRestTemplate(httpProxyConfiguration, false);
        String results = rest.getForObject("http://" + application.getUris()
                                                                  .get(0),
                                           String.class);
        assertTrue(results.contains("Hello world!"));
    }

    @Test
    public void addAndDeleteDomain() {
        connectedClient.addDomain(TEST_DOMAIN);

        assertDomainInList(connectedClient.getPrivateDomains());
        assertDomainInList(connectedClient.getDomainsForOrganization());

        assertDomainNotInList(connectedClient.getSharedDomains());

        connectedClient.deleteDomain(TEST_DOMAIN);

        assertDomainNotInList(connectedClient.getPrivateDomains());
        assertDomainNotInList(connectedClient.getDomainsForOrganization());
    }

    @Test
    public void addAndDeleteRoute() {
        connectedClient.addDomain(TEST_DOMAIN);
        connectedClient.addRoute("my_route1", TEST_DOMAIN, null);
        connectedClient.addRoute("my_route2", TEST_DOMAIN, null);

        List<CloudRoute> routes = connectedClient.getRoutes(TEST_DOMAIN);
        assertNotNull(getRouteWithHost("my_route1", routes));
        assertNotNull(getRouteWithHost("my_route2", routes));

        connectedClient.deleteRoute("my_route2", TEST_DOMAIN, null);
        routes = connectedClient.getRoutes(TEST_DOMAIN);
        assertNotNull(getRouteWithHost("my_route1", routes));
        assertNull(getRouteWithHost("my_route2", routes));

        // test that removing domain that has routes throws exception
        try {
            connectedClient.deleteDomain(TEST_DOMAIN);
            fail("should have thrown exception");
        } catch (IllegalStateException ex) {
            assertTrue(ex.getMessage()
                         .contains("in use"));
        }
    }

    @Test
    public void appEventsAvailable() throws Exception {
        String applicationName = createSpringTravelApp("appEvents");
        List<CloudEvent> events = connectedClient.getApplicationEvents(applicationName);
        assertEvents(events);
        assertEventTimestamps(events);
    }

    @Test
    public void appsWithRoutesAreCounted() throws IOException {
        String applicationName = namespacedAppName("my_route3");
        CloudApplication application = createAndUploadSimpleTestApp(applicationName);
        List<String> uris = application.getUris();
        uris.add("my_route3." + TEST_DOMAIN);
        connectedClient.addDomain(TEST_DOMAIN);
        connectedClient.updateApplicationUris(applicationName, uris);

        List<CloudRoute> routes = connectedClient.getRoutes(TEST_DOMAIN);
        assertNotNull(getRouteWithHost("my_route3", routes));
        assertEquals(1, getRouteWithHost("my_route3", routes).getAppsUsingRoute());
        assertTrue(getRouteWithHost("my_route3", routes).isUsed());

        List<CloudRoute> defaultDomainRoutes = connectedClient.getRoutes(defaultDomainName);
        assertNotNull(getRouteWithHost(applicationName, defaultDomainRoutes));
        assertEquals(1, getRouteWithHost(applicationName, defaultDomainRoutes).getAppsUsingRoute());
        assertTrue(getRouteWithHost(applicationName, defaultDomainRoutes).isUsed());
    }

    //
    // Basic Event Tests
    //

    @Test
    public void assignAllUserRolesInSpaceWithOrgToCurrentUser() {
        String organizationName = CCNG_USER_ORG;
        String spaceName = "assignAllUserRolesInSpaceWithOrgToCurrentUser";
        connectedClient.createSpace(spaceName);

        Map<String, CloudUser> orgUsers = connectedClient.getOrganizationUsers(organizationName);
        String username = CCNG_USER_EMAIL;
        CloudUser user = orgUsers.get(username);
        assertNotNull("Retrieved user should not be null", user);
        String userGuid = user.getMetadata()
                              .getGuid()
                              .toString();

        List<UUID> spaceManagers = connectedClient.getSpaceManagers(organizationName, spaceName);
        assertEquals("Space should have no manager when created", 0, spaceManagers.size());
        connectedClient.associateManagerWithSpace(organizationName, spaceName, userGuid);
        spaceManagers = connectedClient.getSpaceManagers(organizationName, spaceName);
        assertEquals("Space should have one manager", 1, spaceManagers.size());

        List<UUID> spaceDevelopers = connectedClient.getSpaceDevelopers(organizationName, spaceName);
        assertEquals("Space should have no developer when created", 0, spaceDevelopers.size());
        connectedClient.associateDeveloperWithSpace(organizationName, spaceName, userGuid);
        spaceDevelopers = connectedClient.getSpaceDevelopers(organizationName, spaceName);
        assertEquals("Space should have one developer", 1, spaceDevelopers.size());

        List<UUID> spaceAuditors = connectedClient.getSpaceAuditors(organizationName, spaceName);
        assertEquals("Space should have no auditor when created", 0, spaceAuditors.size());
        connectedClient.associateAuditorWithSpace(organizationName, spaceName, userGuid);
        spaceAuditors = connectedClient.getSpaceAuditors(organizationName, spaceName);
        assertEquals("Space should have one auditor ", 1, spaceAuditors.size());

        connectedClient.deleteSpace(spaceName);
        CloudSpace deletedSpace = connectedClient.getSpace(spaceName);
        assertNull("Space '" + spaceName + "' should not exist after deletion", deletedSpace);
    }

    @Test
    public void assignDefaultUserRolesInSpace() {
        String spaceName = "assignDefaultUserRolesInSpace";
        connectedClient.createSpace(spaceName);

        List<UUID> spaceManagers = connectedClient.getSpaceManagers(spaceName);
        assertEquals("Space should have no manager when created", 0, spaceManagers.size());
        connectedClient.associateManagerWithSpace(spaceName);
        spaceManagers = connectedClient.getSpaceManagers(spaceName);
        assertEquals("Space should have one manager", 1, spaceManagers.size());

        List<UUID> spaceDevelopers = connectedClient.getSpaceDevelopers(spaceName);
        assertEquals("Space should have no developer when created", 0, spaceDevelopers.size());
        connectedClient.associateDeveloperWithSpace(spaceName);
        spaceDevelopers = connectedClient.getSpaceDevelopers(spaceName);
        assertEquals("Space should have one developer", 1, spaceDevelopers.size());

        List<UUID> spaceAuditors = connectedClient.getSpaceAuditors(spaceName);
        assertEquals("Space should have no auditor when created", 0, spaceAuditors.size());
        connectedClient.associateAuditorWithSpace(spaceName);
        spaceAuditors = connectedClient.getSpaceAuditors(spaceName);
        assertEquals("Space should have one auditor ", 1, spaceAuditors.size());

        connectedClient.deleteSpace(spaceName);
        CloudSpace deletedSpace = connectedClient.getSpace(spaceName);
        assertNull("Space '" + spaceName + "' should not exist after deletion", deletedSpace);
    }

    @Test(expected = IllegalArgumentException.class)
    public void attemptingToDeleteANonExistentSecurityGroupThrowsAnIllegalArgumentException() {
        assumeTrue(CCNG_USER_IS_ADMIN);

        connectedClient.deleteSecurityGroup(randomSecurityGroupName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void attemptingToUpdateANonExistentSecurityGroupThrowsAnIllegalArgumentException() throws FileNotFoundException {
        assumeTrue(CCNG_USER_IS_ADMIN);

        connectedClient.updateSecurityGroup(randomSecurityGroupName(),
                                            new FileInputStream(new File("src/test/resources/security-groups/test-rules-2.json")));
    }

    //
    // Basic Application tests
    //

    @Test
    public void bindAndUnbindService() throws IOException {
        String serviceName = "test_database";
        createMySqlService(serviceName);

        String applicationName = createSpringTravelApp("bind1");

        CloudApplication application = connectedClient.getApplication(applicationName);
        assertNotNull(application.getServices());
        assertTrue(application.getServices()
                              .isEmpty());

        connectedClient.bindService(applicationName, serviceName);

        application = connectedClient.getApplication(applicationName);
        assertNotNull(application.getServices());
        assertEquals(1, application.getServices()
                                   .size());
        assertEquals(serviceName, application.getServices()
                                             .get(0));

        connectedClient.unbindService(applicationName, serviceName);

        application = connectedClient.getApplication(applicationName);
        assertNotNull(application.getServices());
        assertTrue(application.getServices()
                              .isEmpty());
    }

    @Test
    public void bindingAndUnbindingSecurityGroupToDefaultRunningSet() throws FileNotFoundException {
        assumeTrue(CCNG_USER_IS_ADMIN);

        // Given
        assertFalse(containsSecurityGroupNamed(connectedClient.getRunningSecurityGroups(), CCNG_SECURITY_GROUP_NAME_TEST));
        connectedClient.createSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST,
                                            new FileInputStream(new File("src/test/resources/security-groups/test-rules-2.json")));

        // When
        connectedClient.bindRunningSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);

        // Then
        assertTrue(containsSecurityGroupNamed(connectedClient.getRunningSecurityGroups(), CCNG_SECURITY_GROUP_NAME_TEST));

        // When
        connectedClient.unbindRunningSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);

        // Then
        assertFalse(containsSecurityGroupNamed(connectedClient.getRunningSecurityGroups(), CCNG_SECURITY_GROUP_NAME_TEST));

        // Cleanup
        connectedClient.deleteSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);
    }

    @Test
    public void bindingAndUnbindingSecurityGroupToDefaultStagingSet() throws FileNotFoundException {
        assumeTrue(CCNG_USER_IS_ADMIN);

        // Given
        assertFalse(containsSecurityGroupNamed(connectedClient.getStagingSecurityGroups(), CCNG_SECURITY_GROUP_NAME_TEST));
        connectedClient.createSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST,
                                            new FileInputStream(new File("src/test/resources/security-groups/test-rules-2.json")));

        // When
        connectedClient.bindStagingSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);

        // Then
        assertTrue(containsSecurityGroupNamed(connectedClient.getStagingSecurityGroups(), CCNG_SECURITY_GROUP_NAME_TEST));

        // When
        connectedClient.unbindStagingSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);

        // Then
        assertFalse(containsSecurityGroupNamed(connectedClient.getStagingSecurityGroups(), CCNG_SECURITY_GROUP_NAME_TEST));

        // Cleanup
        connectedClient.deleteSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);
    }

    @Test
    public void bindingAndUnbindingSecurityGroupToSpaces() throws FileNotFoundException {
        assumeTrue(CCNG_USER_IS_ADMIN);

        // Given
        connectedClient.createSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST,
                                            new FileInputStream(new File("src/test/resources/security-groups/test-rules-2.json")));

        // When
        connectedClient.bindSecurityGroup(CCNG_USER_ORG, CCNG_USER_SPACE, CCNG_SECURITY_GROUP_NAME_TEST);
        // Then
        assertTrue(isSpaceBoundToSecurityGroup(CCNG_USER_SPACE, CCNG_SECURITY_GROUP_NAME_TEST));

        // When
        connectedClient.unbindSecurityGroup(CCNG_USER_ORG, CCNG_USER_SPACE, CCNG_SECURITY_GROUP_NAME_TEST);
        // Then
        assertFalse(isSpaceBoundToSecurityGroup(CCNG_USER_SPACE, CCNG_SECURITY_GROUP_NAME_TEST));

        // Cleanup
        connectedClient.deleteSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);
    }

    /**
     * Self tests that the assert mechanisms with jetty and byteman are properly working. If debugging is needed consider enabling one or
     * more of the following system properties -Dorg.jboss.byteman.verbose=true -Dorg.jboss.byteman.debug=true
     * -Dorg.jboss.byteman.rule.debug=true -Dorg.eclipse.jetty.util.log.class=org.eclipse.jetty.util.log.StdErrLog
     * -Dorg.eclipse.jetty.LEVEL=INFO -Dorg.eclipse.jetty.server.LEVEL=INFO -Dorg.eclipse.jetty.server.handler.ConnectHandler=DEBUG
     * Documentation on byteman at http://downloads.jboss.org/byteman/2.1.3/ProgrammersGuideSinglePage.2.1.3.1.html
     */
    @Test
    public void checkByteManrulesAndInJvmProxyAssertMechanisms() {
        if (SKIP_INJVM_PROXY) {
            return; // inJvm Proxy test skipped.
        }
        assertTrue(SocketDestHelper.isSocketRestrictionFlagActive());

        RestUtil restUtil = new RestUtil();
        RestTemplate restTemplateNoProxy = restUtil.createRestTemplate(null, CCNG_API_SSL);

        // When called directly without a proxy, expect an exception to be thrown due to byteman rules
        assertNetworkCallFails(restTemplateNoProxy, new HttpComponentsClientHttpRequestFactory());
        // Repeat that with different request factory used in the code as this exercises different byteman rules
        assertNetworkCallFails(restTemplateNoProxy, new SimpleClientHttpRequestFactory());
        // And with the actual one used by RestUtil, without a proxy configured
        assertNetworkCallFails(restTemplateNoProxy, restUtil.createRequestFactory(null, CCNG_API_SSL));

        // Test with the in-JVM proxy configured
        HttpProxyConfiguration localProxy = new HttpProxyConfiguration("127.0.0.1", inJvmProxyPort);
        RestTemplate restTemplate = restUtil.createRestTemplate(localProxy, CCNG_API_SSL);

        restTemplate.execute(CCNG_API_URL + "/info", HttpMethod.GET, null, null);

        // then executes fine, and the jetty proxy indeed received one request
        assertEquals("expected network calls to make it through the inJvmProxy.", 1, nbInJvmProxyRcvReqs.get());
        nbInJvmProxyRcvReqs.set(0); // reset for next test

        assertTrue(SocketDestHelper.isActivated());
        assertFalse("expected some installed rules, got:" + SocketDestHelper.getInstalledRules(), SocketDestHelper.getInstalledRules()
                                                                                                                  .isEmpty());
    }

    @Test
    public void createAndReCreateApplication() {
        String applicationName = createSpringTravelApp("A");
        assertEquals(1, connectedClient.getApplications()
                                       .size());
        connectedClient.deleteApplication(applicationName);
        applicationName = createSpringTravelApp("A");
        assertEquals(1, connectedClient.getApplications()
                                       .size());
        connectedClient.deleteApplication(applicationName);
    }

    @Test
    public void createApplication() {
        String applicationName = namespacedAppName("travel_test-0");
        List<String> uris = Collections.singletonList(computeApplicationUrl(applicationName));
        Staging staging = ImmutableStaging.builder()
                                          .build();
        connectedClient.createApplication(applicationName, staging, DEFAULT_MEMORY, uris, null);
        CloudApplication application = connectedClient.getApplication(applicationName);
        assertNotNull(application);
        assertEquals(applicationName, application.getName());

        assertNotNull(application.getMetadata()
                                 .getGuid());

        final Calendar now = Calendar.getInstance();
        now.setTime(new Date());
        final Calendar createdDate = Calendar.getInstance();
        createdDate.setTime(application.getMetadata()
                                       .getCreatedAt());

        assertEquals(now.get(Calendar.DATE), createdDate.get(Calendar.DATE));
    }

    @Test
    public void createApplicationWithBuildPack() throws IOException {
        String buildpackUrl = "https://github.com/cloudfoundry/java-buildpack.git";
        String applicationName = namespacedAppName("buildpack");
        createSpringApplication(applicationName, buildpackUrl);

        CloudApplication application = connectedClient.getApplication(applicationName);
        assertNotNull(application);
        assertEquals(CloudApplication.State.STOPPED, application.getState());

        assertEquals(buildpackUrl, application.getStaging()
                                              .getBuildpack());
        assertNull(application.getStaging()
                              .getDetectedBuildpack());
    }

    @Test
    public void createApplicationWithDetectedBuildpack() throws Exception {
        String applicationName = createSpringTravelApp("detectedBuildpack");

        File file = SampleProjects.springTravel();
        connectedClient.uploadApplication(applicationName, file.getCanonicalPath());
        connectedClient.startApplication(applicationName);

        ensureApplicationRunning(applicationName);

        CloudApplication application = connectedClient.getApplication(applicationName);
        Staging staging = application.getStaging();
        assertNotNull(staging.getDetectedBuildpack());
    }

    @Test
    public void createApplicationWithDomainOnly() {
        String applicationName = namespacedAppName("travel_test-tld");

        connectedClient.addDomain(TEST_DOMAIN);
        List<String> uris = Collections.singletonList(TEST_DOMAIN);

        Staging staging = ImmutableStaging.builder()
                                          .build();
        connectedClient.createApplication(applicationName, staging, DEFAULT_MEMORY, uris, null);
        CloudApplication application = connectedClient.getApplication(applicationName);
        assertNotNull(application);
        assertEquals(applicationName, application.getName());

        List<String> actualUris = application.getUris();
        assertTrue(actualUris.size() == 1);
        assertEquals(TEST_DOMAIN, actualUris.get(0));
    }

    @Test
    public void createApplicationWithHealthCheckTimeout() throws IOException {
        String applicationName = namespacedAppName("health_check");
        createSpringApplication(applicationName, null, 2);

        CloudApplication application = connectedClient.getApplication(applicationName);
        assertNotNull(application);
        assertEquals(CloudApplication.State.STOPPED, application.getState());

        assertEquals(2, application.getStaging()
                                   .getHealthCheckTimeout()
                                   .intValue());
    }

    @Test
    public void createApplicationWithService() throws IOException {
        String serviceName = "test_database";
        String applicationName = createSpringTravelApp("application-with-services", Collections.singletonList(serviceName));
        uploadSpringTravelApp(applicationName);
        CloudApplication application = connectedClient.getApplication(applicationName);
        assertNotNull(application);
        assertEquals(applicationName, application.getName());
        assertNotNull(application.getServices());
        assertEquals(1, application.getServices()
                                   .size());
        assertEquals(serviceName, application.getServices()
                                             .get(0));
    }

    @Test
    public void createApplicationWithStack() throws IOException {
        String applicationName = namespacedAppName("stack");
        createSpringApplication(applicationName, DEFAULT_STACK_NAME, null);

        CloudApplication application = connectedClient.getApplication(applicationName);
        assertNotNull(application);
        assertEquals(CloudApplication.State.STOPPED, application.getState());

        assertEquals(DEFAULT_STACK_NAME, application.getStaging()
                                                    .getStack());
    }

    @Test
    public void createGetAndDeleteSpaceOnCurrentOrg() throws Exception {
        String spaceName = "dummy space";
        CloudSpace newSpace = connectedClient.getSpace(spaceName);
        assertNull("Space '" + spaceName + "' should not exist before creation", newSpace);
        connectedClient.createSpace(spaceName);
        newSpace = connectedClient.getSpace(spaceName);
        assertNotNull("newSpace should not be null", newSpace);
        assertEquals(spaceName, newSpace.getName());
        boolean foundSpaceInCurrentOrg = false;
        for (CloudSpace aSpace : connectedClient.getSpaces()) {
            if (spaceName.equals(aSpace.getName())) {
                foundSpaceInCurrentOrg = true;
            }
        }
        assertTrue(foundSpaceInCurrentOrg);
        connectedClient.deleteSpace(spaceName);
        CloudSpace deletedSpace = connectedClient.getSpace(spaceName);
        assertNull("Space '" + spaceName + "' should not exist after deletion", deletedSpace);

    }

    @Test
    public void crudSecurityGroups() throws Exception {
        assumeTrue(CCNG_USER_IS_ADMIN);

        List<SecurityGroupRule> rules = new ArrayList<SecurityGroupRule>();
        SecurityGroupRule rule = ImmutableSecurityGroupRule.builder()
                                                           .protocol("tcp")
                                                           .ports("80, 443")
                                                           .destination("205.158.11.29")
                                                           .build();
        rules.add(rule);
        rule = ImmutableSecurityGroupRule.builder()
                                         .protocol("all")
                                         .destination("0.0.0.0-255.255.255.255")
                                         .build();
        rules.add(rule);
        rule = ImmutableSecurityGroupRule.builder()
                                         .protocol("icmp")
                                         .destination("0.0.0.0/0")
                                         .log(true)
                                         .type(0)
                                         .code(1)
                                         .build();
        rules.add(rule);
        CloudSecurityGroup securityGroup = ImmutableCloudSecurityGroup.builder()
                                                                      .name(CCNG_SECURITY_GROUP_NAME_TEST)
                                                                      .rules(rules)
                                                                      .build();

        // Create
        connectedClient.createSecurityGroup(securityGroup);

        // Verify created
        securityGroup = connectedClient.getSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);
        assertNotNull(securityGroup);
        assertThat(securityGroup.getRules()
                                .size(),
                   is(3));
        assertRulesMatchTestData(securityGroup);

        // Update group
        rules = new ArrayList<SecurityGroupRule>();
        rule = ImmutableSecurityGroupRule.builder()
                                         .protocol("all")
                                         .destination("0.0.0.0-255.255.255.255")
                                         .build();
        rules.add(rule);
        securityGroup = ImmutableCloudSecurityGroup.builder()
                                                   .name(CCNG_SECURITY_GROUP_NAME_TEST)
                                                   .rules(rules)
                                                   .build();
        connectedClient.updateSecurityGroup(securityGroup);

        // Verify update
        securityGroup = connectedClient.getSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);
        assertThat(securityGroup.getRules()
                                .size(),
                   is(1));

        // Delete group
        connectedClient.deleteSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);
        // Verify deleted
        securityGroup = connectedClient.getSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);
        assertNull(securityGroup);
    }

    @Test
    public void defaultDomainFound() throws Exception {
        assertNotNull(connectedClient.getDefaultDomain());
    }

    @Test
    public void deleteApplication() {
        String applicationName = createSpringTravelApp("4");
        assertEquals(1, connectedClient.getApplications()
                                       .size());
        connectedClient.deleteApplication(applicationName);
        assertEquals(0, connectedClient.getApplications()
                                       .size());
    }

    //
    // App configuration tests
    //

    @Test
    public void deleteOrphanedRoutes() {
        connectedClient.addDomain(TEST_DOMAIN);
        connectedClient.addRoute("unbound_route", TEST_DOMAIN, null);

        List<CloudRoute> routes = connectedClient.getRoutes(TEST_DOMAIN);
        CloudRoute unboundRoute = getRouteWithHost("unbound_route", routes);
        assertNotNull(unboundRoute);
        assertEquals(0, unboundRoute.getAppsUsingRoute());

        List<CloudRoute> deletedRoutes = connectedClient.deleteOrphanedRoutes();
        assertNull(getRouteWithHost("unbound_route", connectedClient.getRoutes(TEST_DOMAIN)));

        assertTrue(deletedRoutes.size() > 0);
        boolean found = false;
        for (CloudRoute route : deletedRoutes) {
            if (route.getHost()
                     .equals("unbound_route")) {
                found = true;
            }
        }
        assertTrue(found);
    }

    @Test
    public void deleteServiceThatIsBoundToApp() throws MalformedURLException {
        String serviceName = "mysql-del-svc";
        String applicationName = createSpringTravelApp("del-svc", Collections.singletonList(serviceName));

        CloudApplication application = connectedClient.getApplication(applicationName);
        assertNotNull(application.getServices());
        assertEquals(1, application.getServices()
                                   .size());
        assertEquals(serviceName, application.getServices()
                                             .get(0));

        connectedClient.deleteService(serviceName);
    }

    @Test
    public void eventsAvailable() throws Exception {
        List<CloudEvent> events = connectedClient.getEvents();
        assertEvents(events);
    }

    @Test
    public void getApplicationByGuid() {
        String applicationName = createSpringTravelApp("3");
        CloudApplication application = connectedClient.getApplication(applicationName);
        CloudApplication guidApplication = connectedClient.getApplication(application.getMetadata()
                                                                                     .getGuid());
        assertEquals(application.getName(), guidApplication.getName());
    }

    @Test
    public void getApplicationByName() {
        final String serviceName = "test_database";
        String applicationName = createSpringTravelApp("1", Collections.singletonList(serviceName));
        CloudApplication application = connectedClient.getApplication(applicationName);
        assertNotNull(application);
        assertEquals(applicationName, application.getName());

        assertEquals(1, application.getServices()
                                   .size());
        assertEquals(serviceName, application.getServices()
                                             .get(0));
        assertEquals(CCNG_USER_SPACE, application.getSpace()
                                                 .getName());

        assertEquals(1, application.getInstances());
        assertEquals(DEFAULT_MEMORY, application.getMemory());
        assertEquals(DEFAULT_DISK, application.getDiskQuota());

        assertNull(application.getStaging()
                              .getCommand());
        assertNull(application.getStaging()
                              .getBuildpack());
        assertNull(application.getStaging()
                              .getHealthCheckTimeout());
    }

    @Test
    public void getApplicationEnvironmentByGuid() {
        String applicationName = namespacedAppName("simple-app");
        List<String> uris = Collections.singletonList(computeApplicationUrl(applicationName));
        Staging staging = ImmutableStaging.builder()
                                          .build();
        connectedClient.createApplication(applicationName, staging, DEFAULT_MEMORY, uris, null);
        connectedClient.updateApplicationEnv(applicationName, Collections.singletonMap("testKey", "testValue"));
        CloudApplication application = connectedClient.getApplication(applicationName);
        Map<String, String> env = connectedClient.getApplicationEnvironment(application.getMetadata()
                                                                                       .getGuid());
        assertAppEnvironment(env);
    }

    @Test
    public void getApplicationEnvironmentByName() {
        String applicationName = namespacedAppName("simple-app");
        List<String> uris = Collections.singletonList(computeApplicationUrl(applicationName));
        Staging staging = ImmutableStaging.builder()
                                          .build();
        connectedClient.createApplication(applicationName, staging, DEFAULT_MEMORY, uris, null);
        connectedClient.updateApplicationEnv(applicationName, Collections.singletonMap("testKey", "testValue"));
        Map<String, String> env = connectedClient.getApplicationEnvironment(applicationName);
        assertAppEnvironment(env);
    }

    @Test
    public void getApplicationInstances() throws Exception {
        String applicationName = namespacedAppName("instance1");
        CloudApplication application = createAndUploadAndStartSimpleSpringApp(applicationName);
        assertEquals(1, application.getInstances());

        boolean passSingleInstance = getInstanceInfosWithTimeout(applicationName, 1, true);
        assertTrue("Couldn't get the right application state in 50 tries", passSingleInstance);

        boolean passSingleMultipleInstances = getInstanceInfosWithTimeout(applicationName, 3, true);
        assertTrue("Couldn't get the right application state in 50 tries", passSingleMultipleInstances);

        connectedClient.stopApplication(applicationName);
        InstancesInfo instInfo = connectedClient.getApplicationInstances(applicationName);
        assertNull(instInfo);
    }

    @Test
    public void getApplicationNonExistent() {
        thrown.expect(CloudOperationException.class);
        thrown.expect(hasProperty("statusCode", is(HttpStatus.NOT_FOUND)));
        thrown.expectMessage(containsString("Not Found"));
        String applicationName = namespacedAppName("non_existent");
        connectedClient.getApplication(applicationName);
    }

    //
    // Advanced Application tests
    //

    @Test
    public void getApplications() {
        final String serviceName = "test_database";
        String applicationName = createSpringTravelApp("2", Collections.singletonList(serviceName));
        List<CloudApplication> apps = connectedClient.getApplications();
        assertEquals(1, apps.size());

        CloudApplication application = apps.get(0);
        assertEquals(applicationName, application.getName());
        assertNotNull(application.getMetadata());
        assertNotNull(application.getMetadata()
                                 .getGuid());

        assertEquals(1, application.getServices()
                                   .size());
        assertEquals(serviceName, application.getServices()
                                             .get(0));

        createSpringTravelApp("3");
        apps = connectedClient.getApplications();
        assertEquals(2, apps.size());
    }

    @Test
    public void getApplicationsMatchGetApplication() {
        String applicationName = createSpringTravelApp("1");
        List<CloudApplication> applications = connectedClient.getApplications();
        assertEquals(1, applications.size());
        CloudApplication application = connectedClient.getApplication(applicationName);
        assertEquals(application.getName(), applications.get(0)
                                                        .getName());
        assertEquals(application.getState(), applications.get(0)
                                                         .getState());
        assertEquals(application.getInstances(), applications.get(0)
                                                             .getInstances());
        assertEquals(application.getMemory(), applications.get(0)
                                                          .getMemory());
        assertEquals(application.getMetadata()
                                .getGuid(),
                     applications.get(0)
                                 .getMetadata()
                                 .getGuid());
        assertEquals(application.getMetadata()
                                .getCreatedAt(),
                     applications.get(0)
                                 .getMetadata()
                                 .getCreatedAt());
        assertEquals(application.getMetadata()
                                .getUpdatedAt(),
                     applications.get(0)
                                 .getMetadata()
                                 .getUpdatedAt());
        assertEquals(application.getUris(), applications.get(0)
                                                        .getUris());
    }

    @Test
    @Ignore("Ignore until the Java buildpack detects app crashes upon OOM correctly")
    public void getCrashLogs() throws Exception {
        String applicationName = namespacedAppName("simple_crashlogs");
        createAndUploadSimpleSpringApp(applicationName);
        connectedClient.updateApplicationEnv(applicationName, Collections.singletonMap("crash", "true"));
        connectedClient.startApplication(applicationName);

        boolean pass = getInstanceInfosWithTimeout(applicationName, 1, false);
        assertTrue("Couldn't get the right application state in 50 tries", pass);

        Map<String, String> logs = connectedClient.getCrashLogs(applicationName);
        assertNotNull(logs);
        assertTrue(logs.size() > 0);
        for (String log : logs.keySet()) {
            assertNotNull(logs.get(log));
        }
    }

    @Test
    @Ignore("Ignore until the Java buildpack detects app crashes upon OOM correctly")
    public void getCrashes() throws IOException, InterruptedException {
        String applicationName = namespacedAppName("crashes1");
        createAndUploadSimpleSpringApp(applicationName);
        connectedClient.updateApplicationEnv(applicationName, Collections.singletonMap("crash", "true"));
        connectedClient.startApplication(applicationName);

        boolean pass = getInstanceInfosWithTimeout(applicationName, 1, false);
        assertTrue("Couldn't get the right application state in 50 tries", pass);

        CrashesInfo crashes = connectedClient.getCrashes(applicationName);
        assertNotNull(crashes);
        assertTrue(!crashes.getCrashes()
                           .isEmpty());
        for (CrashInfo info : crashes.getCrashes()) {
            assertNotNull(info.getInstance());
            assertNotNull(info.getSince());
        }
    }

    @Test
    public void getCreateDeleteService() throws MalformedURLException {
        String serviceName = "mysql-test";
        createMySqlService(serviceName);

        CloudService service = connectedClient.getService(serviceName);
        assertNotNull(service);
        assertEquals(serviceName, service.getName());
        assertTimeWithinRange("Creation time should be very recent", service.getMetadata()
                                                                            .getCreatedAt()
                                                                            .getTime(),
                              FIVE_MINUTES);

        connectedClient.deleteService(serviceName);

        List<CloudService> services = connectedClient.getServices();
        assertNotNull(services);
        assertEquals(0, services.size());
    }

    @Test
    public void getCurrentOrganizationUsersAndEnsureCurrentUserIsAMember() {
        String organizationName = CCNG_USER_ORG;
        Map<String, CloudUser> orgUsers = connectedClient.getOrganizationUsers(organizationName);
        assertNotNull(orgUsers);
        assertTrue("Org " + organizationName + " should at least contain 1 user", orgUsers.size() > 0);
        String username = CCNG_USER_EMAIL;
        assertTrue("Organization user list should contain current user", orgUsers.containsKey(username));
    }

    @Test
    public void getDomains() {
        connectedClient.addDomain(TEST_DOMAIN);

        List<CloudDomain> allDomains = connectedClient.getDomains();

        assertNotNull(getDomainNamed(defaultDomainName, allDomains));
        assertNotNull(getDomainNamed(TEST_DOMAIN, allDomains));
    }

    @Test
    public void getFile() throws Exception {
        String applicationName = namespacedAppName("simple_getFile");
        createAndUploadAndStartSimpleSpringApp(applicationName);
        boolean running = getInstanceInfosWithTimeout(applicationName, 1, true);
        assertTrue("App failed to start", running);
        doGetFile(connectedClient, applicationName);
    }

    @Test
    public void getLogs() throws Exception {
        String applicationName = namespacedAppName("simple_logs");
        createAndUploadAndStartSimpleSpringApp(applicationName);
        boolean pass = getInstanceInfosWithTimeout(applicationName, 1, true);
        assertTrue("Couldn't get the right application state", pass);

        Thread.sleep(10000); // let's have some time to get some logs generated
        Map<String, String> logs = connectedClient.getLogs(applicationName);
        assertNotNull(logs);
        assertTrue(logs.size() > 0);
    }

    @Test
    public void getRestLog() throws IOException {
        final List<RestLogEntry> log1 = new ArrayList<RestLogEntry>();
        final List<RestLogEntry> log2 = new ArrayList<RestLogEntry>();
        connectedClient.registerRestLogListener(new RestLogCallback() {
            public void onNewLogEntry(RestLogEntry logEntry) {
                log1.add(logEntry);
            }
        });
        RestLogCallback callback2 = new RestLogCallback() {
            public void onNewLogEntry(RestLogEntry logEntry) {
                log2.add(logEntry);
            }
        };
        connectedClient.registerRestLogListener(callback2);
        getApplications();
        connectedClient.deleteAllApplications();
        connectedClient.deleteAllServices();
        assertTrue(log1.size() > 0);
        assertEquals(log1, log2);
        connectedClient.unRegisterRestLogListener(callback2);
        getApplications();
        connectedClient.deleteAllApplications();
        assertTrue(log1.size() > log2.size());
    }

    @Test
    public void getService() {
        String serviceName = "mysql-test";

        CloudService expectedService = createMySqlService(serviceName);
        CloudService service = connectedClient.getService(serviceName);

        assertNotNull(service);
        assertServicesEqual(expectedService, service);
    }

    @Test
    public void getServiceBrokers() {
        assumeTrue(CCNG_USER_IS_ADMIN);

        List<CloudServiceBroker> brokers = connectedClient.getServiceBrokers();
        assertNotNull(brokers);
        assertTrue(brokers.size() >= 1);
        CloudServiceBroker broker0 = brokers.get(0);
        assertNotNull(broker0.getMetadata());
        assertNotNull(broker0.getName());
        assertNotNull(broker0.getUrl());
        assertNotNull(broker0.getUsername());
    }

    @Test
    public void getServiceInstance() {
        String serviceName = "mysql-instance-test";
        String applicationName = createSpringTravelApp("service-instance-app", Collections.singletonList(serviceName));

        CloudApplication application = connectedClient.getApplication(applicationName);

        CloudServiceInstance serviceInstance = connectedClient.getServiceInstance(serviceName);
        assertNotNull(serviceInstance);
        assertEquals(serviceName, serviceInstance.getName());
        assertNotNull(serviceInstance.getDashboardUrl());
        assertNotNull(serviceInstance.getType());
        assertNotNull(serviceInstance.getCredentials());

        CloudService service = serviceInstance.getService();
        assertNotNull(service);
        assertEquals(MYSQL_SERVICE_LABEL, service.getLabel());
        assertEquals(MYSQL_SERVICE_PLAN, service.getPlan());

        List<CloudServiceBinding> bindings = serviceInstance.getBindings();
        assertNotNull(bindings);
        assertEquals(1, bindings.size());
        CloudServiceBinding binding = bindings.get(0);
        assertEquals(application.getMetadata()
                                .getGuid(),
                     binding.getApplicationGuid());
        assertNotNull(binding.getCredentials());
        assertTrue(binding.getCredentials()
                          .size() > 0);
        assertNotNull(binding.getBindingOptions());
        assertEquals(0, binding.getBindingOptions()
                               .size());
        assertNull(binding.getSyslogDrainUrl());
    }

    @Test
    public void getServiceOfferings() {
        List<CloudServiceOffering> offerings = connectedClient.getServiceOfferings();

        assertNotNull(offerings);
        assertTrue(offerings.size() >= 2);

        CloudServiceOffering offering = null;
        for (CloudServiceOffering so : offerings) {
            if (so.getName()
                  .equals(MYSQL_SERVICE_LABEL)) {
                offering = so;
                break;
            }
        }
        assertNotNull(offering);
        assertNotNull(offering.getName());
        assertEquals(MYSQL_SERVICE_LABEL, offering.getName());
        assertNotNull(offering.getServicePlans());
        assertTrue(offering.getServicePlans()
                           .size() > 0);
        assertNotNull(offering.getDescription());
        assertNotNull(offering.getUniqueId());
        assertNotNull(offering.getExtra());

        CloudServicePlan plan = offering.getServicePlans()
                                        .get(0);
        assertNotNull(plan.getName());
        assertNotNull(plan.getUniqueId());
        assertNotNull(plan.getDescription());
    }

    @Test
    public void getServiceWithVersionAndProvider() {
        String serviceName = "mysql-test-version-provider";

        CloudService expectedService = createMySqlServiceWithVersionAndProvider(serviceName);
        CloudService service = connectedClient.getService(serviceName);

        assertNotNull(service);
        assertServicesEqual(expectedService, service);
        assertEquals(expectedService.getProvider(), service.getProvider());
        assertEquals(expectedService.getVersion(), service.getVersion());
    }

    @Test
    public void getServices() {
        List<CloudService> expectedServices = Arrays.asList(createMySqlService("mysql-test"),
                                                            createUserProvidedService("user-provided-test"),
                                                            createMySqlService("mysql-child"));

        List<CloudService> services = connectedClient.getServices();
        assertNotNull(services);
        assertEquals(3, services.size());
        for (CloudService expectedService : expectedServices) {
            assertServiceMatching(expectedService, services);
        }
    }

    @Test
    public void getStack() throws Exception {
        CloudStack stack = connectedClient.getStack(DEFAULT_STACK_NAME);
        assertNotNull(stack);
        assertNotNull(stack.getMetadata()
                           .getGuid());
        assertEquals(DEFAULT_STACK_NAME, stack.getName());
        assertNotNull(stack.getDescription());
    }

    @Test
    public void getStacks() throws Exception {
        List<CloudStack> stacks = connectedClient.getStacks();
        assert (stacks.size() >= 1);

        CloudStack stack = null;
        for (CloudStack s : stacks) {
            if (DEFAULT_STACK_NAME.equals(s.getName())) {
                stack = s;
            }
        }
        assertNotNull(stack);
        assertNotNull(stack.getMetadata()
                           .getGuid());
        assertEquals(DEFAULT_STACK_NAME, stack.getName());
        assertNotNull(stack.getDescription());
    }

    @Test
    public void getStagingLogs() throws Exception {
        String applicationName = createSpringTravelApp("stagingLogs");

        File file = SampleProjects.springTravel();
        connectedClient.uploadApplication(applicationName, file.getCanonicalPath());

        StartingInfo startingInfo;
        String firstLine = null;
        int i = 0;
        do {
            startingInfo = connectedClient.startApplication(applicationName);

            if (startingInfo != null && startingInfo.getStagingFile() != null) {
                int offset = 0;
                firstLine = connectedClient.getStagingLogs(startingInfo, offset);
            }

            if (startingInfo != null && startingInfo.getStagingFile() != null && firstLine != null) {
                break;
            } else {
                connectedClient.stopApplication(applicationName);
                Thread.sleep(10000);
            }
        } while (++i < 5);

        assertNotNull(startingInfo);
        assertNotNull(startingInfo.getStagingFile());
        assertNotNull(firstLine);
        assertTrue(firstLine.length() > 0);
    }

    //
    // Files and Log tests
    //

    @Test
    public void getUserProvidedService() {
        String serviceName = "user-provided-test-service";

        CloudService expectedService = createUserProvidedService(serviceName);
        CloudService service = connectedClient.getService(serviceName);

        assertNotNull(service);
        assertServicesEqual(expectedService, service);
    }

    @Test
    public void infoAvailable() throws Exception {
        CloudInfo info = connectedClient.getCloudInfo();
        assertNotNull(info.getName());
        assertNotNull(info.getSupport());
        assertNotNull(info.getBuild());
    }

    @Test
    public void infoAvailableWithoutLoggingIn() throws Exception {
        CloudControllerClientImpl infoClient = new CloudControllerClientImpl(new URL(CCNG_API_URL), httpProxyConfiguration, CCNG_API_SSL);
        CloudInfo info = infoClient.getCloudInfo();
        assertNotNull(info.getName());
        assertNotNull(info.getSupport());
        assertNotNull(info.getBuild());
        assertTrue(info.getUser() == null);
    }

    @Test
    public void infoForUserAvailable() throws Exception {
        CloudInfo info = connectedClient.getCloudInfo();

        assertNotNull(info.getName());
        assertNotNull(info.getSupport());
        assertNotNull(info.getBuild());
        assertNotNull(info.getSupport());
        assertNotNull(info.getSupport());

        assertEquals(CCNG_USER_EMAIL, info.getUser());
    }

    @Test
    public void openFile() throws Exception {
        String applicationName = namespacedAppName("simple_openFile");
        createAndUploadAndStartSimpleSpringApp(applicationName);
        boolean running = getInstanceInfosWithTimeout(applicationName, 1, true);
        assertTrue("App failed to start", running);
        doOpenFile(connectedClient, applicationName);
    }

    @Test
    public void orgsAvailable() throws Exception {
        List<CloudOrganization> orgs = connectedClient.getOrganizations();
        assertNotNull(orgs);
        assertTrue(orgs.size() > 0);
    }

    //
    // Basic Services tests
    //

    @Test
    public void paginationWorksForUris() throws IOException {
        String applicationName = namespacedAppName("page-url1");
        CloudApplication application = createAndUploadSimpleTestApp(applicationName);

        List<String> originalUris = application.getUris();
        assertEquals(Collections.singletonList(computeApplicationUrl(applicationName)), originalUris);

        List<String> uris = new ArrayList<String>(application.getUris());
        for (int i = 2; i < 55; i++) {
            uris.add(computeApplicationUrl(namespacedAppName("page-url" + i)));
        }
        connectedClient.updateApplicationUris(applicationName, uris);

        application = connectedClient.getApplication(applicationName);
        List<String> applicationUris = application.getUris();
        assertNotNull(applicationUris);
        assertEquals(uris.size(), applicationUris.size());
        for (String uri : uris) {
            assertTrue("Missing URI: " + uri, applicationUris.contains(uri));
        }
    }

    @Test
    public void quotasAvailable() throws Exception {
        List<CloudQuota> quotas = connectedClient.getQuotas();
        assertNotNull(quotas);
        assertTrue(quotas.size() > 0);
    }

    @Test
    public void refreshTokenOnExpiration() throws Exception {
        URL cloudControllerUrl = new URL(CCNG_API_URL);
        CloudCredentials credentials = new CloudCredentials(CCNG_USER_EMAIL, CCNG_USER_PASS);

        CloudControllerRestClientFactory factory = ImmutableCloudControllerRestClientFactory.builder()
                                                                                            .httpProxyConfiguration(httpProxyConfiguration)
                                                                                            .shouldTrustSelfSignedCertificates(CCNG_API_SSL)
                                                                                            .build();
        CloudControllerRestClient client = factory.createClient(cloudControllerUrl, credentials, CCNG_USER_ORG, CCNG_USER_SPACE);

        client.login();

        validateClientAccess(client);

        OAuthClient oAuthClient = client.getOAuthClient();
        OAuth2AccessToken token = oAuthClient.getToken();
        if (token instanceof DefaultOAuth2AccessToken) {
            // set the token expiration to "now", forcing the access token to be refreshed
            ((DefaultOAuth2AccessToken) token).setExpiration(new Date());
            validateClientAccess(client);
        } else {
            fail("Error forcing expiration of access token");
        }
    }

    @Test
    public void renameApplication() {
        String applicationName = createSpringTravelApp("5");
        CloudApplication application = connectedClient.getApplication(applicationName);
        assertNotNull(application);
        assertEquals(applicationName, application.getName());
        String newName = namespacedAppName("travel_test-6");
        connectedClient.rename(applicationName, newName);
        CloudApplication newApp = connectedClient.getApplication(newName);
        assertNotNull(newApp);
        assertEquals(newName, newApp.getName());
    }

    @Test
    public void securityGroupsCanBeCreatedAndUpdatedFromJsonFiles() throws FileNotFoundException {
        assumeTrue(CCNG_USER_IS_ADMIN);

        // Create
        connectedClient.createSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST,
                                            new FileInputStream(new File("src/test/resources/security-groups/test-rules-1.json")));

        // Verify created
        CloudSecurityGroup securityGroup = connectedClient.getSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);
        assertNotNull(securityGroup);
        assertThat(securityGroup.getRules()
                                .size(),
                   is(4));
        assertRulesMatchThoseInJsonFile1(securityGroup);

        // Update group
        connectedClient.updateSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST,
                                            new FileInputStream(new File("src/test/resources/security-groups/test-rules-2.json")));

        // Verify update
        securityGroup = connectedClient.getSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);
        assertThat(securityGroup.getRules()
                                .size(),
                   is(1));

        // Clean up after ourselves
        connectedClient.deleteSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);
    }

    @Test
    public void serviceBrokerLifecycle() throws IOException {
        assumeTrue(CCNG_USER_IS_ADMIN);

        createAndUploadAndStartSampleServiceBrokerApp("haash-broker");

        boolean pass = ensureApplicationRunning("haash-broker");
        assertTrue("haash-broker failed to start", pass);

        CloudServiceBroker newBroker = ImmutableCloudServiceBroker.builder()
                                                                  .metadata(ImmutableCloudMetadata.builder()
                                                                                                  .build())
                                                                  .name("haash-broker")
                                                                  .username("warreng")
                                                                  .password("snoopdogg")
                                                                  .url("http://haash-broker.cf.deepsouthcloud.com")
                                                                  .build();
        connectedClient.createServiceBroker(newBroker);

        CloudServiceBroker broker = connectedClient.getServiceBroker("haash-broker");
        assertNotNull(broker);
        assertNotNull(broker.getMetadata());
        assertEquals("haash-broker", broker.getName());
        assertEquals("http://haash-broker.cf.deepsouthcloud.com", broker.getUrl());
        assertEquals("warreng", broker.getUsername());
        assertNull(broker.getPassword());

        connectedClient.updateServiceBroker(newBroker);

        connectedClient.updateServicePlanVisibilityForBroker("haash-broker", true);
        connectedClient.updateServicePlanVisibilityForBroker("haash-broker", false);

        connectedClient.deleteServiceBroker("haash-broker");
    }

    /*
     * @Test public void getServiceBroker() { assumeTrue(CCNG_USER_IS_ADMIN);
     * 
     * CloudServiceBroker broker = connectedClient.getServiceBroker("haash-broker"); assertNotNull(broker); assertNotNull(broker.getMeta());
     * assertEquals("haash-broker", broker.getName()); assertEquals("http://haash-broker.cf.deepsouthcloud.com", broker.getUrl());
     * assertEquals("warreng", broker.getUsername()); assertNull(broker.getPassword()); }
     * 
     * @Test public void createServiceBroker() { assumeTrue(CCNG_USER_IS_ADMIN);
     * 
     * CloudServiceBroker newBroker = new CloudServiceBroker(CloudEntity.Meta.defaultMeta(), "haash-broker",
     * "http://haash-broker.cf.deepsouthcloud.com", "warreng", "natedogg"); connectedClient.createServiceBroker(newBroker); }
     * 
     * @Test public void updateServiceBroker() { assumeTrue(CCNG_USER_IS_ADMIN);
     * 
     * CloudServiceBroker newBroker = new CloudServiceBroker(CloudEntity.Meta.defaultMeta(), "haash-broker",
     * "http://haash-broker.cf.deepsouthcloud.com", "warreng", "snoopdogg"); connectedClient.updateServiceBroker(newBroker); }
     */

    @Test
    public void setEnvironmentThroughMap() throws IOException {
        String applicationName = createSpringTravelApp("env3");
        CloudApplication application = connectedClient.getApplication(applicationName);
        assertTrue(application.getEnv()
                              .isEmpty());

        Map<String, String> env1 = new HashMap<String, String>();
        env1.put("foo", "bar");
        env1.put("bar", "baz");
        connectedClient.updateApplicationEnv(applicationName, env1);
        application = connectedClient.getApplication(application.getName());
        assertEquals(env1, application.getEnv());

        Map<String, String> env2 = new HashMap<String, String>();
        env2.put("foo", "baz");
        env2.put("baz", "bong");
        connectedClient.updateApplicationEnv(applicationName, env2);
        application = connectedClient.getApplication(application.getName());

        assertEquals(env2, application.getEnv());

        connectedClient.updateApplicationEnv(applicationName, new HashMap<String, String>());
        application = connectedClient.getApplication(application.getName());
        assertTrue(application.getEnv()
                              .isEmpty());
    }

    @Test
    public void setEnvironmentThroughMapEqualsInValue() throws IOException {
        String applicationName = createSpringTravelApp("env4");
        CloudApplication application = connectedClient.getApplication(applicationName);
        assertTrue(application.getEnv()
                              .isEmpty());

        Map<String, String> env1 = new HashMap<String, String>();
        env1.put("key", "foo=bar,fu=baz");
        connectedClient.updateApplicationEnv(applicationName, env1);
        application = connectedClient.getApplication(application.getName());

        assertEquals(env1, application.getEnv());

        connectedClient.updateApplicationEnv(applicationName, new HashMap<String, String>());
        application = connectedClient.getApplication(application.getName());
        assertTrue(application.getEnv()
                              .isEmpty());
    }

    @Before
    public void setUp() throws Exception {
        URL cloudControllerUrl;

        cloudControllerUrl = new URL(CCNG_API_URL);
        connectedClient = new CloudControllerClientImpl(cloudControllerUrl,
                                                        new CloudCredentials(CCNG_USER_EMAIL, CCNG_USER_PASS),
                                                        CCNG_USER_ORG,
                                                        CCNG_USER_SPACE,
                                                        httpProxyConfiguration,
                                                        CCNG_API_SSL);
        connectedClient.login();
        defaultDomainName = connectedClient.getDefaultDomain()
                                           .getName();

        // Optimization to avoid redoing the work already done is tearDown()
        if (!tearDownComplete) {
            tearDown();
        }
        tearDownComplete = false;
        connectedClient.addDomain(TEST_DOMAIN);

        // connectedClient.registerRestLogListener(new RestLogger("CF_REST"));
        if (nbInJvmProxyRcvReqs != null) {
            nbInJvmProxyRcvReqs.set(0); // reset calls made in setup to leave a clean state for tests to assert
        }

        if (!SKIP_INJVM_PROXY) {
            new SocketDestHelper().setForbiddenOnCurrentThread();
        }
    }

    //
    // Application and Services tests
    //

    @Test
    public void spacesAvailable() throws Exception {
        List<CloudSpace> spaces = connectedClient.getSpaces();
        assertNotNull(spaces);
        assertTrue(spaces.size() > 0);
    }

    @Test
    public void startExplodedApplication() throws IOException {
        String applicationName = namespacedAppName("exploded_app");
        createAndUploadExplodedSpringTestApp(applicationName);
        connectedClient.startApplication(applicationName);
        CloudApplication application = connectedClient.getApplication(applicationName);
        assertEquals(CloudApplication.State.STARTED, application.getState());
    }

    @Test
    public void startStopApplication() throws IOException {
        String applicationName = createSpringTravelApp("upload-start-stop");
        CloudApplication application = uploadSpringTravelApp(applicationName);
        assertNotNull(application);
        assertEquals(CloudApplication.State.STOPPED, application.getState());

        String url = computeApplicationUrlNoProtocol(applicationName);
        assertEquals(url, application.getUris()
                                     .get(0));

        StartingInfo info = connectedClient.startApplication(applicationName);
        application = connectedClient.getApplication(applicationName);
        assertEquals(CloudApplication.State.STARTED, application.getState());
        assertNotNull(info);
        assertNotNull(info.getStagingFile());

        connectedClient.stopApplication(applicationName);
        application = connectedClient.getApplication(applicationName);
        assertEquals(CloudApplication.State.STOPPED, application.getState());
    }

    @Test
    public void streamLogs() throws Exception {
        // disable proxy validation for this test, since Loggregator websockets
        // connectivity does not currently support proxies
        new SocketDestHelper().setAllowedOnCurrentThread();

        String applicationName = namespacedAppName("simple_logs");
        CloudApplication application = createAndUploadAndStartSimpleSpringApp(applicationName);
        boolean pass = getInstanceInfosWithTimeout(applicationName, 1, true);
        assertTrue("Couldn't get the right application state", pass);

        List<ApplicationLog> logs = doGetRecentLogs(applicationName);

        for (int index = 0; index < logs.size() - 1; index++) {
            int comparison = logs.get(index)
                                 .getTimestamp()
                                 .compareTo(logs.get(index + 1)
                                                .getTimestamp());
            assertTrue("Logs are not properly sorted", comparison <= 0);
        }

        AccumulatingApplicationLogListener testListener = new AccumulatingApplicationLogListener();
        connectedClient.streamLogs(applicationName, testListener);
        String appUri = "http://" + application.getUris()
                                               .get(0);
        RestTemplate appTemplate = new RestTemplate();
        int attempt = 0;
        do {
            // no need to sleep, visiting the app uri should be sufficient
            try {
                appTemplate.getForObject(appUri, String.class);
            } catch (HttpClientErrorException ex) {
                // ignore
            }
            if (testListener.logs.size() > 0) {
                break;
            }
            Thread.sleep(1000);
        } while (attempt++ < 30);
        assertTrue("Failed to stream normal log", testListener.logs.size() > 0);
    }

    @After
    public void tearDown() throws Exception {
        // Clean after ourselves so that there are no leftover apps, services, domains, and routes
        if (connectedClient != null) { // may happen if setUp() fails
            connectedClient.deleteAllApplications();
            connectedClient.deleteAllServices();
            clearTestDomainAndRoutes();
            deleteAnyOrphanedTestSecurityGroups();
        }
        tearDownComplete = true;
    }

    @Test
    public void updateApplicationDisk() throws IOException {
        String applicationName = createSpringTravelApp("updateDisk");
        connectedClient.updateApplicationDiskQuota(applicationName, 2048);
        CloudApplication application = connectedClient.getApplication(applicationName);
        assertEquals(2048, application.getDiskQuota());
    }

    @Test
    public void updateApplicationInstances() throws Exception {
        String applicationName = createSpringTravelApp("updateInstances");
        CloudApplication application = connectedClient.getApplication(applicationName);

        assertEquals(1, application.getInstances());

        connectedClient.updateApplicationInstances(applicationName, 3);
        application = connectedClient.getApplication(applicationName);
        assertEquals(3, application.getInstances());

        connectedClient.updateApplicationInstances(applicationName, 1);
        application = connectedClient.getApplication(applicationName);
        assertEquals(1, application.getInstances());
    }

    @Test
    public void updateApplicationMemory() throws IOException {
        String applicationName = createSpringTravelApp("updateMemory");
        connectedClient.updateApplicationMemory(applicationName, 256);
        CloudApplication application = connectedClient.getApplication(applicationName);
        assertEquals(256, application.getMemory());
    }

    @Test
    public void updateApplicationService() throws IOException {
        String serviceName = "test_database";
        createMySqlService(serviceName);
        String applicationName = createSpringTravelApp("7");

        connectedClient.updateApplicationServices(applicationName, Collections.emptyMap(),
                                                  ApplicationServicesUpdateCallback.DEFAULT_APPLICATION_SERVICES_UPDATE_CALLBACK);
        CloudApplication application = connectedClient.getApplication(applicationName);
        assertNotNull(application.getServices());
        assertTrue(application.getServices()
                              .size() > 0);
        assertEquals(serviceName, application.getServices()
                                             .get(0));

        connectedClient.updateApplicationServices(applicationName, Collections.emptyMap(),
                                                  ApplicationServicesUpdateCallback.DEFAULT_APPLICATION_SERVICES_UPDATE_CALLBACK);
        application = connectedClient.getApplication(applicationName);
        assertNotNull(application.getServices());
    }

    @Test
    public void updateApplicationUris() throws IOException {
        String applicationName = namespacedAppName("updateUris");
        CloudApplication application = createAndUploadAndStartSimpleSpringApp(applicationName);

        List<String> originalUris = application.getUris();
        assertEquals(Collections.singletonList(computeApplicationUrlNoProtocol(applicationName)), originalUris);

        List<String> uris = new ArrayList<String>(application.getUris());
        uris.add(computeApplicationUrlNoProtocol(namespacedAppName("url2")));
        connectedClient.updateApplicationUris(applicationName, uris);
        application = connectedClient.getApplication(applicationName);
        List<String> newUris = application.getUris();
        assertNotNull(newUris);
        assertEquals(uris.size(), newUris.size());
        for (String uri : uris) {
            assertTrue(newUris.contains(uri));
        }
        connectedClient.updateApplicationUris(applicationName, originalUris);
        application = connectedClient.getApplication(applicationName);
        assertEquals(originalUris, application.getUris());
    }

    @Test
    public void updatePassword() throws MalformedURLException {
        // Not working currently
        assumeTrue(false);

        String newPassword = "newPass123";
        connectedClient.updatePassword(newPassword);
        CloudControllerClientImpl clientWithChangedPassword = new CloudControllerClientImpl(new URL(CCNG_API_URL),
                                                                                            new CloudCredentials(CCNG_USER_EMAIL,
                                                                                                                 newPassword),
                                                                                            httpProxyConfiguration);
        clientWithChangedPassword.login();

        // Revert
        connectedClient.updatePassword(CCNG_USER_PASS);
        connectedClient.login();
    }

    @Test
    public void updateStandaloneApplicationCommand() throws IOException {
        String applicationName = namespacedAppName("standalone-ruby");
        List<String> uris = new ArrayList<String>();
        List<String> services = new ArrayList<String>();
        createStandaloneRubyTestApp(applicationName, uris, services);
        connectedClient.startApplication(applicationName);
        CloudApplication application = connectedClient.getApplication(applicationName);
        assertNotNull(application);
        assertEquals(CloudApplication.State.STARTED, application.getState());
        assertEquals(uris, application.getUris());
        assertEquals("ruby simple.rb", application.getStaging()
                                                  .getCommand());
        connectedClient.stopApplication(applicationName);

        Staging newStaging = ImmutableStaging.builder()
                                             .command("ruby simple.rb test")
                                             .buildpacks(Arrays.asList("https://github.com/cloudfoundry/heroku-buildpack-ruby"))
                                             .build();
        connectedClient.updateApplicationStaging(applicationName, newStaging);
        application = connectedClient.getApplication(applicationName);
        assertNotNull(application);
        assertEquals(uris, application.getUris());
        assertEquals("ruby simple.rb test", application.getStaging()
                                                       .getCommand());
        assertEquals("https://github.com/cloudfoundry/heroku-buildpack-ruby", application.getStaging()
                                                                                         .getBuildpack());
    }

    @Test
    public void uploadAppFromInputStream() throws IOException {
        String applicationName = namespacedAppName("upload-from-input-stream");
        createSpringApplication(applicationName);
        File file = SampleProjects.springTravel();
        FileInputStream inputStream = new FileInputStream(file);
        connectedClient.uploadApplication(applicationName, inputStream);
        connectedClient.startApplication(applicationName);
        CloudApplication env = connectedClient.getApplication(applicationName);
        assertEquals(CloudApplication.State.STARTED, env.getState());
    }

    @Test
    public void uploadAppWithNonAsciiFileName() throws IOException {
        String applicationName = namespacedAppName("non-ascii");
        List<String> uris = new ArrayList<String>();
        uris.add(computeApplicationUrl(applicationName));

        File war = SampleProjects.nonAsciFileName();
        List<String> serviceNames = new ArrayList<String>();

        Staging staging = ImmutableStaging.builder()
                                          .build();
        connectedClient.createApplication(applicationName, staging, DEFAULT_MEMORY, uris, serviceNames);
        connectedClient.uploadApplication(applicationName, war.getCanonicalPath());

        CloudApplication application = connectedClient.getApplication(applicationName);
        assertNotNull(application);
        assertEquals(CloudApplication.State.STOPPED, application.getState());

        connectedClient.startApplication(applicationName);

        application = connectedClient.getApplication(applicationName);
        assertNotNull(application);
        assertEquals(CloudApplication.State.STARTED, application.getState());

        connectedClient.deleteApplication(applicationName);
    }

    @Test
    public void uploadAppWithNonUnsubscribingCallback() throws IOException {
        String applicationName = namespacedAppName("upload-non-unsubscribing-callback");
        createSpringApplication(applicationName);
        File file = SampleProjects.springTravel();
        NonUnsubscribingUploadStatusCallback callback = new NonUnsubscribingUploadStatusCallback();
        connectedClient.uploadApplication(applicationName, file, callback);
        CloudApplication env = connectedClient.getApplication(applicationName);
        assertEquals(CloudApplication.State.STOPPED, env.getState());
        assertTrue(callback.progressCount >= 1); // must have taken at least 10 seconds
    }

    @Test
    public void uploadAppWithUnsubscribingCallback() throws IOException {
        String applicationName = namespacedAppName("upload-unsubscribing-callback");
        createSpringApplication(applicationName);
        File file = SampleProjects.springTravel();
        UnsubscribingUploadStatusCallback callback = new UnsubscribingUploadStatusCallback();
        connectedClient.uploadApplication(applicationName, file, callback);
        CloudApplication env = connectedClient.getApplication(applicationName);
        assertEquals(CloudApplication.State.STOPPED, env.getState());
        assertTrue(callback.progressCount == 1);
    }

    //
    // Configuration/Metadata tests
    //

    @Test
    public void uploadSinatraApp() throws IOException {
        String applicationName = namespacedAppName("env");
        ClassPathResource cpr = new ClassPathResource("apps/env/");
        File explodedDir = cpr.getFile();
        Staging staging = ImmutableStaging.builder()
                                          .build();
        createAndUploadExplodedTestApp(applicationName, explodedDir, staging);
        connectedClient.startApplication(applicationName);
        CloudApplication env = connectedClient.getApplication(applicationName);
        assertEquals(CloudApplication.State.STARTED, env.getState());
    }

    @Test
    public void uploadStandaloneApplication() throws IOException {
        String applicationName = namespacedAppName("standalone-ruby");
        List<String> uris = new ArrayList<String>();
        List<String> services = new ArrayList<String>();
        createStandaloneRubyTestApp(applicationName, uris, services);
        connectedClient.startApplication(applicationName);
        CloudApplication application = connectedClient.getApplication(applicationName);
        assertNotNull(application);
        assertEquals(CloudApplication.State.STARTED, application.getState());
        assertEquals(uris, application.getUris());
    }

    @Test
    public void uploadStandaloneApplicationWithURLs() throws IOException {
        String applicationName = namespacedAppName("standalone-node");
        List<String> uris = new ArrayList<String>();
        uris.add(computeApplicationUrl(applicationName));
        List<String> services = new ArrayList<String>();
        Staging staging = ImmutableStaging.builder()
                                          .command("node app.js")
                                          .build();
        File file = SampleProjects.standaloneNode();
        connectedClient.createApplication(applicationName, staging, 64, uris, services);
        connectedClient.uploadApplication(applicationName, file.getCanonicalPath());
        connectedClient.startApplication(applicationName);
        CloudApplication application = connectedClient.getApplication(applicationName);
        assertNotNull(application);
        assertEquals(CloudApplication.State.STARTED, application.getState());
        assertEquals(Collections.singletonList(computeApplicationUrlNoProtocol(applicationName)), application.getUris());
    }

    private void assertAppEnvironment(Map<String, String> env) {
        assertMapInEnv(env, "staging_env_json", true);
        assertMapInEnv(env, "running_env_json", true);
        assertMapInEnv(env, "environment_json", true);
        assertMapInEnv(env, "system_env_json", true);
        // this value is not present in Pivotal CF < 1.4
        assertMapInEnv(env, "application_env_json", false);
    }

    private void assertDomainInList(List<CloudDomain> domains) {
        assertTrue(domains.size() >= 1);
        assertNotNull(getDomainNamed(TEST_DOMAIN, domains));
    }

    private void assertDomainNotInList(List<CloudDomain> domains) {
        assertNull(getDomainNamed(TEST_DOMAIN, domains));
    }

    private void assertEventTimestamps(List<CloudEvent> events) {
        for (CloudEvent event : events) {
            if (event.getTimestamp() != null) {
                assertTimeWithinRange("Event time should be very recent", event.getTimestamp()
                                                                               .getTime(),
                                      FIVE_MINUTES);
            }
        }
    }

    private void assertEvents(List<CloudEvent> events) {
        assertNotNull(events);
        assertTrue(events.size() > 0);

        for (CloudEvent event : events) {
            assertNotNull(event.getActee());
            assertNotNull(event.getActee()
                               .getGuid());
            assertNotNull(event.getActee()
                               .getName());
            assertNotNull(event.getActee()
                               .getType());
            assertNotNull(event.getActor());
            assertNotNull(event.getActor()
                               .getGuid());
            assertNotNull(event.getActor()
                               .getName());
            assertNotNull(event.getActor()
                               .getType());
        }
    }

    private void assertMapInEnv(Map<String, String> env, String key, boolean alwaysPresent) {
        String value = env.get(key);

        if (value == null) {
            if (alwaysPresent) {
                fail("Expected key " + key + " was not found");
            } else {
                return;
            }
        }
    }

    private void assertNetworkCallFails(RestTemplate restTemplate, ClientHttpRequestFactory requestFactory) {
        restTemplate.setRequestFactory(requestFactory);
        try {
            HttpStatus status = restTemplate.execute(CCNG_API_URL + "/info", HttpMethod.GET, null, new ResponseExtractor<HttpStatus>() {
                public HttpStatus extractData(ClientHttpResponse response) throws IOException {
                    return response.getStatusCode();
                }
            });
            Assert.fail("Expected byteman rules to detect direct socket connections, status is:" + status);
        } catch (Exception e) {
            // good, byteman rejected it as expected
            // e.printStackTrace();
        }
        assertEquals("Not expecting Jetty to receive requests since we asked direct connections", 0, nbInJvmProxyRcvReqs.get());
    }

    private void assertRulesMatchTestData(CloudSecurityGroup securityGroup) {
        // This asserts against the test data defined in the crudSecurityGroups method
        // Rule ordering is preserved so we can depend on it here
        SecurityGroupRule rule = securityGroup.getRules()
                                              .get(0);
        assertThat(rule.getProtocol(), is("tcp"));
        assertThat(rule.getPorts(), is("80, 443"));
        assertThat(rule.getDestination(), is("205.158.11.29"));
        assertNull(rule.getLog());
        assertNull(rule.getType());
        assertNull(rule.getCode());

        rule = securityGroup.getRules()
                            .get(1);
        assertThat(rule.getProtocol(), is("all"));
        assertNull(rule.getPorts());
        assertThat(rule.getDestination(), is("0.0.0.0-255.255.255.255"));
        assertNull(rule.getLog());
        assertNull(rule.getType());
        assertNull(rule.getCode());

        rule = securityGroup.getRules()
                            .get(2);
        assertThat(rule.getProtocol(), is("icmp"));
        assertNull(rule.getPorts());
        assertThat(rule.getDestination(), is("0.0.0.0/0"));
        assertTrue(rule.getLog());
        assertThat(rule.getType(), is(0));
        assertThat(rule.getCode(), is(1));
    }

    private void assertRulesMatchThoseInJsonFile1(CloudSecurityGroup securityGroup) {
        // Rule ordering is preserved so we can depend on it here

        SecurityGroupRule rule = securityGroup.getRules()
                                              .get(0);
        assertThat(rule.getProtocol(), is("icmp"));
        assertNull(rule.getPorts());
        assertThat(rule.getDestination(), is("0.0.0.0/0"));
        assertNull(rule.getLog());
        assertThat(rule.getType(), is(0));
        assertThat(rule.getCode(), is(1));

        rule = securityGroup.getRules()
                            .get(1);
        assertThat(rule.getProtocol(), is("tcp"));
        assertThat(rule.getPorts(), is("2048-3000"));
        assertThat(rule.getDestination(), is("1.0.0.0/0"));
        assertTrue(rule.getLog());
        assertNull(rule.getType());
        assertNull(rule.getCode());

        rule = securityGroup.getRules()
                            .get(2);
        assertThat(rule.getProtocol(), is("udp"));
        assertThat(rule.getPorts(), is("53, 5353"));
        assertThat(rule.getDestination(), is("2.0.0.0/0"));
        assertNull(rule.getLog());
        assertNull(rule.getType());
        assertNull(rule.getCode());

        rule = securityGroup.getRules()
                            .get(3);
        assertThat(rule.getProtocol(), is("all"));
        assertNull(rule.getPorts());
        assertThat(rule.getDestination(), is("3.0.0.0/0"));
        assertNull(rule.getLog());
        assertNull(rule.getType());
        assertNull(rule.getCode());
    }

    private void assertServiceMatching(CloudService expectedService, List<CloudService> services) {
        for (CloudService service : services) {
            if (service.getName()
                       .equals(expectedService.getName())) {
                assertServicesEqual(expectedService, service);
                return;
            }
        }
        fail("No service found matching " + expectedService.getName());
    }

    private void assertServicesEqual(CloudService expectedService, CloudService service) {
        assertEquals(expectedService.getName(), service.getName());
        assertEquals(expectedService.getLabel(), service.getLabel());
        assertEquals(expectedService.getPlan(), service.getPlan());
        assertEquals(expectedService.isUserProvided(), service.isUserProvided());
    }

    private void assertTimeWithinRange(String message, long actual, int timeTolerance) {
        // Allow more time deviations due to local clock being out of sync with cloud
        assertTrue(message, Math.abs(System.currentTimeMillis() - actual) < timeTolerance);
    }

    private void clearTestDomainAndRoutes() {
        CloudDomain domain = getDomainNamed(TEST_DOMAIN, connectedClient.getDomains());
        if (domain != null) {
            List<CloudRoute> routes = connectedClient.getRoutes(domain.getName());
            for (CloudRoute route : routes) {
                connectedClient.deleteRoute(route.getHost(), route.getDomain()
                                                                  .getName(),
                                            route.getPath());
            }
            connectedClient.deleteDomain(domain.getName());
        }
    }

    private String computeApplicationUrl(String applicationName) {
        return applicationName + "." + defaultDomainName;
    }

    private String computeApplicationUrlNoProtocol(String applicationName) {
        return computeApplicationUrl(applicationName);
    }

    private boolean containsSecurityGroupNamed(List<CloudSecurityGroup> groups, String groupName) {
        for (CloudSecurityGroup group : groups) {
            if (groupName.equalsIgnoreCase(group.getName())) {
                return true;
            }
        }
        return false;
    }

    private CloudApplication createAndUploadAndStartSampleServiceBrokerApp(String applicationName) throws IOException {
        createSpringApplication(applicationName);
        File jar = SampleProjects.sampleServiceBrokerApp();
        connectedClient.uploadApplication(applicationName, jar.getCanonicalPath());
        connectedClient.startApplication(applicationName);
        return connectedClient.getApplication(applicationName);
    }

    private CloudApplication createAndUploadAndStartSimpleSpringApp(String applicationName) throws IOException {
        createAndUploadSimpleSpringApp(applicationName);
        connectedClient.startApplication(applicationName);
        return connectedClient.getApplication(applicationName);
    }

    private CloudApplication createAndUploadExplodedSpringTestApp(String applicationName) throws IOException {
        File explodedDir = SampleProjects.springTravelUnpacked(temporaryFolder);
        assertTrue("Expected exploded test app at " + explodedDir.getCanonicalPath(), explodedDir.exists());
        Staging staging = ImmutableStaging.builder()
                                          .build();
        createTestApp(applicationName, null, staging);
        connectedClient.uploadApplication(applicationName, explodedDir.getCanonicalPath());
        return connectedClient.getApplication(applicationName);
    }

    //
    // Shared test methods
    //

    private CloudApplication createAndUploadExplodedTestApp(String applicationName, File explodedDir, Staging staging) throws IOException {
        assertTrue("Expected exploded test app at " + explodedDir.getCanonicalPath(), explodedDir.exists());
        createTestApp(applicationName, null, staging);
        connectedClient.uploadApplication(applicationName, explodedDir.getCanonicalPath());
        return connectedClient.getApplication(applicationName);
    }

    private CloudApplication createAndUploadSimpleSpringApp(String applicationName) throws IOException {
        createSpringApplication(applicationName);
        File war = SampleProjects.simpleSpringApp();
        connectedClient.uploadApplication(applicationName, war.getCanonicalPath());
        return connectedClient.getApplication(applicationName);
    }

    private CloudApplication createAndUploadSimpleTestApp(String name) throws IOException {
        createAndUploadSimpleSpringApp(name);
        return connectedClient.getApplication(name);
    }

    //
    // Helper methods
    //

    private CloudService createMySqlService(String serviceName) {
        CloudService service = ImmutableCloudService.builder()
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .build())
                                                    .name(serviceName)
                                                    .label(MYSQL_SERVICE_LABEL)
                                                    .plan(MYSQL_SERVICE_PLAN)
                                                    .build();

        connectedClient.createService(service);

        return service;
    }

    private CloudService createMySqlServiceWithVersionAndProvider(String serviceName) {
        CloudServiceOffering databaseServiceOffering = getCloudServiceOffering(MYSQL_SERVICE_LABEL);

        CloudService service = ImmutableCloudService.builder()
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .build())
                                                    .name(serviceName)
                                                    .provider(databaseServiceOffering.getProvider())
                                                    .label(databaseServiceOffering.getName())
                                                    .version(databaseServiceOffering.getVersion())
                                                    .plan(MYSQL_SERVICE_PLAN)
                                                    .build();

        connectedClient.createService(service);

        return service;
    }

    private void createSpringApplication(String applicationName) {
        Staging staging = ImmutableStaging.builder()
                                          .build();
        createTestApp(applicationName, null, staging);
    }

    //
    // helper methods
    //

    private void createSpringApplication(String applicationName, List<String> serviceNames) {
        Staging staging = ImmutableStaging.builder()
                                          .build();
        createTestApp(applicationName, serviceNames, staging);
    }

    private void createSpringApplication(String applicationName, String buildpackUrl) {
        createTestApp(applicationName, null, ImmutableStaging.builder()
                                                             .buildpacks(Arrays.asList(buildpackUrl))
                                                             .build());
    }

    private void createSpringApplication(String applicationName, String stack, Integer healthCheckTimeout) {
        createTestApp(applicationName, null, ImmutableStaging.builder()
                                                             .stack(stack)
                                                             .healthCheckTimeout(healthCheckTimeout)
                                                             .build());
    }

    private String createSpringTravelApp(String suffix) {
        return createSpringTravelApp(suffix, null);
    }

    private String createSpringTravelApp(String suffix, List<String> serviceNames) {
        String applicationName = namespacedAppName("travel_test-" + suffix);
        createSpringApplication(applicationName, serviceNames);
        return applicationName;
    }

    private void createStandaloneRubyTestApp(String applicationName, List<String> uris, List<String> services) throws IOException {
        Staging staging = ImmutableStaging.builder()
                                          .command("ruby simple.rb")
                                          .build();
        File file = SampleProjects.standaloneRuby();
        connectedClient.createApplication(applicationName, staging, 128, uris, services);
        connectedClient.uploadApplication(applicationName, file.getCanonicalPath());
    }

    private void createTestApp(String applicationName, List<String> serviceNames, Staging staging) {
        List<String> uris = new ArrayList<String>();
        uris.add(computeApplicationUrl(applicationName));
        if (serviceNames != null) {
            for (String serviceName : serviceNames) {
                createMySqlService(serviceName);
            }
        }
        connectedClient.createApplication(applicationName, staging, DEFAULT_MEMORY, uris, serviceNames);
    }

    private CloudService createUserProvidedService(String serviceName) {
        CloudService service = ImmutableCloudService.builder()
                                                    .metadata(ImmutableCloudMetadata.builder()
                                                                                    .build())
                                                    .name(serviceName)
                                                    .build();

        Map<String, Object> credentials = new HashMap<String, Object>();
        credentials.put("host", "example.com");
        credentials.put("port", 1234);
        credentials.put("user", "me");

        connectedClient.createUserProvidedService(service, credentials);

        return service;
    }

    /**
     * Try to clean up any security group test data left behind in the case of assertions failing and test security groups not being deleted
     * as part of test logic.
     */
    private void deleteAnyOrphanedTestSecurityGroups() {
        try {
            CloudSecurityGroup securityGroup = connectedClient.getSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);
            if (securityGroup != null) {
                connectedClient.deleteSecurityGroup(CCNG_SECURITY_GROUP_NAME_TEST);
            }
        } catch (Exception e) {
            // Nothing we can do at this point except protect other teardown logic from not running
        }
    }

    private void doGetFile(CloudControllerClient client, String applicationName) throws Exception {
        String applicationDirectory = "app";
        String fileName = applicationDirectory + "/WEB-INF/web.xml";
        String emptyPropertiesFileName = applicationDirectory + "/WEB-INF/classes/empty.properties";

        // File is often not available immediately after starting an app... so allow up to 60 seconds wait
        for (int i = 0; i < 60; i++) {
            try {
                client.getFile(applicationName, 0, fileName);
                break;
            } catch (HttpServerErrorException ex) {
                Thread.sleep(1000);
            }
        }

        // Test downloading full file
        String fileContent = client.getFile(applicationName, 0, fileName);
        assertNotNull(fileContent);
        assertTrue(fileContent.length() > 5);

        // Test downloading range of file with start and end position
        int end = fileContent.length() - 3;
        int start = end / 2;
        String fileContent2 = client.getFile(applicationName, 0, fileName, start, end);
        assertEquals(fileContent.substring(start, end), fileContent2);

        // Test downloading range of file with just start position
        String fileContent3 = client.getFile(applicationName, 0, fileName, start);
        assertEquals(fileContent.substring(start), fileContent3);

        // Test downloading range of file with start position and end position exceeding the length
        int positionPastEndPosition = fileContent.length() + 999;
        String fileContent4 = client.getFile(applicationName, 0, fileName, start, positionPastEndPosition);
        assertEquals(fileContent.substring(start), fileContent4);

        // Test downloading end portion of file with length
        int length = fileContent.length() / 2;
        String fileContent5 = client.getFileTail(applicationName, 0, fileName, length);
        assertEquals(fileContent.substring(fileContent.length() - length), fileContent5);

        // Test downloading one byte of file with start and end position
        String fileContent6 = client.getFile(applicationName, 0, fileName, start, start + 1);
        assertEquals(fileContent.substring(start, start + 1), fileContent6);
        assertEquals(1, fileContent6.length());

        // Test downloading range of file with invalid start position
        int invalidStartPosition = fileContent.length() + 999;
        try {
            client.getFile(applicationName, 0, fileName, invalidStartPosition);
            fail("should have thrown exception");
        } catch (CloudOperationException e) {
            assertTrue(e.getStatusCode()
                        .equals(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE));
        }

        // Test downloading empty file
        String fileContent7 = client.getFile(applicationName, 0, emptyPropertiesFileName);
        assertNull(fileContent7);

        // Test downloading with invalid parameters - should all throw exceptions
        try {
            client.getFile(applicationName, 0, fileName, -2);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage()
                        .contains("start position"));
        }
        try {
            client.getFile(applicationName, 0, fileName, 10, -2);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage()
                        .contains("end position"));
        }
        try {
            client.getFile(applicationName, 0, fileName, 29, 28);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage()
                        .contains("end position"));
        }
        try {
            client.getFile(applicationName, 0, fileName, 29, 28);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage()
                        .contains("29"));
        }
        try {
            client.getFileTail(applicationName, 0, fileName, 0);
            fail("should have thrown exception");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage()
                        .contains("length"));
        }
    }

    private List<ApplicationLog> doGetRecentLogs(String applicationName) throws InterruptedException {
        int attempt = 0;
        do {
            List<ApplicationLog> logs = connectedClient.getRecentLogs(applicationName);

            if (logs.size() > 0) {
                return logs;
            }
            Thread.sleep(1000);
        } while (attempt++ < 20);
        fail("Failed to see recent logs");
        return null;
    }

    private void doOpenFile(CloudControllerClient client, String applicationName) throws Exception {
        String applicationDirectory = "app";
        String fileName = applicationDirectory + "/WEB-INF/web.xml";
        String emptyPropertiesFileName = applicationDirectory + "/WEB-INF/classes/empty.properties";

        // File is often not available immediately after starting an app... so
        // allow up to 60 seconds wait
        for (int i = 0; i < 60; i++) {
            try {
                client.getFile(applicationName, 0, fileName);
                break;
            } catch (HttpServerErrorException ex) {
                Thread.sleep(1000);
            }
        }
        // Test open file

        client.openFile(applicationName, 0, fileName, new ClientHttpResponseCallback() {

            public void onClientHttpResponse(ClientHttpResponse clientHttpResponse) throws IOException {
                InputStream in = clientHttpResponse.getBody();
                assertNotNull(in);
                byte[] fileContents = IOUtils.toByteArray(in);
                assertTrue(fileContents.length > 5);
            }
        });

        client.openFile(applicationName, 0, emptyPropertiesFileName, new ClientHttpResponseCallback() {

            public void onClientHttpResponse(ClientHttpResponse clientHttpResponse) throws IOException {
                InputStream in = clientHttpResponse.getBody();
                assertNotNull(in);
                byte[] fileContents = IOUtils.toByteArray(in);
                assertTrue(fileContents.length == 0);
            }
        });

    }

    private boolean ensureApplicationRunning(String applicationName) {
        InstancesInfo instances;
        boolean pass = false;
        for (int i = 0; i < 50; i++) {
            try {
                instances = getInstancesWithTimeout(connectedClient, applicationName);
                assertNotNull(instances);

                List<InstanceInfo> infos = instances.getInstances();
                assertEquals(1, infos.size());

                int passCount = 0;
                for (InstanceInfo info : infos) {
                    if (InstanceState.RUNNING.equals(info.getState())) {
                        passCount++;
                    }
                }
                if (passCount == infos.size()) {
                    pass = true;
                    break;
                }
            } catch (CloudOperationException ex) {
                // ignore (we may get this when staging is still ongoing)
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return pass;
    }

    private CloudServiceOffering getCloudServiceOffering(String label) {
        List<CloudServiceOffering> serviceOfferings = connectedClient.getServiceOfferings();
        for (CloudServiceOffering so : serviceOfferings) {
            if (so.getName()
                  .equals(label)) {
                return so;
            }
        }
        throw new IllegalStateException("No CloudServiceOffering found with label " + label + ".");
    }

    private CloudDomain getDomainNamed(String domainName, List<CloudDomain> domains) {
        for (CloudDomain domain : domains) {
            if (domain.getName()
                      .equals(domainName)) {
                return domain;
            }
        }
        return null;
    }

    private boolean getInstanceInfosWithTimeout(String applicationName, int count, boolean shouldBeRunning) {
        if (count > 1) {
            connectedClient.updateApplicationInstances(applicationName, count);
            CloudApplication application = connectedClient.getApplication(applicationName);
            assertEquals(count, application.getInstances());
        }

        InstancesInfo instances;
        boolean pass = false;
        for (int i = 0; i < 50; i++) {
            try {
                instances = getInstancesWithTimeout(connectedClient, applicationName);
                assertNotNull(instances);

                List<InstanceInfo> infos = instances.getInstances();
                assertEquals(count, infos.size());

                int passCount = 0;
                for (InstanceInfo info : infos) {
                    if (shouldBeRunning) {
                        if (InstanceState.RUNNING.equals(info.getState()) || InstanceState.STARTING.equals(info.getState())) {
                            passCount++;
                        }
                    } else {
                        if (InstanceState.CRASHED.equals(info.getState()) || InstanceState.FLAPPING.equals(info.getState())) {
                            passCount++;
                        }
                    }
                }
                if (passCount == infos.size()) {
                    pass = true;
                    break;
                }
            } catch (CloudOperationException ex) {
                // ignore (we may get this when staging is still ongoing)
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return pass;
    }

    private InstancesInfo getInstancesWithTimeout(CloudControllerClient client, String applicationName) {
        long start = System.currentTimeMillis();
        while (true) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e1) {
                // ignore
            }

            final InstancesInfo applicationInstances = client.getApplicationInstances(applicationName);
            if (applicationInstances != null) {
                return applicationInstances;
            }

            if (System.currentTimeMillis() - start > STARTUP_TIMEOUT) {
                fail("Timed out waiting for startup");
                break; // for the compiler
            }
        }

        return null; // for the compiler
    }

    private CloudRoute getRouteWithHost(String hostName, List<CloudRoute> routes) {
        for (CloudRoute route : routes) {
            if (route.getHost()
                     .equals(hostName)) {
                return route;
            }
        }
        return null;
    }

    private boolean isSpaceBoundToSecurityGroup(String spaceName, String securityGroupName) {
        List<CloudSpace> boundSpaces = connectedClient.getSpacesBoundToSecurityGroup(securityGroupName);
        for (CloudSpace space : boundSpaces) {
            if (spaceName.equals(space.getName())) {
                return true;
            }
        }
        return false;
    }

    private HashSet<String> listToHashSet(List<String> list) {
        return new HashSet<String>(list);
    }

    private String namespacedAppName(String basename) {
        return TEST_NAMESPACE + "-" + basename;
    }

    private String randomSecurityGroupName() {
        return UUID.randomUUID()
                   .toString();
    }

    private CloudApplication uploadSpringTravelApp(String applicationName) throws IOException {
        File file = SampleProjects.springTravel();
        connectedClient.uploadApplication(applicationName, file.getCanonicalPath());
        return connectedClient.getApplication(applicationName);
    }

    private void validateClientAccess(CloudControllerRestClient client) {
        List<CloudServiceOffering> offerings = client.getServiceOfferings();
        assertNotNull(offerings);
        assertTrue(offerings.size() >= 2);
    }

    private static abstract class NoOpUploadStatusCallback implements UploadStatusCallback {

        @Override
        public void onCheckResources() {
        }

        @Override
        public void onMatchedFileNames(Set<String> matchedFileNames) {
        }

        @Override
        public void onProcessMatchedResources(int length) {
        }

        @Override
        public void onError(String description) {
        }

    }

    private static class NonUnsubscribingUploadStatusCallback extends NoOpUploadStatusCallback {

        public int progressCount = 0;

        public boolean onProgress(String status) {
            progressCount++;
            return false;
        }
    }

    private static class UnsubscribingUploadStatusCallback extends NoOpUploadStatusCallback {

        public int progressCount = 0;

        public boolean onProgress(String status) {
            progressCount++;
            // unsubscribe after the first report
            return progressCount == 1;
        }
    }

    private class AccumulatingApplicationLogListener implements ApplicationLogListener {

        private List<ApplicationLog> logs = new ArrayList<ApplicationLog>();

        public void onComplete() {
        }

        public void onError(Throwable exception) {
            fail(exception.getMessage());
        }

        public void onMessage(ApplicationLog log) {
            logs.add(log);
        }

    }
}
