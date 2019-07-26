package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.domain.ImmutableCloudStack;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.stacks.StackEntity;
import org.cloudfoundry.client.v2.stacks.StackResource;
import org.junit.jupiter.api.Test;

public class RawCloudStackTest {

    private static final String NAME = "cflinuxfs3";

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedStack(), buildRawStack());
    }

    private static CloudStack buildExpectedStack() {
        return ImmutableCloudStack.builder()
            .metadata(RawCloudEntityTest.EXPECTED_METADATA)
            .name(NAME)
            .build();
    }

    private static RawCloudStack buildRawStack() {
        return ImmutableRawCloudStack.of(buildTestResource());
    }

    private static Resource<StackEntity> buildTestResource() {
        return StackResource.builder()
            .metadata(RawCloudEntityTest.METADATA)
            .entity(buildTestEntity())
            .build();
    }

    private static StackEntity buildTestEntity() {
        return StackEntity.builder()
            .name(NAME)
            .build();
    }

}
