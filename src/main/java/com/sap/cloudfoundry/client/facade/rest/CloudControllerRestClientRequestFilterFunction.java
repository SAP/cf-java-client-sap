package com.sap.cloudfoundry.client.facade.rest;

import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;

import reactor.core.publisher.Mono;

public class CloudControllerRestClientRequestFilterFunction implements ExchangeFilterFunction {

    private final OAuthClient oAuthClient;

    public CloudControllerRestClientRequestFilterFunction(OAuthClient oAuthClient) {
        this.oAuthClient = oAuthClient;
    }

    @Override
    public Mono<ClientResponse> filter(ClientRequest clientRequest, ExchangeFunction nextFilter) {
        String authorizationHeaderValue = oAuthClient.getAuthorizationHeaderValue();
        if (authorizationHeaderValue != null) {
            clientRequest.headers()
                         .add(HttpHeaders.AUTHORIZATION, authorizationHeaderValue);
        }
        return nextFilter.exchange(clientRequest);
    }
}