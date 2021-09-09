package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v3.serviceplans.ServicePlan;
import org.cloudfoundry.client.v3.serviceplans.Visibility;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudServicePlan;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServicePlan;

@Value.Immutable
public abstract class RawCloudServicePlan extends RawCloudEntity<CloudServicePlan> {

    @Value.Parameter
    public abstract ServicePlan getResource();

    @Override
    public CloudServicePlan derive() {
        ServicePlan resource = getResource();
        return ImmutableCloudServicePlan.builder()
                                        .metadata(parseResourceMetadata(resource))
                                        .name(resource.getName())
                                        .description(resource.getDescription())
                                        .extra(resource.getBrokerCatalog()
                                                       .getMetadata())
                                        .uniqueId(resource.getBrokerCatalog()
                                                          .getBrokerCatalogId())
                                        .serviceOfferingId(resource.getRelationships()
                                                                   .getServiceOffering()
                                                                   .getData()
                                                                   .getId())
                                        .isPublic(resource.getVisibilityType()
                                                          .equals(Visibility.PUBLIC))
                                        .isFree(resource.getFree())
                                        .build();
    }

}
