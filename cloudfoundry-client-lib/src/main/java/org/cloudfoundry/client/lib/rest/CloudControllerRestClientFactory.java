/*
 * Copyright 2009-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.client.lib.rest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.adapters.CloudControllerV3ClientFactory;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.client.lib.util.RestUtil;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Factory used to create cloud controller client implementations.
 *
 * @author Thgomas Risberg
 * @author Ramnivas Laddad
 */
public class CloudControllerRestClientFactory {

    private final HttpProxyConfiguration httpProxyConfiguration;
    private final Map<URL, Map<String, Object>> infoCache = new HashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate generalPurposeRestTemplate;
    private final RestUtil restUtil = new RestUtil();
    private final boolean trustSelfSignedCerts;

    private final CloudControllerV3ClientFactory v3ClientFactory = new CloudControllerV3ClientFactory();

    public CloudControllerRestClientFactory(boolean trustSelfSignedCerts) {
        this(null, trustSelfSignedCerts);
    }

    public CloudControllerRestClientFactory(HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
        this.httpProxyConfiguration = httpProxyConfiguration;
        this.trustSelfSignedCerts = trustSelfSignedCerts;
        this.generalPurposeRestTemplate = restUtil.createRestTemplate(httpProxyConfiguration, trustSelfSignedCerts);
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials) {
        return createClient(controllerUrl, credentials, (CloudSpace) null);
    }

    private CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, OAuthClient oAuthClient) {
        return createClient(controllerUrl, credentials, (CloudSpace) null, oAuthClient);
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, String organizationName,
        String spaceName) {
        return createClient(controllerUrl, credentials, organizationName, spaceName, createOAuthClient(controllerUrl));
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, String organizationName,
        String spaceName, OAuthClient oAuthClient) {
        CloudControllerRestClient clientWithoutTarget = createClient(controllerUrl, credentials, oAuthClient);
        CloudSpace target = clientWithoutTarget.getSpace(organizationName, spaceName);

        return createClient(controllerUrl, credentials, target, oAuthClient);
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, CloudSpace target) {
        return createClient(controllerUrl, credentials, target, createOAuthClient(controllerUrl));
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, CloudSpace target,
        OAuthClient oAuthClient) {
        RestTemplate restTemplate = createAuthorizationSettingRestTemplate(credentials, oAuthClient);
        LoggregatorClient loggregatorClient = new LoggregatorClient(trustSelfSignedCerts);
        CloudFoundryOperations v3OperationsClient = v3ClientFactory.createOperationsClient(controllerUrl, oAuthClient, target);
        CloudFoundryClient v3Client = v3ClientFactory.createClient(controllerUrl, oAuthClient);

        return new CloudControllerRestClientImpl(controllerUrl, credentials, restTemplate, oAuthClient, loggregatorClient,
            v3OperationsClient, v3Client, target);
    }

    private OAuthClient createOAuthClient(URL controllerUrl) {
        Map<String, Object> infoMap = getInfoMap(controllerUrl);
        URL authorizationEndpoint = getAuthorizationEndpoint(infoMap);
        return restUtil.createOAuthClient(authorizationEndpoint, httpProxyConfiguration, trustSelfSignedCerts);
    }

    private URL getAuthorizationEndpoint(Map<String, Object> infoMap) {
        String authorizationEndpoint = (String) infoMap.get("authorization_endpoint");
        try {
            return new URL(authorizationEndpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                MessageFormat.format("Error creating authorization endpoint URL for endpoint {0}.", authorizationEndpoint), e);
        }
    }

    private Map<String, Object> getInfoMap(URL controllerUrl) {
        if (infoCache.containsKey(controllerUrl)) {
            return infoCache.get(controllerUrl);
        }

        String infoResponse = generalPurposeRestTemplate.getForObject(controllerUrl + "/v2/info", String.class);
        try {
            return objectMapper.readValue(infoResponse, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new IllegalArgumentException("Error getting /v2/info from cloud controller.", e);
        }
    }

    private RestTemplate createAuthorizationSettingRestTemplate(CloudCredentials credentials, OAuthClient oAuthClient) {
        RestTemplate restTemplate = restUtil.createRestTemplate(httpProxyConfiguration, trustSelfSignedCerts);
        oAuthClient.init(credentials);
        setAuthorizingRequestFactory(restTemplate, credentials, oAuthClient);
        return restTemplate;
    }

    private void setAuthorizingRequestFactory(RestTemplate restTemplate, CloudCredentials credentials, OAuthClient oAuthClient) {
        ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
        if (!(requestFactory instanceof CloudControllerRestClientHttpRequestFactory)) {
            restTemplate.setRequestFactory(new CloudControllerRestClientHttpRequestFactory(requestFactory, credentials, oAuthClient));
        }
    }

}
