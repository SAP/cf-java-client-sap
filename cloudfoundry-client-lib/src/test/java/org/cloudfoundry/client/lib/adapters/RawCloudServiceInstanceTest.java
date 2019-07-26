package org.cloudfoundry.client.lib.adapters;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudService;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBinding;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.serviceinstances.UnionServiceInstanceEntity;
import org.cloudfoundry.client.v2.serviceinstances.UnionServiceInstanceResource;
import org.junit.jupiter.api.Test;

public class RawCloudServiceInstanceTest {

    private static final String NAME = "foo";
    private static final String TYPE = "user_provided_service_instance";
    private static final String DASHBOARD_URL = "/dashboard";
    private static final Map<String, Object> CREDENTIALS = buildTestCredentials();
    private static final CloudService SERVICE = buildTestService();
    private static final List<CloudServiceBinding> BINDINGS = buildTestServiceBindings();

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedServiceInstance(), buildRawServiceInstance());
    }

    private static CloudServiceInstance buildExpectedServiceInstance() {
        return ImmutableCloudServiceInstance.builder()
            .metadata(RawCloudEntityTest.EXPECTED_METADATA)
            .dashboardUrl(DASHBOARD_URL)
            .bindings(BINDINGS)
            .name(NAME)
            .type(TYPE)
            .service(SERVICE)
            .credentials(CREDENTIALS)
            .build();
    }

    private static RawCloudServiceInstance buildRawServiceInstance() {
        return ImmutableRawCloudServiceInstance.builder()
            .resource(buildTestResource())
            .serviceBindings(BINDINGS)
            .service(SERVICE)
            .build();
    }

    private static Resource<UnionServiceInstanceEntity> buildTestResource() {
        return UnionServiceInstanceResource.builder()
            .metadata(RawCloudEntityTest.METADATA)
            .entity(buildTestEntity())
            .build();
    }

    private static UnionServiceInstanceEntity buildTestEntity() {
        return UnionServiceInstanceEntity.builder()
            .name(NAME)
            .type(TYPE)
            .dashboardUrl(DASHBOARD_URL)
            .credentials(CREDENTIALS)
            .build();
    }

    private static Map<String, Object> buildTestCredentials() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("foo", "bar");
        parameters.put("baz", false);
        parameters.put("qux", 3.141);
        return parameters;
    }

    private static CloudService buildTestService() {
        return ImmutableCloudService.builder()
            .name(NAME)
            .build();
    }

    private static List<CloudServiceBinding> buildTestServiceBindings() {
        return Arrays.asList(buildTestServiceBinding());
    }

    private static CloudServiceBinding buildTestServiceBinding() {
        return ImmutableCloudServiceBinding.builder()
            .applicationGuid(UUID.fromString("c9a0d86b-9f53-4a26-93a5-b42fa1dff17d"))
            .build();
    }

}
