package com.sap.cloudfoundry.client.facade.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.stream.Stream;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v3.ClientV3Exception;
import org.cloudfoundry.client.v3.serviceofferings.GetServiceOfferingRequest;
import org.cloudfoundry.client.v3.serviceofferings.GetServiceOfferingResponse;
import org.cloudfoundry.client.v3.serviceofferings.ServiceOffering;
import org.cloudfoundry.client.v3.serviceofferings.ServiceOfferingsV3;
import org.cloudfoundry.client.v3.serviceplans.GetServicePlanRequest;
import org.cloudfoundry.client.v3.serviceplans.GetServicePlanResponse;
import org.cloudfoundry.client.v3.serviceplans.ServicePlan;
import org.cloudfoundry.client.v3.serviceplans.ServicePlansV3;
import org.cloudfoundry.doppler.DopplerClient;
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
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.adapters.RawCloudServiceOfferingTest;
import com.sap.cloudfoundry.client.facade.adapters.RawCloudServicePlanTest;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;

import reactor.core.publisher.Mono;

class CloudControllerRestClientImplTest {

    private static final CloudCredentials CREDENTIALS = new CloudCredentials("admin", "admin");
    private static final URL CONTROLLER_URL = createUrl("https://localhost:8080");

    private static final String SERVICE_PLAN_GUID = "1803e5a7-40c7-438e-b2be-e2045c9b7cda";
    private static final String SERVICE_INSTANCE_GUID = "26949ebb-a624-35c0-000-1110a01f1880";
    private static final String SERVICE_OFFERING_GUID = "1803e5a7-40c7-438e-b2be-e2045c9b7cda";
    private static final String PLAN_NAME = "test-plan";

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
    void testGetServiceOffering() {
        GetServiceOfferingRequest request = GetServiceOfferingRequest.builder()
                                                                     .serviceOfferingId(SERVICE_PLAN_GUID)
                                                                     .build();
        GetServiceOfferingResponse response = GetServiceOfferingResponse.builder()
                                                                        .from(RawCloudServiceOfferingTest.buildTestServiceOffering())
                                                                        .build();

        ServiceOfferingsV3 serviceOfferingsV3 = Mockito.mock(ServiceOfferingsV3.class);
        Mockito.when(delegate.serviceOfferingsV3())
               .thenReturn(serviceOfferingsV3);
        Mockito.when(serviceOfferingsV3.get(request))
               .thenReturn(Mono.just(response));

        ServiceOffering serviceOffering = controllerClient.getServiceOffering(SERVICE_PLAN_GUID)
                                                          .block();

        assertEquals(response, serviceOffering);
    }

    public static Stream<Arguments> testGetServiceOfferingWithError() {
        return Stream.of(
// @formatter:off
                Arguments.of(HttpStatus.FORBIDDEN.value(),
                        "403 Forbidden: Service offering with guid \"1803e5a7-40c7-438e-b2be-e2045c9b7cda\" is not available."),
                Arguments.of(HttpStatus.NOT_FOUND.value(),
                        "404 Not Found: Service offering with guid \"1803e5a7-40c7-438e-b2be-e2045c9b7cda\" not found.")
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGetServiceOfferingWithError(int errorCode, String expectedErrorMessage) {
        GetServiceOfferingRequest request = GetServiceOfferingRequest.builder()
                                                                     .serviceOfferingId(SERVICE_OFFERING_GUID)
                                                                     .build();
        ServiceOfferingsV3 serviceOfferingsV3 = Mockito.mock(ServiceOfferingsV3.class);
        Mockito.when(delegate.serviceOfferingsV3())
               .thenReturn(serviceOfferingsV3);
        Mockito.when(serviceOfferingsV3.get(request))
               .thenReturn(Mono.error(clientV3Exception(errorCode)));
        Exception cloudControllerException = assertThrows(CloudOperationException.class,
                                                          () -> controllerClient.getServiceOffering(SERVICE_OFFERING_GUID)
                                                                                .block());
        assertEquals(expectedErrorMessage, cloudControllerException.getMessage());
    }

    public static Stream<Arguments> testGetServicePlanWithError() {
        return Stream.of(
// @formatter:off
                Arguments.of(HttpStatus.FORBIDDEN.value(),
                        "403 Forbidden: Service plan with guid \"1803e5a7-40c7-438e-b2be-e2045c9b7cda\" is not available for service instance \"26949ebb-a624-35c0-000-1110a01f1880\"."),
                Arguments.of(HttpStatus.NOT_FOUND.value(),
                        "404 Not Found: Service plan with guid \"1803e5a7-40c7-438e-b2be-e2045c9b7cda\" for service instance with name \"26949ebb-a624-35c0-000-1110a01f1880\" was not found.")
// @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource
    void testGetServicePlanWithError(int errorCode, String expectedErrorMessage) {
        GetServicePlanRequest request = GetServicePlanRequest.builder()
                                                             .servicePlanId(SERVICE_PLAN_GUID)
                                                             .build();

        ServicePlansV3 servicePlans = Mockito.mock(ServicePlansV3.class);
        Mockito.when(delegate.servicePlansV3())
               .thenReturn(servicePlans);
        Mockito.when(servicePlans.get(request))
               .thenReturn(Mono.error(clientV3Exception(errorCode)));

        Exception cloudControllerException = assertThrows(CloudOperationException.class,
                                                          () -> controllerClient.getServicePlanResource(SERVICE_PLAN_GUID,
                                                                                                        SERVICE_INSTANCE_GUID)
                                                                                .block());
        assertEquals(expectedErrorMessage, cloudControllerException.getMessage());
    }

    @Test
    void testGetServicePlanResource() {
        GetServicePlanRequest request = GetServicePlanRequest.builder()
                                                             .servicePlanId(SERVICE_PLAN_GUID)
                                                             .build();
        GetServicePlanResponse response = GetServicePlanResponse.builder()
                                                                .from(RawCloudServicePlanTest.buildTestServicePlan(PLAN_NAME))
                                                                .build();

        ServicePlansV3 servicePlans = Mockito.mock(ServicePlansV3.class);
        Mockito.when(delegate.servicePlansV3())
               .thenReturn(servicePlans);
        Mockito.when(servicePlans.get(request))
               .thenReturn(Mono.just(response));

        ServicePlan servicePlanResource = controllerClient.getServicePlanResource(SERVICE_PLAN_GUID, SERVICE_INSTANCE_GUID)
                                                          .block();

        assertEquals(response, servicePlanResource);
    }

    private ClientV3Exception clientV3Exception(int statusCode) {
        return new ClientV3Exception(statusCode, Collections.emptyList());
    }
}
