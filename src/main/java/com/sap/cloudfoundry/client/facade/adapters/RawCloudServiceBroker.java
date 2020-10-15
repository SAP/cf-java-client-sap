package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.servicebrokers.ServiceBrokerEntity;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceBroker;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBroker;

@Value.Immutable
public abstract class RawCloudServiceBroker extends RawCloudEntity<CloudServiceBroker> {

    @Value.Parameter
    public abstract Resource<ServiceBrokerEntity> getResource();

    @Override
    public CloudServiceBroker derive() {
        Resource<ServiceBrokerEntity> resource = getResource();
        ServiceBrokerEntity entity = resource.getEntity();
        return ImmutableCloudServiceBroker.builder()
                                          .metadata(parseResourceMetadata(resource))
                                          .name(entity.getName())
                                          // The password is not returned by the CF controller.
                                          .password(null)
                                          .username(entity.getAuthenticationUsername())
                                          .url(entity.getBrokerUrl())
                                          .spaceGuid(entity.getSpaceId())
                                          .build();
    }

}
