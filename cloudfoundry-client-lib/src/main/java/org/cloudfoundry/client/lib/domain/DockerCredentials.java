package org.cloudfoundry.client.lib.domain;

import java.util.Objects;

public class DockerCredentials {

    private String username;
    private String password;

    // Required by Jackson.
    public DockerCredentials() {
    }

    public DockerCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DockerCredentials)) {
            return false;
        }
        DockerCredentials dockerCredentials = (DockerCredentials) obj;

        return Objects.equals(username, dockerCredentials.username) && Objects.equals(password, dockerCredentials.password);
    }

}
