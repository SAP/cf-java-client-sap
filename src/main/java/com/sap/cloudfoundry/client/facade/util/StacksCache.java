package com.sap.cloudfoundry.client.facade.util;

import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.stacks.StackEntity;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.UUID;

public class StacksCache {

    private final ConcurrentReferenceHashMap<UUID, Resource<StackEntity>> cachedStacks = new ConcurrentReferenceHashMap<>();

    public Resource<StackEntity> getStack(UUID stackId) {
        return cachedStacks.get(stackId);
    }

    public boolean containsStack(UUID stackId) {
        return cachedStacks.containsKey(stackId);
    }

    public void setStack(UUID stackId, Resource<StackEntity> stack) {
        cachedStacks.putIfAbsent(stackId, stack);
    }

}
