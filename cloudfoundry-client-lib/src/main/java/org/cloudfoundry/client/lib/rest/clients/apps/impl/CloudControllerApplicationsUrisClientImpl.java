package org.cloudfoundry.client.lib.rest.clients.apps.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsBaseClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsUrisClient;
import org.cloudfoundry.client.lib.rest.clients.domains.CloudControllerDomainsClient;
import org.cloudfoundry.client.lib.rest.clients.util.UriInfoUtil;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.applications.AssociateApplicationRouteRequest;
import org.cloudfoundry.client.v2.applications.RemoveApplicationRouteRequest;
import org.cloudfoundry.client.v2.routes.CreateRouteRequest;
import org.cloudfoundry.client.v2.routes.CreateRouteResponse;
import org.cloudfoundry.client.v2.routes.RouteEntity;
import org.cloudfoundry.client.v2.spaces.ListSpaceRoutesRequest;
import org.cloudfoundry.util.PaginationUtils;
import org.springframework.util.CollectionUtils;

import reactor.core.publisher.Flux;

public class CloudControllerApplicationsUrisClientImpl extends CloudControllerApplicationsBaseClient
    implements CloudControllerApplicationsUrisClient {
    
    private CloudControllerDomainsClient domainsClient;

    protected CloudControllerApplicationsUrisClientImpl(CloudSpace target, CloudFoundryClient v3Client,
                                                        CloudControllerApplicationsClient applicationsClient,
                                                        CloudControllerDomainsClient domainsClient) {
        super(target, v3Client, applicationsClient);
        this.domainsClient = domainsClient;
    }

    @Override
    public void add(UUID applicationGuid, List<String> uris) {
        Map<String, UUID> domains = getDomainGuids();
        for (String uri : uris) {
            Map<String, String> uriInfo = new HashMap<>(2);
            UriInfoUtil.extractUriInfo(domains, uri, uriInfo);
            UUID domainGuid = domains.get(uriInfo.get("domainName"));
            String host = uriInfo.get("host");
            String path = uriInfo.get("path");
            bindRoute(domainGuid, host, path, applicationGuid);
        }
    }

    @Override
    public void update(String applicationName, List<String> uris) {
        CloudApplication application = applicationsClient.getApplication(applicationName);
        List<String> newUris = new ArrayList<>(uris);
        newUris.removeAll(application.getUris());
        List<String> removeUris = new ArrayList<>(application.getUris());
        removeUris.removeAll(uris);
        remove(application.getMetadata()
                              .getGuid(),
                   removeUris);
        add(application.getMetadata()
                       .getGuid(),
            newUris);
    }

    @Override
    public void remove(UUID applicationGuid, List<String> uris) {
        Map<String, UUID> domains = getDomainGuids();
        for (String uri : uris) {
            Map<String, String> uriInfo = new HashMap<>(2);
            UriInfoUtil.extractUriInfo(domains, uri, uriInfo);
            UUID domainGuid = domains.get(uriInfo.get("domainName"));
            String host = uriInfo.get("host");
            String path = uriInfo.get("path");
            unbindRoute(domainGuid, host, path, applicationGuid);
        }
    }

    private Map<String, UUID> getDomainGuids() {
        List<CloudDomain> availableDomains = new ArrayList<>();
        availableDomains.addAll(domainsClient.getDomainsForOrganization());
        availableDomains.addAll(domainsClient.getSharedDomains());
        Map<String, UUID> domains = new HashMap<>(availableDomains.size());
        for (CloudDomain availableDomain : availableDomains) {
            domains.put(availableDomain.getName(), availableDomain.getMetadata()
                                                                  .getGuid());
        }
        return domains;
    }

    private void bindRoute(UUID domainGuid, String host, String path, UUID applicationGuid) {
        UUID routeGuid = getOrAddRoute(domainGuid, host, path);
        v3Client.applicationsV2()
                .associateRoute(AssociateApplicationRouteRequest.builder()
                                                                .applicationId(applicationGuid.toString())
                                                                .routeId(routeGuid.toString())
                                                                .build())
                .block();
    }

    private UUID getOrAddRoute(UUID domainGuid, String host, String path) {
        UUID routeGuid = getRouteGuid(domainGuid, host, path);
        if (routeGuid == null) {
            routeGuid = doAddRoute(domainGuid, host, path);
        }
        return routeGuid;
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
    
    private void unbindRoute(UUID domainGuid, String host, String path, UUID applicationGuid) {
        UUID routeGuid = getRouteGuid(domainGuid, host, path);
        if (routeGuid == null) {
            return;
        }
        v3Client.applicationsV2()
                .removeRoute(RemoveApplicationRouteRequest.builder()
                                                          .applicationId(applicationGuid.toString())
                                                          .routeId(routeGuid.toString())
                                                          .build())
                .block();
    }
}
