package org.cloudfoundry.client.lib.rest.clients.logs;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.domain.ApplicationLog;

public interface CloudControllerLogsClient {

    List<ApplicationLog> getRecentLogs(String applicationName);

    List<ApplicationLog> getRecentLogs(UUID applicationGuid);

    /**
     * Returns null if no further content is available. Two errors that will lead to a null value are 404 Bad Request errors, which are
     * handled in the implementation, meaning that no further log file contents are available, or ResourceAccessException, also handled in
     * the implementation, indicating a possible timeout in the server serving the content. Note that any other
     * {@link org.cloudfoundry.client.lib.CloudOperationException}s not related to the two errors mentioned above may still be thrown (e.g.
     * 500 level errors, Unauthorized or Forbidden ptions, etc..)
     *
     * @return content if available, which may contain multiple lines, or null if no further content is available.
     */
    String getStagingLogs(StartingInfo info, int offset);

}
