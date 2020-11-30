package com.sap.cloudfoundry.client.facade.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.ClientV2Exception;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.serviceplans.GetServicePlanRequest;
import org.cloudfoundry.client.v2.serviceplans.GetServicePlanResponse;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanEntity;
import org.cloudfoundry.client.v2.serviceplans.ServicePlans;
import org.cloudfoundry.client.v2.services.GetServiceRequest;
import org.cloudfoundry.client.v2.services.GetServiceResponse;
import org.cloudfoundry.client.v2.services.ServiceEntity;
import org.cloudfoundry.client.v2.services.Services;
import org.cloudfoundry.doppler.DopplerClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudRouteSummary;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;

import reactor.core.publisher.Mono;

class CloudControllerRestClientImplTest {

    private static final CloudCredentials CREDENTIALS = new CloudCredentials("admin", "admin");
    private static final URL CONTROLLER_URL = createUrl("https://localhost:8080");

    private static final String GUID = "1803e5a7-40c7-438e-b2be-e2045c9b7cda";

    private static URL createUrl(String string) {
        try {
            return new URL(string);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Mock
    private OAuthClient oAuthClient;
    @Mock
    private WebClient webClient;
    @Mock
    private DopplerClient dopplerClient;
    @Mock
    private CloudFoundryClient delegate;
    private CloudControllerRestClientImpl controllerClient;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this)
                          .close();
        controllerClient = new CloudControllerRestClientImpl(CONTROLLER_URL, CREDENTIALS, webClient, oAuthClient, delegate);
    }

    @Test
    void testGetServiceResource() {
        GetServiceRequest request = GetServiceRequest.builder()
                                                     .serviceId(GUID)
                                                     .build();
        GetServiceResponse response = GetServiceResponse.builder()
                                                        .entity(ServiceEntity.builder()
                                                                             .label("postgresql")
                                                                             .build())
                                                        .build();

        Services services = Mockito.mock(Services.class);
        Mockito.when(delegate.services())
               .thenReturn(services);
        Mockito.when(services.get(request))
               .thenReturn(Mono.just(response));

        Resource<ServiceEntity> serviceResource = controllerClient.getServiceResource(UUID.fromString(GUID))
                                                                  .block();

        assertEquals(response, serviceResource);
    }

    @Test
    void testGetServiceResourceWithForbidden() {
        GetServiceRequest request = GetServiceRequest.builder()
                                                     .serviceId(GUID)
                                                     .build();

        Services services = Mockito.mock(Services.class);
        Mockito.when(delegate.services())
               .thenReturn(services);
        Mockito.when(services.get(request))
               .thenReturn(Mono.error(clientV2Exception(HttpStatus.FORBIDDEN.value())));

        Resource<ServiceEntity> serviceResource = controllerClient.getServiceResource(UUID.fromString(GUID))
                                                                  .block();

        assertNull(serviceResource);
    }

    @Test
    void testGetServicePlanResource() {
        GetServicePlanRequest request = GetServicePlanRequest.builder()
                                                             .servicePlanId(GUID)
                                                             .build();
        GetServicePlanResponse response = GetServicePlanResponse.builder()
                                                                .entity(ServicePlanEntity.builder()
                                                                                         .name("v9.4-large")
                                                                                         .free(false)
                                                                                         .build())
                                                                .build();

        ServicePlans servicePlans = Mockito.mock(ServicePlans.class);
        Mockito.when(delegate.servicePlans())
               .thenReturn(servicePlans);
        Mockito.when(servicePlans.get(request))
               .thenReturn(Mono.just(response));

        Resource<ServicePlanEntity> servicePlanResource = controllerClient.getServicePlanResource(UUID.fromString(GUID))
                                                                          .block();

        assertEquals(response, servicePlanResource);
    }

    @Test
    void testGetServicePlanResourceWithForbidden() {
        GetServicePlanRequest request = GetServicePlanRequest.builder()
                                                             .servicePlanId(GUID)
                                                             .build();

        ServicePlans servicePlans = Mockito.mock(ServicePlans.class);
        Mockito.when(delegate.servicePlans())
               .thenReturn(servicePlans);
        Mockito.when(servicePlans.get(request))
               .thenReturn(Mono.error(clientV2Exception(HttpStatus.FORBIDDEN.value())));

        Resource<ServicePlanEntity> servicePlanResource = controllerClient.getServicePlanResource(UUID.fromString(GUID))
                                                                          .block();

        assertNull(servicePlanResource);
    }

    private ClientV2Exception clientV2Exception(int statusCode) {
        return new ClientV2Exception(statusCode, 0, "", "");
    }

    // @formatter:off
    public static Stream<Arguments> testSelectRoutesOnlyInFirst() {
        return Stream.of(
                // (1) two input routes, have to subtract one of them
                Arguments.of(Set.of(routeSummary("foo", "domain.com", "/path"), 
                                    routeSummary("bar", "domain.com", "/path")),
                             Set.of(routeSummary("foo", "domain.com", "/path")),
                             Set.of(routeSummary("bar", "domain.com", "/path"))),
                // (2) the second set contains all the same routes but foo.domain.com has no hostname this time
                Arguments.of(Set.of(routeSummary("foo", "domain.com", "/path"), 
                                    routeSummary("bar", "domain.com", "/path"),
                                    routeSummary("baz", "another.domain.com", ""), 
                                    routeSummary("", "sub.domain", "/some/complex/path"),
                                    routeSummary("complex-host-not-that-it-matters", "third.domain.com", "/complex/path")),
                             Set.of(routeSummary("", "foo.domain.com", "/path"), 
                                    routeSummary("bar", "domain.com", "/path"),
                                    routeSummary("baz", "another.domain.com", null), 
                                    routeSummary(null, "sub.domain", "/some/complex/path"),
                                    routeSummary("complex-host-not-that-it-matters", "third.domain.com", "/complex/path")),
                             Set.of(routeSummary("foo", "domain.com", "/path"))),
                // (3) the second set contains all the same routes but slightly different - difference is the same as first set
                Arguments.of(Set.of(routeSummary("foo", "domain.com", "/path"), 
                                    routeSummary("bar", "domain.com", "/path"),
                                    routeSummary("baz", "another.domain.com", ""), 
                                    routeSummary("", "sub.domain", "/some/complex/path"),
                                    routeSummary("complex-host-this-time-it-matters", "third.domain.com", "/complex/path")),
                             Set.of(routeSummary("foo", "domain.com", ""), 
                                    routeSummary("", "bar.domain.com", "/path"),
                                    routeSummary("baz", "another.domain.com", "/has/path"), 
                                    routeSummary("", "sub.domain", "/other/complex/path"),
                                    routeSummary("complex-host-it-matters", "third.domain.com", "/complex/path")),
                             Set.of(routeSummary("foo", "domain.com", "/path"), 
                                    routeSummary("bar", "domain.com", "/path"),
                                    routeSummary("baz", "another.domain.com", ""), 
                                    routeSummary("", "sub.domain", "/some/complex/path"),
                                    routeSummary("complex-host-this-time-it-matters", "third.domain.com", "/complex/path"))),
                // (4) two input routes, have to subtract one of them
                Arguments.of(Set.of(routeSummary("foo", "domain.com", "/path"), 
                                    routeSummary("bar", "domain.com", "/path"),
                                    routeSummary("baz", "another.domain.com", ""), 
                                    routeSummary("", "sub.domain", "/some/complex/path"),
                                    routeSummary("complex-host-this-time-it-matters", "third.domain.com", "/complex/path")),
                             Set.of(routeSummary("complex-host-this-time-it-matters", "third.domain.com", "/complex/path"),
                                    routeSummary("baz", "another.domain.com", ""),
                                    routeSummary("foo", "domain.com", "/path"), 
                                    routeSummary("", "sub.domain", "/some/complex/path"),
                                    routeSummary("bar", "domain.com", "/path")),
                             Set.of())
        );
    }
    // @formatter:on

    @ParameterizedTest
    @MethodSource
    void testSelectRoutesOnlyInFirst(Set<CloudRouteSummary> routes, Set<CloudRouteSummary> routesToSubtract,
                                     Set<CloudRouteSummary> resultRoutes) {
        Assertions.assertEquals(controllerClient.selectRoutesOnlyInFirst(routes, routesToSubtract), resultRoutes);
    }

    private static CloudRouteSummary routeSummary(String host, String domain, String path) {
        return ImmutableCloudRouteSummary.builder()
                                         .host(host)
                                         .domain(domain)
                                         .path(path)
                                         .build();
    }
}
