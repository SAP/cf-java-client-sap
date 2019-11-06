package org.cloudfoundry.client.lib.rest.clients.packages;

import java.util.UUID;

import org.cloudfoundry.client.lib.domain.Upload;

public interface CloudControllerPackagesClient {

    Upload getUploadStatus(UUID packageGuid);
}
