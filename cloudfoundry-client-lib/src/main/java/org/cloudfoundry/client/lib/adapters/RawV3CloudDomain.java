package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.ImmutableCloudDomain;
import org.cloudfoundry.client.v3.domains.Domain;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawV3CloudDomain extends RawCloudEntity<CloudDomain> {

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
