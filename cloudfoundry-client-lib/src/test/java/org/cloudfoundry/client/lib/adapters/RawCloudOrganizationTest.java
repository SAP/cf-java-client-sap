package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.ImmutableCloudOrganization;
import org.cloudfoundry.client.v3.Link;
import org.cloudfoundry.client.v3.organizations.Organization;
import org.cloudfoundry.client.v3.organizations.OrganizationResource;
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

    private static Organization buildTestResource() {
        return OrganizationResource.builder()
                                   .name(ORGANIZATION_NAME)
                                   .id(RawCloudEntityTest.EXPECTED_METADATA.getGuid()
                                                                           .toString())
                                   .createdAt(RawCloudEntityTest.METADATA.getCreatedAt())
                                   .updatedAt(RawCloudEntityTest.METADATA.getUpdatedAt())
                                   .link("self", Link.builder()
                                                     .href(RawCloudEntityTest.URL_STRING)
                                                     .build())
                                   .build();
    }

}
