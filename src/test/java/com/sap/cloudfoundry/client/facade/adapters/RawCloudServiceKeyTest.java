package com.sap.cloudfoundry.client.facade.adapters;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.v3.servicebindings.ServiceBindingRelationships;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingResource;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingType;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;

public class RawCloudServiceKeyTest {

    private static final String SERVICE_NAME = "foo";
    private static final String NAME = "bar";
    private static final Map<String, Object> CREDENTIALS = buildTestCredentials();
    private static final CloudServiceInstance SERVICE_INSTANCE = ImmutableCloudServiceInstance.builder()
                                                                                              .name(SERVICE_NAME)
                                                                                              .build();

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedServiceKey(), buildRawServiceKey());
    }

    private static CloudServiceKey buildExpectedServiceKey() {
        return ImmutableCloudServiceKey.builder()
                                       .metadata(RawCloudEntityTest.EXPECTED_METADATA_V3)
                                       .name(NAME)
                                       .credentials(CREDENTIALS)
                                       .serviceInstance(SERVICE_INSTANCE)
                                       .build();
    }

    private static RawCloudServiceKey buildRawServiceKey() {
        return ImmutableRawCloudServiceKey.builder()
                                          .serviceBinding(buildTestResource())
                                          .serviceInstance(SERVICE_INSTANCE)
                                          .credentials(CREDENTIALS)
                                          .build();
    }

    private static ServiceBindingResource buildTestResource() {
        return ServiceBindingResource.builder()
                                     .id(RawCloudEntityTest.GUID_STRING)
                                     .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                                     .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                     .metadata(RawCloudEntityTest.V3_METADATA)
                                     .name(NAME)
                                     .type(ServiceBindingType.KEY)
                                     .relationships(ServiceBindingRelationships.builder()
                                                                               .serviceInstance(RawCloudEntityTest.buildToOneRelationship(""))
                                                                               .build())
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
