package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.ImmutableCloudDomain;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.domains.DomainEntity;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawCloudDomain extends RawCloudEntity<CloudDomain> {

    @Value.Parameter
    public abstract Resource<DomainEntity> getResource();

    @Override
    public CloudDomain derive() {
        Resource<DomainEntity> resource = getResource();
        DomainEntity entity = resource.getEntity();
        return ImmutableCloudDomain.builder()
                                   .metadata(parseResourceMetadata(resource))
                                   .name(entity.getName())
                                   .build();
    }

}
