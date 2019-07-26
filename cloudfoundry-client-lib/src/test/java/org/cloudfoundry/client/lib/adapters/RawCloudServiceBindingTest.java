package org.cloudfoundry.client.lib.adapters;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBinding;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.servicebindings.ServiceBindingEntity;
import org.cloudfoundry.client.v2.servicebindings.ServiceBindingResource;
import org.junit.jupiter.api.Test;

public class RawCloudServiceBindingTest {

    private static final String APPLICATION_GUID_STRING = "3725650a-8725-4401-a949-c68f83d54a86";
    private static final String SYSLOG_DRAIN_URL = "/syslog";
    private static final Map<String, Object> PARAMETERS = buildTestParameters();

    private static final UUID APPLICATION_GUID = UUID.fromString(APPLICATION_GUID_STRING);

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedServiceBinding(PARAMETERS), buildRawServiceBinding(PARAMETERS));
    }

    @Test
    public void testDeriveWithoutParameters() {
        RawCloudEntityTest.testDerive(buildExpectedServiceBinding(), buildRawServiceBinding());
    }

    private static CloudServiceBinding buildExpectedServiceBinding() {
        return buildExpectedServiceBinding(null);
    }

    private static CloudServiceBinding buildExpectedServiceBinding(Map<String, Object> parameters) {
        return ImmutableCloudServiceBinding.builder()
            .metadata(RawCloudEntityTest.EXPECTED_METADATA)
            .applicationGuid(APPLICATION_GUID)
            .syslogDrainUrl(SYSLOG_DRAIN_URL)
            .bindingOptions(PARAMETERS)
            .bindingParameters(parameters)
            .credentials(PARAMETERS)
            .build();
    }

    private static RawCloudServiceBinding buildRawServiceBinding() {
        return buildRawServiceBinding(null);
    }

    private static RawCloudServiceBinding buildRawServiceBinding(Map<String, Object> parameters) {
        return ImmutableRawCloudServiceBinding.builder()
            .resource(buildTestResource())
            .parameters(parameters)
            .build();
    }

    private static Resource<ServiceBindingEntity> buildTestResource() {
        return ServiceBindingResource.builder()
            .metadata(RawCloudEntityTest.METADATA)
            .entity(buildTestEntity())
            .build();
    }

    private static ServiceBindingEntity buildTestEntity() {
        return ServiceBindingEntity.builder()
            .applicationId(APPLICATION_GUID_STRING)
            .syslogDrainUrl(SYSLOG_DRAIN_URL)
            .bindingOptions(PARAMETERS)
            .credentials(PARAMETERS)
            .build();
    }

    private static Map<String, Object> buildTestParameters() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("foo", "bar");
        parameters.put("baz", false);
        parameters.put("qux", 3.141);
        return parameters;
    }

}
