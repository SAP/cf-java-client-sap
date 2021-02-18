package com.sap.cloudfoundry.client.facade.oauth2;

import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.cloudfoundry.reactor.TokenProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.adapters.OAuthTokenProvider;

/**
 * Client that can handle authentication against a UAA instance
 *
 */
public class OAuthClient {

    private final URL authorizationUrl;
    protected OAuth2AccessTokenWithAdditionalInfo token;
    protected CloudCredentials credentials;
    protected final WebClient webClient;
    protected final TokenFactory tokenFactory;

    public OAuthClient(URL authorizationUrl, WebClient webClient) {
        this.authorizationUrl = authorizationUrl;
        this.webClient = webClient;
        this.tokenFactory = new TokenFactory();
    }

    public void init(CloudCredentials credentials) {
        if (credentials != null) {
            this.credentials = credentials;
            if (credentials.getToken() != null) {
                this.token = credentials.getToken();
            } else {
                this.token = createToken();
            }
        }
    }

    public void clear() {
        this.token = null;
        this.credentials = null;
    }

    public OAuth2AccessTokenWithAdditionalInfo getToken() {
        if (token == null) {
            return null;
        }
        if (shouldRefreshToken()) {
            token = createToken();
        }
        return token;
    }

    public String getAuthorizationHeaderValue() {
        OAuth2AccessTokenWithAdditionalInfo accessToken = getToken();
        if (accessToken != null) {
            return accessToken.getAuthorizationHeaderValue();
        }
        return null;
    }

    public TokenProvider getTokenProvider() {
        return new OAuthTokenProvider(this);
    }

    private boolean shouldRefreshToken() {
        return credentials.isRefreshable() && token.getOAuth2AccessToken()
                                                   .getExpiresAt()
                                                   .isBefore(Instant.now()
                                                                    .plus(50, ChronoUnit.SECONDS));
    }

    protected OAuth2AccessTokenWithAdditionalInfo createToken() {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", credentials.getClientId());
        formData.add("client_secret", credentials.getClientSecret());
        formData.add("username", credentials.getEmail());
        formData.add("password", credentials.getPassword());
        formData.add("response_type", "token");
        Oauth2AccessTokenResponse oauth2AccessTokenResponse = fetchOauth2AccessToken(formData);
        return tokenFactory.createToken(oauth2AccessTokenResponse);
    }

    private Oauth2AccessTokenResponse fetchOauth2AccessToken(MultiValueMap<String, String> formData) {
        try {
            return webClient.post()
                            .uri(authorizationUrl + "/oauth/token")
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                            .body(BodyInserters.fromFormData(formData))
                            .retrieve()
                            .bodyToFlux(Oauth2AccessTokenResponse.class)
                            .blockFirst();
        } catch (WebClientResponseException e) {
            throw new ResponseStatusException(e.getStatusCode(), e.getMessage(), e);
        }
    }

}
