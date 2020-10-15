package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.domains.DomainEntity;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;

@Value.Immutable
public abstract class RawV2CloudDomain extends RawCloudEntity<CloudDomain> {

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
