package org.cloudfoundry.client.lib.rest.clients.stacks;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudStack;

public interface CloudControllerStacksClient {
    
    CloudStack getStack(String name, boolean required);

    List<CloudStack> getStacks();
}
