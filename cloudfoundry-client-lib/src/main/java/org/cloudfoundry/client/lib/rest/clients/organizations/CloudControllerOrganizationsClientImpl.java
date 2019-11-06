package org.cloudfoundry.client.lib.rest.clients.organizations;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetch;
import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchList;

import java.util.List;
import java.util.function.IntFunction;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.v3.organizations.ListOrganizationsRequest;
import org.cloudfoundry.client.v3.organizations.Organization;
import org.cloudfoundry.util.PaginationUtils;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CloudControllerOrganizationsClientImpl extends CloudControllerBaseClient implements CloudControllerOrganizationsClient {

    public CloudControllerOrganizationsClientImpl(CloudSpace target, CloudFoundryClient v3Client) {
        super(target, v3Client);
    }

    @Override
    public CloudOrganization getOrganization(String organizationName, boolean required) {
        CloudOrganization organization = findOrganization(organizationName);
        if (organization == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Organization " + organizationName + " not found.");
        }
        return organization;
    }

    @Override
    public List<CloudOrganization> getOrganizations() {
        return fetchList(this::getOrganizationResources, ImmutableRawCloudOrganization::of);
    }

    private CloudOrganization findOrganization(String name) {
        return fetch(() -> getOrganizationResourceByName(name), ImmutableRawCloudOrganization::of);
    }

    private Mono<? extends Organization> getOrganizationResourceByName(String name) {
        IntFunction<ListOrganizationsRequest> pageRequestSupplier = page -> ListOrganizationsRequest.builder()
                                                                                                    .name(name)
                                                                                                    .page(page)
                                                                                                    .build();
        return getOrganizationResources(pageRequestSupplier).singleOrEmpty();
    }

    private Flux<? extends Organization> getOrganizationResources() {
        IntFunction<ListOrganizationsRequest> pageRequestSupplier = page -> ListOrganizationsRequest.builder()
                                                                                                    .page(page)
                                                                                                    .build();
        return getOrganizationResources(pageRequestSupplier);
    }

    private Flux<? extends Organization> getOrganizationResources(IntFunction<ListOrganizationsRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV3Resources(page -> v3Client.organizationsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

}
