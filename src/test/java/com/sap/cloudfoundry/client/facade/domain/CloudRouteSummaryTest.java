package com.sap.cloudfoundry.client.facade.domain;

import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CloudRouteSummaryTest {

    private static Stream<Arguments> testToUriString() {
        return Stream.of(
// @formatter:off
            // (1) testing generic concatenation
            Arguments.of(routeSummary("foo", "example.com", "/does/this/work"), 
                         "foo.example.com/does/this/work"),
            // (2) uri string with port
            Arguments.of(routeSummaryWithPort("bar", "example.com", null, 30030),
                         "bar.example.com:30030"),
            // (3) uri string without a path
            Arguments.of(routeSummary("baz", "example.com", null),
                         "baz.example.com"),
            // (4) uri string with only a domain
            Arguments.of(routeSummary("", "example.com", ""),
                         "example.com"),
            // (5) uri string with only a domain, but the host and path are null
            Arguments.of(routeSummary(null, "example.com", null),
                         "example.com"),
            // (6) port is given as null
            Arguments.of(routeSummaryWithPort("foo", "example.com", null, null),
                         "foo.example.com")
// @formatter:on
        );
    }

    @MethodSource
    @ParameterizedTest
    void testToUriString(CloudRouteSummary route, String expectedUri) {
        Assertions.assertEquals(route.toUriString(), expectedUri);
    }

    private static Stream<Arguments> testDescribesTheSameUri() {
        return Stream.of(
// @formatter:off
            // (1) the same routes, this is the basic test
            Arguments.of(routeSummary("foo", "example.com", "/does/this/work"),
                         routeSummary("foo", "example.com", "/does/this/work"),
                         true),
            // (2) uri strings are the same, but dommains aren't
            Arguments.of(routeSummary("foo", "example.com", "/does/this/work"),
                         routeSummary("foo", "example.com/", "does/this/work"),
                         false),
            // (3) hosts are different
            Arguments.of(routeSummary("foo", "example.com", "/does/this/work"),
                         routeSummary("bar", "example.com", "/does/this/work"),
                         false),
            // (4) routes are the same but empty host is input in 2 different ways
            Arguments.of(routeSummary(null, "example.com", "/does/this/work"),
                         routeSummary("", "example.com", "/does/this/work"),
                         true),
            // (5) routes are the same again, testing null value vs empty string
            Arguments.of(routeSummary("", "example.com", null),
                         routeSummary(null, "example.com", ""),
                         true),
            // (6) same as before, but has port
            Arguments.of(routeSummaryWithPort("", "example.com", "", 3030),
                         routeSummaryWithPort("", "example.com", null, 3030),
                         true),
            // (7) obviously different
            Arguments.of(routeSummary(null, "example.com", "/does/this/work"),
                         routeSummary("something", "example.com", ""),
                         false)
// @formatter:on
        );
    }

    @MethodSource
    @ParameterizedTest
    void testDescribesTheSameUri(CloudRouteSummary route, CloudRouteSummary otherRoute, boolean expectedResult) {
        Assertions.assertEquals(route.describesTheSameUri(otherRoute), expectedResult);

        Assertions.assertEquals(route.describesTheSameUri(otherRoute), expectedResult);
    }

    private static Stream<Arguments> testMatchesCloudRoute() {
        return Stream.of(
//@formatter:off
            // (1) routes differ by path
            Arguments.of(route("valid-host", "valid-domain", "/valid/path"),
                         routeSummary("valid-host", "valid-domain", ""),
                         false),
            // (2) the same routes, this is the basic test
            Arguments.of(route("valid-host", "valid-domain", "/valid/path"),
                         routeSummary("valid-host", "valid-domain", "/valid/path"),
                         true),
            // (3) the same routes, null vs empty string in init
            Arguments.of(route(null, "valid-domain", ""),
                         routeSummary("", "valid-domain", null),
                         true),
            // (4) different routes but their uris are identical
            Arguments.of(route("", "host.domain", "/valid/path"),
                         routeSummary("host", "domain", "/valid/path"),
                         false));
//@formatter:on
    }

    @MethodSource
    @ParameterizedTest
    void testMatchesCloudRoute(CloudRoute route, CloudRouteSummary routeSummary, boolean shouldMatch) {
        Assertions.assertEquals(shouldMatch, routeSummary.describesTheSameUri(route));
    }

    private static CloudRouteSummary routeSummary(String host, String domain, String path) {
        return routeSummaryWithPort(host, domain, path, null);
    }

    private static CloudRouteSummary routeSummaryWithPort(String host, String domain, String path, Integer port) {
        return ImmutableCloudRouteSummary.builder()
                                         .host(host)
                                         .domain(domain)
                                         .path(path)
                                         .port(port)
                                         .build();
    }

    private static CloudRoute route(String host, String domain, String path) {
        CloudDomain cloudDomain = ImmutableCloudDomain.builder()
                                                      .name(domain)
                                                      .build();
        return ImmutableCloudRoute.builder()
                                  .host(host)
                                  .domain(cloudDomain)
                                  .path(path)
                                  .build();
    }

    private static Stream<Arguments> testEqualsHashcodeContract() {
        return Stream.of(
//@formatter:off
            // (1) routes differ by path
            Arguments.of(routeSummaryWithEverything("host", "domain.com", "/path", null, UUID.randomUUID(), UUID.randomUUID()),
                         routeSummary("host", "domain.com", "/path"),
                         true),
            // (2) the same routes, this is the basic test
            Arguments.of(routeSummaryWithEverything("host", "domain.com", "/path", null, UUID.randomUUID(), UUID.randomUUID()),
                         routeSummaryWithEverything("host", "domain.com", "/path", null, null, null),
                         true),
            // (3) the same routes, null vs empty string in init
            Arguments.of(routeSummary(null, "valid-domain", null),
                         routeSummary("", "valid-domain", ""),
                         true),
            // (4) different routes but their uris are identical
            Arguments.of(routeSummary("", "host.domain", "/valid/path"),
                         routeSummary("host", "domain", "/valid/path"),
                         false),
            // (5) different routes, the same uris incidentally
            Arguments.of(routeSummary("host.sub", "domain.com", "/path"),
                         routeSummary("host", "sub.domain.com", "/path"),
                         false),
            // (6) routes differ by random GUIDs, this should not happen in reality, only for testing equals and hashcode
            Arguments.of(routeSummaryWithEverything("host", "domain.com", "", null, UUID.randomUUID(), UUID.randomUUID()),
                         routeSummaryWithEverything("host", "domain.com", "", null, UUID.randomUUID(), UUID.randomUUID()),
                         true));
//@formatter:on
    }

    @ParameterizedTest
    @MethodSource("testEqualsHashcodeContract")
    void testImmutableEqualsMethod(ImmutableCloudRouteSummary firstRoute, ImmutableCloudRouteSummary secondRoute, boolean shouldBeEqual) {
        Assertions.assertEquals(shouldBeEqual, firstRoute.equals(secondRoute));
        Assertions.assertEquals(shouldBeEqual, secondRoute.equals(firstRoute));
    }

    @ParameterizedTest
    @MethodSource("testEqualsHashcodeContract")
    void testImmutableHashCodeMethod(ImmutableCloudRouteSummary firstRoute, ImmutableCloudRouteSummary secondRoute, boolean shouldBeEqual) {
        Assertions.assertEquals(shouldBeEqual, firstRoute.hashCode() == secondRoute.hashCode());
    }

    private static CloudRouteSummary routeSummaryWithEverything(String host, String domain, String path, Integer port, UUID routeGUID,
                                                                UUID domainGUID) {
        return ImmutableCloudRouteSummary.builder()
                                         .host(host)
                                         .domain(domain)
                                         .path(path)
                                         .port(port)
                                         .domainGuid(domainGUID)
                                         .guid(routeGUID)
                                         .build();
    }
}
