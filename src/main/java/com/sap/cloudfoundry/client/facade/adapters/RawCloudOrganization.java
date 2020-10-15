package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.organizations.OrganizationEntity;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudOrganization;

@Value.Immutable
public abstract class RawCloudOrganization extends RawCloudEntity<CloudOrganization> {

    @Value.Parameter
    public abstract Resource<OrganizationEntity> getResource();

    @Override
    public CloudOrganization derive() {
        Resource<OrganizationEntity> resource = getResource();
        OrganizationEntity entity = resource.getEntity();
        return ImmutableCloudOrganization.builder()
                                         .metadata(parseResourceMetadata(resource))
                                         .name(entity.getName())
                                         .build();
    }

}
