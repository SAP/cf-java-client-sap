package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.ImmutableCloudDomain;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.domains.DomainEntity;
import org.cloudfoundry.client.v2.domains.DomainResource;
import org.junit.jupiter.api.Test;

public class RawCloudDomainTest {

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

    private static RawCloudDomain buildRawDomain() {
        return ImmutableRawCloudDomain.of(buildTestResource());
    }

    private static Resource<DomainEntity> buildTestResource() {
        return DomainResource.builder()
            .metadata(RawCloudEntityTest.METADATA)
            .entity(buildTestEntity())
            .build();
    }

    private static DomainEntity buildTestEntity() {
        return DomainEntity.builder()
            .name(DOMAIN_NAME)
            .build();
    }

}
