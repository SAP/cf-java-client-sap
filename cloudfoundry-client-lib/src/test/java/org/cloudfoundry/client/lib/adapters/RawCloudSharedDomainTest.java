package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.ImmutableCloudDomain;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.shareddomains.SharedDomainEntity;
import org.cloudfoundry.client.v2.shareddomains.SharedDomainResource;
import org.junit.jupiter.api.Test;

public class RawCloudSharedDomainTest {

    private static final String DOMAIN_NAME = "example.com";

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedDomain(), buildRawDomain());
    }

    private static CloudDomain buildExpectedDomain() {
        return ImmutableCloudDomain.builder()
            .metadata(RawCloudEntityTest.EXPECTED_METADATA)
            .name(DOMAIN_NAME)
            .build();
    }

    private static RawCloudSharedDomain buildRawDomain() {
        return ImmutableRawCloudSharedDomain.of(buildTestResource());
    }

    private static Resource<SharedDomainEntity> buildTestResource() {
        return SharedDomainResource.builder()
            .metadata(RawCloudEntityTest.METADATA)
            .entity(buildTestEntity())
            .build();
    }

    private static SharedDomainEntity buildTestEntity() {
        return SharedDomainEntity.builder()
            .name(DOMAIN_NAME)
            .build();
    }

}
