package org.cloudfoundry.client.lib.adapters;

import java.util.Map;

import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBinding;
import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.servicebindings.ServiceBindingEntity;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawCloudServiceBinding extends RawCloudEntity<CloudServiceBinding> {

    public abstract Resource<ServiceBindingEntity> getResource();

    @Nullable
    public abstract Map<String, Object> getParameters();

    @Override
    public CloudServiceBinding derive() {
        Resource<ServiceBindingEntity> resource = getResource();
        ServiceBindingEntity entity = resource.getEntity();
        return ImmutableCloudServiceBinding.builder()
            .metadata(parseResourceMetadata(resource))
            .applicationGuid(parseGuid(entity.getApplicationId()))
            .syslogDrainUrl(entity.getSyslogDrainUrl())
            .bindingOptions(entity.getBindingOptions())
            .bindingParameters(getParameters())
            .credentials(entity.getCredentials())
            .build();
    }

}
