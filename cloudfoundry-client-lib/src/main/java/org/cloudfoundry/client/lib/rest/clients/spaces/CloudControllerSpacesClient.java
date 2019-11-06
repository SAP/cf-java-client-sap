package org.cloudfoundry.client.lib.rest.clients.spaces;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudSpace;

public interface CloudControllerSpacesClient {

    CloudSpace getSpace(UUID spaceGuid);

    CloudSpace getSpace(String organizationName, String spaceName, boolean required);

    CloudSpace getSpace(String spaceName, boolean required);

    List<UUID> getSpaceAuditors(String spaceName);

    List<UUID> getSpaceAuditors(String organizationName, String spaceName);

    List<UUID> getSpaceAuditors();

    public List<UUID> getSpaceAuditors(UUID spaceGuid);

    public List<UUID> getSpaceDevelopers(String spaceName);

    public List<UUID> getSpaceDevelopers(String organizationName, String spaceName);

    public List<UUID> getSpaceDevelopers();

    public List<UUID> getSpaceDevelopers(UUID spaceGuid);

    public List<UUID> getSpaceManagers(String spaceName);

    public List<UUID> getSpaceManagers(String organizationName, String spaceName);

    public List<UUID> getSpaceManagers();

    public List<UUID> getSpaceManagers(UUID spaceGuid);

    public List<CloudSpace> getSpaces();

    public List<CloudSpace> getSpaces(String organizationName);
}
