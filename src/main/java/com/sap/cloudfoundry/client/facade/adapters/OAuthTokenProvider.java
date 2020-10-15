package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;

import reactor.core.publisher.Mono;

public class OAuthTokenProvider implements TokenProvider {

    private OAuthClient oAuthClient;

    public OAuthTokenProvider(OAuthClient oAuthClient) {
        this.oAuthClient = oAuthClient;
    }

    @Override
    public Mono<String> getToken(ConnectionContext connectionContext) {
        return Mono.fromSupplier(() -> {
            OAuth2AccessToken token = oAuthClient.getToken();
            return token.getTokenType() + " " + token.getValue();
        });
    }

}
