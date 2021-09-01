package com.sap.cloudfoundry.client.facade.util;

import com.sap.cloudfoundry.client.facade.domain.CloudStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CloudStackCacheTest {

    private static final UUID EXISTING_UUID = UUID.fromString("00000000-1111-1111-1111-556642440000");
    private CloudStackCache cloudStackCache;

    @BeforeEach
    void setUp() {
        cloudStackCache = new CloudStackCache();
        cloudStackCache.getCloudStack(UUID.randomUUID(), () -> Mockito.mock(CloudStack.class));
        cloudStackCache.getCloudStack(UUID.randomUUID(), () -> Mockito.mock(CloudStack.class));
        cloudStackCache.getCloudStack(EXISTING_UUID, () -> Mockito.mock(CloudStack.class));
    }

    @Test
    void getCloudStackWithExistingValueTest() {
        int currentSize = cloudStackCache.getCurrentCacheSize();

        assertEquals(currentSize, cloudStackCache.getCurrentCacheSize());
        assertTrue(cloudStackCache.containsCloudStack(EXISTING_UUID));
    }

    @Test
    void containsCloudStackTest() {
        assertTrue(cloudStackCache.containsCloudStack(EXISTING_UUID));
    }

    @Test
    void getMissingCloudStackFromCacheTest() {
        int currentSize = cloudStackCache.getCurrentCacheSize();
        cloudStackCache.getCloudStack(UUID.fromString("123e4567-e89b-42d3-a456-556642440000"), () -> Mockito.mock(CloudStack.class));

        assertNotEquals(currentSize, cloudStackCache.getCurrentCacheSize());
        assertTrue(cloudStackCache.containsCloudStack(UUID.fromString("123e4567-e89b-42d3-a456-556642440000")));
    }

    @Test
    void getCurrentCacheSizeTest() {
        assertEquals(3, cloudStackCache.getCurrentCacheSize());
    }

    @Test
    void removeStackFromCacheTest() {
        int currentSize = cloudStackCache.getCurrentCacheSize();
        cloudStackCache.removeStackFromCache(EXISTING_UUID);
        assertNotEquals(currentSize, cloudStackCache.getCurrentCacheSize());
        assertFalse(cloudStackCache.containsCloudStack(EXISTING_UUID));
    }
}