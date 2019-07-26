package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServicePlan;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanEntity;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanResource;
import org.junit.jupiter.api.Test;

public class RawCloudServicePlanTest {

    private static final String NAME = "v9.4-small";
    private static final String DESCRIPTION = "description";
    private static final String EXTRA = "extra";
    private static final String UNIQUE_ID = "unique-id";
    private static final boolean PUBLIC = true;
    private static final boolean FREE = false;

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedPlan(), buildRawServicePlan());
    }

    private static final CloudServicePlan buildExpectedPlan() {
        return ImmutableCloudServicePlan.builder()
            .metadata(RawCloudEntityTest.EXPECTED_METADATA)
            .name(NAME)
            .description(DESCRIPTION)
            .extra(EXTRA)
            .uniqueId(UNIQUE_ID)
            .isPublic(PUBLIC)
            .isFree(FREE)
            .build();
    }

    private static final RawCloudServicePlan buildRawServicePlan() {
        return ImmutableRawCloudServicePlan.of(buildTestResource());
    }

    private static Resource<ServicePlanEntity> buildTestResource() {
        return ServicePlanResource.builder()
            .metadata(RawCloudEntityTest.METADATA)
            .entity(buildTestEntity())
            .build();
    }

    private static ServicePlanEntity buildTestEntity() {
        return ServicePlanEntity.builder()
            .name(NAME)
            .description(DESCRIPTION)
            .extra(EXTRA)
            .uniqueId(UNIQUE_ID)
            .publiclyVisible(PUBLIC)
            .free(FREE)
            .build();
    }

}
