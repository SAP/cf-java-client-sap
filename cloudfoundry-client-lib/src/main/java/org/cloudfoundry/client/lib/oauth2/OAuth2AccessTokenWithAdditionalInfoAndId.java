package org.cloudfoundry.client.lib.oauth2;

public class OAuth2AccessTokenWithAdditionalInfoAndId {

    private final long id;
    private final OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo;

    public OAuth2AccessTokenWithAdditionalInfoAndId(long id, OAuth2AccessTokenWithAdditionalInfo oAuth2AccessTokenWithAdditionalInfo) {
        this.id = id;
        this.oAuth2AccessTokenWithAdditionalInfo = oAuth2AccessTokenWithAdditionalInfo;
    }

    public long getId() {
        return id;
    }

    public OAuth2AccessTokenWithAdditionalInfo getOAuth2AccessTokenWithAdditionalInfo() {
        return oAuth2AccessTokenWithAdditionalInfo;
    }
}
