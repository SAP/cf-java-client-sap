package com.sap.cloudfoundry.client.facade.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;

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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.adapters.RawCloudServiceOfferingTest;
import com.sap.cloudfoundry.client.facade.adapters.RawCloudServicePlanTest;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;

import reactor.core.publisher.Mono;

class CloudControllerRestClientImplTest {

    private static final CloudCredentials CREDENTIALS = new CloudCredentials("admin", "admin");
    private static final URL CONTROLLER_URL = createUrl("https://localhost:8080");

    private static final String GUID = "1803e5a7-40c7-438e-b2be-e2045c9b7cda";
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
                                                                     .serviceOfferingId(GUID)
                                                                     .build();
        GetServiceOfferingResponse response = GetServiceOfferingResponse.builder()
                                                                        .from(RawCloudServiceOfferingTest.buildTestServiceOffering())
                                                                        .build();

        ServiceOfferingsV3 serviceOfferingsV3 = Mockito.mock(ServiceOfferingsV3.class);
        Mockito.when(delegate.serviceOfferingsV3())
               .thenReturn(serviceOfferingsV3);
        Mockito.when(serviceOfferingsV3.get(request))
               .thenReturn(Mono.just(response));

        ServiceOffering serviceOffering = controllerClient.getServiceOffering(GUID)
                                                          .block();

        assertEquals(response, serviceOffering);
    }

    @Test
    void testGetServiceOfferingWithForbidden() {
        GetServiceOfferingRequest request = GetServiceOfferingRequest.builder()
                                                                     .serviceOfferingId(GUID)
                                                                     .build();

        ServiceOfferingsV3 serviceOfferingsV3 = Mockito.mock(ServiceOfferingsV3.class);
        Mockito.when(delegate.serviceOfferingsV3())
               .thenReturn(serviceOfferingsV3);
        Mockito.when(serviceOfferingsV3.get(request))
               .thenReturn(Mono.error(clientV3Exception(HttpStatus.FORBIDDEN.value())));

        ServiceOffering serviceResource = controllerClient.getServiceOffering(GUID)
                                                          .block();

        assertNull(serviceResource);
    }

    @Test
    void testGetServicePlanResource() {
        GetServicePlanRequest request = GetServicePlanRequest.builder()
                                                             .servicePlanId(GUID)
                                                             .build();
        GetServicePlanResponse response = GetServicePlanResponse.builder()
                                                                .from(RawCloudServicePlanTest.buildTestServicePlan(PLAN_NAME))
                                                                .build();

        ServicePlansV3 servicePlans = Mockito.mock(ServicePlansV3.class);
        Mockito.when(delegate.servicePlansV3())
               .thenReturn(servicePlans);
        Mockito.when(servicePlans.get(request))
               .thenReturn(Mono.just(response));

        ServicePlan servicePlanResource = controllerClient.getServicePlanResource(GUID)
                                                          .block();

        assertEquals(response, servicePlanResource);
    }

    @Test
    void testGetServicePlanResourceWithForbidden() {
        GetServicePlanRequest request = GetServicePlanRequest.builder()
                                                             .servicePlanId(GUID)
                                                             .build();

        ServicePlansV3 servicePlans = Mockito.mock(ServicePlansV3.class);
        Mockito.when(delegate.servicePlansV3())
               .thenReturn(servicePlans);
        Mockito.when(servicePlans.get(request))
               .thenReturn(Mono.error(clientV3Exception(HttpStatus.FORBIDDEN.value())));

        ServicePlan servicePlanResource = controllerClient.getServicePlanResource(GUID)
                                                          .block();

        assertNull(servicePlanResource);
    }

    private ClientV3Exception clientV3Exception(int statusCode) {
        return new ClientV3Exception(statusCode, Collections.emptyList());
    }
}
