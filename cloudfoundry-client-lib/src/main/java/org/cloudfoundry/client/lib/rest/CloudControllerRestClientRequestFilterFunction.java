package org.cloudfoundry.client.lib.rest;

import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import reactor.core.publisher.Mono;

public class CloudControllerRestClientRequestFilterFunction implements ExchangeFilterFunction {

    private OAuthClient oAuthClient;

    public CloudControllerRestClientRequestFilterFunction(OAuthClient oAuthClient) {
        this.oAuthClient = oAuthClient;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest clientRequest, ExchangeFunction nextFilter) {
        String authorizationHeader = oAuthClient.getAuthorizationHeader();
        if (authorizationHeader != null) {
            clientRequest.headers()
                         .add(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        return nextFilter.exchange(clientRequest);
    }
}