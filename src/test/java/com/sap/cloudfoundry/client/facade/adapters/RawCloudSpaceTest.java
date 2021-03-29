package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.spaces.SpaceEntity;
import org.cloudfoundry.client.v2.spaces.SpaceResource;
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
                                  .metadata(RawCloudEntityTest.EXPECTED_METADATA)
                                  .name(NAME)
                                  .build();
    }

    private static RawCloudSpace buildRawSpace() {
        return ImmutableRawCloudSpace.of(buildTestResource());
    }

    private static Resource<SpaceEntity> buildTestResource() {
        return SpaceResource.builder()
                            .metadata(RawCloudEntityTest.METADATA)
                            .entity(buildTestEntity())
                            .build();
    }

    private static SpaceEntity buildTestEntity() {
        return SpaceEntity.builder()
                          .name(NAME)
                          .build();
    }

}
