package org.cloudfoundry.client.lib.rest.clients.events;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchList;

import java.util.List;
import java.util.function.IntFunction;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudEvent;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.events.EventEntity;
import org.cloudfoundry.client.v2.events.ListEventsRequest;
import org.cloudfoundry.util.PaginationUtils;

import reactor.core.publisher.Flux;

public class CloudControllerEventsClientImpl extends CloudControllerBaseClient implements CloudControllerEventsClient {

    public CloudControllerEventsClientImpl(CloudSpace target, CloudFoundryClient v3Client) {
        super(target, v3Client);
    }

    @Override
    public List<CloudEvent> getEvents() {
        return fetchList(this::getEventResources, ImmutableRawCloudEvent::of);
    }

    private Flux<? extends Resource<EventEntity>> getEventResources() {
        IntFunction<ListEventsRequest> pageRequestSupplier = page -> ListEventsRequest.builder()
                                                                                      .page(page)
                                                                                      .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.events()
                                                                        .list(pageRequestSupplier.apply(page)));
    }
}
