package com.sap.cloudfoundry.client.facade.util;

import org.cloudfoundry.client.v3.stacks.Stack;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.UUID;

public class StacksCache {

    private final ConcurrentReferenceHashMap<UUID, Stack> cachedStacks = new ConcurrentReferenceHashMap<>();

    public Stack getStack(UUID stackId) {
        return cachedStacks.get(stackId);
    }

    public boolean containsStack(UUID stackId) {
        return cachedStacks.containsKey(stackId);
    }

    public void setStack(UUID stackId, Stack stack) {
        cachedStacks.putIfAbsent(stackId, stack);
    }

}
