package org.cloudfoundry.client.lib.adapters;

import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.serviceinstances.UnionServiceInstanceEntity;
import org.immutables.value.Value;

@Value.Immutable
public abstract class RawCloudServiceInstance extends RawCloudEntity<CloudServiceInstance> {

    public abstract Resource<UnionServiceInstanceEntity> getResource();

    public abstract Derivable<CloudService> getService();

    public abstract List<Derivable<CloudServiceBinding>> getServiceBindings();

    @Override
    public CloudServiceInstance derive() {
        Resource<UnionServiceInstanceEntity> resource = getResource();
        UnionServiceInstanceEntity entity = resource.getEntity();
        return ImmutableCloudServiceInstance.builder()
            .metadata(parseResourceMetadata(resource))
            .name(entity.getName())
            .type(entity.getType())
            .service(getService().derive())
            .credentials(entity.getCredentials())
            .bindings(derive(getServiceBindings()))
            .dashboardUrl(entity.getDashboardUrl())
            .build();
    }

}
