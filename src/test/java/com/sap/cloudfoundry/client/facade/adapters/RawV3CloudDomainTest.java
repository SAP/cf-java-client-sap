package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v3.ToManyRelationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.domains.Domain;
import org.cloudfoundry.client.v3.domains.DomainRelationships;
import org.cloudfoundry.client.v3.organizations.GetOrganizationDefaultDomainResponse;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;

public class RawV3CloudDomainTest {

    private static final String DOMAIN_NAME = "example.com";

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedDomain(), buildRawDomain());
    }

    private static CloudDomain buildExpectedDomain() {
        return ImmutableCloudDomain.builder()
                                   .metadata(RawCloudEntityTest.EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE)
                                   .name(DOMAIN_NAME)
                                   .build();
    }

    private static RawV3CloudDomain buildRawDomain() {
        return ImmutableRawV3CloudDomain.of(buildTestResource());
    }

    private static Domain buildTestResource() {
        return GetOrganizationDefaultDomainResponse.builder()
                                                   .id(RawCloudEntityTest.GUID_STRING)
                                                   .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                                                   .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                                                   .name(DOMAIN_NAME)
                                                   .isInternal(false)
                                                   .relationships(DomainRelationships.builder()
                                                                                     .organization(ToOneRelationship.builder()
                                                                                                                    .build())
                                                                                     .sharedOrganizations(ToManyRelationship.builder()
                                                                                                                            .build())
                                                                                     .build())
                                                   .build();
    }

}
