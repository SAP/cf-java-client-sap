package com.sap.cloudfoundry.client.facade.oauth2;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cloudfoundry.client.facade.util.JsonUtil;

public class OAuthClientWithLoginHint extends OAuthClient {

    private static final String ORIGIN_KEY = "origin";

    private ConnectionContext connectionContext;
    private TokenProvider tokenProvider;
    private Map<String, String> loginHintMap;

    public OAuthClientWithLoginHint(URL authorizationUrl, ConnectionContext connectionContext, String origin, WebClient webClient) {
        super(authorizationUrl, webClient);
        this.connectionContext = connectionContext;
        this.loginHintMap = new HashMap<>();
        loginHintMap.put(ORIGIN_KEY, origin);
    }

    @Override
    protected OAuth2AccessTokenWithAdditionalInfo createToken() {
        return getOrRefreshToken();
    }

    @Override
    public TokenProvider getTokenProvider() {
        if (tokenProvider == null) {
            tokenProvider = createTokenProvider();
        }
        return tokenProvider;
    }

    private TokenProvider createTokenProvider() {
        String loginHintAsJson = JsonUtil.convertToJson(loginHintMap);
        return PasswordGrantTokenProvider.builder()
                                         .clientId(credentials.getClientId())
                                         .clientSecret(credentials.getClientSecret())
                                         .username(credentials.getEmail())
                                         .password(credentials.getPassword())
                                         .loginHint(loginHintAsJson)
                                         .build();
    }

    private OAuth2AccessTokenWithAdditionalInfo getOrRefreshToken() {
        String token = getTokenProvider().getToken(connectionContext)
                                         .block();
        return new TokenFactory().createToken(getTokenValue(token));
    }

    private String getTokenValue(String token) {
        // DefaultOAuth2AccessToken constructor requires a string containing only the token value,
        // but the tokenProvider returns the token with token type included in the string
        String[] split = token.split(" ");
        return split[split.length - 1];
    }

}
