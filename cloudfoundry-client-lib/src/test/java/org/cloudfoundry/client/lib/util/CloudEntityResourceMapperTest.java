package org.cloudfoundry.client.lib.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.junit.Test;

public class CloudEntityResourceMapperTest {

    private static final Map<String, Object> SERVICE_INSTANCE_RESOURCE = getResourceAsMap("service-instance.json");
    private static final String SERVICE_INSTANCE_GUID = "cc3b67fa-cda6-4df7-ba47-eb5f2a123992";
    private static final String SERVICE_INSTANCE_NAME = "my-service-instance";

    private static final Map<String, Object> TASK_RESOURCE = getResourceAsMap("task.json");
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
        String name = resourceMapper.getNameOfV2Resource(V2_RESOURCE);
        assertEquals(V2_RESOURCE_NAME, name);
    }

    @Test
    public void testGetNameOfV3Resource() {
        String name = resourceMapper.getNameOfV3Resource(V3_RESOURCE);
        assertEquals(V3_RESOURCE_NAME, name);
    }

    @Test
    public void testGetV2MetaWithV2Resource() {
        Meta meta = CloudEntityResourceMapper.getV2Meta(V2_RESOURCE);
        assertEquals(V2_RESOURCE_GUID, meta.getGuid()
            .toString());
        assertNotNull(meta.getCreated());
        assertNotNull(meta.getUpdated());
    }

    @Test
    public void testGetV2MetaWithV3Resource() {
        Meta meta = CloudEntityResourceMapper.getV2Meta(V3_RESOURCE);
        assertNull(meta);
    }

    @Test
    public void testGetV3MetaWithV3Resource() {
        Meta meta = CloudEntityResourceMapper.getV3Meta(V3_RESOURCE);
        assertEquals(V3_RESOURCE_GUID, meta.getGuid()
            .toString());
        assertNotNull(meta.getCreated());
        assertNotNull(meta.getUpdated());
    }

    @Test
    public void testGetV3MetaWithV2Resource() {
        Meta meta = CloudEntityResourceMapper.getV3Meta(V2_RESOURCE);
        assertNull(meta);
    }

    @Test
    public void testGetAttributeOfV2ResourceWithV2Resource() {
        String name = CloudEntityResourceMapper.getAttributeOfV2Resource(V2_RESOURCE, "name", String.class);
        assertEquals(V2_RESOURCE_NAME, name);
    }

    @Test
    public void testGetAttributeOfV2ResourceWithV3Resource() {
        String name = CloudEntityResourceMapper.getAttributeOfV2Resource(V3_RESOURCE, "name", String.class);
        assertNull(name);
    }

    @Test
    public void testGetAttributeOfV2ResourceWithNull() {
        String name = CloudEntityResourceMapper.getAttributeOfV2Resource(null, "name", String.class);
        assertNull(name);
    }

    @Test
    public void testGetAttributeOfV3ResourceWithV3Resource() {
        String name = CloudEntityResourceMapper.getAttributeOfV3Resource(V3_RESOURCE, "name", String.class);
        assertEquals(V3_RESOURCE_NAME, name);
    }

    @Test
    public void testGetAttributeOfV3ResourceWithV2Resource() {
        String name = CloudEntityResourceMapper.getAttributeOfV3Resource(V2_RESOURCE, "name", String.class);
        assertNull(name);
    }

    @Test
    public void testGetAttributeOfV3ResourceWithNull() {
        String name = CloudEntityResourceMapper.getAttributeOfV3Resource(null, "name", String.class);
        assertNull(name);
    }

    @Test
    public void testMapTaskResource() {
        CloudTask task = resourceMapper.mapResource(V3_RESOURCE, CloudTask.class);
        assertEquals(TASK_NAME, task.getName());
        assertEquals(TASK_GUID, task.getMeta()
            .getGuid()
            .toString());
        assertEquals("rake db:migrate", task.getCommand());
        assertEquals(Integer.valueOf(512), task.getMemory());
        assertEquals(Integer.valueOf(1024), task.getDiskQuota());
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
