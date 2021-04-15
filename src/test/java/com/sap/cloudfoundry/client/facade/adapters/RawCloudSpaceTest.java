package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v3.spaces.SpaceResource;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudSpace;

public class RawCloudSpaceTest {

    private static final String NAME = "test";

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedSpace(), buildRawSpace());
    }

    private static CloudSpace buildExpectedSpace() {
        return ImmutableCloudSpace.builder()
                                  .metadata(RawCloudEntityTest.EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE)
                                  .name(NAME)
                                  .build();
    }

    private static RawCloudSpace buildRawSpace() {
        return ImmutableRawCloudSpace.of(buildTestResource());
    }

    private static SpaceResource buildTestResource() {
        return SpaceResource.builder()
                            .id(RawCloudEntityTest.GUID_STRING)
                            .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                            .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                            .metadata(RawCloudEntityTest.V3_METADATA)
                            .name(NAME)
                            .build();
    }

}
