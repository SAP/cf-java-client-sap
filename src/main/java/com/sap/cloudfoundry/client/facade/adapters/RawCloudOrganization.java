package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v3.organizations.Organization;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudOrganization;

@Value.Immutable
public abstract class RawCloudOrganization extends RawCloudEntity<CloudOrganization> {

    @Value.Parameter
    public abstract Organization getOrganization();

    @Override
    public CloudOrganization derive() {
        Organization org = getOrganization();
        return ImmutableCloudOrganization.builder()
                                         .metadata(parseResourceMetadata(org))
                                         .name(org.getName())
                                         .build();
    }

}
