package com.sap.cloudfoundry.client.facade.oauth2;

import java.net.URL;

import org.cloudfoundry.reactor.TokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.client.resource.OAuth2AccessDeniedException;
import org.springframework.security.oauth2.client.resource.OAuth2ProtectedResourceDetails;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultAccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordAccessTokenProvider;
import org.springframework.security.oauth2.client.token.grant.password.ResourceOwnerPasswordResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.adapters.OAuthTokenProvider;

/**
 * Client that can handle authentication against a UAA instance
 *
 */
public class OAuthClient {

    private URL authorizationUrl;
    private WebClient webClient;
    protected OAuth2AccessToken token;
    protected CloudCredentials credentials;

    public OAuthClient(URL authorizationUrl, WebClient webClient) {
        this.authorizationUrl = authorizationUrl;
        this.webClient = webClient;
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

    public OAuth2AccessToken getToken() {
        if (token == null) {
            return null;
        }
        if (credentials.isRefreshable() && token.getExpiresIn() < 50) { // 50 seconds before expiration? Then refresh it.
            token = refreshToken();
        }
        return token;
    }

    public String getAuthorizationHeader() {
        OAuth2AccessToken accessToken = getToken();
        if (accessToken != null) {
            return accessToken.getTokenType() + " " + accessToken.getValue();
        }
        return null;
    }

    protected OAuth2AccessToken createToken() {
        OAuth2ProtectedResourceDetails resource = getResourceDetails(credentials.getEmail(), credentials.getPassword(),
                                                                     credentials.getClientId(), credentials.getClientSecret());
        AccessTokenRequest request = createAccessTokenRequest(credentials.getEmail(), credentials.getPassword());

        ResourceOwnerPasswordAccessTokenProvider provider = new ResourceOwnerPasswordAccessTokenProvider();
        try {
            return provider.obtainAccessToken(resource, request);
        } catch (OAuth2AccessDeniedException oauthEx) {
            HttpStatus status = HttpStatus.valueOf(oauthEx.getHttpErrorCode());
            throw new CloudOperationException(status, oauthEx.getMessage(), oauthEx.getSummary());
        }
    }

    protected OAuth2AccessToken refreshToken() {
        OAuth2ProtectedResourceDetails resource = getResourceDetails(credentials.getEmail(), credentials.getPassword(),
                                                                     credentials.getClientId(), credentials.getClientSecret());
        AccessTokenRequest request = createAccessTokenRequest(credentials.getEmail(), credentials.getPassword());

        ResourceOwnerPasswordAccessTokenProvider provider = new ResourceOwnerPasswordAccessTokenProvider();

        return provider.refreshAccessToken(resource, token.getRefreshToken(), request);
    }

    public TokenProvider getTokenProvider() {
        return new OAuthTokenProvider(this);
    }

    private AccessTokenRequest createAccessTokenRequest(String username, String password) {
        return new DefaultAccessTokenRequest();
    }

    private OAuth2ProtectedResourceDetails getResourceDetails(String username, String password, String clientId, String clientSecret) {
        ResourceOwnerPasswordResourceDetails resource = new ResourceOwnerPasswordResourceDetails();
        resource.setUsername(username);
        resource.setPassword(password);

        resource.setClientId(clientId);
        resource.setClientSecret(clientSecret);
        resource.setId(clientId);
        resource.setClientAuthenticationScheme(AuthenticationScheme.header);
        resource.setAccessTokenUri(authorizationUrl + "/oauth/token");

        return resource;
    }
}
