package org.cloudfoundry.client.lib.rest.clients.servicebrokers;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetch;
import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchFlux;
import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchList;
import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchListWithAuxiliaryContent;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudServiceBroker;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudServiceOffering;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudServicePlan;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.servicebrokers.CreateServiceBrokerRequest;
import org.cloudfoundry.client.v2.servicebrokers.DeleteServiceBrokerRequest;
import org.cloudfoundry.client.v2.servicebrokers.ListServiceBrokersRequest;
import org.cloudfoundry.client.v2.servicebrokers.ServiceBrokerEntity;
import org.cloudfoundry.client.v2.servicebrokers.UpdateServiceBrokerRequest;
import org.cloudfoundry.client.v2.serviceplans.ListServicePlansRequest;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanEntity;
import org.cloudfoundry.client.v2.serviceplans.UpdateServicePlanRequest;
import org.cloudfoundry.client.v2.services.ListServicesRequest;
import org.cloudfoundry.client.v2.services.ServiceEntity;
import org.cloudfoundry.util.PaginationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CloudControllerServiceBrokersClientImpl extends CloudControllerBaseClient implements CloudControllerServiceBrokersClient {

    public CloudControllerServiceBrokersClientImpl(CloudSpace target, CloudFoundryClient v3Client) {
        super(target, v3Client);
    }

    @Override
    public void create(CloudServiceBroker serviceBroker) {
        Assert.notNull(serviceBroker, "Service broker must not be null.");

        v3Client.serviceBrokers()
                .create(CreateServiceBrokerRequest.builder()
                                                  .name(serviceBroker.getName())
                                                  .brokerUrl(serviceBroker.getUrl())
                                                  .authenticationUsername(serviceBroker.getUsername())
                                                  .authenticationPassword(serviceBroker.getPassword())
                                                  .spaceId(serviceBroker.getSpaceGuid())
                                                  .build())
                .block();
    }

    @Override
    public void delete(String name) {
        CloudServiceBroker broker = get(name, true);
        UUID guid = broker.getMetadata()
                          .getGuid();
        v3Client.serviceBrokers()
                .delete(DeleteServiceBrokerRequest.builder()
                                                  .serviceBrokerId(guid.toString())
                                                  .build())
                .block();
    }

    @Override
    public CloudServiceBroker get(String name, boolean required) {
        CloudServiceBroker serviceBroker = findServiceBrokerByName(name);
        if (serviceBroker == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service broker " + name + " not found.");
        }
        return serviceBroker;
    }

    @Override
    public List<CloudServiceBroker> getAll() {
        return fetchList(this::getServiceBrokerResources, ImmutableRawCloudServiceBroker::of);
    }

    @Override
    public void update(CloudServiceBroker serviceBroker) {
        Assert.notNull(serviceBroker, "Service broker must not be null.");

        CloudServiceBroker existingBroker = get(serviceBroker.getName(), true);
        UUID brokerGuid = existingBroker.getMetadata()
                                        .getGuid();

        v3Client.serviceBrokers()
                .update(UpdateServiceBrokerRequest.builder()
                                                  .serviceBrokerId(brokerGuid.toString())
                                                  .name(serviceBroker.getName())
                                                  .authenticationUsername(serviceBroker.getUsername())
                                                  .authenticationPassword(serviceBroker.getPassword())
                                                  .brokerUrl(serviceBroker.getUrl())
                                                  .build())
                .block();
    }

    @Override
    public void updateServicePlanVisibility(String name, boolean visibility) {
        CloudServiceBroker broker = get(name, true);
        List<CloudServicePlan> servicePlans = findServicePlansByBrokerGuid(getGuid(broker));
        for (CloudServicePlan servicePlan : servicePlans) {
            updateServicePlanVisibility(servicePlan, visibility);
        }
    }

    private CloudServiceBroker findServiceBrokerByName(String name) {
        return fetch(() -> getServiceBrokerResourceByName(name), ImmutableRawCloudServiceBroker::of);
    }

    private Mono<? extends Resource<ServiceBrokerEntity>> getServiceBrokerResourceByName(String name) {
        IntFunction<ListServiceBrokersRequest> pageRequestSupplier = page -> ListServiceBrokersRequest.builder()
                                                                                                      .page(page)
                                                                                                      .name(name)
                                                                                                      .build();
        return getServiceBrokerResources(pageRequestSupplier).singleOrEmpty();
    }

    private Flux<? extends Resource<ServiceBrokerEntity>> getServiceBrokerResources() {
        IntFunction<ListServiceBrokersRequest> pageRequestSupplier = page -> ListServiceBrokersRequest.builder()
                                                                                                      .page(page)
                                                                                                      .build();
        return getServiceBrokerResources(pageRequestSupplier);
    }

    private Flux<? extends Resource<ServiceBrokerEntity>>
            getServiceBrokerResources(IntFunction<ListServiceBrokersRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV2Resources(page -> v3Client.serviceBrokers()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private List<CloudServicePlan> findServicePlansByBrokerGuid(UUID brokerGuid) {
        List<CloudServiceOffering> offerings = findServiceOfferingsByBrokerGuid(brokerGuid);
        return getPlans(offerings);
    }

    private List<CloudServiceOffering> findServiceOfferingsByBrokerGuid(UUID brokerGuid) {
        return fetchListWithAuxiliaryContent(() -> getServiceResourcesByBrokerGuid(brokerGuid),
                                             this::zipWithAuxiliaryServiceOfferingContent);
    }

    private Flux<? extends Resource<ServiceEntity>> getServiceResourcesByBrokerGuid(UUID brokerGuid) {
        IntFunction<ListServicesRequest> pageRequestSupplier = page -> ListServicesRequest.builder()
                                                                                          .serviceBrokerId(brokerGuid.toString())
                                                                                          .page(page)
                                                                                          .build();
        return getServiceResources(pageRequestSupplier);
    }

    private Flux<? extends Resource<ServiceEntity>> getServiceResources(IntFunction<ListServicesRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV2Resources(page -> v3Client.services()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Mono<Derivable<CloudServiceOffering>> zipWithAuxiliaryServiceOfferingContent(Resource<ServiceEntity> resource) {
        UUID serviceGuid = getGuid(resource);
        return getServicePlansFlux(serviceGuid).collectList()
                                               .map(servicePlans -> ImmutableRawCloudServiceOffering.builder()
                                                                                                    .resource(resource)
                                                                                                    .servicePlans(servicePlans)
                                                                                                    .build());
    }
    
    private Flux<CloudServicePlan> getServicePlansFlux(UUID serviceGuid) {
        return fetchFlux(() -> getServicePlanResourcesByServiceGuid(serviceGuid), ImmutableRawCloudServicePlan::of);
    }

    private Flux<? extends Resource<ServicePlanEntity>> getServicePlanResourcesByServiceGuid(UUID serviceGuid) {
        IntFunction<ListServicePlansRequest> pageRequestSupplier = page -> ListServicePlansRequest.builder()
                                                                                                  .serviceId(serviceGuid.toString())
                                                                                                  .page(page)
                                                                                                  .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.servicePlans()
                                                                        .list(pageRequestSupplier.apply(page)));
    }
    
    private List<CloudServicePlan> getPlans(List<CloudServiceOffering> offerings) {
        return offerings.stream()
                        .map(CloudServiceOffering::getServicePlans)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
    }
    
    private void updateServicePlanVisibility(CloudServicePlan servicePlan, boolean visibility) {
        updateServicePlanVisibility(getGuid(servicePlan), visibility);
    }

    private void updateServicePlanVisibility(UUID servicePlanGuid, boolean visibility) {
        UpdateServicePlanRequest request = UpdateServicePlanRequest.builder()
                                                                   .servicePlanId(servicePlanGuid.toString())
                                                                   .publiclyVisible(visibility)
                                                                   .build();
        v3Client.servicePlans()
                .update(request)
                .block();
    }

}
