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

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.client.lib.util.RestUtil;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
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
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestUtil restUtil = new RestUtil();
    private final boolean trustSelfSignedCerts;
    private OAuthClient oAuthClient;

    public CloudControllerRestClientFactory(HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
        this.restTemplate = restUtil.createRestTemplate(httpProxyConfiguration, trustSelfSignedCerts);

        this.httpProxyConfiguration = httpProxyConfiguration;
        this.trustSelfSignedCerts = trustSelfSignedCerts;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    public OAuthClient getOAuthClient() {
        return oAuthClient;
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, CloudSpace target) {
        this.oAuthClient = createOAuthClient(controllerUrl);
        LoggregatorClient loggregatorClient = new LoggregatorClient(trustSelfSignedCerts);

        return new CloudControllerRestClientImpl(controllerUrl, credentials, restTemplate, oAuthClient, loggregatorClient, target);
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, CloudSpace target,
        OAuthClient oAuthClient) {
        this.oAuthClient = oAuthClient;
        LoggregatorClient loggregatorClient = new LoggregatorClient(trustSelfSignedCerts);

        return new CloudControllerRestClientImpl(controllerUrl, credentials, restTemplate, oAuthClient, loggregatorClient, target);
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, String organizationName,
        String spaceName) {
        this.oAuthClient = createOAuthClient(controllerUrl);
        LoggregatorClient loggregatorClient = new LoggregatorClient(trustSelfSignedCerts);

        return new CloudControllerRestClientImpl(controllerUrl, credentials, restTemplate, oAuthClient, loggregatorClient, organizationName,
            spaceName);
    }

    public CloudControllerRestClient createClient(URL controllerUrl, CloudCredentials credentials, String organizationName,
        String spaceName, OAuthClient oAuthClient) {
        this.oAuthClient = oAuthClient;
        LoggregatorClient loggregatorClient = new LoggregatorClient(trustSelfSignedCerts);

        return new CloudControllerRestClientImpl(controllerUrl, credentials, restTemplate, oAuthClient, loggregatorClient,
            organizationName, spaceName);
    }

    private OAuthClient createOAuthClient(URL cloudControllerUrl) {
        Map<String, Object> infoMap = getInfoMap(cloudControllerUrl);
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

    private Map<String, Object> getInfoMap(URL cloudControllerUrl) {
        if (infoCache.containsKey(cloudControllerUrl)) {
            return infoCache.get(cloudControllerUrl);
        }

        String response = restTemplate.getForObject(cloudControllerUrl + "/v2/info", String.class);

        try {
            return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
            });
        } catch (IOException e) {
            throw new IllegalArgumentException("Error getting /v2/info from cloud controller.", e);
        }
    }
}
