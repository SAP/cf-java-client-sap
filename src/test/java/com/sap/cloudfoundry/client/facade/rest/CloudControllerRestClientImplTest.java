package com.sap.cloudfoundry.client.facade.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
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
        controllerClient = new CloudControllerRestClientImpl(CONTROLLER_URL,
                                                             CREDENTIALS,
                                                             webClient,
                                                             oAuthClient,
                                                             delegate);
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

}
