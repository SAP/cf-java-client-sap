package org.cloudfoundry.client.lib.domain;

import java.util.Objects;

public class DockerInfo {

    private String image;

    private DockerCredentials dockerCredentials;

    public DockerInfo(String image) {
        this.image = image;
    }

    public String getImage() {
        return image;
    }

    public DockerCredentials getDockerCredentials() {
        return dockerCredentials;
    }

    public void setDockerCredentials(DockerCredentials dockerCredentials) {
        this.dockerCredentials = dockerCredentials;
    }

    @Override
    public int hashCode() {
        return Objects.hash(image, dockerCredentials);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DockerInfo)) {
            return false;
        }
        DockerInfo dockerInfo = (DockerInfo) obj;

        return Objects.equals(image, dockerInfo.image) && Objects.equals(dockerCredentials, dockerInfo.dockerCredentials);
    }

}
