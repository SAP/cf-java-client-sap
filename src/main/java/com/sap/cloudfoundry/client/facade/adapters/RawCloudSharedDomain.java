package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.shareddomains.SharedDomainEntity;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;

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
