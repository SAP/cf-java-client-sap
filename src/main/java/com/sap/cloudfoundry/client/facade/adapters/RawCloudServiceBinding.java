package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v3.servicebindings.ServiceBinding;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBinding;

import java.util.UUID;

@Value.Immutable
public abstract class RawCloudServiceBinding extends RawCloudEntity<CloudServiceBinding> {

    @Value.Parameter
    public abstract ServiceBinding getServiceBinding();

    @Override
    public CloudServiceBinding derive() {
        ServiceBinding serviceBinding = getServiceBinding();
        var appRelationship = serviceBinding.getRelationships()
                                            .getApplication();
        return ImmutableCloudServiceBinding.builder()
                                           .metadata(parseResourceMetadata(serviceBinding))
                                           .applicationGuid(parseNullableGuid(appRelationship == null ? null : appRelationship.getData()
                                                                                                                              .getId()))
                                           .serviceInstanceGuid(UUID.fromString(serviceBinding.getRelationships()
                                                                                              .getServiceInstance()
                                                                                              .getData()
                                                                                              .getId()))
                                           .build();
    }

}
