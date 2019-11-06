package org.cloudfoundry.client.lib.rest.clients.apps;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;

public interface CloudControllerApplicationsUploadClient {

    UploadToken async(String applicationName, File file, UploadStatusCallback callback) throws IOException;

    Upload status(UUID packageGuid);
    
    void sync(String applicationName, File file, UploadStatusCallback callback) throws IOException;
    
    void sync(String applicationName, InputStream inputStream, UploadStatusCallback callback) throws IOException;

}
