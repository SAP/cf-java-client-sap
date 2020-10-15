package com.sap.cloudfoundry.client.facade.adapters;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.applications.ApplicationEntity;
import org.cloudfoundry.client.v2.applications.ApplicationResource;
import org.cloudfoundry.client.v2.applications.SummaryApplicationResponse;
import org.cloudfoundry.client.v2.domains.Domain;
import org.cloudfoundry.client.v2.routes.Route;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.CloudStack;
import com.sap.cloudfoundry.client.facade.domain.DockerInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudSpace;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudStack;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerCredentials;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableStaging;
import com.sap.cloudfoundry.client.facade.domain.PackageState;
import com.sap.cloudfoundry.client.facade.domain.Staging;

public class RawCloudApplicationTest {

    private static final int MEMORY = 256;
    private static final int DISK_QUOTA = 512;
    private static final int INSTANCES = 3;
    private static final int RUNNING_INSTANCES = 2;
    private static final String STATE = "STARTED";
    private static final String PACKAGE_STATE = "PENDING";
    private static final List<Route> ROUTES = buildRoutes();
    private static final List<ServiceInstance> SERVICE_INSTANCES = buildServiceInstances();
    private static final Map<String, Object> ENVIRONMENT = buildEnvironment();
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
    private static final org.cloudfoundry.client.v2.applications.DockerCredentials DOCKER_CREDENTIALS = buildDockerCredentials();
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
    private static final DockerInfo DOCKER_INFO_WITHOUT_CREDENTIALS = ImmutableDockerInfo.builder()
                                                                                         .image(DOCKER_IMAGE)
                                                                                         .build();
    private static final DockerInfo DOCKER_INFO = ImmutableDockerInfo.builder()
                                                                     .image(DOCKER_IMAGE)
                                                                     .credentials(ImmutableDockerCredentials.builder()
                                                                                                            .username(DOCKER_USERNAME)
                                                                                                            .password(DOCKER_PASSWORD)
                                                                                                            .build())
                                                                     .build();

    @MethodSource
    @ParameterizedTest
    public void testDerive(CloudApplication expectedApplication, RawCloudApplication rawApplication) {
        RawCloudEntityTest.testDerive(expectedApplication, rawApplication);
    }

    public static Stream<Arguments> testDerive() {
        return Stream.of(Arguments.of(buildApplicationWithoutEnvironment(), buildRawApplicationWithoutEnvironment()),
                         Arguments.of(buildApplicationWithoutDockerInfo(), buildRawApplicationWithoutDockerInfo()),
                         Arguments.of(buildApplicationWithoutDockerCredentials(), buildRawApplicationWithoutDockerCredentials()),
                         Arguments.of(buildApplication(), buildRawApplication()));
    }

    private static CloudApplication buildApplicationWithoutEnvironment() {
        return ImmutableCloudApplication.copyOf(buildApplication())
                                        .withEnv(Collections.emptyMap());
    }

    private static CloudApplication buildApplicationWithoutDockerInfo() {
        Staging staging = buildStaging();
        Staging stagingWithoutDockerInfo = ImmutableStaging.copyOf(staging)
                                                           .withDockerInfo(null);
        return buildApplication(stagingWithoutDockerInfo);
    }

    private static CloudApplication buildApplicationWithoutDockerCredentials() {
        Staging staging = buildStaging();
        Staging stagingWithoutDockerCredentials = ImmutableStaging.copyOf(staging)
                                                                  .withDockerInfo(DOCKER_INFO_WITHOUT_CREDENTIALS);
        return buildApplication(stagingWithoutDockerCredentials);
    }

    private static CloudApplication buildApplication() {
        return buildApplication(buildStaging());
    }

    private static CloudApplication buildApplication(Staging staging) {
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
                                        .staging(staging)
                                        .services(EXPECTED_SERVICES)
                                        .env(EXPECTED_ENVIRONMENT)
                                        .space(SPACE)
                                        .build();
    }

    private static Staging buildStaging() {
        return ImmutableStaging.builder()
                               .buildpacks(Arrays.asList(BUILDPACK))
                               .command(COMMAND)
                               .detectedBuildpack(DETECTED_BUILDPACK)
                               .dockerInfo(DOCKER_INFO)
                               .healthCheckHttpEndpoint(HEALTH_CHECK_HTTP_ENDPOINT)
                               .healthCheckTimeout(HEALTH_CHECK_TIMEOUT)
                               .healthCheckType(HEALTH_CHECK_TYPE)
                               .isSshEnabled(SSH_ENABLED)
                               .stack(STACK_NAME)
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

    private static RawCloudApplication buildRawApplicationWithoutEnvironment() {
        return buildRawApplication(buildApplicationResourceWithoutEnvironment(), buildApplicationSummary());
    }

    private static RawCloudApplication buildRawApplicationWithoutDockerInfo() {
        SummaryApplicationResponse summary = buildApplicationSummary();
        SummaryApplicationResponse summaryWithoutDockerInfo = SummaryApplicationResponse.builder()
                                                                                        .from(summary)
                                                                                        .dockerImage(null)
                                                                                        .dockerCredentials(null)
                                                                                        .build();
        return buildRawApplication(buildApplicationResource(), summaryWithoutDockerInfo);
    }

    private static RawCloudApplication buildRawApplicationWithoutDockerCredentials() {
        SummaryApplicationResponse summary = buildApplicationSummary();
        SummaryApplicationResponse summaryWithoutDockerCredentials = SummaryApplicationResponse.builder()
                                                                                               .from(summary)
                                                                                               .dockerCredentials(null)
                                                                                               .build();
        return buildRawApplication(buildApplicationResource(), summaryWithoutDockerCredentials);
    }

    private static RawCloudApplication buildRawApplication() {
        return buildRawApplication(buildApplicationResource(), buildApplicationSummary());
    }

    private static RawCloudApplication buildRawApplication(Resource<ApplicationEntity> applicationResource,
                                                           SummaryApplicationResponse summary) {
        return ImmutableRawCloudApplication.builder()
                                           .resource(applicationResource)
                                           .summary(summary)
                                           .stack(STACK)
                                           .space(SPACE)
                                           .build();
    }

    private static Resource<ApplicationEntity> buildApplicationResource() {
        return buildApplicationResource(ENVIRONMENT);
    }

    private static Resource<ApplicationEntity> buildApplicationResourceWithoutEnvironment() {
        return buildApplicationResource(null);
    }

    private static Resource<ApplicationEntity> buildApplicationResource(Map<String, Object> environmentJsons) {
        return ApplicationResource.builder()
                                  .metadata(RawCloudEntityTest.METADATA)
                                  .entity(ApplicationEntity.builder()
                                                           .environmentJsons(environmentJsons)
                                                           .build())
                                  .build();
    }

    private static SummaryApplicationResponse buildApplicationSummary() {
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
                                         .dockerImage(DOCKER_IMAGE)
                                         .dockerCredentials(DOCKER_CREDENTIALS)
                                         .command(COMMAND)
                                         .buildpack(BUILDPACK)
                                         .detectedBuildpack(DETECTED_BUILDPACK)
                                         .services(SERVICE_INSTANCES)
                                         .build();
    }

    private static List<Route> buildRoutes() {
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

    private static List<ServiceInstance> buildServiceInstances() {
        ServiceInstance foo = ServiceInstance.builder()
                                             .name("foo")
                                             .build();
        ServiceInstance bar = ServiceInstance.builder()
                                             .name("bar")
                                             .build();
        return Arrays.asList(foo, bar);
    }

    private static org.cloudfoundry.client.v2.applications.DockerCredentials buildDockerCredentials() {
        return org.cloudfoundry.client.v2.applications.DockerCredentials.builder()
                                                                        .username(DOCKER_USERNAME)
                                                                        .password(DOCKER_PASSWORD)
                                                                        .build();
    }

    private static Map<String, Object> buildEnvironment() {
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
