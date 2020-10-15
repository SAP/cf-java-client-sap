package com.sap.cloudfoundry.client.facade.adapters;

import java.util.List;

import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.services.ServiceEntity;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudServiceOffering;
import com.sap.cloudfoundry.client.facade.domain.CloudServicePlan;
import com.sap.cloudfoundry.client.facade.domain.Derivable;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceOffering;

@Value.Immutable
public abstract class RawCloudServiceOffering extends RawCloudEntity<CloudServiceOffering> {

    public abstract Resource<ServiceEntity> getResource();

    public abstract List<Derivable<CloudServicePlan>> getServicePlans();

    @Override
    public CloudServiceOffering derive() {
        Resource<ServiceEntity> resource = getResource();
        ServiceEntity entity = resource.getEntity();
        return ImmutableCloudServiceOffering.builder()
                                            .metadata(parseResourceMetadata(resource))
                                            .name(entity.getLabel())
                                            .isActive(entity.getActive())
                                            .isBindable(entity.getBindable())
                                            .description(entity.getDescription())
                                            .extra(entity.getExtra())
                                            .docUrl(entity.getDocumentationUrl())
                                            .infoUrl(entity.getInfoUrl())
                                            .version(entity.getVersion())
                                            .provider(entity.getProvider())
                                            .brokerName(entity.getServiceBrokerName())
                                            .uniqueId(entity.getUniqueId())
                                            .url(entity.getUrl())
                                            .servicePlans(derive(getServicePlans()))
                                            .build();
    }

}
