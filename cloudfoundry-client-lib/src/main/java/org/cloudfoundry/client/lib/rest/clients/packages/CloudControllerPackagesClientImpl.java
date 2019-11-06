package org.cloudfoundry.client.lib.rest.clients.packages;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetch;

import java.util.UUID;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudPackage;
import org.cloudfoundry.client.lib.domain.CloudPackage;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.ErrorDetails;
import org.cloudfoundry.client.lib.domain.ImmutableErrorDetails;
import org.cloudfoundry.client.lib.domain.ImmutableUpload;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.v3.packages.GetPackageRequest;

import reactor.core.publisher.Mono;

public class CloudControllerPackagesClientImpl extends CloudControllerBaseClient implements CloudControllerPackagesClient {

    public CloudControllerPackagesClientImpl(CloudSpace target, CloudFoundryClient v3Client) {
        super(target, v3Client);
    }

    @Override
    public Upload getUploadStatus(UUID packageGuid) {
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
}
