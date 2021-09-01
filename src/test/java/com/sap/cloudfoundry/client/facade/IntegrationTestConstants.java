package com.sap.cloudfoundry.client.facade;

public class IntegrationTestConstants {
    private IntegrationTestConstants() {
    }

    public static final String HEALTH_CHECK_ENDPOINT = "/public/ping";
    public static final String JAVA_BUILDPACK = "java_buildpack";
    public static final int HEALTH_CHECK_TIMEMOUT = 100;
    public static final int DISK_IN_MB = 128;
    public static final int MEMORY_IN_MB = 128;
    public static final String DEFAULT_DOMAIN = "deploy-service.custom.domain.for.integration.tests";
    public static final String APPLICATION_HOST = "test-application-hostname-ztana-test";
    public static final String STATICFILE_APPLICATION_CONTENT = "staticfile.zip";
}
