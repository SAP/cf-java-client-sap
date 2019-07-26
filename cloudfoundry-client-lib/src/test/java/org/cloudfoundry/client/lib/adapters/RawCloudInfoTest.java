package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.ImmutableCloudInfo;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.junit.jupiter.api.Test;

public class RawCloudInfoTest {

    private static final String DOPPLER_LOGGING_ENDPOINT = "wss://doppler.example.com:443";
    private static final String AUTHORIZATION_ENDPOINT = "https://login.example.com";
    private static final String BUILD = "build";
    private static final String DESCRIPTION = "Cloud Foundry";
    private static final String NAME = "name";
    private static final String USER = "john";
    private static final String SUPPORT = "support";
    private static final Integer VERSION = 0;

    private static final String VERSION_STRING = "0";

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedInfo(), buildRawInfo());
    }

    @Test
    public void testDeriveWithEmptyResponse() {
        RawCloudEntityTest.testDerive(buildEmptyExpectedInfo(), buildEmptyRawInfo());
    }

    private CloudInfo buildExpectedInfo() {
        return ImmutableCloudInfo.builder()
            .authorizationEndpoint(AUTHORIZATION_ENDPOINT)
            .loggingEndpoint(DOPPLER_LOGGING_ENDPOINT)
            .build(BUILD)
            .description(DESCRIPTION)
            .name(NAME)
            .user(USER)
            .support(SUPPORT)
            .version(VERSION_STRING)
            .build();
    }

    private CloudInfo buildEmptyExpectedInfo() {
        return ImmutableCloudInfo.builder()
            .build();
    }

    private RawCloudInfo buildRawInfo() {
        return ImmutableRawCloudInfo.of(buildTestResource());
    }

    private GetInfoResponse buildTestResource() {
        return GetInfoResponse.builder()
            .dopplerLoggingEndpoint(DOPPLER_LOGGING_ENDPOINT)
            .authorizationEndpoint(AUTHORIZATION_ENDPOINT)
            .buildNumber(BUILD)
            .description(DESCRIPTION)
            .name(NAME)
            .user(USER)
            .support(SUPPORT)
            .version(VERSION)
            .build();
    }

    private RawCloudInfo buildEmptyRawInfo() {
        return ImmutableRawCloudInfo.of(buildEmptyTestResource());
    }

    private GetInfoResponse buildEmptyTestResource() {
        return GetInfoResponse.builder()
            .build();
    }

}
