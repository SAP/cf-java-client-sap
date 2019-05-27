package org.cloudfoundry.client.lib.oauth2;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.lib.util.JsonUtil;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.RestTemplate;

public class OAuthClientWithLoginHint extends OAuthClient {

    private static final String ORIGIN_KEY = "origin";

    private ConnectionContext connectionContext;
    private TokenProvider tokenProvider;
    private Map<String, String> loginHintMap;

    public OAuthClientWithLoginHint(URL authorizationUrl, RestTemplate restTemplate, ConnectionContext connectionContext, String origin) {
        super(authorizationUrl, restTemplate);
        this.connectionContext = connectionContext;
        this.loginHintMap = new HashMap<>();
        loginHintMap.put(ORIGIN_KEY, origin);
    }

    @Override
    protected OAuth2AccessToken createToken() {
        return getOrRefreshToken();
    }

    @Override
    protected OAuth2AccessToken refreshToken() {
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

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        throw new UnsupportedOperationException();
    }

    private OAuth2AccessToken getOrRefreshToken() {
        getTokenProvider();
        String token = tokenProvider.getToken(connectionContext)
            .block();
        return new DefaultOAuth2AccessToken(getTokenValue(token));
    }

    private String getTokenValue(String token) {
        // DefaultOAuth2AccessToken constructor requires a string containing only the token value,
        // but the tokenProvider returns the token with token type included in the string
        String[] split = token.split(" ");
        return split[split.length - 1];
    }

}
