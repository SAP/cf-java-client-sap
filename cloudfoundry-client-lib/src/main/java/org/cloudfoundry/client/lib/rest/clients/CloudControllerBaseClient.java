package org.cloudfoundry.client.lib.rest.clients;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.v2.Resource;
import org.springframework.util.Assert;

import reactor.core.publisher.Flux;

public abstract class CloudControllerBaseClient {
    protected CloudSpace target;
    protected CloudFoundryClient v3Client;

    protected CloudControllerBaseClient(CloudSpace target, CloudFoundryClient v3Client) {
        this.target = target;
        this.v3Client = v3Client;
    }

    protected void assertSpaceProvided(String operation) {
        Assert.notNull(target, "Unable to " + operation + " without specifying organization and space to use.");
    }

    protected UUID getTargetSpaceGuid() {
        return getGuid(target);
    }

    protected UUID getGuid(CloudEntity entity) {
        return Optional.ofNullable(entity)
                       .map(CloudEntity::getMetadata)
                       .map(CloudMetadata::getGuid)
                       .orElse(null);
    }

    protected UUID getGuid(org.cloudfoundry.client.v2.Resource<?> resource) {
        return Optional.ofNullable(resource)
                       .map(org.cloudfoundry.client.v2.Resource::getMetadata)
                       .map(org.cloudfoundry.client.v2.Metadata::getId)
                       .map(UUID::fromString)
                       .orElse(null);
    }
    
    protected List<UUID> getGuids(Flux<? extends Resource<?>> resources) {
        return resources.map(this::getGuid)
                        .collectList()
                        .block();
    }
    
    protected UUID getGuid(org.cloudfoundry.client.v3.Resource resource) {
        return Optional.ofNullable(resource)
                       .map(org.cloudfoundry.client.v3.Resource::getId)
                       .map(UUID::fromString)
                       .orElse(null);
    }
}
