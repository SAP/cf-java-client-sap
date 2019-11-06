package org.cloudfoundry.client.lib.rest.clients.apps.impl;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchList;

import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudEvent;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsEventsClient;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.events.EventEntity;
import org.cloudfoundry.client.v2.events.ListEventsRequest;
import org.cloudfoundry.util.PaginationUtils;

import reactor.core.publisher.Flux;

public class CloudControllerApplicationsEventsClientImpl extends CloudControllerBaseClient
    implements CloudControllerApplicationsEventsClient {

    private CloudControllerApplicationsClient applicationsClient;

    protected CloudControllerApplicationsEventsClientImpl(CloudSpace target, CloudFoundryClient v3Client,
                                                          CloudControllerApplicationsClient applicationsClient) {
        super(target, v3Client);
        this.applicationsClient = applicationsClient;
    }

    @Override
    public List<CloudEvent> get(String applicationName) {
        UUID applicationGuid = getGuid(applicationsClient.findApplicationByName(applicationName, true));
        return get(applicationGuid);
    }

    @Override
    public List<CloudEvent> get(UUID applicationGuid) {
        return findEventsByActee(applicationGuid.toString());
    }

    private List<CloudEvent> findEventsByActee(String actee) {
        return fetchList(() -> getEventResourcesByActee(actee), ImmutableRawCloudEvent::of);
    }

    private Flux<? extends Resource<EventEntity>> getEventResourcesByActee(String actee) {
        IntFunction<ListEventsRequest> pageRequestSupplier = page -> ListEventsRequest.builder()
                                                                                      .actee(actee)
                                                                                      .page(page)
                                                                                      .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.events()
                                                                        .list(pageRequestSupplier.apply(page)));
    }
}
