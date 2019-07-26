package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.ImmutableCloudDomain;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.privatedomains.PrivateDomainEntity;
import org.cloudfoundry.client.v2.privatedomains.PrivateDomainResource;
import org.junit.jupiter.api.Test;

public class RawCloudPrivateDomainTest {

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

    private static RawCloudPrivateDomain buildRawDomain() {
        return ImmutableRawCloudPrivateDomain.of(buildTestResource());
    }

    private static Resource<PrivateDomainEntity> buildTestResource() {
        return PrivateDomainResource.builder()
            .metadata(RawCloudEntityTest.METADATA)
            .entity(buildTestEntity())
            .build();
    }

    private static PrivateDomainEntity buildTestEntity() {
        return PrivateDomainEntity.builder()
            .name(DOMAIN_NAME)
            .build();
    }

}
