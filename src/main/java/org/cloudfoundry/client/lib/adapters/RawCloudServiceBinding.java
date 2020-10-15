package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBinding;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.servicebindings.ServiceBindingEntity;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawCloudServiceBinding extends RawCloudEntity<CloudServiceBinding> {

    @Value.Parameter
    public abstract Resource<ServiceBindingEntity> getResource();

    @Override
    public CloudServiceBinding derive() {
        Resource<ServiceBindingEntity> resource = getResource();
        ServiceBindingEntity entity = resource.getEntity();
        return ImmutableCloudServiceBinding.builder()
                                           .metadata(parseResourceMetadata(resource))
                                           .applicationGuid(parseNullableGuid(entity.getApplicationId()))
                                           .syslogDrainUrl(entity.getSyslogDrainUrl())
                                           .bindingOptions(entity.getBindingOptions())
                                           .credentials(entity.getCredentials())
                                           .build();
    }

}
