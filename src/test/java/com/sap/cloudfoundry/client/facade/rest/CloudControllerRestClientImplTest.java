package com.sap.cloudfoundry.client.facade.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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
import org.junit.jupiter.api.function.Executable;
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
import com.sap.cloudfoundry.client.facade.TestUtil;
import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;

import reactor.core.publisher.Mono;

public class CloudControllerRestClientImplTest {

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
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        controllerClient = new CloudControllerRestClientImpl(CONTROLLER_URL, CREDENTIALS, webClient, oAuthClient, delegate);
    }

    @Test
    public void testGetServiceResource() {
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
    public void testGetServiceResourceWithForbidden() {
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
    public void testGetServicePlanResource() {
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
    public void testGetServicePlanResourceWithForbidden() {
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
    public static Stream<Arguments> testExtractUriInfo() {
        return Stream.of(
                // Select matching domain
                Arguments.of("domain-1.json", null),
                // Test with port and user
                Arguments.of("domain-2.json", null),
                // Test with route path
                Arguments.of("domain-3.json", null),
                // Test with invalid uri, should throw an exception
                Arguments.of("domain-4.json", CloudOperationException.class),
                // Uri equals the domain -> no host
                Arguments.of("domain-5.json", null),
                // Uri equals the domain (with path)
                Arguments.of("domain-6.json", null),
                // Test with domain which does not exist, should throw exception
                Arguments.of("domain-7.json", CloudOperationException.class)
        );
    }
    // @formatter:on

    @ParameterizedTest
    @MethodSource
    public void testExtractUriInfo(String fileName, Class<? extends RuntimeException> expectedException) throws Throwable {
        String fileContent = TestUtil.readFileContent(fileName, getClass());
        Input input = TestUtil.fromJson(fileContent, Input.class);
        Map<String, String> uriInfo = new HashMap<>();
        Map<String, UUID> domainsAsMap = input.getDomainsAsMap();

        executeWithErrorHandling(() -> controllerClient.extractUriInfo(domainsAsMap, input.getUri(), uriInfo), expectedException,
                                 input.getErrorMessage());

        validateUriInfo(uriInfo, domainsAsMap, input.getExpectedDomain(), input.getExpectedHost(), input.getExpectedPath());
    }

    private void executeWithErrorHandling(Executable executable, Class<? extends RuntimeException> expectedException,
                                          String exceptionMessage)
        throws Throwable {
        if (expectedException != null) {
            RuntimeException runtimeException = Assertions.assertThrows(expectedException, executable);
            Assertions.assertEquals(exceptionMessage, runtimeException.getMessage());
            return;
        }
        executable.execute();
    }

    private void validateUriInfo(Map<String, String> uriInfo, Map<String, UUID> domainsAsMap, String expectedDomain, String expectedHost,
                                 String expectedPath) {
        Assertions.assertEquals(domainsAsMap.get(expectedDomain), domainsAsMap.get(uriInfo.get("domainName")));
        Assertions.assertEquals(expectedHost, uriInfo.get("host"));
        Assertions.assertEquals(expectedPath, uriInfo.get("path"));
    }

    private static class Input {

        private String uri;
        private List<CloudDomain> domains;
        private String expectedPath;
        private String expectedHost;
        private String expectedDomain;
        private String errorMessage;

        public String getUri() {
            return uri;
        }

        public List<CloudDomain> getDomains() {
            return domains;
        }

        public String getExpectedPath() {
            return expectedPath;
        }

        public String getExpectedHost() {
            return expectedHost;
        }

        public String getExpectedDomain() {
            return expectedDomain;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public Map<String, UUID> getDomainsAsMap() {
            return getDomains().stream()
                               .collect(Collectors.toMap(CloudDomain::getName, domain -> domain.getMetadata()
                                                                                               .getGuid()));
        }

    }
}
