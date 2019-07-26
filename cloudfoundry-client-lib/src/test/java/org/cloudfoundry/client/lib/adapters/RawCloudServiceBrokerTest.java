package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBroker;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.servicebrokers.ServiceBrokerEntity;
import org.cloudfoundry.client.v2.servicebrokers.ServiceBrokerResource;
import org.junit.jupiter.api.Test;

public class RawCloudServiceBrokerTest {

    private static final String NAME = "auditlog-broker";
    private static final String USERNAME = "admin";
    private static final String URL = "https://auditlog-broker.example.com";
    private static final String SPACE_GUID_STRING = "ef93547f-74c3-4bad-ba69-b7dc4f212622";

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedServiceBroker(), buildRawServiceBroker());
    }

    private static final CloudServiceBroker buildExpectedServiceBroker() {
        return ImmutableCloudServiceBroker.builder()
            .metadata(RawCloudEntityTest.EXPECTED_METADATA)
            .username(USERNAME)
            .name(NAME)
            .spaceGuid(SPACE_GUID_STRING)
            .url(URL)
            .build();
    }

    private static final RawCloudServiceBroker buildRawServiceBroker() {
        return ImmutableRawCloudServiceBroker.builder()
            .resource(buildTestResource())
            .build();
    }

    private static Resource<ServiceBrokerEntity> buildTestResource() {
        return ServiceBrokerResource.builder()
            .metadata(RawCloudEntityTest.METADATA)
            .entity(buildTestEntity())
            .build();
    }

    private static ServiceBrokerEntity buildTestEntity() {
        return ServiceBrokerEntity.builder()
            .authenticationUsername(USERNAME)
            .spaceId(SPACE_GUID_STRING)
            .name(NAME)
            .brokerUrl(URL)
            .build();
    }

}
