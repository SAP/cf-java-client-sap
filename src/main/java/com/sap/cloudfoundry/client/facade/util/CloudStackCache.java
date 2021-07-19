package com.sap.cloudfoundry.client.facade.util;

import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.domain.CloudStack;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.UUID;
import java.util.function.Supplier;

public class CloudStackCache {

    private final ConcurrentReferenceHashMap<UUID, CloudStack> cloudStacks = new ConcurrentReferenceHashMap<>();

    public CloudStack getCloudStack(UUID cloudStackId, Supplier<CloudStack> supplier) {
        try {
            return getCloudStackFromCache(cloudStackId, supplier);
        } catch (CloudOperationException e) {
            throw new CloudOperationException(e.getStatusCode(), e.getStatusText(), Messages.COULD_NOT_RETRIEVE_CLOUD_STACK);
        }
    }

    public void removeStackFromCache(UUID cloudStackId) {
        cloudStacks.remove(cloudStackId);
    }

    public boolean containsCloudStack(UUID cloudStackId) {
        return this.cloudStacks.containsKey(cloudStackId);
    }

    public int getCurrentCacheSize() {
        return cloudStacks.size();
    }

    private CloudStack getCloudStackFromCache(UUID cloudStackId, Supplier<CloudStack> supplier) {
        return cloudStacks.computeIfAbsent(cloudStackId, stack -> supplier.get());
    }
}
