package org.cloudfoundry.client.lib.rest;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.client.lib.util.RestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@RunWith(org.mockito.runners.MockitoJUnitRunner.class)
public class CloudControllerRestClientImplTest {

    @Mock
    private ClientHttpRequestFactory clientHttpRequestFactory;

    private CloudControllerRestClientImpl controllerClient;

    @Mock
    private LoggregatorClient loggregatorClient;

    @Mock
    private OAuthClient oAuthClient;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RestUtil restUtil;

    @Test
    public void extractUriInfo_selects_matching_domain() throws Exception {
        // given
        String uri = "xyz.domain.com";
        Map<String, UUID> domains = new LinkedHashMap<String, UUID>(); // Since impl iterates key, need to control
        // iteration order with a LinkedHashMap
        domains.put("domain.com", UUID.randomUUID());
        domains.put("z.domain.com", UUID.randomUUID());
        Map<String, String> uriInfo = new HashMap<String, String>(3);

        // when
        controllerClient.extractUriInfo(domains, uri, uriInfo);

        // then
        Assert.assertEquals(domains.get("domain.com"), domains.get(uriInfo.get("domainName")));
        Assert.assertEquals("xyz", uriInfo.get("host"));
    }

    @Test
    public void extractUriInfo_with_port_and_user() {
        Map<String, String> uriInfo = new HashMap<>(2);
        String uri = "http://bob:hq@bang.foo.bar.com:8181";
        Map<String, UUID> domains = new HashMap<>();
        domains.put("foo.bar.com", UUID.randomUUID());
        domains.put("anotherdomain.com", UUID.randomUUID());

        controllerClient.extractUriInfo(domains, uri, uriInfo);

        Assert.assertEquals(domains.get("foo.bar.com"), domains.get(uriInfo.get("domainName")));
        Assert.assertEquals("bang", uriInfo.get("host"));
    }

    @Test
    public void extractUriInfo_with_route_path() {
        String uri = "xyz.domain.com/path";
        Map<String, UUID> domains = new LinkedHashMap<String, UUID>();
        domains.put("domain.com", UUID.randomUUID());
        domains.put("z.domain.com", UUID.randomUUID());
        Map<String, String> uriInfo = new HashMap<String, String>(3);

        controllerClient.extractUriInfo(domains, uri, uriInfo);

        Assert.assertEquals(domains.get("domain.com"), domains.get(uriInfo.get("domainName")));
        Assert.assertEquals("xyz", uriInfo.get("host"));
        Assert.assertEquals("/path", uriInfo.get("path"));
    }

    @Before
    public void setUpWithEmptyConstructor() throws Exception {
        controllerClient = new CloudControllerRestClientImpl();
    }

}
