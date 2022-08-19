package com.sap.cloudfoundry.client.facade.adapters;

import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.DockerData;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.LifecycleType;
import org.cloudfoundry.client.v3.applications.ApplicationResource;
import org.cloudfoundry.client.v3.applications.ApplicationState;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudSpace;
import com.sap.cloudfoundry.client.facade.domain.ImmutableLifecycle;

class RawCloudApplicationTest {

    private static final String STATE = "STARTED";
    private static final String BUILDPACK = "ruby_buildpack";
    private static final String STACK_NAME = "cflinuxfs3";
    private static final String SPACE_NAME = "test";
    private static final CloudSpace SPACE = ImmutableCloudSpace.builder()
                                                               .name(SPACE_NAME)
                                                               .build();
    private static final String EXPECTED_BUILDPACK = "ruby_buildpack";
    private static final String EXPECTED_STACK = "cflinuxfs3";
    private static final CloudApplication.State EXPECTED_STATE = CloudApplication.State.STARTED;

    @Test
    void testDeriveForBuildpackApp() {
        RawCloudEntityTest.testDerive(buildApplication(buildBuildpackLifecycle()),
                                      buildRawApplication(buildBuildpackLifecycleResource()));
    }

    @Test
    void testDeriveForDockerApp() {
        RawCloudEntityTest.testDerive(buildApplication(buildDockerLifecycle()),
                                      buildRawApplication(buildDockerLifecycleResource()));
    }

    private static CloudApplication buildApplication(com.sap.cloudfoundry.client.facade.domain.Lifecycle lifecycle) {
        return ImmutableCloudApplication.builder()
                                        .metadata(RawCloudEntityTest.EXPECTED_METADATA_V3)
                                        .v3Metadata(RawCloudEntityTest.V3_METADATA)
                                        .name(RawCloudEntityTest.NAME)
                                        .state(EXPECTED_STATE)
                                        .lifecycle(lifecycle)
                                        .space(SPACE)
                                        .build();
    }

    private static com.sap.cloudfoundry.client.facade.domain.Lifecycle buildBuildpackLifecycle() {
        return ImmutableLifecycle.builder()
                                 .type(com.sap.cloudfoundry.client.facade.domain.LifecycleType.BUILDPACK)
                                 .data(buildBuildpackLifecycleData())
                                 .build();
    }

    private static Map<String, Object> buildBuildpackLifecycleData() {
        return Map.of("buildpacks", List.of(EXPECTED_BUILDPACK),
                      "stack", EXPECTED_STACK);
    }

    private static com.sap.cloudfoundry.client.facade.domain.Lifecycle buildDockerLifecycle() {
        return ImmutableLifecycle.builder()
                                 .type(com.sap.cloudfoundry.client.facade.domain.LifecycleType.DOCKER)
                                 .data(Map.of())
                                 .build();
    }

    private static RawCloudApplication buildRawApplication(Lifecycle lifecycle) {
        return ImmutableRawCloudApplication.builder()
                                           .application(buildApplicationResource(lifecycle))
                                           .space(SPACE)
                                           .build();
    }

    private static ApplicationResource buildApplicationResource(Lifecycle lifecycle) {
        return ApplicationResource.builder()
                                  .metadata(RawCloudEntityTest.V3_METADATA)
                                  .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                                  .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                  .state(ApplicationState.valueOf(STATE))
                                  .id(RawCloudEntityTest.GUID.toString())
                                  .lifecycle(lifecycle)
                                  .name("foo")
                                  .build();
    }

    private static Lifecycle buildBuildpackLifecycleResource() {
        return Lifecycle.builder()
                        .type(LifecycleType.BUILDPACK)
                        .data(BuildpackData.builder()
                                           .buildpack(BUILDPACK)
                                           .stack(STACK_NAME)
                                           .build())
                        .build();
    }

    private static Lifecycle buildDockerLifecycleResource() {
        return Lifecycle.builder()
                        .type(LifecycleType.DOCKER)
                        .data(DockerData.builder()
                                        .build())
                        .build();
    }

}
