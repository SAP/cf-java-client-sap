package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.servicekeys.ServiceKeyEntity;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.Derivable;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;

@Value.Immutable
public abstract class RawCloudServiceKey extends RawCloudEntity<CloudServiceKey> {

    public abstract Resource<ServiceKeyEntity> getResource();

    public abstract Derivable<CloudServiceInstance> getServiceInstance();

    @Override
    public CloudServiceKey derive() {
        Resource<ServiceKeyEntity> resource = getResource();
        ServiceKeyEntity entity = resource.getEntity();
        return ImmutableCloudServiceKey.builder()
                                       .metadata(parseResourceMetadata(resource))
                                       .name(entity.getName())
                                       .serviceInstance(getServiceInstance().derive())
                                       .credentials(entity.getCredentials())
                                       .build();
    }

}
