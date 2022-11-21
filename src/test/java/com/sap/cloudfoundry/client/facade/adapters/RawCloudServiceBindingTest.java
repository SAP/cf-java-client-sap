package com.sap.cloudfoundry.client.facade.adapters;

import java.util.UUID;

import org.cloudfoundry.client.v3.servicebindings.ServiceBindingRelationships;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingResource;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingType;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBinding;

class RawCloudServiceBindingTest {

    private static final String APPLICATION_GUID_STRING = "3725650a-8725-4401-a949-c68f83d54a86";
    private static final String SERVICE_INSTANCE_GUID_STRING = "3725650a-8725-4401-a949-c68f83d54a86";
    private static final UUID APPLICATION_GUID = UUID.fromString(APPLICATION_GUID_STRING);
    private static final UUID SERVICE_INSTANCE_GUID = UUID.fromString(SERVICE_INSTANCE_GUID_STRING);

    @Test
    void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedServiceBinding(), buildRawServiceBinding());
    }

    private static CloudServiceBinding buildExpectedServiceBinding() {
        return ImmutableCloudServiceBinding.builder()
                                           .metadata(RawCloudEntityTest.EXPECTED_METADATA_V3)
                                           .applicationGuid(APPLICATION_GUID)
                                           .serviceInstanceGuid(SERVICE_INSTANCE_GUID)
                                           .build();
    }

    private static RawCloudServiceBinding buildRawServiceBinding() {
        return ImmutableRawCloudServiceBinding.builder()
                                              .serviceBinding(buildTestResource())
                                              .build();
    }

    private static ServiceBindingResource buildTestResource() {
        return ServiceBindingResource.builder()
                                     .id(RawCloudEntityTest.GUID_STRING)
                                     .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                                     .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                     .type(ServiceBindingType.APPLICATION)
                                     .relationships(ServiceBindingRelationships.builder()
                                                                               .serviceInstance(RawCloudEntityTest.buildToOneRelationship(SERVICE_INSTANCE_GUID_STRING))
                                                                               .application(RawCloudEntityTest.buildToOneRelationship(APPLICATION_GUID_STRING))
                                                                               .build())
                                     .build();
    }

}
