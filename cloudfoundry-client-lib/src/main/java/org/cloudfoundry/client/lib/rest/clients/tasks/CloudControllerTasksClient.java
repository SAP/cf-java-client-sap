package org.cloudfoundry.client.lib.rest.clients.tasks;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudTask;

public interface CloudControllerTasksClient {
    CloudTask getTask(UUID taskGuid);

    List<CloudTask> getTasks(String applicationName);

    CloudTask runTask(String applicationName, CloudTask task);

    CloudTask cancelTask(UUID taskGuid);

}
