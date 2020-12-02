package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v3.organizations.OrganizationResource;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudOrganization;

public class RawCloudOrganizationTest {

    private static final String ORGANIZATION_NAME = "example";

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedOrganization(), buildRawOrganization());
    }

    private static CloudOrganization buildExpectedOrganization() {
        return ImmutableCloudOrganization.builder()
                                         .metadata(RawCloudEntityTest.EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE)
                                         .name(ORGANIZATION_NAME)
                                         .build();
    }

    private static RawCloudOrganization buildRawOrganization() {
        return ImmutableRawCloudOrganization.of(buildTestResource());
    }

    private static OrganizationResource buildTestResource() {
        return OrganizationResource.builder()
                                   .id(RawCloudEntityTest.GUID_STRING)
                                   .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                                   .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                   .metadata(RawCloudEntityTest.V3_METADATA)
                                   .name(ORGANIZATION_NAME)
                                   .build();
    }

}
