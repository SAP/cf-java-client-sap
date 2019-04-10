package org.cloudfoundry.client.lib.rest;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
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

    private static final String CCNG_API_URL = System.getProperty("ccng.target", "http://api.run.pivotal.io");

    private static final String CCNG_USER_EMAIL = System.getProperty("ccng.email", "java-authenticatedClient-test-user@vmware.com");

    private static final String CCNG_USER_ORG = System.getProperty("ccng.org", "gopivotal.com");

    private static final String CCNG_USER_PASS = System.getProperty("ccng.passwd");

    private static final String CCNG_USER_SPACE = System.getProperty("ccng.space", "test");

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

    /**
     * Failed attempt to instantiate CloudControllerClientImpl with existing constructors. Just here to illustrate the need to move the
     * initialize() method out of the constructor.
     */
    public void setUpWithNonEmptyConstructorWithoutLuck() throws Exception {
        restUtil = mock(RestUtil.class);
        when(restUtil.createRestTemplate(any(HttpProxyConfiguration.class), false)).thenReturn(restTemplate);
        when(restUtil.createOAuthClient(any(URL.class), any(HttpProxyConfiguration.class), false)).thenReturn(oAuthClient);
        when(restTemplate.getRequestFactory()).thenReturn(clientHttpRequestFactory);

        restUtil.createRestTemplate(null, false);
        restUtil.createOAuthClient(new URL(CCNG_API_URL), null, false);

        controllerClient = new CloudControllerRestClientImpl(new URL("http://api.dummyendpoint.com/login"), new CloudCredentials(CCNG_USER_EMAIL, CCNG_USER_PASS), restTemplate,
            oAuthClient, loggregatorClient, CCNG_USER_ORG, CCNG_USER_SPACE);
    }

}
