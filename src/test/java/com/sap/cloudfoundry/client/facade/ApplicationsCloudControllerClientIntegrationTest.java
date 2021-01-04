package com.sap.cloudfoundry.client.facade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.processes.HealthCheckType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudBuild;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;
import com.sap.cloudfoundry.client.facade.domain.DockerInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRouteSummary;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableStaging;
import com.sap.cloudfoundry.client.facade.domain.InstancesInfo;
import com.sap.cloudfoundry.client.facade.domain.Staging;
import com.sap.cloudfoundry.client.facade.domain.Status;

class ApplicationsCloudControllerClientIntegrationTest extends CloudControllerClientIntegrationTest {

    private static final String HEALTH_CHECK_ENDPOINT = "/public/ping";
    private static final String JAVA_BUILDPACK = "java_buildpack";
    private static final int HEALTH_CHECK_TIMEMOUT = 100;
    private static final int DISK_IN_MB = 128;
    private static final int MEMORY_IN_MB = 128;
    private static final String DEFAULT_DOMAIN = "deploy-service.custom.domain.for.integration.tests";
    private static final String APPLICATION_HOST = "test-application-hostname-ztana-test";
    private static final String STATICFILE_APPLICATION_CONTENT = "staticfile.zip";
    private static final Path STATICFILE_APPLICATION_PATH = getStaticfileApplicationContentPath();

    @BeforeAll
    static void createDefaultDomain() {
        client.addDomain(DEFAULT_DOMAIN);
    }

    @AfterAll
    static void deleteDefaultDomain() {
        List<CloudRoute> routes = client.getRoutes(DEFAULT_DOMAIN);
        routes.forEach(route -> client.deleteRoute(route.getHost(), DEFAULT_DOMAIN, null));
        client.deleteDomain(DEFAULT_DOMAIN);
    }

    @Test
    @DisplayName("Create application and verify its attributes")
    void createApplication() {
        String applicationName = "test-app-1";
        Staging staging = ImmutableStaging.builder()
                                          .addBuildpack(JAVA_BUILDPACK)
                                          .healthCheckType(HealthCheckType.PROCESS.getValue())
                                          .healthCheckHttpEndpoint(HEALTH_CHECK_ENDPOINT)
                                          .healthCheckTimeout(HEALTH_CHECK_TIMEMOUT)
                                          .build();
        CloudRouteSummary cloudRouteSummary = ImmutableCloudRouteSummary.builder()
                                                                        .host(APPLICATION_HOST)
                                                                        .domain(DEFAULT_DOMAIN)
                                                                        .build();
        try {
            verifyApplicationWillBeCreated(applicationName, staging, Set.of(cloudRouteSummary));
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create, delete and verify the application is deleted")
    void deleteApplication() {
        String applicationName = "test-application-2";
        createAndVerifyDefaultApplication(applicationName);
        client.deleteApplication(applicationName);
        Exception exception = assertThrows(CloudOperationException.class, () -> client.getApplication(applicationName));
        assertTrue(exception.getMessage()
                            .contains(HttpStatus.NOT_FOUND.getReasonPhrase()));
    }

    @Test
    @DisplayName("Create application and verify its GUID")
    void getApplicationGuid() {
        String applicationName = "test-application-3";
        try {
            createAndVerifyDefaultApplication(applicationName);
            assertNotNull(client.getApplicationGuid(applicationName));
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application, update and update its environment")
    void testApplicationEnvironment() {
        String applicationName = "test-application-4";
        try {
            createAndVerifyDefaultApplication(applicationName);
            client.updateApplicationEnv(applicationName, Map.of("foo", "bar"));
            UUID applicationGuid = client.getApplicationGuid(applicationName);
            Map<String, String> applicationEnvironment = client.getApplicationEnvironment(applicationGuid);
            assertEquals("bar", applicationEnvironment.get("foo"));
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application, upload a package, verify package exists")
    void uploadApplication() throws URISyntaxException, InterruptedException {
        String applicationName = "test-application-5";
        try {
            createAndVerifyDefaultApplication(applicationName);
            CloudPackage cloudPackage = uploadApplication(applicationName);
            assertEquals(Status.READY, cloudPackage.getStatus());
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application, upload a package, create a build and restart the application")
    void restartApplication() throws URISyntaxException, InterruptedException {
        String applicationName = "test-application-6";
        try {
            createAndVerifyDefaultApplication(applicationName);
            CloudPackage cloudPackage = uploadApplication(applicationName);
            stageApplication(applicationName, cloudPackage);
            client.startApplication(applicationName);
            assertEquals(CloudApplication.State.STARTED, client.getApplication(applicationName)
                                                               .getState());
            client.stopApplication(applicationName);
            assertEquals(CloudApplication.State.STOPPED, client.getApplication(applicationName)
                                                               .getState());
            client.startApplication(applicationName);
            assertEquals(CloudApplication.State.STARTED, client.getApplication(applicationName)
                                                               .getState());
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create, upload, stage start and get instances")
    void getAppInstances() throws URISyntaxException, InterruptedException {
        String applicationName = "test-application-7";
        try {
            createAndVerifyDefaultApplication(applicationName);
            CloudPackage cloudPackage = uploadApplication(applicationName);
            client.updateApplicationInstances(applicationName, 3);
            stageApplication(applicationName, cloudPackage);
            client.startApplication(applicationName);
            CloudApplication application = client.getApplication(applicationName);
            InstancesInfo applicationInstances = client.getApplicationInstances(application);
            assertEquals(3, applicationInstances.getInstances()
                                                .size());
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application and verify its GUID")
    void renameApplication() {
        String applicationName = "test-application-8";
        String newApplicationName = "new-test-application-8";
        try {
            createAndVerifyDefaultApplication(applicationName);
            UUID applicationGuid = client.getApplicationGuid(applicationName);
            client.rename(applicationName, newApplicationName);
            assertEquals(applicationGuid, client.getApplication(newApplicationName)
                                                .getGuid());
        } finally {
            client.deleteApplication(newApplicationName);
        }
    }

    @Test
    @DisplayName("Create app and update its memory")
    void updateApplicationMemory() {
        String applicationName = "test-application-9";
        try {
            createAndVerifyDefaultApplication(applicationName);
            client.updateApplicationMemory(applicationName, 256);
            CloudApplication application = client.getApplication(applicationName);
            assertEquals(256, application.getMemory());
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create app and update its staging")
    void updateApplicationStaging() {
        String applicationName = "test-application-10";
        try {
            createAndVerifyDefaultApplication(applicationName);
            client.updateApplicationStaging(applicationName, ImmutableStaging.builder()
                                                                             .command("echo 1")
                                                                             .build());
            CloudApplication application = client.getApplication(applicationName);
            assertEquals("echo 1", application.getStaging()
                                              .getCommand());
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application with docker package")
    void createDockerPackage() {
        String applicationName = "test-application-11";
        DockerInfo dockerInfo = ImmutableDockerInfo.builder()
                                                   .image("test/image")
                                                   .build();
        try {
            verifyApplicationWillBeCreated(applicationName, ImmutableStaging.builder()
                                                                            .dockerInfo(dockerInfo)
                                                                            .build(),
                                           Set.of(ImmutableCloudRouteSummary.builder()
                                                                            .host(APPLICATION_HOST)
                                                                            .domain(DEFAULT_DOMAIN)
                                                                            .build()));
            UUID applicationGuid = client.getApplicationGuid(applicationName);
            CloudPackage dockerPackage = client.createDockerPackage(applicationGuid, dockerInfo);
            assertEquals(CloudPackage.Type.DOCKER, dockerPackage.getType());
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application and update its routes")
    void updateApplicationRoutes() {
        String applicationName = "test-application-12";
        Set<CloudRouteSummary> newRoutes = Set.of(ImmutableCloudRouteSummary.builder()
                                                                            .host("test-application-hostname-modified")
                                                                            .domain(DEFAULT_DOMAIN)
                                                                            .build());
        try {
            createAndVerifyDefaultApplication(applicationName);
            client.updateApplicationRoutes(applicationName, newRoutes);
            CloudApplication application = client.getApplication(applicationName);
            assertEquals(newRoutes, application.getRoutes());
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application with docker package and check packages")
    void getPackagesForApplication() {
        String applicationName = "test-application-13";
        DockerInfo dockerInfo = ImmutableDockerInfo.builder()
                                                   .image("test/image")
                                                   .build();
        try {
            verifyApplicationWillBeCreated(applicationName, ImmutableStaging.builder()
                                                                            .dockerInfo(dockerInfo)
                                                                            .build(),
                                           Set.of(ImmutableCloudRouteSummary.builder()
                                                                            .host(APPLICATION_HOST)
                                                                            .host(APPLICATION_HOST)
                                                                            .domain(DEFAULT_DOMAIN)
                                                                            .build()));
            UUID applicationGuid = client.getApplicationGuid(applicationName);
            CloudPackage dockerPackage = client.createDockerPackage(applicationGuid, dockerInfo);
            List<CloudPackage> packagesForApplication = client.getPackagesForApplication(applicationGuid);
            assertTrue(packagesForApplication.stream()
                                             .map(CloudPackage::getGuid)
                                             .anyMatch(packageGuid -> packageGuid.equals(dockerPackage.getGuid())));
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application, upload a package, create a build and test its builds")
    void getBuildsForApplication() throws URISyntaxException, InterruptedException {
        String applicationName = "test-application-14";
        try {
            createAndVerifyDefaultApplication(applicationName);
            CloudPackage cloudPackage = uploadApplication(applicationName);
            UUID applicationGuid = client.getApplicationGuid(applicationName);
            CloudBuild build = createBuildForPackage(cloudPackage);
            List<CloudBuild> buildsForApplication = client.getBuildsForApplication(applicationGuid);
            assertTrue(buildsForApplication.stream()
                                           .map(CloudBuild::getMetadata)
                                           .map(CloudMetadata::getGuid)
                                           .anyMatch(buildGuid -> buildGuid.equals(build.getGuid())));
            assertEquals(build.getMetadata()
                              .getGuid(),
                         client.getBuild(build.getMetadata()
                                              .getGuid())
                               .getGuid());
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    @Test
    @DisplayName("Create application and update its metadata")
    void updateApplicationMetadata() {
        String applicationName = "test-application-15";
        Metadata metadata = Metadata.builder()
                                    .label("test-app", "test-app")
                                    .build();
        try {
            createAndVerifyDefaultApplication(applicationName);
            UUID applicationGuid = client.getApplicationGuid(applicationName);
            client.updateApplicationMetadata(applicationGuid, metadata);
            List<CloudApplication> applicationsByMetadataLabelSelector = client.getApplicationsByMetadataLabelSelector("test-app");
            assertEquals(1, applicationsByMetadataLabelSelector.size());
            assertEquals(applicationName, applicationsByMetadataLabelSelector.get(0)
                                                                             .getName());
        } finally {
            client.deleteApplication(applicationName);
        }
    }

    private CloudPackage uploadApplication(String applicationName) throws URISyntaxException, InterruptedException {
        CloudPackage cloudPackage = client.asyncUploadApplication(applicationName, STATICFILE_APPLICATION_PATH);
        while (cloudPackage.getStatus() != Status.READY && !hasFailed(cloudPackage.getStatus())) {
            cloudPackage = client.asyncUploadApplication(applicationName, STATICFILE_APPLICATION_PATH);
            Thread.sleep(1000);
        }
        if (hasFailed(cloudPackage.getStatus())) {
            fail(MessageFormat.format("Cloud package is in invalid state: \"{0}\"", cloudPackage.getStatus()));
        }
        return cloudPackage;
    }

    private boolean hasFailed(Status status) {
        return status == Status.EXPIRED || status == Status.FAILED;
    }

    private void stageApplication(String applicationName, CloudPackage cloudPackage) throws InterruptedException {
        UUID applicationGuid = client.getApplicationGuid(applicationName);
        CloudBuild build = createBuildForPackage(cloudPackage);
        client.bindDropletToApp(build.getDropletInfo()
                                     .getGuid(),
                                applicationGuid);
    }

    private CloudBuild createBuildForPackage(CloudPackage cloudPackage) throws InterruptedException {
        CloudBuild build = client.createBuild(cloudPackage.getGuid());
        while (build.getState() == CloudBuild.State.STAGING) {
            build = client.getBuild(build.getGuid());
            Thread.sleep(1000);
        }
        if (build.getState() == CloudBuild.State.FAILED) {
            fail(MessageFormat.format("Cloud build is in invalid state: \"{0}\"", build.getState()));
        }
        return build;
    }

    private void verifyApplicationWillBeCreated(String applicationName, Staging staging, Set<CloudRouteSummary> cloudRoutesSummary) {
        client.createApplication(applicationName, staging, DISK_IN_MB, MEMORY_IN_MB, cloudRoutesSummary);
        assertApplicationExists(ImmutableCloudApplication.builder()
                                                         .name(applicationName)
                                                         .diskQuota(DISK_IN_MB)
                                                         .memory(MEMORY_IN_MB)
                                                         .instances(1)
                                                         .routes(cloudRoutesSummary)
                                                         .build());
    }

    private static void assertApplicationExists(CloudApplication cloudApplication) {
        CloudApplication application = client.getApplication(cloudApplication.getName(), true);
        assertEquals(cloudApplication.getDiskQuota(), application.getDiskQuota());
        assertEquals(cloudApplication.getMemory(), application.getMemory());
        assertEquals(cloudApplication.getRoutes(), application.getRoutes());
        assertEquals(cloudApplication.getInstances(), application.getInstances());
    }

    private void createAndVerifyDefaultApplication(String applicationName) {
        verifyApplicationWillBeCreated(applicationName, ImmutableStaging.builder()
                                                                        .build(),
                                       Set.of(ImmutableCloudRouteSummary.builder()
                                                                        .host(APPLICATION_HOST)
                                                                        .domain(DEFAULT_DOMAIN)
                                                                        .build()));
    }

    private static Path getStaticfileApplicationContentPath() {
        URL url = ApplicationsCloudControllerClientIntegrationTest.class.getResource(STATICFILE_APPLICATION_CONTENT);
        try {
            return Paths.get(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

}
