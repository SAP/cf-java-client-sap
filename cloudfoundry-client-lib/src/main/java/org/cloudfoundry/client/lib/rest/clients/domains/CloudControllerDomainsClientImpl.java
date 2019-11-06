package org.cloudfoundry.client.lib.rest.clients.domains;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetch;
import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchList;

import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudDomain;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudPrivateDomain;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudSharedDomain;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.domains.CreateDomainRequest;
import org.cloudfoundry.client.v2.domains.DomainEntity;
import org.cloudfoundry.client.v2.domains.ListDomainsRequest;
import org.cloudfoundry.client.v2.organizations.ListOrganizationPrivateDomainsRequest;
import org.cloudfoundry.client.v2.privatedomains.DeletePrivateDomainRequest;
import org.cloudfoundry.client.v2.privatedomains.ListPrivateDomainsRequest;
import org.cloudfoundry.client.v2.privatedomains.PrivateDomainEntity;
import org.cloudfoundry.client.v2.shareddomains.ListSharedDomainsRequest;
import org.cloudfoundry.client.v2.shareddomains.SharedDomainEntity;
import org.cloudfoundry.util.PaginationUtils;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CloudControllerDomainsClientImpl extends CloudControllerBaseClient implements CloudControllerDomainsClient {

    public CloudControllerDomainsClientImpl(CloudSpace target, CloudFoundryClient v3Client) {
        super(target, v3Client);
    }

    @Override
    public void addDomain(String domainName) {
        assertSpaceProvided("add domain");
        CloudDomain domain = findDomainByName(domainName);
        if (domain == null) {
            doCreateDomain(domainName);
        }
    }

    private void doCreateDomain(String name) {
        v3Client.domains()
                .create(CreateDomainRequest.builder()
                                           .wildcard(true)
                                           .owningOrganizationId(getTargetOrganizationGuid().toString())
                                           .name(name)
                                           .build())
                .block();
    }

    @Override
    public void deleteDomain(String domainName) {
        assertSpaceProvided("delete domain");
        CloudDomain domain = findDomainByName(domainName, true);
        // TODO: what about this?
        // List<CloudRoute> routes = findRoutes(domain);
        // if (!routes.isEmpty()) {
        // throw new IllegalStateException("Unable to remove domain that is in use --" + " it has " + routes.size() + " routes.");
        // }
        doDeleteDomain(getGuid(domain));
    }

    private void doDeleteDomain(UUID guid) {
        v3Client.privateDomains()
                .delete(DeletePrivateDomainRequest.builder()
                                                  .privateDomainId(guid.toString())
                                                  .build())
                .block();
    }

    @Override
    public CloudDomain getDefaultDomain() {
        List<CloudDomain> sharedDomains = getSharedDomains();
        if (sharedDomains.isEmpty()) {
            return null;
        } else {
            return sharedDomains.get(0);
        }
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return fetchList(this::getSharedDomainResources, ImmutableRawCloudSharedDomain::of);
    }

    private Flux<? extends Resource<SharedDomainEntity>> getSharedDomainResources() {
        IntFunction<ListSharedDomainsRequest> pageRequestSupplier = page -> ListSharedDomainsRequest.builder()
                                                                                                    .page(page)
                                                                                                    .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.sharedDomains()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    @Override
    public List<CloudDomain> getDomains() {
        return getPrivateDomains();
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return fetchList(this::getPrivateDomainResources, ImmutableRawCloudPrivateDomain::of);
    }

    private Flux<? extends Resource<PrivateDomainEntity>> getPrivateDomainResources() {
        IntFunction<ListPrivateDomainsRequest> pageRequestSupplier = page -> ListPrivateDomainsRequest.builder()
                                                                                                      .page(page)
                                                                                                      .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.privateDomains()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    @Override
    public UUID getRequiredDomainGuid(String name) {
        return getGuid(findDomainByName(name, true));
    }

    @Override
    public List<CloudDomain> getDomainsForOrganization() {
        assertSpaceProvided("access organization domains");
        return findDomainsByOrganizationGuid(getTargetOrganizationGuid());
    }

    private UUID getTargetOrganizationGuid() {
        return getGuid(target.getOrganization());
    }

    private List<CloudDomain> findDomainsByOrganizationGuid(UUID organizationGuid) {
        return fetchList(() -> getPrivateDomainResourcesByOrganizationGuid(organizationGuid), ImmutableRawCloudPrivateDomain::of);
    }

    private Flux<? extends Resource<PrivateDomainEntity>> getPrivateDomainResourcesByOrganizationGuid(UUID organizationGuid) {
        IntFunction<ListOrganizationPrivateDomainsRequest> pageRequestSupplier = page -> ListOrganizationPrivateDomainsRequest.builder()
                                                                                                                              .organizationId(organizationGuid.toString())
                                                                                                                              .page(page)
                                                                                                                              .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.organizations()
                                                                        .listPrivateDomains(pageRequestSupplier.apply(page)));
    }

    @Override
    public CloudDomain findDomainByName(String name, boolean required) {
        CloudDomain domain = findDomainByName(name);
        if (domain == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Domain " + name + " not found.");
        }
        return domain;
    }

    @Override
    public CloudDomain findDomainByName(String name) {
        return fetch(() -> getDomainResourceByName(name), ImmutableRawCloudDomain::of);
    }

    private Mono<? extends Resource<DomainEntity>> getDomainResourceByName(String name) {
        IntFunction<ListDomainsRequest> pageRequestSupplier = page -> ListDomainsRequest.builder()
                                                                                        .name(name)
                                                                                        .page(page)
                                                                                        .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.domains()
                                                                        .list(pageRequestSupplier.apply(page)))
                              .singleOrEmpty();
    }

}
