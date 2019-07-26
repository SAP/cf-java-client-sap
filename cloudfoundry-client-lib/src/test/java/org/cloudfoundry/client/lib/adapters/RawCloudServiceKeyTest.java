package org.cloudfoundry.client.lib.adapters;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.ImmutableCloudService;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.servicekeys.ServiceKeyEntity;
import org.cloudfoundry.client.v2.servicekeys.ServiceKeyResource;
import org.junit.jupiter.api.Test;

public class RawCloudServiceKeyTest {

    private static final String SERVICE_NAME = "foo";
    private static final String NAME = "bar";
    private static final Map<String, Object> CREDENTIALS = buildTestCredentials();
    private static final CloudService SERVICE = ImmutableCloudService.builder()
        .name(SERVICE_NAME)
        .build();

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedServiceKey(), buildRawServiceKey());
    }

    private static CloudServiceKey buildExpectedServiceKey() {
        return ImmutableCloudServiceKey.builder()
            .metadata(RawCloudEntityTest.EXPECTED_METADATA)
            .name(NAME)
            .credentials(CREDENTIALS)
            .service(SERVICE)
            .build();
    }

    private static RawCloudServiceKey buildRawServiceKey() {
        return ImmutableRawCloudServiceKey.builder()
            .resource(buildTestResource())
            .service(SERVICE)
            .build();
    }

    private static Resource<ServiceKeyEntity> buildTestResource() {
        return ServiceKeyResource.builder()
            .metadata(RawCloudEntityTest.METADATA)
            .entity(buildTestEntity())
            .build();
    }

    private static ServiceKeyEntity buildTestEntity() {
        return ServiceKeyEntity.builder()
            .name(NAME)
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

}
