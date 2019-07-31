package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.ImmutableCloudOrganization;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.organizations.OrganizationEntity;
import org.immutables.value.Value;

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
