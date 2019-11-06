package org.cloudfoundry.client.lib.rest.clients.spaces;

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchListWithAuxiliaryContent;
import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchMono;
import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetchWithAuxiliaryContent;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.IntFunction;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudOrganization;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudSpace;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.lib.rest.clients.organizations.CloudControllerOrganizationsClient;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.spaces.ListSpaceAuditorsRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceDevelopersRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceManagersRequest;
import org.cloudfoundry.client.v2.users.UserEntity;
import org.cloudfoundry.client.v3.organizations.GetOrganizationRequest;
import org.cloudfoundry.client.v3.organizations.Organization;
import org.cloudfoundry.client.v3.spaces.GetSpaceRequest;
import org.cloudfoundry.client.v3.spaces.GetSpaceResponse;
import org.cloudfoundry.client.v3.spaces.ListSpacesRequest;
import org.cloudfoundry.client.v3.spaces.SpaceResource;
import org.cloudfoundry.util.PaginationUtils;
import org.springframework.http.HttpStatus;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class CloudControllerSpacesClientImpl extends CloudControllerBaseClient implements CloudControllerSpacesClient {

    private CloudControllerOrganizationsClient organizationsClient;

    public CloudControllerSpacesClientImpl(CloudSpace target, CloudFoundryClient v3Client,
                                           CloudControllerOrganizationsClient organizationsClient) {
        super(target, v3Client);
        this.organizationsClient = organizationsClient;
    }

    @Override
    public CloudSpace getSpace(UUID spaceGuid) {
        return fetchWithAuxiliaryContent(() -> getSpaceResource(spaceGuid), this::zipWithAuxiliarySpaceContent);
    }

    @Override
    public CloudSpace getSpace(String organizationName, String spaceName, boolean required) {
        UUID organizationGuid = getOrganizationGuid(organizationName, required);
        return findSpaceByOrganizationGuidAndName(organizationGuid, spaceName, required);
    }

    @Override
    public CloudSpace getSpace(String spaceName, boolean required) {
        UUID organizationGuid = getGuid(target.getOrganization());
        return findSpaceByOrganizationGuidAndName(organizationGuid, spaceName, required);
    }

    @Override
    public List<UUID> getSpaceAuditors(String spaceName) {
        return getSpaceAuditors(getTargetOrganizationName(), spaceName);
    }

    @Override
    public List<UUID> getSpaceAuditors(String organizationName, String spaceName) {
        return findSpaceUsers(organizationName, spaceName, this::getSpaceAuditors);
    }

    @Override
    public List<UUID> getSpaceAuditors() {
        return getSpaceAuditors(getTargetSpaceGuid());
    }

    @Override
    public List<UUID> getSpaceAuditors(UUID spaceGuid) {
        return getGuids(getSpaceAuditorResourcesBySpaceGuid(spaceGuid));
    }

    @Override
    public List<UUID> getSpaceDevelopers(String spaceName) {
        return getSpaceDevelopers(getTargetOrganizationName(), spaceName);
    }

    @Override
    public List<UUID> getSpaceDevelopers(String organizationName, String spaceName) {
        return findSpaceUsers(organizationName, spaceName, this::getSpaceDevelopers);
    }

    @Override
    public List<UUID> getSpaceDevelopers() {
        return getSpaceDevelopers(getTargetSpaceGuid());
    }

    @Override
    public List<UUID> getSpaceDevelopers(UUID spaceGuid) {
        return getGuids(getSpaceDeveloperResourcesBySpaceGuid(spaceGuid));
    }

    @Override
    public List<UUID> getSpaceManagers(String spaceName) {
        return getSpaceManagers(getTargetOrganizationName(), spaceName);
    }

    @Override
    public List<UUID> getSpaceManagers(String organizationName, String spaceName) {
        return findSpaceUsers(organizationName, spaceName, this::getSpaceManagers);
    }

    @Override
    public List<UUID> getSpaceManagers() {
        return getSpaceManagers(getTargetSpaceGuid());
    }

    @Override
    public List<UUID> getSpaceManagers(UUID spaceGuid) {
        return getGuids(getSpaceManagerResourcesBySpaceGuid(spaceGuid));
    }

    @Override
    public List<CloudSpace> getSpaces() {
        return fetchListWithAuxiliaryContent(this::getSpaceResources, this::zipWithAuxiliarySpaceResourceContent);
    }

    @Override
    public List<CloudSpace> getSpaces(String organizationName) {
        UUID organizationGuid = getOrganizationGuid(organizationName, true);
        return findSpacesByOrganizationGuid(organizationGuid);
    }

    private Mono<GetSpaceResponse> getSpaceResource(UUID guid) {
        GetSpaceRequest request = GetSpaceRequest.builder()
                                                 .spaceId(guid.toString())
                                                 .build();

        return v3Client.spacesV3()
                       .get(request);
    }

    private Mono<Derivable<CloudSpace>> zipWithAuxiliarySpaceContent(GetSpaceResponse spaceResponse) {
        UUID organizationGuid = UUID.fromString(spaceResponse.getRelationships()
                                                             .getOrganization()
                                                             .getData()
                                                             .getId());
        return getOrganizationMono(organizationGuid).map(organization -> ImmutableRawCloudSpace.builder()
                                                                                               .resource(spaceResponse)
                                                                                               .organization(organization)
                                                                                               .build());
    }

    private Mono<? extends Derivable<CloudOrganization>> getOrganizationMono(UUID guid) {
        return fetchMono(() -> getOrganizationResource(guid), ImmutableRawCloudOrganization::of);
    }

    private Mono<? extends Organization> getOrganizationResource(UUID guid) {
        GetOrganizationRequest request = GetOrganizationRequest.builder()
                                                               .organizationId(guid.toString())
                                                               .build();
        return v3Client.organizationsV3()
                       .get(request);
    }

    private UUID getOrganizationGuid(String organizationName, boolean required) {
        CloudOrganization organization = organizationsClient.getOrganization(organizationName, required);
        return organization != null ? organization.getMetadata()
                                                  .getGuid()
            : null;
    }

    private CloudSpace findSpaceByOrganizationGuidAndName(UUID organizationGuid, String spaceName, boolean required) {
        CloudSpace space = findSpaceByOrganizationGuidAndName(organizationGuid, spaceName);
        if (space == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND,
                                              "Not Found",
                                              "Space " + spaceName + " not found in organization with GUID " + organizationGuid + ".");
        }
        return space;
    }

    private CloudSpace findSpaceByOrganizationGuidAndName(UUID organizationGuid, String spaceName) {
        return fetchWithAuxiliaryContent(() -> getSpaceResourceByOrganizationGuidAndName(organizationGuid, spaceName),
                                         this::zipWithAuxiliarySpaceResourceContent);
    }

    private Mono<? extends SpaceResource> getSpaceResourceByOrganizationGuidAndName(UUID organizationGuid, String name) {
        IntFunction<ListSpacesRequest> pageRequestSupplier = page -> ListSpacesRequest.builder()
                                                                                      .organizationId(organizationGuid.toString())
                                                                                      .name(name)
                                                                                      .page(page)
                                                                                      .build();
        return PaginationUtils.requestClientV3Resources(page -> v3Client.spacesV3()
                                                                        .list(pageRequestSupplier.apply(page)))
                              .singleOrEmpty();
    }

    private Mono<Derivable<CloudSpace>> zipWithAuxiliarySpaceResourceContent(SpaceResource spaceResource) {
        UUID organizationGuid = UUID.fromString(spaceResource.getRelationships()
                                                             .getOrganization()
                                                             .getData()
                                                             .getId());
        return getOrganizationMono(organizationGuid).map(organization -> ImmutableRawCloudSpace.builder()
                                                                                               .resource(spaceResource)
                                                                                               .organization(organization)
                                                                                               .build());
    }

    private String getTargetOrganizationName() {
        return getName(target.getOrganization());
    }

    private String getName(CloudEntity entity) {
        return entity == null ? null : entity.getName();
    }

    private List<UUID> findSpaceUsers(String organizationName, String spaceName, Function<UUID, List<UUID>> usersRetriever) {
        CloudSpace space = getSpace(organizationName, spaceName, true);
        return usersRetriever.apply(getGuid(space));
    }

    private Flux<? extends Resource<UserEntity>> getSpaceAuditorResourcesBySpaceGuid(UUID spaceGuid) {
        IntFunction<ListSpaceAuditorsRequest> pageRequestSupplier = page -> ListSpaceAuditorsRequest.builder()
                                                                                                    .spaceId(spaceGuid.toString())
                                                                                                    .page(page)
                                                                                                    .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.spaces()
                                                                        .listAuditors(pageRequestSupplier.apply(page)));
    }

    private Flux<? extends Resource<UserEntity>> getSpaceDeveloperResourcesBySpaceGuid(UUID spaceGuid) {
        IntFunction<ListSpaceDevelopersRequest> pageRequestSupplier = page -> ListSpaceDevelopersRequest.builder()
                                                                                                        .spaceId(spaceGuid.toString())
                                                                                                        .page(page)
                                                                                                        .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.spaces()
                                                                        .listDevelopers(pageRequestSupplier.apply(page)));
    }

    private Flux<? extends Resource<UserEntity>> getSpaceManagerResourcesBySpaceGuid(UUID spaceGuid) {
        IntFunction<ListSpaceManagersRequest> pageRequestSupplier = page -> ListSpaceManagersRequest.builder()
                                                                                                    .spaceId(spaceGuid.toString())
                                                                                                    .page(page)
                                                                                                    .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.spaces()
                                                                        .listManagers(pageRequestSupplier.apply(page)));
    }

    private Flux<? extends SpaceResource> getSpaceResources() {
        IntFunction<ListSpacesRequest> pageRequestSupplier = page -> ListSpacesRequest.builder()
                                                                                      .page(page)
                                                                                      .build();
        return getSpaceResources(pageRequestSupplier);
    }

    private Flux<? extends SpaceResource> getSpaceResources(IntFunction<ListSpacesRequest> requestForPage) {
        return PaginationUtils.requestClientV3Resources(page -> v3Client.spacesV3()
                                                                        .list(requestForPage.apply(page)));
    }

    private List<CloudSpace> findSpacesByOrganizationGuid(UUID organizationGuid) {
        return fetchListWithAuxiliaryContent(() -> getSpaceResourcesByOrganizationGuid(organizationGuid),
                                             this::zipWithAuxiliarySpaceResourceContent);
    }

    private Flux<? extends SpaceResource> getSpaceResourcesByOrganizationGuid(UUID organizationGuid) {
        IntFunction<ListSpacesRequest> pageRequestSupplier = page -> ListSpacesRequest.builder()
                                                                                      .organizationId(organizationGuid.toString())
                                                                                      .page(page)
                                                                                      .build();
        return getSpaceResources(pageRequestSupplier);
    }
}
