package org.cloudfoundry.client.lib.rest.clients.services;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchFlux;
import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchFluxWithAuxiliaryContent;
import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchListWithAuxiliaryContent;
import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchWithAuxiliaryContent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntFunction;

import org.cloudfoundry.AbstractCloudFoundryException;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudService;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudServiceBinding;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudServiceInstance;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudServiceOffering;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudServicePlan;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsClient;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.applications.ListApplicationServiceBindingsRequest;
import org.cloudfoundry.client.v2.servicebindings.CreateServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.DeleteServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.GetServiceBindingParametersRequest;
import org.cloudfoundry.client.v2.servicebindings.GetServiceBindingParametersResponse;
import org.cloudfoundry.client.v2.servicebindings.ListServiceBindingsRequest;
import org.cloudfoundry.client.v2.servicebindings.ServiceBindingEntity;
import org.cloudfoundry.client.v2.serviceinstances.CreateServiceInstanceRequest;
import org.cloudfoundry.client.v2.serviceinstances.DeleteServiceInstanceRequest;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceParametersRequest;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceParametersResponse;
import org.cloudfoundry.client.v2.serviceinstances.UnionServiceInstanceEntity;
import org.cloudfoundry.client.v2.serviceplans.GetServicePlanRequest;
import org.cloudfoundry.client.v2.serviceplans.ListServicePlansRequest;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanEntity;
import org.cloudfoundry.client.v2.services.GetServiceRequest;
import org.cloudfoundry.client.v2.services.ListServicesRequest;
import org.cloudfoundry.client.v2.services.ServiceEntity;
import org.cloudfoundry.client.v2.spaces.ListSpaceServiceInstancesRequest;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.CreateUserProvidedServiceInstanceRequest;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.ListUserProvidedServiceInstanceServiceBindingsRequest;
import org.cloudfoundry.util.PaginationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CloudControllerServicesClientImpl extends CloudControllerBaseClient implements CloudControllerServicesClient {

    private static final String USER_PROVIDED_SERVICE_INSTANCE_TYPE = "user_provided_service_instance";

    private final CloudControllerApplicationsClient applicationsClient;

    public CloudControllerServicesClientImpl(CloudSpace target, CloudFoundryClient v3Client,
                                             CloudControllerApplicationsClient applicationsClient) {
        super(target, v3Client);
        this.applicationsClient = applicationsClient;
    }

    @Override
    public void create(CloudService service) {
        assertSpaceProvided("create service");
        Assert.notNull(service, "Service must not be null.");

        CloudServicePlan servicePlan = findPlanForService(service);
        UUID servicePlanGuid = servicePlan.getMetadata()
                                          .getGuid();
        v3Client.serviceInstances()
                .create(CreateServiceInstanceRequest.builder()
                                                    .spaceId(getTargetSpaceGuid().toString())
                                                    .name(service.getName())
                                                    .servicePlanId(servicePlanGuid.toString())
                                                    .build())
                .block();
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials) {
        createUserProvidedService(service, credentials, "");
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials, String syslogDrainUrl) {
        assertSpaceProvided("create service");
        Assert.notNull(service, "Service must not be null.");

        v3Client.userProvidedServiceInstances()
                .create(CreateUserProvidedServiceInstanceRequest.builder()
                                                                .spaceId(getTargetSpaceGuid().toString())
                                                                .name(service.getName())
                                                                .credentials(credentials)
                                                                .syslogDrainUrl(syslogDrainUrl)
                                                                .build())
                .block();
    }

    @Override
    public void deleteAll() {
        List<CloudService> services = getServices();
        for (CloudService service : services) {
            doDeleteService(service);
        }
    }

    @Override
    public void delete(String serviceName) {
        CloudService service = getService(serviceName);
        doDeleteService(service);
    }

    @Override
    public CloudService getService(String serviceName) {
        return getService(serviceName, true);
    }

    @Override
    public CloudService getService(String serviceName, boolean required) {
        CloudService service = findServiceByName(serviceName);
        if (service == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service " + serviceName + " not found.");
        }
        return service;
    }

    @Override
    public List<CloudService> getServices() {
        return fetchListWithAuxiliaryContent(this::getServiceInstanceResources, this::zipWithAuxiliaryServiceContent);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceName) {
        return getServiceInstance(serviceName, true);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceName, boolean required) {
        CloudServiceInstance serviceInstance = findServiceInstanceByName(serviceName);
        if (serviceInstance == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service instance " + serviceName + " not found.");
        }
        return serviceInstance;
    }

    @Override
    public Map<String, Object> getServiceParameters(UUID guid) {
        return v3Client.serviceInstances()
                       .getParameters(GetServiceInstanceParametersRequest.builder()
                                                                         .serviceInstanceId(guid.toString())
                                                                         .build())
                       .map(GetServiceInstanceParametersResponse::getParameters)
                       .block();
    }

    @Override
    public void bindService(String applicationName, String serviceName, Map<String, Object> parameters) {
        UUID applicationGuid = getGuid(applicationsClient.findApplicationByName(applicationName, true));
        UUID serviceGuid = getService(serviceName).getMetadata()
                                                  .getGuid();
        v3Client.serviceBindingsV2()
                .create(CreateServiceBindingRequest.builder()
                                                   .applicationId(applicationGuid.toString())
                                                   .serviceInstanceId(serviceGuid.toString())
                                                   .parameters(parameters)
                                                   .build())
                .block();
    }

    @Override
    public void unbindService(String applicationName, String serviceName) {
        UUID applicationGuid = getGuid(applicationsClient.findApplicationByName(applicationName, true));
        UUID serviceGuid = getService(serviceName).getMetadata()
                                                  .getGuid();
        doUnbindService(applicationGuid, serviceGuid);
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        return fetchListWithAuxiliaryContent(this::getServiceResources, this::zipWithAuxiliaryServiceOfferingContent);
    }

    private CloudServicePlan findPlanForService(CloudService service) {
        List<CloudServiceOffering> offerings = findServiceOfferingsByLabel(service.getLabel());
        for (CloudServiceOffering offering : offerings) {
            if (service.getVersion() == null || service.getVersion()
                                                       .equals(offering.getVersion())) {
                for (CloudServicePlan plan : offering.getServicePlans()) {
                    if (service.getPlan() != null && service.getPlan()
                                                            .equals(plan.getName())) {
                        return plan;
                    }
                }
            }
        }
        throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service plan " + service.getPlan() + " not found.");
    }

    private List<CloudServiceOffering> findServiceOfferingsByLabel(String label) {
        Assert.notNull(label, "Service label must not be null");
        return fetchListWithAuxiliaryContent(() -> getServiceResourcesByLabel(label), this::zipWithAuxiliaryServiceOfferingContent);
    }

    private Flux<? extends Resource<ServiceEntity>> getServiceResourcesByLabel(String label) {
        IntFunction<ListServicesRequest> pageRequestSupplier = page -> ListServicesRequest.builder()
                                                                                          .label(label)
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

    private void doDeleteService(CloudService service) {
        List<UUID> serviceBindingGuids = getServiceBindingGuids(service);
        for (UUID serviceBindingGuid : serviceBindingGuids) {
            doUnbindService(serviceBindingGuid);
        }
        UUID serviceGuid = service.getMetadata()
                                  .getGuid();
        v3Client.serviceInstances()
                .delete(DeleteServiceInstanceRequest.builder()
                                                    .acceptsIncomplete(true)
                                                    .serviceInstanceId(serviceGuid.toString())
                                                    .build())
                .block();
    }

    private List<UUID> getServiceBindingGuids(CloudService service) {
        Flux<? extends Resource<ServiceBindingEntity>> bindings = getServiceBindingResources(service);
        return getGuids(bindings);
    }

    private Flux<? extends Resource<ServiceBindingEntity>> getServiceBindingResources(CloudService service) {
        UUID serviceGuid = getGuid(service);
        if (service.isUserProvided()) {
            return getUserProvidedServiceBindingResourcesByServiceInstanceGuid(serviceGuid);
        }
        return getServiceBindingResourcesByServiceInstanceGuid(serviceGuid);
    }

    private Flux<? extends Resource<ServiceBindingEntity>>
            getUserProvidedServiceBindingResourcesByServiceInstanceGuid(UUID serviceInstanceGuid) {
        IntFunction<ListUserProvidedServiceInstanceServiceBindingsRequest> pageRequestSupplier = page -> ListUserProvidedServiceInstanceServiceBindingsRequest.builder()
                                                                                                                                                              .userProvidedServiceInstanceId(serviceInstanceGuid.toString())
                                                                                                                                                              .page(page)
                                                                                                                                                              .build();

        return PaginationUtils.requestClientV2Resources(page -> v3Client.userProvidedServiceInstances()
                                                                        .listServiceBindings(pageRequestSupplier.apply(page)));

    }

    private Flux<? extends Resource<ServiceBindingEntity>> getServiceBindingResourcesByServiceInstanceGuid(UUID serviceInstanceGuid) {
        IntFunction<ListServiceBindingsRequest> pageRequestSupplier = page -> ListServiceBindingsRequest.builder()
                                                                                                        .serviceInstanceId(serviceInstanceGuid.toString())
                                                                                                        .page(page)
                                                                                                        .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.serviceBindingsV2()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private void doUnbindService(UUID serviceBindingGuid) {
        v3Client.serviceBindingsV2()
                .delete(DeleteServiceBindingRequest.builder()
                                                   .serviceBindingId(serviceBindingGuid.toString())
                                                   .build())
                .block();
    }

    private CloudService findServiceByName(String name) {
        return fetchWithAuxiliaryContent(() -> getServiceInstanceResourceByName(name), this::zipWithAuxiliaryServiceContent);
    }

    private Mono<? extends Resource<UnionServiceInstanceEntity>> getServiceInstanceResourceByName(String name) {
        IntFunction<ListSpaceServiceInstancesRequest> pageRequestSupplier = page -> ListSpaceServiceInstancesRequest.builder()
                                                                                                                    .returnUserProvidedServiceInstances(true)
                                                                                                                    .spaceId(getTargetSpaceGuid().toString())
                                                                                                                    .name(name)
                                                                                                                    .page(page)
                                                                                                                    .build();

        return getServiceInstanceResources(pageRequestSupplier).singleOrEmpty();
    }

    private Flux<? extends Resource<UnionServiceInstanceEntity>> getServiceInstanceResources() {
        IntFunction<ListSpaceServiceInstancesRequest> pageRequestSupplier = page -> ListSpaceServiceInstancesRequest.builder()
                                                                                                                    .returnUserProvidedServiceInstances(true)
                                                                                                                    .spaceId(getTargetSpaceGuid().toString())
                                                                                                                    .page(page)
                                                                                                                    .build();
        return getServiceInstanceResources(pageRequestSupplier);
    }

    private Flux<? extends Resource<UnionServiceInstanceEntity>>
            getServiceInstanceResources(IntFunction<ListSpaceServiceInstancesRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV2Resources(page -> v3Client.spaces()
                                                                        .listServiceInstances(pageRequestSupplier.apply(page)));
    }

    private Mono<Derivable<CloudService>> zipWithAuxiliaryServiceContent(Resource<UnionServiceInstanceEntity> serviceInstanceResource) {
        UnionServiceInstanceEntity serviceInstance = serviceInstanceResource.getEntity();
        if (isUserProvided(serviceInstance)) {
            return Mono.just(ImmutableRawCloudService.of(serviceInstanceResource));
        }
        UUID serviceGuid = UUID.fromString(serviceInstance.getServiceId());
        UUID servicePlanGuid = UUID.fromString(serviceInstance.getServicePlanId());
        return Mono.zip(Mono.just(serviceInstanceResource), getServiceResource(serviceGuid), getServicePlanResource(servicePlanGuid))
                   .map(tuple -> ImmutableRawCloudService.builder()
                                                         .resource(tuple.getT1())
                                                         .serviceResource(tuple.getT2())
                                                         .servicePlanResource(tuple.getT3())
                                                         .build());
    }

    private boolean isUserProvided(UnionServiceInstanceEntity serviceInstance) {
        return USER_PROVIDED_SERVICE_INSTANCE_TYPE.equals(serviceInstance.getType());
    }

    private Mono<? extends Resource<ServiceEntity>> getServiceResource(UUID serviceGuid) {
        GetServiceRequest request = GetServiceRequest.builder()
                                                     .serviceId(serviceGuid.toString())
                                                     .build();
        return v3Client.services()
                       .get(request);
    }

    private Mono<? extends Resource<ServicePlanEntity>> getServicePlanResource(UUID servicePlanGuid) {
        GetServicePlanRequest request = GetServicePlanRequest.builder()
                                                             .servicePlanId(servicePlanGuid.toString())
                                                             .build();
        return v3Client.servicePlans()
                       .get(request);
    }

    private CloudServiceInstance findServiceInstanceByName(String name) {
        return fetchWithAuxiliaryContent(() -> getServiceInstanceResourceByName(name), this::zipWithAuxiliaryServiceInstanceContent);
    }

    private Mono<Derivable<CloudServiceInstance>>
            zipWithAuxiliaryServiceInstanceContent(Resource<UnionServiceInstanceEntity> serviceInstanceResource) {
        Mono<List<CloudServiceBinding>> serviceBindings = getServiceInstanceBindingsFlux(getGuid(serviceInstanceResource)).collectList();
        Mono<Derivable<CloudService>> service = zipWithAuxiliaryServiceContent(serviceInstanceResource);
        return Mono.zip(service, serviceBindings)
                   .map(tuple -> ImmutableRawCloudServiceInstance.builder()
                                                                 .resource(serviceInstanceResource)
                                                                 .serviceBindings(tuple.getT2())
                                                                 .service(tuple.getT1())
                                                                 .build());
    }

    private Flux<CloudServiceBinding> getServiceInstanceBindingsFlux(UUID serviceInstanceGuid) {
        return fetchFluxWithAuxiliaryContent(() -> getServiceBindingResourcesByServiceInstanceGuid(serviceInstanceGuid),
                                             this::zipWithAuxiliaryServiceBindingContent);
    }

    private Mono<Derivable<CloudServiceBinding>> zipWithAuxiliaryServiceBindingContent(Resource<ServiceBindingEntity> resource) {
        return getServiceBindingParameters(getGuid(resource)).map(parameters -> ImmutableRawCloudServiceBinding.builder()
                                                                                                               .resource(resource)
                                                                                                               .parameters(parameters)
                                                                                                               .build());
    }

    private Mono<Map<String, Object>> getServiceBindingParameters(UUID serviceBindingGuid) {
        return v3Client.serviceBindingsV2()
                       .getParameters(GetServiceBindingParametersRequest.builder()
                                                                        .serviceBindingId(serviceBindingGuid.toString())
                                                                        .build())
                       .map(GetServiceBindingParametersResponse::getParameters)
                       .onErrorResume(AbstractCloudFoundryException.class, e -> Mono.empty());
    }

    private void doUnbindService(UUID applicationGuid, UUID serviceGuid) {
        UUID serviceBindingGuid = getServiceBindingGuid(applicationGuid, serviceGuid);
        doUnbindService(serviceBindingGuid);
    }

    private UUID getServiceBindingGuid(UUID applicationGuid, UUID serviceGuid) {
        return getServiceBindingResourceByApplicationGuidAndServiceInstanceGuid(applicationGuid, serviceGuid).map(this::getGuid)
                                                                                                             .block();
    }

    private Mono<? extends Resource<ServiceBindingEntity>>
            getServiceBindingResourceByApplicationGuidAndServiceInstanceGuid(UUID applicationGuid, UUID serviceInstanceGuid) {
        IntFunction<ListApplicationServiceBindingsRequest> pageRequestSupplier = page -> ListApplicationServiceBindingsRequest.builder()
                                                                                                                              .applicationId(applicationGuid.toString())
                                                                                                                              .serviceInstanceId(serviceInstanceGuid.toString())
                                                                                                                              .page(page)
                                                                                                                              .build();
        return getApplicationServiceBindingResources(pageRequestSupplier).singleOrEmpty();
    }

    private Flux<? extends Resource<ServiceBindingEntity>>
            getApplicationServiceBindingResources(IntFunction<ListApplicationServiceBindingsRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV2Resources(page -> v3Client.applicationsV2()
                                                                        .listServiceBindings(pageRequestSupplier.apply(page)));
    }

    private Flux<? extends Resource<ServiceEntity>> getServiceResources() {
        IntFunction<ListServicesRequest> pageRequestSupplier = page -> ListServicesRequest.builder()
                                                                                          .page(page)
                                                                                          .build();
        return getServiceResources(pageRequestSupplier);
    }

}
