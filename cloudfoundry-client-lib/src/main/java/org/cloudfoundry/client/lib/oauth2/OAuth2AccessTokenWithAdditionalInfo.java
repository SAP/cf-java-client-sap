package org.cloudfoundry.client.lib.oauth2;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;

import org.cloudfoundry.client.constants.Constants;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

public class OAuth2AccessTokenWithAdditionalInfo {

    private final OAuth2AccessToken oAuth2AccessToken;
    private final Map<String, Object> additionalInfo;

    public OAuth2AccessTokenWithAdditionalInfo(OAuth2AccessToken oAuth2AccessToken, Map<String, Object> additionalInfo) {
        this.oAuth2AccessToken = oAuth2AccessToken;
        this.additionalInfo = additionalInfo;
    }

    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    public Set<String> getScopes() {
        return oAuth2AccessToken.getScopes();
    }

    public OAuth2AccessToken.TokenType getType() {
        return oAuth2AccessToken.getTokenType();
    }

    public Instant getExpiresAt() {
        return oAuth2AccessToken.getExpiresAt();
    }

    public String getDefaultValue() {
        return oAuth2AccessToken.getTokenValue();
    }

    public String getUserName() {
        return (String) additionalInfo.get("user_name");
    }

    public boolean expiresBefore(Instant instant) {
        return oAuth2AccessToken.getExpiresAt()
                                .isBefore(instant);
    }

    public LocalDateTime calculateExpirationDate() {
        long expirationInSeconds = ((Number) getAdditionalInfo().get("exp")).longValue();
        return Instant.ofEpochSecond(expirationInSeconds)
                      .atZone(ZoneId.systemDefault())
                      .toLocalDateTime();
    }

    public String getValue() {
        String exchangedTokenValue = (String) additionalInfo.get(Constants.EXCHANGED_TOKEN);
        if (exchangedTokenValue != null) {
            return exchangedTokenValue;
        }
        return oAuth2AccessToken.getTokenValue();
    }

}
