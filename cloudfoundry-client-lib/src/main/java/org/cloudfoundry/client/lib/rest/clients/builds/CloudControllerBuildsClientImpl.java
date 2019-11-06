package org.cloudfoundry.client.lib.rest.clients.builds;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetch;
import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchList;

import java.util.List;
import java.util.UUID;
import java.util.function.IntFunction;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudBuild;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.applications.ListApplicationBuildsRequest;
import org.cloudfoundry.client.v3.builds.Build;
import org.cloudfoundry.client.v3.builds.CreateBuildRequest;
import org.cloudfoundry.client.v3.builds.GetBuildRequest;
import org.cloudfoundry.client.v3.builds.ListBuildsRequest;
import org.cloudfoundry.util.PaginationUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CloudControllerBuildsClientImpl extends CloudControllerBaseClient implements CloudControllerBuildsClient {

    public CloudControllerBuildsClientImpl(CloudSpace target, CloudFoundryClient v3Client) {
        super(target, v3Client);
    }

    @Override
    public CloudBuild getBuild(UUID buildGuid) {
        return fetch(() -> getBuildResource(buildGuid), ImmutableRawCloudBuild::of);
    }

    @Override
    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid) {
        return fetchList(() -> getBuildResourcesByApplicationGuid(applicationGuid), ImmutableRawCloudBuild::of);
    }

    @Override
    public List<CloudBuild> getBuildsForPackage(UUID packageGuid) {
        return fetchList(() -> getBuildResourcesByPackageGuid(packageGuid), ImmutableRawCloudBuild::of);
    }

    @Override
    public CloudBuild createBuild(UUID packageGuid) {
        return fetch(() -> createBuildResource(packageGuid), ImmutableRawCloudBuild::of);
    }

    private Mono<? extends Build> createBuildResource(UUID packageGuid) {
        CreateBuildRequest request = CreateBuildRequest.builder()
                                                       .getPackage(buildRelationship(packageGuid))
                                                       .build();
        return v3Client.builds()
                       .create(request);
    }

    private Relationship buildRelationship(UUID guid) {
        return Relationship.builder()
                           .id(guid.toString())
                           .build();
    }

    private Mono<? extends Build> getBuildResource(UUID buildGuid) {
        GetBuildRequest request = GetBuildRequest.builder()
                                                 .buildId(buildGuid.toString())
                                                 .build();
        return v3Client.builds()
                       .get(request);
    }

    private Flux<? extends Build> getBuildResourcesByApplicationGuid(UUID applicationGuid) {
        IntFunction<ListApplicationBuildsRequest> pageRequestSupplier = page -> ListApplicationBuildsRequest.builder()
                                                                                                            .applicationId(applicationGuid.toString())
                                                                                                            .page(page)
                                                                                                            .build();
        return PaginationUtils.requestClientV3Resources(page -> v3Client.applicationsV3()
                                                                        .listBuilds(pageRequestSupplier.apply(page)));
    }

    private Flux<? extends Build> getBuildResourcesByPackageGuid(UUID packageGuid) {
        IntFunction<ListBuildsRequest> pageRequestSupplier = page -> ListBuildsRequest.builder()
                                                                                      .packageId(packageGuid.toString())
                                                                                      .page(page)
                                                                                      .build();
        return PaginationUtils.requestClientV3Resources(page -> v3Client.builds()
                                                                        .list(pageRequestSupplier.apply(page)));
    }
}
