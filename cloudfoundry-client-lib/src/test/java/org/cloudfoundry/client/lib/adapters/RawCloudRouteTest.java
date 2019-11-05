package org.cloudfoundry.client.lib.adapters;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.ImmutableCloudDomain;
import org.cloudfoundry.client.lib.domain.ImmutableCloudRoute;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.routemappings.RouteMappingEntity;
import org.cloudfoundry.client.v2.routemappings.RouteMappingResource;
import org.cloudfoundry.client.v3.Link;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.routes.Route;
import org.cloudfoundry.client.v3.routes.RouteRelationships;
import org.cloudfoundry.client.v3.routes.RouteResource;
import org.junit.jupiter.api.Test;

public class RawCloudRouteTest {

    private static final String HOST = "foo";
    private static final String PATH = "/baz";
    private static final String DOMAIN_NAME = "example.com";
    private static final CloudDomain DOMAIN = buildTestDomain();
    private static final Integer APPS_USING_ROUTE = 2;
    private static final List<Resource<RouteMappingEntity>> ROUTE_MAPPINGS = buildTestRouteMappings(APPS_USING_ROUTE);

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedRoute(), buildRawRoute());
    }

    private static CloudRoute buildExpectedRoute() {
        return ImmutableCloudRoute.builder()
                                  .metadata(RawCloudEntityTest.EXPECTED_METADATA)
                                  .host(HOST)
                                  .domain(DOMAIN)
                                  .appsUsingRoute(APPS_USING_ROUTE)
                                  .build();
    }

    private static RawCloudRoute buildRawRoute() {
        return ImmutableRawCloudRoute.builder()
                                     .resource(buildTestResource())
                                     .domain(DOMAIN)
                                     .routeMappingResources(ROUTE_MAPPINGS)
                                     .build();
    }

    private static Route buildTestResource() {
        return RouteResource.builder()
                            .id(RawCloudEntityTest.GUID_STRING)
                            .createdAt(RawCloudEntityTest.METADATA.getCreatedAt())
                            .updatedAt(RawCloudEntityTest.METADATA.getUpdatedAt())
                            .url(RawCloudEntityTest.URL_STRING)
                            .host(HOST)
                            .relationships(RouteRelationships.builder()
                                                             .domain(buildToOneRelationShip())
                                                             .space(buildToOneRelationShip())
                                                             .build())
                            .path(PATH)
                            .link("self", Link.builder()
                                              .href(RawCloudEntityTest.URL_STRING)
                                              .build())
                            .build();
    }

    private static ToOneRelationship buildToOneRelationShip() {
        return ToOneRelationship.builder()
                                  .data(Relationship.builder()
                                                    .id("test")
                                                    .build())
                                  .build();
    }

    private static CloudDomain buildTestDomain() {
        return ImmutableCloudDomain.builder()
                                   .metadata(RawCloudEntityTest.EXPECTED_METADATA)
                                   .name(DOMAIN_NAME)
                                   .build();
    }

    private static List<Resource<RouteMappingEntity>> buildTestRouteMappings(int count) {
        return Stream.generate(RawCloudRouteTest::buildTestRouteMapping)
                     .limit(count)
                     .collect(Collectors.toList());
    }

    private static Resource<RouteMappingEntity> buildTestRouteMapping() {
        return RouteMappingResource.builder()
                                   .build();
    }

}
