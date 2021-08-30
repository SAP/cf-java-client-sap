package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v3.stacks.Stack;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudStack;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudStack;

@Value.Immutable
public abstract class RawCloudStack extends RawCloudEntity<CloudStack> {

    @Value.Parameter
    public abstract Stack getStack();

    @Override
    public CloudStack derive() {
        Stack stack = getStack();
        return ImmutableCloudStack.builder()
                                  .metadata(parseResourceMetadata(stack))
                                  .name(stack.getName())
                                  .description(stack.getDescription())
                                  .build();
    }

}
