package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.ImmutableCloudOrganization;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.organizations.OrganizationEntity;
import org.cloudfoundry.client.v2.organizations.OrganizationResource;
import org.junit.jupiter.api.Test;

public class RawCloudOrganizationTest {

    private static final String ORGANIZATION_NAME = "example";

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedOrganization(), buildRawOrganization());
    }

    private static CloudOrganization buildExpectedOrganization() {
        return ImmutableCloudOrganization.builder()
            .metadata(RawCloudEntityTest.EXPECTED_METADATA)
            .name(ORGANIZATION_NAME)
            .build();
    }

    private static RawCloudOrganization buildRawOrganization() {
        return ImmutableRawCloudOrganization.of(buildTestResource());
    }

    private static Resource<OrganizationEntity> buildTestResource() {
        return OrganizationResource.builder()
            .metadata(RawCloudEntityTest.METADATA)
            .entity(buildTestEntity())
            .build();
    }

    private static OrganizationEntity buildTestEntity() {
        return OrganizationEntity.builder()
            .name(ORGANIZATION_NAME)
            .build();
    }

}
