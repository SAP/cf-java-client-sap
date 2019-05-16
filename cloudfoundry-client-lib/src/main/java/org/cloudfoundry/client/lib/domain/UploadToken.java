package org.cloudfoundry.client.lib.domain;

import java.util.UUID;

public class UploadToken {

    private String token;
    private UUID packageGuid;

    // Required by Jackson.
    public UploadToken() {
    }

    public UploadToken(String token, UUID packageGuid) {
        this.token = token;
        this.packageGuid = packageGuid;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UUID getPackageGuid() {
        return packageGuid;
    }

    public void setPackageGuid(UUID packageGuid) {
        this.packageGuid = packageGuid;
    }

}
