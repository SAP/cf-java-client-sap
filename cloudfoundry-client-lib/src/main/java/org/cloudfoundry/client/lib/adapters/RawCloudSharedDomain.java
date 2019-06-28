package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.ImmutableCloudDomain;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.shareddomains.SharedDomainEntity;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawCloudSharedDomain extends RawCloudEntity<CloudDomain> {

    @Value.Parameter
    public abstract Resource<SharedDomainEntity> getResource();

    public CloudDomain derive() {
        Resource<SharedDomainEntity> resource = getResource();
        SharedDomainEntity entity = resource.getEntity();
        return ImmutableCloudDomain.builder()
            .metadata(parseResourceMetadata(resource))
            .name(entity.getName())
            .build();
    }

}
