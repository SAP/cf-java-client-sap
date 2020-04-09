package org.cloudfoundry.client.lib.adapters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ServiceInstanceType;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.serviceinstances.UnionServiceInstanceEntity;
import org.cloudfoundry.client.v2.serviceinstances.UnionServiceInstanceResource;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanEntity;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanResource;
import org.cloudfoundry.client.v2.services.ServiceEntity;
import org.cloudfoundry.client.v2.services.ServiceResource;
import org.junit.jupiter.api.Test;

public class RawCloudServiceTest {

    private static final String NAME = "my-db";
    private static final String OFFERING_NAME = "postgresql";
    private static final String PLAN_NAME = "v9.4-small";
    private static final Map<String, Object> CREDENTIALS = buildTestCredentials();
    private static final List<String> TAGS = Arrays.asList("test-tag-1", "test-tag-2");

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedService(), buildRawService());
    }

    @Test
    public void testDeriveWithUserProvidedService() {
        RawCloudEntityTest.testDerive(buildExpectedUserProvidedService(), buildRawUserProvidedService());
    }

    private static CloudServiceInstance buildExpectedService() {
        return ImmutableCloudServiceInstance.builder()
                                            .metadata(RawCloudEntityTest.EXPECTED_METADATA)
                                            .name(NAME)
                                            .plan(PLAN_NAME)
                                            .label(OFFERING_NAME)
                                            .type(ServiceInstanceType.MANAGED)
                                            .credentials(CREDENTIALS)
                                            .tags(TAGS)
                                            .build();
    }

    private static CloudServiceInstance buildExpectedUserProvidedService() {
        return ImmutableCloudServiceInstance.builder()
                                            .metadata(RawCloudEntityTest.EXPECTED_METADATA)
                                            .name(NAME)
                                            .type(ServiceInstanceType.USER_PROVIDED)
                                            .credentials(CREDENTIALS)
                                            .tags(TAGS)
                                            .build();
    }

    private static RawCloudServiceInstance buildRawService() {
        return ImmutableRawCloudServiceInstance.builder()
                                               .resource(buildTestResource())
                                               .servicePlanResource(buildTestServicePlanResource())
                                               .serviceResource(buildTestServiceResource())
                                               .build();
    }

    private static RawCloudServiceInstance buildRawUserProvidedService() {
        return ImmutableRawCloudServiceInstance.builder()
                                               .resource(buildUserProvidedTestResource())
                                               .build();
    }

    private static Resource<UnionServiceInstanceEntity> buildTestResource() {
        return UnionServiceInstanceResource.builder()
                                           .metadata(RawCloudEntityTest.METADATA)
                                           .entity(buildTestEntity())
                                           .build();
    }

    private static Resource<UnionServiceInstanceEntity> buildUserProvidedTestResource() {
        return UnionServiceInstanceResource.builder()
                                           .metadata(RawCloudEntityTest.METADATA)
                                           .entity(buildUserProvidedTestEntity())
                                           .build();
    }

    private static UnionServiceInstanceEntity buildTestEntity() {
        return UnionServiceInstanceEntity.builder()
                                         .name(NAME)
                                         .type("managed_service_instance")
                                         .credentials(CREDENTIALS)
                                         .addAllTags(TAGS)
                                         .build();
    }

    private static UnionServiceInstanceEntity buildUserProvidedTestEntity() {
        return UnionServiceInstanceEntity.builder()
                                         .name(NAME)
                                         .type("user_provided_service_instance")
                                         .credentials(CREDENTIALS)
                                         .addAllTags(TAGS)
                                         .build();
    }

    private static Resource<ServicePlanEntity> buildTestServicePlanResource() {
        return ServicePlanResource.builder()
                                  .entity(buildTestServicePlanEntity())
                                  .build();
    }

    private static ServicePlanEntity buildTestServicePlanEntity() {
        return ServicePlanEntity.builder()
                                .name(PLAN_NAME)
                                .build();
    }

    private static Resource<ServiceEntity> buildTestServiceResource() {
        return ServiceResource.builder()
                              .entity(buildTestServiceEntity())
                              .build();
    }

    private static ServiceEntity buildTestServiceEntity() {
        return ServiceEntity.builder()
                            .label(OFFERING_NAME)
                            .build();
    }

    private static Map<String, Object> buildTestCredentials() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("foo", "bar");
        parameters.put("baz", false);
        parameters.put("qux", 3.141);
        return parameters;
    }

}
