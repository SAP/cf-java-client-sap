package org.cloudfoundry.client.lib.rest.clients.apps.impl;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudPackage;
import org.cloudfoundry.client.lib.domain.CloudPackage;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.ErrorDetails;
import org.cloudfoundry.client.lib.domain.ImmutableErrorDetails;
import org.cloudfoundry.client.lib.domain.ImmutableUpload;
import org.cloudfoundry.client.lib.domain.ImmutableUploadToken;
import org.cloudfoundry.client.lib.domain.Status;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsUploadClient;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.packages.CreatePackageRequest;
import org.cloudfoundry.client.v3.packages.GetPackageRequest;
import org.cloudfoundry.client.v3.packages.PackageRelationships;
import org.cloudfoundry.client.v3.packages.PackageType;
import org.cloudfoundry.client.v3.packages.UploadPackageRequest;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import reactor.core.publisher.Mono;

public class CloudControllerApplicationsUploadClientImpl extends CloudControllerBaseClient
    implements CloudControllerApplicationsUploadClient {

    private static final long JOB_POLLING_PERIOD = TimeUnit.SECONDS.toMillis(5);
    private CloudControllerApplicationsClient applicationsClient;

    protected CloudControllerApplicationsUploadClientImpl(CloudSpace target, CloudFoundryClient v3Client,
                                                          CloudControllerApplicationsClient applicationsClient) {
        super(target, v3Client);
        this.applicationsClient = applicationsClient;
    }

    @Override
    public UploadToken async(String applicationName, File file, UploadStatusCallback callback) throws IOException {
        UploadToken uploadToken = startUpload(applicationName, file);
        processAsyncUploadInBackground(uploadToken, callback);
        return uploadToken;
    }

    @Override
    public Upload status(UUID packageGuid) {
        CloudPackage cloudPackage = findPackage(packageGuid);
        ErrorDetails errorDetails = ImmutableErrorDetails.builder()
                                                         .description(cloudPackage.getData()
                                                                                  .getError())
                                                         .build();

        return ImmutableUpload.builder()
                              .status(cloudPackage.getStatus())
                              .errorDetails(errorDetails)
                              .build();
    }

    @Override
    public void sync(String applicationName, InputStream inputStream, UploadStatusCallback callback) throws IOException {
        Assert.notNull(inputStream, "InputStream must not be null");

        File file = null;
        try {
            file = createTemporaryUploadFile(inputStream);
            sync(applicationName, file, callback);
        } finally {
            IOUtils.closeQuietly(inputStream);
            if (file != null) {
                file.delete();
            }
        }
    }

    @Override
    public void sync(String applicationName, File file, UploadStatusCallback callback) throws IOException {
        UploadToken uploadToken = startUpload(applicationName, file);
        processAsyncUpload(uploadToken, callback);
    }

    private void processAsyncUploadInBackground(UploadToken uploadToken, UploadStatusCallback callback) {
        String threadName = String.format("App upload monitor: %s", uploadToken.getPackageGuid());
        new Thread(() -> processAsyncUpload(uploadToken, callback), threadName).start();
    }

    private CloudPackage findPackage(UUID guid) {
        return fetch(() -> getPackageResource(guid), ImmutableRawCloudPackage::of);
    }

    private Mono<? extends org.cloudfoundry.client.v3.packages.Package> getPackageResource(UUID guid) {
        GetPackageRequest request = GetPackageRequest.builder()
                                                     .packageId(guid.toString())
                                                     .build();
        return v3Client.packages()
                       .get(request);
    }

    private UploadToken startUpload(String applicationName, File file) {
        Assert.notNull(applicationName, "AppName must not be null");
        Assert.notNull(file, "File must not be null");

        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UUID packageGuid = getGuid(createPackageForApplication(applicationGuid));

        v3Client.packages()
                .upload(UploadPackageRequest.builder()
                                            .bits(file.toPath())
                                            .packageId(packageGuid.toString())
                                            .build())
                .block();

        return ImmutableUploadToken.builder()
                                   .packageGuid(packageGuid)
                                   .build();
    }

    private void processAsyncUpload(UploadToken uploadToken, UploadStatusCallback callback) {
        if (callback == null) {
            callback = UploadStatusCallback.NONE;
        }
        while (true) {
            Upload upload = status(uploadToken.getPackageGuid());
            boolean unsubscribe = callback.onProgress(upload.getStatus()
                                                            .toString());
            if (unsubscribe || upload.getStatus() == Status.READY) {
                return;
            }
            if (upload.getStatus() == Status.EXPIRED) {
                callback.onError(upload.getErrorDetails()
                                       .getDescription());
                return;
            }

            try {
                Thread.sleep(JOB_POLLING_PERIOD);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private UUID getRequiredApplicationGuid(String name) {
        return getGuid(applicationsClient.findApplicationByName(name, true));
    }

    private CloudPackage createPackageForApplication(UUID applicationGuid) {
        return fetch(() -> createPackageResource(applicationGuid), ImmutableRawCloudPackage::of);
    }

    private Mono<? extends org.cloudfoundry.client.v3.packages.Package> createPackageResource(UUID applicationGuid) {
        CreatePackageRequest request = CreatePackageRequest.builder()
                                                           .type(PackageType.BITS)
                                                           .relationships(buildPackageRelationships(applicationGuid))
                                                           .build();
        return v3Client.packages()
                       .create(request);
    }

    private PackageRelationships buildPackageRelationships(UUID applicationGuid) {
        return PackageRelationships.builder()
                                   .application(buildToOneRelationship(applicationGuid))
                                   .build();
    }

    private ToOneRelationship buildToOneRelationship(UUID guid) {
        return ToOneRelationship.builder()
                                .data(buildRelationship(guid))
                                .build();
    }

    private Relationship buildRelationship(UUID guid) {
        return Relationship.builder()
                           .id(guid.toString())
                           .build();
    }

    private File createTemporaryUploadFile(InputStream inputStream) throws IOException {
        File file = File.createTempFile("cfjava", null);
        FileOutputStream outputStream = new FileOutputStream(file);
        FileCopyUtils.copy(inputStream, outputStream);
        outputStream.close();
        return file;
    }
}
