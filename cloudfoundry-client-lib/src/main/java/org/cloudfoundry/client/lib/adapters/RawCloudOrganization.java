package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.ImmutableCloudOrganization;
import org.cloudfoundry.client.v3.organizations.Organization;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawCloudOrganization extends RawCloudEntity<CloudOrganization> {

    @Value.Parameter
    public abstract Organization getResource();

    @Override
    public CloudOrganization derive() {
        return ImmutableCloudOrganization.builder()
                                         .metadata(parseResourceMetadata(getResource()))
                                         .name(getResource().getName())
                                         .build();
    }

}
