package org.cloudfoundry.client.lib.rest;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

public class CloudControllerRestClientHttpRequestFactory implements ClientHttpRequestFactory {

    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";
    private static final String PROXY_USER_HEADER_KEY = "Proxy-User";

    private Integer defaultSocketTimeout = 0;
    private ClientHttpRequestFactory delegate;
    private CloudCredentials credentials;
    private OAuthClient oAuthClient;

    public CloudControllerRestClientHttpRequestFactory(ClientHttpRequestFactory delegate, CloudCredentials credentials,
                                                       OAuthClient oAuthClient) {
        this.delegate = delegate;
        this.credentials = credentials;
        this.oAuthClient = oAuthClient;
        captureDefaultReadTimeout();
    }

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
        ClientHttpRequest request = delegate.createRequest(uri, httpMethod);

        String authorizationHeader = oAuthClient.getAuthorizationHeader();
        if (authorizationHeader != null) {
            request.getHeaders()
                   .add(AUTHORIZATION_HEADER_KEY, authorizationHeader);
        }

        if (credentials != null && credentials.getProxyUser() != null) {
            request.getHeaders()
                   .add(PROXY_USER_HEADER_KEY, credentials.getProxyUser());
        }

        return request;
    }

    public void increaseReadTimeoutForStreamedTailedLogs(int timeout) {
        // May temporary increase read timeout on other unrelated concurrent
        // threads, but per-request read timeout don't seem easily
        // accessible
        if (delegate instanceof HttpComponentsClientHttpRequestFactory) {
            HttpComponentsClientHttpRequestFactory httpRequestFactory = (HttpComponentsClientHttpRequestFactory) delegate;

            if (timeout > 0) {
                httpRequestFactory.setReadTimeout(timeout);
            } else {
                httpRequestFactory.setReadTimeout(defaultSocketTimeout);
            }
        }
    }

    private void captureDefaultReadTimeout() {
        // As of HttpClient 4.3.x, obtaining the default parameters is deprecated and removed,
        // so we fallback to java.net.Socket.

        if (defaultSocketTimeout == null) {
            try {
                defaultSocketTimeout = new Socket().getSoTimeout();
            } catch (SocketException e) {
                defaultSocketTimeout = 0;
            }
        }
    }
}