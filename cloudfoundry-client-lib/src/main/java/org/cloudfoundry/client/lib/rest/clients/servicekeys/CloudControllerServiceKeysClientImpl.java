package org.cloudfoundry.client.lib.rest.clients.servicekeys;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetch;
import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchList;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.IntFunction;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudServiceKey;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.lib.rest.clients.services.CloudControllerServicesClient;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.servicekeys.CreateServiceKeyRequest;
import org.cloudfoundry.client.v2.servicekeys.DeleteServiceKeyRequest;
import org.cloudfoundry.client.v2.servicekeys.ListServiceKeysRequest;
import org.cloudfoundry.client.v2.servicekeys.ServiceKeyEntity;
import org.cloudfoundry.util.PaginationUtils;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class CloudControllerServiceKeysClientImpl extends CloudControllerBaseClient implements CloudControllerServiceKeysClient {

    private final CloudControllerServicesClient servicesClient;

    public CloudControllerServiceKeysClientImpl(CloudSpace target, CloudFoundryClient v3Client,
                                                CloudControllerServicesClient servicesClient) {
        super(target, v3Client);
        this.servicesClient = servicesClient;
    }

    @Override
    public CloudServiceKey create(String serviceName, String name, Map<String, Object> parameters) {
        CloudService service = servicesClient.getService(serviceName);
        return fetch(() -> createServiceKeyResource(service, name, parameters), resource -> ImmutableRawCloudServiceKey.builder()
                                                                                                                       .service(service)
                                                                                                                       .resource(resource)
                                                                                                                       .build());

    }

    @Override
    public void delete(String serviceName, String serviceKeyName) {
        List<CloudServiceKey> serviceKeys = get(serviceName);
        for (CloudServiceKey serviceKey : serviceKeys) {
            if (serviceKey.getName()
                          .equals(serviceKeyName)) {
                doDeleteServiceKey(serviceKey.getMetadata()
                                             .getGuid());
                return;
            }
        }
        throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service key " + serviceKeyName + " not found.");
    }

    @Override
    public List<CloudServiceKey> get(String serviceName) {
        CloudService service = servicesClient.getService(serviceName);
        return getServiceKeys(service);
    }

    private Mono<? extends Resource<ServiceKeyEntity>> createServiceKeyResource(CloudService service, String name,
                                                                                Map<String, Object> parameters) {
        UUID serviceGuid = getGuid(service);

        return v3Client.serviceKeys()
                       .create(CreateServiceKeyRequest.builder()
                                                      .serviceInstanceId(serviceGuid.toString())
                                                      .name(name)
                                                      .parameters(parameters)
                                                      .build());
    }

    private void doDeleteServiceKey(UUID guid) {
        v3Client.serviceKeys()
                .delete(DeleteServiceKeyRequest.builder()
                                               .serviceKeyId(guid.toString())
                                               .build())
                .block();
    }

    private List<CloudServiceKey> getServiceKeys(CloudService service) {
        return fetchList(() -> getServiceKeyTuple(service), tuple -> ImmutableRawCloudServiceKey.builder()
                                                                                                .resource(tuple.getT1())
                                                                                                .service(tuple.getT2())
                                                                                                .build());
    }

    private Flux<Tuple2<? extends Resource<ServiceKeyEntity>, CloudService>> getServiceKeyTuple(CloudService service) {
        UUID serviceInstanceGuid = getGuid(service);
        return getServiceKeyResourcesByServiceInstanceGuid(serviceInstanceGuid).map(serviceKeyResource -> Tuples.of(serviceKeyResource,
                                                                                                                    service));
    }

    private Flux<? extends Resource<ServiceKeyEntity>> getServiceKeyResourcesByServiceInstanceGuid(UUID serviceInstanceGuid) {
        IntFunction<ListServiceKeysRequest> pageRequestSupplier = page -> ListServiceKeysRequest.builder()
                                                                                                .serviceInstanceId(serviceInstanceGuid.toString())
                                                                                                .page(page)
                                                                                                .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.serviceKeys()
                                                                        .list(pageRequestSupplier.apply(page)));
    }
}
