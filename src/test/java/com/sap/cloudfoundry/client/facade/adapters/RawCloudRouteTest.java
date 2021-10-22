package com.sap.cloudfoundry.client.facade.adapters;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.routes.Application;
import org.cloudfoundry.client.v3.routes.Destination;
import org.cloudfoundry.client.v3.routes.Route;
import org.cloudfoundry.client.v3.routes.RouteRelationships;
import org.cloudfoundry.client.v3.routes.RouteResource;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudDomain;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRoute;

public class RawCloudRouteTest {

    private static final String HOST = "foo";
    private static final String DOMAIN_NAME = "example.com";
    private static final CloudDomain DOMAIN = buildTestDomain();
    private static final Integer APPS_USING_ROUTE = 2;
    private static final List<Destination> DESTINATIONS = buildTestDestinations(APPS_USING_ROUTE);

    @Test
    public void testDerive() {
        RawCloudEntityTest.testDerive(buildExpectedRoute(), buildRawRoute());
    }

    private static CloudRoute buildExpectedRoute() {
        return ImmutableCloudRoute.builder()
                                  .metadata(RawCloudEntityTest.EXPECTED_METADATA_V3)
                                  .host(HOST)
                                  .domain(DOMAIN)
                                  .path("")
                                  .appsUsingRoute(APPS_USING_ROUTE)
                                  .build();
    }

    private static RawCloudRoute buildRawRoute() {
        return ImmutableRawCloudRoute.builder()
                                     .route(buildTestRoute())
                                     .domain(DOMAIN)
                                     .build();
    }

    private static Route buildTestRoute() {
        return RouteResource.builder()
                            .id(RawCloudEntityTest.GUID_STRING)
                            .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
                            .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
                            .relationships(RouteRelationships.builder()
                                                             .space(buildToOneRelationship(RawCloudEntityTest.GUID))
                                                             .domain(buildToOneRelationship(RawCloudEntityTest.GUID))
                                                             .build())
                            .metadata(RawCloudEntityTest.V3_METADATA)
                            .host(HOST)
                            .path("")
                            .url(RawCloudEntityTest.URL_STRING)
                            .addAllDestinations(DESTINATIONS)
                            .build();
    }

    private static CloudDomain buildTestDomain() {
        return ImmutableCloudDomain.builder()
                                   .metadata(RawCloudEntityTest.EXPECTED_METADATA)
                                   .name(DOMAIN_NAME)
                                   .build();
    }

    private static List<Destination> buildTestDestinations(int count) {
        return Collections.nCopies(count, Destination.builder()
                                                     .application(Application.builder()
                                                                             .applicationId("")
                                                                             .build())
                                                     .build());
    }

    private static ToOneRelationship buildToOneRelationship(UUID guid) {
        return ToOneRelationship.builder()
                                .data(buildRelationship(guid))
                                .build();
    }

    private static Relationship buildRelationship(UUID guid) {
        return Relationship.builder()
                           .id(guid.toString())
                           .build();
    }

}
