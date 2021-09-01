package com.sap.cloudfoundry.client.facade.adapters;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.v2.applications.SummaryApplicationResponse;
import org.cloudfoundry.client.v2.domains.Domain;
import org.cloudfoundry.client.v2.routes.Route;
import org.cloudfoundry.client.v2.serviceinstances.ServiceInstance;
import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.LifecycleType;
import org.cloudfoundry.client.v3.applications.ApplicationResource;
import org.cloudfoundry.client.v3.applications.ApplicationState;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.CloudStack;
import com.sap.cloudfoundry.client.facade.domain.DockerInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRouteSummary;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudSpace;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudStack;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerCredentials;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDockerInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableStaging;
import com.sap.cloudfoundry.client.facade.domain.PackageState;
import com.sap.cloudfoundry.client.facade.domain.Staging;

class RawCloudApplicationTest {

    private static final int MEMORY = 256;
    private static final int DISK_QUOTA = 512;
    private static final int INSTANCES = 3;
    private static final int RUNNING_INSTANCES = 2;
    private static final String STATE = "STARTED";
    private static final String PACKAGE_STATE = "PENDING";
    private static final List<Route> ROUTES = buildRoutes();
    private static final Set<CloudRouteSummary> EXPECTED_ROUTES = buildRouteSummaries();
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
    void testDerive(CloudApplication expectedApplication, RawCloudApplication rawApplication) {
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
                                        .metadata(RawCloudEntityTest.EXPECTED_METADATA_V3)
                                        .name(RawCloudEntityTest.NAME)
                                        .routes(EXPECTED_ROUTES)
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
                               .buildpacks(List.of(BUILDPACK))
                               .command(COMMAND)
                               .detectedBuildpack(DETECTED_BUILDPACK)
                               .dockerInfo(DOCKER_INFO)
                               .healthCheckHttpEndpoint(HEALTH_CHECK_HTTP_ENDPOINT)
                               .healthCheckTimeout(HEALTH_CHECK_TIMEOUT)
                               .healthCheckType(HEALTH_CHECK_TYPE)
                               .isSshEnabled(SSH_ENABLED)
                               .stackName(STACK_NAME)
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
        SummaryApplicationResponse summary = buildApplicationSummary();
        SummaryApplicationResponse summaryWithoutEnvironment = SummaryApplicationResponse.builder()
                                                                                         .from(summary)
                                                                                         .environmentJsons(null)
                                                                                         .build();
        return buildRawApplication(summaryWithoutEnvironment);
    }

    private static RawCloudApplication buildRawApplicationWithoutDockerInfo() {
        SummaryApplicationResponse summary = buildApplicationSummary();
        SummaryApplicationResponse summaryWithoutDockerInfo = SummaryApplicationResponse.builder()
                                                                                        .from(summary)
                                                                                        .dockerImage(null)
                                                                                        .dockerCredentials(null)
                                                                                        .build();
        return buildRawApplication(summaryWithoutDockerInfo);
    }

    private static RawCloudApplication buildRawApplicationWithoutDockerCredentials() {
        SummaryApplicationResponse summary = buildApplicationSummary();
        SummaryApplicationResponse summaryWithoutDockerCredentials = SummaryApplicationResponse.builder()
                                                                                               .from(summary)
                                                                                               .dockerCredentials(null)
                                                                                               .build();
        return buildRawApplication(summaryWithoutDockerCredentials);
    }

    private static RawCloudApplication buildRawApplication() {
        return buildRawApplication(buildApplicationSummary());
    }

    private static RawCloudApplication buildRawApplication(SummaryApplicationResponse summary) {
        return ImmutableRawCloudApplication.builder()
                                           .application(buildApplicationResource())
                                           .summary(summary)
                                           .stack(STACK)
                                           .space(SPACE)
                                           .build();
    }

    private static ApplicationResource buildApplicationResource() {
        return ApplicationResource.builder()
                                  .metadata(RawCloudEntityTest.V3_METADATA)
                                  .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                                  .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                  .state(ApplicationState.STARTED)
                                  .id(RawCloudEntityTest.GUID.toString())
                                  .lifecycle(Lifecycle.builder()
                                                      .type(LifecycleType.BUILDPACK)
                                                      .data(BuildpackData.builder()
                                                                         .buildpack("buildpack")
                                                                         .build())
                                                      .build())
                                  .name("foo")
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
                                         .environmentJsons(ENVIRONMENT)
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
        return Stream.of(Route.builder()
                              .host("foo")
                              .domain(domain)
                              .path("/does/this/work")
                              .build(),
                         Route.builder()
                              .host("bar")
                              .domain(domain)
                              .port(30030)
                              .build(),
                         Route.builder()
                              .host("baz")
                              .domain(domain)
                              .build(),
                         Route.builder()
                              .domain(domain)
                              .build(),
                         Route.builder()
                              .host("")
                              .domain(domain)
                              .build())
                     .collect(Collectors.toList());
    }

    private static Set<CloudRouteSummary> buildRouteSummaries() {
        return Stream.of(ImmutableCloudRouteSummary.builder()
                                                   .host("foo")
                                                   .domain("example.com")
                                                   .path("/does/this/work")
                                                   .build(),
                         ImmutableCloudRouteSummary.builder()
                                                   .host("bar")
                                                   .domain("example.com")
                                                   .port(30030)
                                                   .build(),
                         ImmutableCloudRouteSummary.builder()
                                                   .host("baz")
                                                   .domain("example.com")
                                                   .build(),
                         ImmutableCloudRouteSummary.builder()
                                                   .domain("example.com")
                                                   .build(),
                         ImmutableCloudRouteSummary.builder()
                                                   .host("")
                                                   .domain("example.com")
                                                   .build())
                     .collect(Collectors.toSet());
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
