package org.cloudfoundry.client.lib.rest.clients.routes;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchListWithAuxiliaryContent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudRoute;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.lib.rest.clients.domains.CloudControllerDomainsClient;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.routemappings.ListRouteMappingsRequest;
import org.cloudfoundry.client.v2.routemappings.RouteMappingEntity;
import org.cloudfoundry.client.v2.routes.CreateRouteRequest;
import org.cloudfoundry.client.v2.routes.CreateRouteResponse;
import org.cloudfoundry.client.v2.routes.DeleteRouteRequest;
import org.cloudfoundry.client.v2.routes.RouteEntity;
import org.cloudfoundry.client.v2.spaces.ListSpaceRoutesRequest;
import org.cloudfoundry.util.PaginationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.util.CollectionUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

public class CloudControllerRoutesClientImpl extends CloudControllerBaseClient implements CloudControllerRoutesClient {

    private CloudControllerDomainsClient domainsClient;

    public CloudControllerRoutesClientImpl(CloudSpace target, CloudFoundryClient v3Client, CloudControllerDomainsClient domainsClient) {
        super(target, v3Client);
        this.domainsClient = domainsClient;
    }

    @Override
    public void add(String host, String domainName, String path) {
        assertSpaceProvided("add route for domain");
        UUID domainGuid = domainsClient.getRequiredDomainGuid(domainName);
        doAddRoute(domainGuid, host, path);
    }

    private UUID doAddRoute(UUID domainGuid, String host, String path) {
        assertSpaceProvided("add route");
        CreateRouteResponse response = v3Client.routes()
                                               .create(CreateRouteRequest.builder()
                                                                         .domainId(domainGuid.toString())
                                                                         .host(host)
                                                                         .path(path)
                                                                         .spaceId(getTargetSpaceGuid().toString())
                                                                         .build())
                                               .block();
        return getGuid(response);
    }

    @Override
    public List<CloudRoute> deleteOrphaned() {
        List<CloudRoute> orphanRoutes = new ArrayList<>();
        for (CloudDomain domain : domainsClient.getDomainsForOrganization()) {
            orphanRoutes.addAll(fetchOrphanRoutes(domain.getName()));
        }

        List<CloudRoute> deletedRoutes = new ArrayList<>();
        for (CloudRoute orphanRoute : orphanRoutes) {
            delete(orphanRoute.getHost(), orphanRoute.getDomain()
                                                          .getName(),
                        orphanRoute.getPath());
            deletedRoutes.add(orphanRoute);
        }
        return deletedRoutes;
    }

    private List<CloudRoute> fetchOrphanRoutes(String domainName) {
        List<CloudRoute> orphanRoutes = new ArrayList<>();
        for (CloudRoute route : getAll(domainName)) {
            if (!route.isUsed()) {
                orphanRoutes.add(route);
            }
        }
        return orphanRoutes;
    }

    @Override
    public void delete(String host, String domainName, String path) {
        assertSpaceProvided("delete route for domain");
        UUID routeGuid = getRouteGuid(domainsClient.getRequiredDomainGuid(domainName), host, path);
        if (routeGuid == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND,
                                              "Not Found",
                                              "Host " + host + " not found for domain " + domainName + ".");
        }
        doDeleteRoute(routeGuid);
    }

    private UUID getRouteGuid(UUID domainGuid, String host, String path) {
        List<? extends Resource<RouteEntity>> routeEntitiesResource = getRouteResourcesByDomainGuidHostAndPath(domainGuid, host,
                                                                                                               path).collect(Collectors.toList())
                                                                                                                    .block();
        if (CollectionUtils.isEmpty(routeEntitiesResource)) {
            return null;
        }
        return getGuid(routeEntitiesResource.get(0));
    }

    private Flux<? extends Resource<RouteEntity>> getRouteResourcesByDomainGuidHostAndPath(UUID domainGuid, String host, String path) {
        ListSpaceRoutesRequest.Builder requestBuilder = ListSpaceRoutesRequest.builder();
        if (host != null) {
            requestBuilder.host(host);
        }
        if (path != null) {
            requestBuilder.path(path);
        }
        requestBuilder.spaceId(getTargetSpaceGuid().toString())
                      .domainId(domainGuid.toString());

        return PaginationUtils.requestClientV2Resources(page -> v3Client.spaces()
                                                                        .listRoutes(requestBuilder.page(page)
                                                                                                  .build()));
    }

    private void doDeleteRoute(UUID guid) {
        v3Client.routes()
                .delete(DeleteRouteRequest.builder()
                                          .routeId(guid.toString())
                                          .build())
                .block();
    }

    @Override
    public List<CloudRoute> getAll(String domainName) {
        assertSpaceProvided("get routes for domain");
        CloudDomain domain = domainsClient.findDomainByName(domainName, true);
        return findRoutes(domain);
    }

    private List<CloudRoute> findRoutes(CloudDomain domain) {
        return fetchListWithAuxiliaryContent(() -> getRouteTuple(domain, getTargetSpaceGuid()), this::zipWithAuxiliaryRouteContent);
    }

    private Flux<Tuple2<? extends Resource<RouteEntity>, CloudDomain>> getRouteTuple(CloudDomain domain, UUID spaceGuid) {
        UUID domainGuid = getGuid(domain);
        return getRouteResourcesByDomainGuidAndSpaceGuid(domainGuid, spaceGuid).map(routeResource -> Tuples.of(routeResource, domain));
    }

    private Flux<? extends Resource<RouteEntity>> getRouteResourcesByDomainGuidAndSpaceGuid(UUID domainGuid, UUID spaceGuid) {
        IntFunction<ListSpaceRoutesRequest> pageRequestSupplier = page -> ListSpaceRoutesRequest.builder()
                                                                                                .domainId(domainGuid.toString())
                                                                                                .spaceId(spaceGuid.toString())
                                                                                                .page(page)
                                                                                                .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.spaces()
                                                                        .listRoutes(pageRequestSupplier.apply(page)));
    }

    private Mono<Derivable<CloudRoute>> zipWithAuxiliaryRouteContent(Tuple2<? extends Resource<RouteEntity>, CloudDomain> routeTuple) {
        UUID routeGuid = getGuid(routeTuple.getT1());
        return getRouteMappingResourcesByRouteGuid(routeGuid).collectList()
                                                             .map(routeMappingResources -> ImmutableRawCloudRoute.builder()
                                                                                                                 .resource(routeTuple.getT1())
                                                                                                                 .domain(routeTuple.getT2())
                                                                                                                 .routeMappingResources(routeMappingResources)
                                                                                                                 .build());
    }

    private Flux<? extends Resource<RouteMappingEntity>> getRouteMappingResourcesByRouteGuid(UUID routeGuid) {
        IntFunction<ListRouteMappingsRequest> pageRequestSupplier = page -> ListRouteMappingsRequest.builder()
                                                                                                    .routeId(routeGuid.toString())
                                                                                                    .page(page)
                                                                                                    .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.routeMappings()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

}
