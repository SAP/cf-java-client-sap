package org.cloudfoundry.client.lib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.CloudPackage;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.Status;
import org.junit.jupiter.api.Test;

public class CloudEntityResourceMapperTest {

    private static final Map<String, Object> SERVICE_INSTANCE_RESOURCE = getResourceAsMap("service-instance.json");
    private static final String SERVICE_INSTANCE_GUID = "cc3b67fa-cda6-4df7-ba47-eb5f2a123992";
    private static final String SERVICE_INSTANCE_NAME = "my-service-instance";

    private static final Map<String, Object> TASK_RESOURCE = getResourceAsMap("task.json");
    private static final Map<String, Object> V3_PACKAGE_RESOURCE = getResourceAsMap("package.json");
    private static final Map<String, Object> V3_BUILD_RESOURCE = getResourceAsMap("build.json");
    private static final String TASK_GUID = "d5cc22ec-99a3-4e6a-af91-a44b4ab7b6fa";
    private static final String TASK_NAME = "migrate";

    private static final Map<String, Object> V2_RESOURCE = SERVICE_INSTANCE_RESOURCE;
    private static final Map<String, Object> V3_RESOURCE = TASK_RESOURCE;
    private static final String V2_RESOURCE_GUID = SERVICE_INSTANCE_GUID;
    private static final String V3_RESOURCE_GUID = TASK_GUID;
    private static final String V2_RESOURCE_NAME = SERVICE_INSTANCE_NAME;
    private static final String V3_RESOURCE_NAME = TASK_NAME;

    private CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

    @Test
    public void testGetGuidOfV2Resource() {
        UUID guid = resourceMapper.getGuidOfV2Resource(V2_RESOURCE);
        assertEquals(V2_RESOURCE_GUID, guid.toString());
    }

    @Test
    public void testGetGuidOfV3Resource() {
        UUID guid = resourceMapper.getGuidOfV3Resource(V3_RESOURCE);
        assertEquals(V3_RESOURCE_GUID, guid.toString());
    }

    @Test
    public void testGetNameOfV2Resource() {
        String name = resourceMapper.getV2ResourceName(V2_RESOURCE);
        assertEquals(V2_RESOURCE_NAME, name);
    }

    @Test
    public void testGetNameOfV3Resource() {
        String name = resourceMapper.getV3ResourceName(V3_RESOURCE);
        assertEquals(V3_RESOURCE_NAME, name);
    }

    @Test
    public void testGetV2MetaWithV2Resource() {
        CloudMetadata meta = CloudEntityResourceMapper.getV2Metadata(V2_RESOURCE);
        assertEquals(V2_RESOURCE_GUID, meta.getGuid()
            .toString());
        assertNotNull(meta.getCreatedAt());
        assertNotNull(meta.getUpdatedAt());
    }

    @Test
    public void testGetV2MetaWithV3Resource() {
        CloudMetadata meta = CloudEntityResourceMapper.getV2Metadata(V3_RESOURCE);
        assertNull(meta);
    }

    @Test
    public void testGetV3MetaWithV3Resource() {
        CloudMetadata meta = CloudEntityResourceMapper.getV3Metadata(V3_RESOURCE);
        assertEquals(V3_RESOURCE_GUID, meta.getGuid()
            .toString());
        assertNotNull(meta.getCreatedAt());
        assertNotNull(meta.getUpdatedAt());
    }

    @Test
    public void testGetV3MetaWithV2Resource() {
        CloudMetadata meta = CloudEntityResourceMapper.getV3Metadata(V2_RESOURCE);
        assertNull(meta);
    }

    @Test
    public void testGetAttributeOfV2ResourceWithV2Resource() {
        String name = CloudEntityResourceMapper.getV2ResourceAttribute(V2_RESOURCE, "name", String.class);
        assertEquals(V2_RESOURCE_NAME, name);
    }

    @Test
    public void testGetAttributeOfV2ResourceWithV3Resource() {
        String name = CloudEntityResourceMapper.getV2ResourceAttribute(V3_RESOURCE, "name", String.class);
        assertNull(name);
    }

    @Test
    public void testGetAttributeOfV2ResourceWithNull() {
        String name = CloudEntityResourceMapper.getV2ResourceAttribute(null, "name", String.class);
        assertNull(name);
    }

    @Test
    public void testGetAttributeOfV3ResourceWithV3Resource() {
        String name = CloudEntityResourceMapper.getV3ResourceAttribute(V3_RESOURCE, "name", String.class);
        assertEquals(V3_RESOURCE_NAME, name);
    }

    @Test
    public void testGetAttributeOfV3ResourceWithV2Resource() {
        String name = CloudEntityResourceMapper.getV3ResourceAttribute(V2_RESOURCE, "name", String.class);
        assertNull(name);
    }

    @Test
    public void testGetAttributeOfV3ResourceWithNull() {
        String name = CloudEntityResourceMapper.getV3ResourceAttribute(null, "name", String.class);
        assertNull(name);
    }

    @Test
    public void testMapTaskResource() {
        CloudTask task = resourceMapper.mapResource(V3_RESOURCE, CloudTask.class);
        assertEquals(TASK_NAME, task.getName());
        assertEquals(TASK_GUID, task.getMetadata()
            .getGuid()
            .toString());
        assertEquals("rake db:migrate", task.getCommand());
        assertEquals(Integer.valueOf(512), task.getLimits()
            .getMemory());
        assertEquals(Integer.valueOf(1024), task.getLimits()
            .getDisk());
    }

    @Test
    public void testV3MapCloudPackageResource() throws ParseException {
        CloudPackage cloudPackage = resourceMapper.mapResource(V3_PACKAGE_RESOURCE, CloudPackage.class);
        assertEquals(UUID.fromString("44f7c078-0934-470f-9883-4fcddc5b8f13"), cloudPackage.getMetadata()
            .getGuid());
        assertEquals(CloudPackage.Type.BITS, cloudPackage.getType());
        assertEquals("sha256", cloudPackage.getData()
            .getChecksum()
            .getAlgorithm());
        assertEquals(null, cloudPackage.getData()
            .getChecksum()
            .getValue());
        assertEquals(null, cloudPackage.getData()
            .getError());
        assertEquals(Status.PROCESSING_UPLOAD, cloudPackage.getStatus());
    }

    @Test
    public void testV3MapCloudBuildResource() throws ParseException {
        CloudBuild cloudBuild = resourceMapper.mapResource(V3_BUILD_RESOURCE, CloudBuild.class);
        assertEquals(UUID.fromString("585bc3c1-3743-497d-88b0-403ad6b56d16"), cloudBuild.getMetadata()
            .getGuid());
        assertEquals("3cb4e243-bed4-49d5-8739-f8b45abdec1c", cloudBuild.getCreatedBy()
            .getGuid()
            .toString());
        assertEquals("bill", cloudBuild.getCreatedBy()
            .getName());
        assertEquals(CloudBuild.State.STAGING, cloudBuild.getState());
        assertEquals(null, cloudBuild.getError());
        assertEquals(UUID.fromString("8e4da443-f255-499c-8b47-b3729b5b7432"), cloudBuild.getPackageInfo()
            .getGuid());
        assertEquals(null, cloudBuild.getDropletInfo());
    }

    private static Map<String, Object> getResourceAsMap(String resourceName) {
        String resource = getResourceAsString(resourceName);
        return JsonUtil.convertJsonToMap(resource);
    }

    private static String getResourceAsString(String resourceName) {
        InputStream resource = CloudEntityResourceMapperTest.class.getResourceAsStream(resourceName);
        try {
            return IOUtils.toString(resource, "UTF-8");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
