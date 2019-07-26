package org.cloudfoundry.client.lib.adapters;

import java.util.Arrays;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceOffering;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServicePlan;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.services.ServiceEntity;
import org.cloudfoundry.client.v2.services.ServiceResource;
import org.junit.jupiter.api.Test;

public class RawCloudServiceOfferingTest {

    private static final String NAME = "postgresql";
    private static final boolean ACTIVE = true;
    private static final boolean BINDABLE = true;
    private static final String DESCRIPTION = "description";
    private static final String EXTRA = "extra";
    private static final String DOCUMENTATION_URL = "/documentation";
    private static final String INFO_URL = "/info";
    private static final String PROVIDER = "provider";
    private static final String VERSION = "9.4";
    private static final String UNIQUE_ID = "unique-id";
    private static final String URL = "/url";
    private static final List<CloudServicePlan> PLANS = buildTestServiceBindings();

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedServiceOffering(), buildRawServiceOffering());
    }

    private static CloudServiceOffering buildExpectedServiceOffering() {
        return ImmutableCloudServiceOffering.builder()
            .metadata(RawCloudEntityTest.EXPECTED_METADATA)
            .name(NAME)
            .isActive(ACTIVE)
            .isBindable(BINDABLE)
            .description(DESCRIPTION)
            .extra(EXTRA)
            .docUrl(DOCUMENTATION_URL)
            .infoUrl(INFO_URL)
            .version(VERSION)
            .provider(PROVIDER)
            .uniqueId(UNIQUE_ID)
            .url(URL)
            .servicePlans(PLANS)
            .build();
    }

    private static RawCloudServiceOffering buildRawServiceOffering() {
        return ImmutableRawCloudServiceOffering.builder()
            .resource(buildTestResource())
            .servicePlans(PLANS)
            .build();
    }

    private static Resource<ServiceEntity> buildTestResource() {
        return ServiceResource.builder()
            .metadata(RawCloudEntityTest.METADATA)
            .entity(buildTestEntity())
            .build();
    }

    private static ServiceEntity buildTestEntity() {
        return ServiceEntity.builder()
            .label(NAME)
            .active(ACTIVE)
            .bindable(BINDABLE)
            .description(DESCRIPTION)
            .extra(EXTRA)
            .documentationUrl(DOCUMENTATION_URL)
            .infoUrl(INFO_URL)
            .version(VERSION)
            .provider(PROVIDER)
            .uniqueId(UNIQUE_ID)
            .url(URL)
            .build();
    }

    private static List<CloudServicePlan> buildTestServiceBindings() {
        return Arrays.asList(buildTestServiceBinding());
    }

    private static CloudServicePlan buildTestServiceBinding() {
        return ImmutableCloudServicePlan.builder()
            .name("v9.4-small")
            .build();
    }

}
