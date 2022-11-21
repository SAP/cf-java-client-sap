package com.sap.cloudfoundry.client.facade.adapters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.cloudfoundry.client.v3.servicebindings.ServiceBinding;
import org.cloudfoundry.client.v3.servicebindings.ServiceBindingResource;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.ImmutableServiceBindingOperation;
import com.sap.cloudfoundry.client.facade.domain.ServiceBindingOperation;

@Value.Immutable
public abstract class RawCloudServiceBinding extends RawCloudEntity<CloudServiceBinding> {

    @Value.Parameter
    public abstract ServiceBindingResource getServiceBinding();

    @Override
    public CloudServiceBinding derive() {
        ServiceBinding serviceBinding = getServiceBinding();
        var appRelationship = serviceBinding.getRelationships()
                                            .getApplication();
        String lastOperationType = getServiceBinding().getLastOperation()
                                                      .getType();
        String lastOperationState = getServiceBinding().getLastOperation()
                                                       .getState();
        String lastOperationDescription = getServiceBinding().getLastOperation()
                                                             .getDescription();
        String lastOperationCreatedAt = getServiceBinding().getLastOperation()
                                                           .getCreatedAt();
        String lastOperationUpdatedAt = getServiceBinding().getLastOperation()
                                                           .getUpdatedAt();
        return ImmutableCloudServiceBinding.builder()
                                           .metadata(parseResourceMetadata(serviceBinding))
                                           .applicationGuid(parseNullableGuid(appRelationship == null ? null
                                               : appRelationship.getData()
                                                                .getId()))
                                           .serviceInstanceGuid(UUID.fromString(serviceBinding.getRelationships()
                                                                                              .getServiceInstance()
                                                                                              .getData()
                                                                                              .getId()))
                                           .serviceBindingOperation(ImmutableServiceBindingOperation.builder()
                                                                                                    .type(ServiceBindingOperation.Type.fromString(lastOperationType))
                                                                                                    .state(ServiceBindingOperation.State.fromString(lastOperationState))
                                                                                                    .description(lastOperationDescription)
                                                                                                    .createdAt(LocalDateTime.parse(lastOperationCreatedAt,
                                                                                                                                   DateTimeFormatter.ISO_DATE_TIME))
                                                                                                    .updatedAt(LocalDateTime.parse(lastOperationUpdatedAt,
                                                                                                                                   DateTimeFormatter.ISO_DATE_TIME))
                                                                                                    .build())
                                           .build();
    }

}
