package org.cloudfoundry.client.lib.util;

import static org.apache.http.conn.ssl.SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;

import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.cloudfoundry.client.lib.HttpProxyConfiguration;
import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.client.lib.oauth2.OAuthClientWithLoginHint;
import org.cloudfoundry.client.lib.rest.CloudControllerResponseErrorHandler;
import org.cloudfoundry.client.lib.rest.LoggingRestTemplate;
import org.cloudfoundry.client.lib.rest.LoggregatorHttpMessageConverter;
import org.cloudfoundry.reactor.ConnectionContext;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 * Some helper utilities for creating classes used for the REST support.
 *
 * @author Thomas Risberg
 */
public class RestUtil {

    public OAuthClient createOAuthClient(URL authorizationUrl, HttpProxyConfiguration httpProxyConfiguration,
        boolean trustSelfSignedCerts) {
        return new OAuthClient(authorizationUrl, createRestTemplate(httpProxyConfiguration, trustSelfSignedCerts));
    }

    public OAuthClient createOAuthClient(URL authorizationUrl, HttpProxyConfiguration httpProxyConfiguration,
        boolean trustSelfSignedCerts, ConnectionContext connectionContext, String origin) {
        return new OAuthClientWithLoginHint(authorizationUrl, createRestTemplate(httpProxyConfiguration, trustSelfSignedCerts),
            connectionContext, origin);
    }

    public ClientHttpRequestFactory createRequestFactory(HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
        HttpClientBuilder httpClientBuilder = HttpClients.custom()
            .useSystemProperties();

        if (trustSelfSignedCerts) {
            httpClientBuilder.setSslcontext(buildSslContext());
            httpClientBuilder.setHostnameVerifier(BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
        }

        if (httpProxyConfiguration != null) {
            HttpHost proxy = new HttpHost(httpProxyConfiguration.getProxyHost(), httpProxyConfiguration.getProxyPort());
            httpClientBuilder.setProxy(proxy);

            if (httpProxyConfiguration.isAuthRequired()) {
                BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(
                    new AuthScope(httpProxyConfiguration.getProxyHost(), httpProxyConfiguration.getProxyPort()),
                    new UsernamePasswordCredentials(httpProxyConfiguration.getUsername(), httpProxyConfiguration.getPassword()));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }

            HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            httpClientBuilder.setRoutePlanner(routePlanner);
        }

        HttpClient httpClient = httpClientBuilder.build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        return requestFactory;
    }

    public RestTemplate createRestTemplate(HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
        RestTemplate restTemplate = new LoggingRestTemplate();
        restTemplate.setRequestFactory(createRequestFactory(httpProxyConfiguration, trustSelfSignedCerts));
        restTemplate.setErrorHandler(new CloudControllerResponseErrorHandler());
        restTemplate.setMessageConverters(getHttpMessageConverters());

        return restTemplate;
    }

    private javax.net.ssl.SSLContext buildSslContext() {
        try {
            return new SSLContextBuilder().useSSL()
                .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                .build();
        } catch (GeneralSecurityException gse) {
            throw new RuntimeException("An error occurred setting up the SSLContext", gse);
        }
    }

    private List<HttpMessageConverter<?>> getHttpMessageConverters() {
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
        messageConverters.add(new ByteArrayHttpMessageConverter());
        messageConverters.add(new StringHttpMessageConverter());
        messageConverters.add(new ResourceHttpMessageConverter());
        messageConverters.add(new MappingJackson2HttpMessageConverter());
        messageConverters.add(new LoggregatorHttpMessageConverter());
        return messageConverters;
    }
}
