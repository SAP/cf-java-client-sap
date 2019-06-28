package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.ImmutableCloudDomain;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.privatedomains.PrivateDomainEntity;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawCloudPrivateDomain extends RawCloudEntity<CloudDomain> {

    @Value.Parameter
    public abstract Resource<PrivateDomainEntity> getResource();

    @Override
    public CloudDomain derive() {
        Resource<PrivateDomainEntity> resource = getResource();
        PrivateDomainEntity entity = resource.getEntity();
        return ImmutableCloudDomain.builder()
            .metadata(parseResourceMetadata(resource))
            .name(entity.getName())
            .build();
    }

}
