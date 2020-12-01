package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v3.domains.Domain;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;

@Value.Immutable
public abstract class RawCloudDomain extends RawCloudEntity<CloudDomain> {

    @Value.Parameter
    public abstract Domain getResource();

    @Override
    public CloudDomain derive() {
        Domain resource = getResource();
        return ImmutableCloudDomain.builder()
                                   .metadata(parseResourceMetadata(resource))
                                   .name(resource.getName())
                                   .build();
    }

}
