package org.cloudfoundry.client.lib.adapters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudSpace;
import org.cloudfoundry.client.lib.domain.ImmutableCloudStack;
import org.cloudfoundry.client.lib.domain.ImmutableDockerCredentials;
import org.cloudfoundry.client.lib.domain.ImmutableDockerInfo;
import org.cloudfoundry.client.lib.domain.ImmutableStaging;
import org.cloudfoundry.client.lib.domain.PackageState;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.applications.ApplicationEntity;
import org.cloudfoundry.client.v2.applications.ApplicationResource;
import org.cloudfoundry.client.v2.applications.SummaryApplicationResponse;
import org.cloudfoundry.client.v2.domains.Domain;
import org.cloudfoundry.client.v2.routes.Route;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstance;
import org.junit.jupiter.api.Test;

public class RawCloudApplicationTest {

    private static final int MEMORY = 256;
    private static final int DISK_QUOTA = 512;
    private static final int INSTANCES = 3;
    private static final int RUNNING_INSTANCES = 2;
    private static final String STATE = "STARTED";
    private static final String PACKAGE_STATE = "PENDING";
    private static final List<Route> ROUTES = buildTestRoutes();
    private static final List<ServiceInstance> SERVICE_INSTANCES = buildTestServiceInstances();
    private static final Map<String, Object> ENVIRONMENT = buildTestEnvironment();
    private static final String BUILDPACK = "ruby_buildpack";
    private static final String COMMAND = "rails server";
    private static final String DETECTED_BUILDPACK = "ruby_buildpack";
    private static final String HEALTH_CHECK_HTTP_ENDPOINT = "/ping";
    private static final Integer HEALTH_CHECK_TIMEOUT = 180;
    private static final String HEALTH_CHECK_TYPE = "port";
    private static final String STAGING_ERROR = "blabla";
    private static final Boolean SSH_ENABLED = true;
    private static final String DOCKER_IMAGE = "cloudfoundry/my-image";
    private static final String DOCKER_USERNAME = "admin";
    private static final String DOCKER_PASSWORD = "troll";
    private static final org.cloudfoundry.client.v2.applications.DockerCredentials DOCKER_CREDENTIALS = buildTestDockerCredentials();
    private static final String STACK_NAME = "cflinuxfs3";
    private static final String SPACE_NAME = "test";
    private static final CloudStack STACK = ImmutableCloudStack.builder()
        .name(STACK_NAME)
        .build();
    private static final CloudSpace SPACE = ImmutableCloudSpace.builder()
        .name(SPACE_NAME)
        .build();

    private static final CloudApplication.State EXPECTED_STATE = CloudApplication.State.STARTED;
    private static final PackageState EXPECTED_PACKAGE_STATE = PackageState.PENDING;
    private static final List<String> EXPECTED_URIS = Arrays.asList("foo.example.com/does/this/work", "bar.example.com:30030",
        "baz.example.com", "example.com", "example.com");
    private static final List<String> EXPECTED_SERVICES = Arrays.asList("foo", "bar");
    private static final Map<String, String> EXPECTED_ENVIRONMENT = buildExpectedEnvironment();

    @Test
    public void testDeriveWithoutDockerInfo() {
        RawCloudEntityTest.testDerive(buildExpectedApplication(), buildRawApplication());
    }

    @Test
    public void testDeriveWithoutDockerCredentials() {
        RawCloudApplication rawApplication = buildRawApplication(DOCKER_IMAGE, null);
        DockerInfo expectedDockerInfo = ImmutableDockerInfo.builder()
            .image(DOCKER_IMAGE)
            .build();
        RawCloudEntityTest.testDerive(buildExpectedApplication(expectedDockerInfo), rawApplication);
    }

    @Test
    public void testDerive() {
        RawCloudApplication rawApplication = buildRawApplication(DOCKER_IMAGE, DOCKER_CREDENTIALS);
        DockerInfo expectedDockerInfo = ImmutableDockerInfo.builder()
            .image(DOCKER_IMAGE)
            .credentials(ImmutableDockerCredentials.builder()
                .username(DOCKER_USERNAME)
                .password(DOCKER_PASSWORD)
                .build())
            .build();
        RawCloudEntityTest.testDerive(buildExpectedApplication(expectedDockerInfo), rawApplication);
    }

    private static CloudApplication buildExpectedApplication() {
        return buildExpectedApplication(null);
    }

    private static CloudApplication buildExpectedApplication(DockerInfo dockerInfo) {
        return ImmutableCloudApplication.builder()
            .metadata(RawCloudEntityTest.EXPECTED_METADATA)
            .name(RawCloudEntityTest.NAME)
            .uris(EXPECTED_URIS)
            .memory(MEMORY)
            .diskQuota(DISK_QUOTA)
            .instances(INSTANCES)
            .runningInstances(RUNNING_INSTANCES)
            .state(EXPECTED_STATE)
            .packageState(EXPECTED_PACKAGE_STATE)
            .stagingError(STAGING_ERROR)
            .staging(buildExpectedStaging(dockerInfo))
            .services(EXPECTED_SERVICES)
            .env(EXPECTED_ENVIRONMENT)
            .space(SPACE)
            .build();
    }

    private static ImmutableStaging buildExpectedStaging(DockerInfo dockerInfo) {
        return ImmutableStaging.builder()
            .buildpackUrl(BUILDPACK)
            .command(COMMAND)
            .detectedBuildpack(DETECTED_BUILDPACK)
            .dockerInfo(dockerInfo)
            .healthCheckHttpEndpoint(HEALTH_CHECK_HTTP_ENDPOINT)
            .healthCheckTimeout(HEALTH_CHECK_TIMEOUT)
            .healthCheckType(HEALTH_CHECK_TYPE)
            .isSshEnabled(SSH_ENABLED)
            .stack(STACK_NAME)
            .build();
    }

    private static RawCloudApplication buildRawApplication() {
        return buildRawApplication(null, null);
    }

    private static RawCloudApplication buildRawApplication(String dockerImage,
        org.cloudfoundry.client.v2.applications.DockerCredentials dockerCredentials) {
        return ImmutableRawCloudApplication.builder()
            .resource(buildTestResource())
            .summary(buildTestSummary(dockerImage, dockerCredentials))
            .stack(STACK)
            .space(SPACE)
            .build();
    }

    private static Resource<ApplicationEntity> buildTestResource() {
        return ApplicationResource.builder()
            .metadata(RawCloudEntityTest.METADATA)
            .build();
    }

    private static SummaryApplicationResponse buildTestSummary(String dockerImage,
        org.cloudfoundry.client.v2.applications.DockerCredentials dockerCredentials) {
        return SummaryApplicationResponse.builder()
            .name(RawCloudEntityTest.NAME)
            .memory(MEMORY)
            .diskQuota(DISK_QUOTA)
            .routes(ROUTES)
            .instances(INSTANCES)
            .runningInstances(RUNNING_INSTANCES)
            .state(STATE)
            .packageState(PACKAGE_STATE)
            .stagingFailedDescription(STAGING_ERROR)
            .healthCheckHttpEndpoint(HEALTH_CHECK_HTTP_ENDPOINT)
            .healthCheckTimeout(HEALTH_CHECK_TIMEOUT)
            .healthCheckType(HEALTH_CHECK_TYPE)
            .enableSsh(SSH_ENABLED)
            .dockerImage(dockerImage)
            .dockerCredentials(dockerCredentials)
            .environmentJsons(ENVIRONMENT)
            .command(COMMAND)
            .buildpack(BUILDPACK)
            .detectedBuildpack(DETECTED_BUILDPACK)
            .services(SERVICE_INSTANCES)
            .build();
    }

    private static List<Route> buildTestRoutes() {
        Domain domain = Domain.builder()
            .name("example.com")
            .build();
        Route foo = Route.builder()
            .host("foo")
            .domain(domain)
            .path("/does/this/work")
            .build();
        Route bar = Route.builder()
            .host("bar")
            .domain(domain)
            .port(30030)
            .build();
        Route baz = Route.builder()
            .host("baz")
            .domain(domain)
            .build();
        Route qux = Route.builder()
            .domain(domain)
            .build();
        Route quux = Route.builder()
            .host("")
            .domain(domain)
            .build();
        return Arrays.asList(foo, bar, baz, qux, quux);
    }

    private static List<ServiceInstance> buildTestServiceInstances() {
        ServiceInstance foo = ServiceInstance.builder()
            .name("foo")
            .build();
        ServiceInstance bar = ServiceInstance.builder()
            .name("bar")
            .build();
        return Arrays.asList(foo, bar);
    }

    private static org.cloudfoundry.client.v2.applications.DockerCredentials buildTestDockerCredentials() {
        return org.cloudfoundry.client.v2.applications.DockerCredentials.builder()
            .username(DOCKER_USERNAME)
            .password(DOCKER_PASSWORD)
            .build();
    }

    private static Map<String, String> buildExpectedEnvironment() {
        Map<String, String> environment = new HashMap<>();
        environment.put("a", "foo");
        environment.put("b", "12345");
        environment.put("c", "false");
        environment.put("d", "3.141");
        environment.put("e", "[\"bar\",\"baz\"]");
        environment.put("f", "{\"foo\":\"bar\",\"baz\":\"qux\"}");
        environment.put("g", null);
        return environment;
    }

    private static Map<String, Object> buildTestEnvironment() {
        Map<String, Object> environment = new HashMap<>();
        environment.put("a", "foo");
        environment.put("b", 12345);
        environment.put("c", false);
        environment.put("d", 3.141);
        environment.put("e", Arrays.asList("bar", "baz"));
        Map<String, Object> innerMap = new HashMap<>();
        innerMap.put("foo", "bar");
        innerMap.put("baz", "qux");
        environment.put("f", innerMap);
        environment.put("g", null);
        return environment;
    }

}
