package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.servicekeys.ServiceKeyEntity;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawCloudServiceKey extends RawCloudEntity<CloudServiceKey> {

    public abstract Resource<ServiceKeyEntity> getResource();

    public abstract Derivable<CloudService> getService();

    @Override
    public CloudServiceKey derive() {
        Resource<ServiceKeyEntity> resource = getResource();
        ServiceKeyEntity entity = resource.getEntity();
        return ImmutableCloudServiceKey.builder()
                                       .metadata(parseResourceMetadata(resource))
                                       .name(entity.getName())
                                       .service(getService().derive())
                                       .credentials(entity.getCredentials())
                                       .build();
    }

}
