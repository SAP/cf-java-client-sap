package com.sap.cloudfoundry.client.facade.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.sap.cloudfoundry.client.facade.oauth2.OAuth2AccessTokenWithAdditionalInfo;
import org.apache.commons.io.IOUtils;
import org.cloudfoundry.AbstractCloudFoundryException;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesResponse;
import org.cloudfoundry.client.v2.applications.AssociateApplicationRouteRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationServiceBindingsRequest;
import org.cloudfoundry.client.v2.applications.RemoveApplicationRouteRequest;
import org.cloudfoundry.client.v2.applications.SummaryApplicationRequest;
import org.cloudfoundry.client.v2.applications.SummaryApplicationResponse;
import org.cloudfoundry.client.v2.events.EventEntity;
import org.cloudfoundry.client.v2.events.ListEventsRequest;
import org.cloudfoundry.client.v2.routemappings.ListRouteMappingsRequest;
import org.cloudfoundry.client.v2.routemappings.RouteMappingEntity;
import org.cloudfoundry.client.v2.routes.CreateRouteRequest;
import org.cloudfoundry.client.v2.routes.CreateRouteResponse;
import org.cloudfoundry.client.v2.routes.DeleteRouteRequest;
import org.cloudfoundry.client.v2.routes.RouteEntity;
import org.cloudfoundry.client.v2.servicebindings.CreateServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.DeleteServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.GetServiceBindingParametersRequest;
import org.cloudfoundry.client.v2.servicebindings.GetServiceBindingParametersResponse;
import org.cloudfoundry.client.v2.servicebindings.ListServiceBindingsRequest;
import org.cloudfoundry.client.v2.servicebindings.ServiceBindingEntity;
import org.cloudfoundry.client.v2.servicebrokers.CreateServiceBrokerRequest;
import org.cloudfoundry.client.v2.servicebrokers.DeleteServiceBrokerRequest;
import org.cloudfoundry.client.v2.servicebrokers.ListServiceBrokersRequest;
import org.cloudfoundry.client.v2.servicebrokers.ServiceBrokerEntity;
import org.cloudfoundry.client.v2.servicebrokers.UpdateServiceBrokerRequest;
import org.cloudfoundry.client.v2.serviceinstances.CreateServiceInstanceRequest;
import org.cloudfoundry.client.v2.serviceinstances.DeleteServiceInstanceRequest;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceParametersRequest;
import org.cloudfoundry.client.v2.serviceinstances.GetServiceInstanceParametersResponse;
import org.cloudfoundry.client.v2.serviceinstances.UnionServiceInstanceEntity;
import org.cloudfoundry.client.v2.servicekeys.CreateServiceKeyRequest;
import org.cloudfoundry.client.v2.servicekeys.DeleteServiceKeyRequest;
import org.cloudfoundry.client.v2.servicekeys.ListServiceKeysRequest;
import org.cloudfoundry.client.v2.servicekeys.ServiceKeyEntity;
import org.cloudfoundry.client.v2.serviceplans.GetServicePlanRequest;
import org.cloudfoundry.client.v2.serviceplans.ListServicePlansRequest;
import org.cloudfoundry.client.v2.serviceplans.ServicePlanEntity;
import org.cloudfoundry.client.v2.serviceplans.UpdateServicePlanRequest;
import org.cloudfoundry.client.v2.services.GetServiceRequest;
import org.cloudfoundry.client.v2.services.ServiceEntity;
import org.cloudfoundry.client.v2.spaces.ListSpaceRoutesRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceServiceInstancesRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceServicesRequest;
import org.cloudfoundry.client.v2.stacks.GetStackRequest;
import org.cloudfoundry.client.v2.stacks.ListStacksRequest;
import org.cloudfoundry.client.v2.stacks.StackEntity;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.CreateUserProvidedServiceInstanceRequest;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.UpdateUserProvidedServiceInstanceRequest;
import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.DockerData;
import org.cloudfoundry.client.v3.Lifecycle;
import org.cloudfoundry.client.v3.LifecycleType;
import org.cloudfoundry.client.v3.Metadata;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.applications.Application;
import org.cloudfoundry.client.v3.applications.ApplicationRelationships;
import org.cloudfoundry.client.v3.applications.ApplicationResource;
import org.cloudfoundry.client.v3.applications.ApplicationState;
import org.cloudfoundry.client.v3.applications.CreateApplicationRequest;
import org.cloudfoundry.client.v3.applications.CreateApplicationResponse;
import org.cloudfoundry.client.v3.applications.DeleteApplicationRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationCurrentDropletRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationCurrentDropletResponse;
import org.cloudfoundry.client.v3.applications.GetApplicationEnvironmentRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationEnvironmentResponse;
import org.cloudfoundry.client.v3.applications.GetApplicationProcessRequest;
import org.cloudfoundry.client.v3.applications.GetApplicationProcessResponse;
import org.cloudfoundry.client.v3.applications.GetApplicationRequest;
import org.cloudfoundry.client.v3.applications.ListApplicationBuildsRequest;
import org.cloudfoundry.client.v3.applications.ListApplicationPackagesRequest;
import org.cloudfoundry.client.v3.applications.ListApplicationsRequest;
import org.cloudfoundry.client.v3.applications.ScaleApplicationRequest;
import org.cloudfoundry.client.v3.applications.SetApplicationCurrentDropletRequest;
import org.cloudfoundry.client.v3.applications.StartApplicationRequest;
import org.cloudfoundry.client.v3.applications.StopApplicationRequest;
import org.cloudfoundry.client.v3.applications.UpdateApplicationEnvironmentVariablesRequest;
import org.cloudfoundry.client.v3.applications.UpdateApplicationFeatureRequest;
import org.cloudfoundry.client.v3.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v3.auditevents.AuditEventResource;
import org.cloudfoundry.client.v3.auditevents.ListAuditEventsRequest;
import org.cloudfoundry.client.v3.builds.Build;
import org.cloudfoundry.client.v3.builds.CreateBuildRequest;
import org.cloudfoundry.client.v3.builds.GetBuildRequest;
import org.cloudfoundry.client.v3.builds.ListBuildsRequest;
import org.cloudfoundry.client.v3.domains.CreateDomainRequest;
import org.cloudfoundry.client.v3.domains.DeleteDomainRequest;
import org.cloudfoundry.client.v3.domains.Domain;
import org.cloudfoundry.client.v3.domains.DomainRelationships;
import org.cloudfoundry.client.v3.domains.DomainResource;
import org.cloudfoundry.client.v3.domains.ListDomainsRequest;
import org.cloudfoundry.client.v3.organizations.GetOrganizationDefaultDomainRequest;
import org.cloudfoundry.client.v3.organizations.ListOrganizationDomainsRequest;
import org.cloudfoundry.client.v3.organizations.GetOrganizationRequest;
import org.cloudfoundry.client.v3.organizations.ListOrganizationsRequest;
import org.cloudfoundry.client.v3.organizations.Organization;
import org.cloudfoundry.client.v3.organizations.OrganizationResource;
import org.cloudfoundry.client.v3.packages.CreatePackageRequest;
import org.cloudfoundry.client.v3.packages.CreatePackageResponse;
import org.cloudfoundry.client.v3.packages.GetPackageRequest;
import org.cloudfoundry.client.v3.packages.PackageRelationships;
import org.cloudfoundry.client.v3.packages.PackageResource;
import org.cloudfoundry.client.v3.packages.PackageType;
import org.cloudfoundry.client.v3.packages.UploadPackageRequest;
import org.cloudfoundry.client.v3.processes.Data;
import org.cloudfoundry.client.v3.processes.HealthCheck;
import org.cloudfoundry.client.v3.processes.HealthCheckType;
import org.cloudfoundry.client.v3.processes.UpdateProcessRequest;
import org.cloudfoundry.client.v3.roles.ListRolesRequest;
import org.cloudfoundry.client.v3.roles.RoleResource;
import org.cloudfoundry.client.v3.serviceInstances.ListServiceInstancesRequest;
import org.cloudfoundry.client.v3.serviceInstances.ServiceInstanceResource;
import org.cloudfoundry.client.v3.serviceInstances.UpdateServiceInstanceRequest;
import org.cloudfoundry.client.v3.spaces.GetSpaceRequest;
import org.cloudfoundry.client.v3.spaces.ListSpacesRequest;
import org.cloudfoundry.client.v3.spaces.Space;
import org.cloudfoundry.client.v3.spaces.SpaceResource;
import org.cloudfoundry.client.v3.tasks.CancelTaskRequest;
import org.cloudfoundry.client.v3.tasks.CreateTaskRequest;
import org.cloudfoundry.client.v3.tasks.GetTaskRequest;
import org.cloudfoundry.client.v3.tasks.ListTasksRequest;
import org.cloudfoundry.client.v3.tasks.Task;
import org.cloudfoundry.doppler.DopplerClient;
import org.cloudfoundry.doppler.RecentLogsRequest;
import org.cloudfoundry.util.PaginationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.CloudOperationException;
import com.sap.cloudfoundry.client.facade.Constants;
import com.sap.cloudfoundry.client.facade.UploadStatusCallback;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawApplicationLog;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudApplication;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudBuild;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudDomain;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudEvent;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudOrganization;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudPackage;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudRoute;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudServiceBinding;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudServiceBroker;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudServiceInstance;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudServiceKey;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudServiceOffering;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudServicePlan;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudSpace;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudStack;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawCloudTask;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawInstancesInfo;
import com.sap.cloudfoundry.client.facade.adapters.ImmutableRawUserRole;
import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudBuild;
import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.CloudEntity;
import com.sap.cloudfoundry.client.facade.domain.CloudEvent;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.CloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBroker;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceOffering;
import com.sap.cloudfoundry.client.facade.domain.CloudServicePlan;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.CloudStack;
import com.sap.cloudfoundry.client.facade.domain.CloudTask;
import com.sap.cloudfoundry.client.facade.domain.Derivable;
import com.sap.cloudfoundry.client.facade.domain.DockerCredentials;
import com.sap.cloudfoundry.client.facade.domain.DockerInfo;
import com.sap.cloudfoundry.client.facade.domain.DropletInfo;
import com.sap.cloudfoundry.client.facade.domain.ErrorDetails;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.ImmutableDropletInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableErrorDetails;
import com.sap.cloudfoundry.client.facade.domain.ImmutableInstancesInfo;
import com.sap.cloudfoundry.client.facade.domain.ImmutableUpload;
import com.sap.cloudfoundry.client.facade.domain.InstancesInfo;
import com.sap.cloudfoundry.client.facade.domain.ServiceInstanceType;
import com.sap.cloudfoundry.client.facade.domain.Staging;
import com.sap.cloudfoundry.client.facade.domain.Status;
import com.sap.cloudfoundry.client.facade.domain.Upload;
import com.sap.cloudfoundry.client.facade.domain.UserRole;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;
import com.sap.cloudfoundry.client.facade.util.EnvironmentUtil;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * Abstract implementation of the CloudControllerClient intended to serve as the base.
 *
 */
public class CloudControllerRestClientImpl implements CloudControllerRestClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudControllerRestClientImpl.class);
    private static final long JOB_POLLING_PERIOD = TimeUnit.SECONDS.toMillis(5);
    private static final int MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST = 4000;

    private CloudCredentials credentials;
    private URL controllerUrl;
    private OAuthClient oAuthClient;
    private WebClient webClient;
    private CloudSpace target;

    private CloudFoundryClient delegate;
    private DopplerClient dopplerClient;

    /**
     * Only for unit tests. This works around the fact that the initialize method is called within the constructor and hence can not be
     * overloaded, making it impossible to write unit tests that don't trigger network calls.
     */
    protected CloudControllerRestClientImpl() {
    }

    public CloudControllerRestClientImpl(URL controllerUrl, CloudCredentials credentials, WebClient webClient, OAuthClient oAuthClient,
                                         CloudFoundryClient delegate) {
        this(controllerUrl, credentials, webClient, oAuthClient, delegate, null, null);
    }

    public CloudControllerRestClientImpl(URL controllerUrl, CloudCredentials credentials, WebClient webClient, OAuthClient oAuthClient,
                                         CloudFoundryClient delegate, DopplerClient dopplerClient, CloudSpace target) {
        Assert.notNull(controllerUrl, "CloudControllerUrl cannot be null");
        Assert.notNull(webClient, "WebClient cannot be null");
        Assert.notNull(oAuthClient, "OAuthClient cannot be null");

        this.controllerUrl = controllerUrl;
        this.credentials = credentials;
        this.webClient = webClient;
        this.oAuthClient = oAuthClient;
        this.target = target;

        this.delegate = delegate;
        this.dopplerClient = dopplerClient;
    }

    @Override
    public WebClient getWebClient() {
        return webClient;
    }

    @Override
    public OAuthClient getOAuthClient() {
        return oAuthClient;
    }

    @Override
    public URL getControllerUrl() {
        return controllerUrl;
    }

    @Override
    public void addDomain(String domainName) {
        assertSpaceProvided("add domain");
        CloudDomain domain = findDomainByName(domainName);
        if (domain == null) {
            doCreateDomain(domainName);
        }
    }

    @Override
    public void addRoute(String host, String domainName, String path) {
        assertSpaceProvided("add route for domain");
        UUID domainGuid = getRequiredDomainGuid(domainName);
        doAddRoute(domainGuid, host, path);
    }

    @Override
    public void bindServiceInstance(String applicationName, String serviceInstanceName) {
        bindServiceInstance(applicationName, serviceInstanceName, null);
    }

    @Override
    public void bindServiceInstance(String applicationName, String serviceInstanceName, Map<String, Object> parameters) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UUID serviceInstanceGuid = getRequiredServiceInstanceGuid(serviceInstanceName);
        delegate.serviceBindingsV2()
                .create(CreateServiceBindingRequest.builder()
                                                   .applicationId(applicationGuid.toString())
                                                   .serviceInstanceId(serviceInstanceGuid.toString())
                                                   .parameters(parameters)
                                                   .build())
                .block();
    }

    @Override
    public void createApplication(String name, Staging staging, Integer disk, Integer memory, Set<CloudRouteSummary> routes) {
        CreateApplicationRequest applicationRequest = createApplicationRequestBuilder(name).lifecycle(buildApplicationLifecycle(staging))
                                                                                           .build();
        doCreateApplication(staging, disk, memory, routes, applicationRequest);
    }

    private Lifecycle buildApplicationLifecycle(Staging staging) {
        return staging.getDockerInfo() != null ? createDockerLifecycle() : createBuildpackLifecycle(staging);
    }

    private void doCreateApplication(Staging staging, Integer disk, Integer memory, Set<CloudRouteSummary> routes,
                                     CreateApplicationRequest applicationRequest) {
        CreateApplicationResponse createApplicationResponse = delegate.applicationsV3()
                                                                      .create(applicationRequest)
                                                                      .block();
        updateApplicationAttributes(staging, disk, memory, routes, createApplicationResponse);
    }

    private CreateApplicationRequest.Builder createApplicationRequestBuilder(String name) {
        return CreateApplicationRequest.builder()
                                       .name(name)
                                       .relationships(buildApplicationRelationships());
    }

    private BuildpackData createBuildpackData(Staging staging) {
        BuildpackData.Builder buildpackDataBuilder = BuildpackData.builder()
                                                                  .stack(staging.getStack());
        if (staging.getBuildpack() != null) {
            buildpackDataBuilder.buildpack(staging.getBuildpack());
        }
        if (shouldUpdateWithV3Buildpacks(staging)) {
            buildpackDataBuilder.addAllBuildpacks(staging.getBuildpacks());
        }
        return buildpackDataBuilder.build();
    }

    private void updateApplicationAttributes(Staging staging, Integer disk, Integer memory, Set<CloudRouteSummary> routes,
                                             CreateApplicationResponse createApplicationResponse) {
        UUID createdApplicationGuid = UUID.fromString(createApplicationResponse.getId());
        GetApplicationProcessResponse applicationProcess = getApplicationProcess(createdApplicationGuid);
        updateApplicationProcess(createdApplicationGuid, staging, applicationProcess);
        delegate.applicationsV3()
                .scale(ScaleApplicationRequest.builder()
                                              .applicationId(createdApplicationGuid.toString())
                                              .type("web")
                                              .memoryInMb(memory)
                                              .diskInMb(disk)
                                              .build())
                .block();
        if (!CollectionUtils.isEmpty(routes)) {
            addRoutes(routes, createdApplicationGuid);
        }
    }

    private Lifecycle createBuildpackLifecycle(Staging staging) {
        BuildpackData buildpackData = createBuildpackData(staging);
        return Lifecycle.builder()
                        .type(LifecycleType.BUILDPACK)
                        .data(buildpackData)
                        .build();
    }

    private Lifecycle createDockerLifecycle() {
        return Lifecycle.builder()
                        .type(LifecycleType.DOCKER)
                        .data(DockerData.builder()
                                        .build())
                        .build();
    }

    private ApplicationRelationships buildApplicationRelationships() {
        return ApplicationRelationships.builder()
                                       .space(buildToOneRelationship(getTargetSpaceGuid()))
                                       .build();
    }

    private GetApplicationProcessResponse getApplicationProcess(UUID applicationGuid) {
        return delegate.applicationsV3()
                       .getProcess(GetApplicationProcessRequest.builder()
                                                               .type("web")
                                                               .applicationId(applicationGuid.toString())
                                                               .build())
                       .block();
    }

    private void updateApplicationProcess(UUID applicationGuid, Staging staging, GetApplicationProcessResponse applicationProcess) {
        if (staging.isSshEnabled() != null) {
            updateSsh(applicationGuid, staging.isSshEnabled());
        }
        UpdateProcessRequest.Builder updateProcessRequestBuilder = UpdateProcessRequest.builder()
                                                                                       .processId(applicationProcess.getId())
                                                                                       .command(staging.getCommand());
        if (staging.getHealthCheckType() != null) {
            updateProcessRequestBuilder.healthCheck(buildHealthCheck(staging));
        }
        delegate.processes()
                .update(updateProcessRequestBuilder.build())
                .block();
    }

    private void updateSsh(UUID applicationGuid, boolean isSshEnabled) {
        delegate.applicationsV3()
                .updateFeature(UpdateApplicationFeatureRequest.builder()
                                                              .featureName("ssh")
                                                              .enabled(isSshEnabled)
                                                              .applicationId(applicationGuid.toString())
                                                              .build())
                .block();
    }

    private HealthCheck buildHealthCheck(Staging staging) {
        HealthCheckType healthCheckType = HealthCheckType.from(staging.getHealthCheckType());
        return HealthCheck.builder()
                          .type(healthCheckType)
                          .data(Data.builder()
                                    .endpoint(staging.getHealthCheckHttpEndpoint())
                                    .timeout(staging.getHealthCheckTimeout())
                                    .invocationTimeout(staging.getInvocationTimeout())
                                    .build())
                          .build();
    }

    private boolean shouldUpdateWithV3Buildpacks(Staging staging) {
        return staging.getBuildpacks()
                      .size() > 1;
    }

    @Override
    public void createServiceInstance(CloudServiceInstance serviceInstance) {
        assertSpaceProvided("create service instance");
        Assert.notNull(serviceInstance, "Service instance must not be null.");

        CloudServicePlan servicePlan = findPlanForService(serviceInstance, serviceInstance.getPlan());
        UUID servicePlanGuid = servicePlan.getMetadata()
                                          .getGuid();
        delegate.serviceInstances()
                .create(CreateServiceInstanceRequest.builder()
                                                    .spaceId(getTargetSpaceGuid().toString())
                                                    .name(serviceInstance.getName())
                                                    .servicePlanId(servicePlanGuid.toString())
                                                    .addAllTags(serviceInstance.getTags())
                                                    .parameters(serviceInstance.getCredentials())
                                                    .acceptsIncomplete(true)
                                                    .build())
                .block();
    }

    @Override
    public void createServiceBroker(CloudServiceBroker serviceBroker) {
        Assert.notNull(serviceBroker, "Service broker must not be null.");

        delegate.serviceBrokers()
                .create(CreateServiceBrokerRequest.builder()
                                                  .name(serviceBroker.getName())
                                                  .brokerUrl(serviceBroker.getUrl())
                                                  .authenticationUsername(serviceBroker.getUsername())
                                                  .authenticationPassword(serviceBroker.getPassword())
                                                  .spaceId(serviceBroker.getSpaceGuid())
                                                  .build())
                .block();
    }

    @Override
    public CloudServiceKey createServiceKey(String serviceInstanceName, String serviceKeyName, Map<String, Object> parameters) {
        CloudServiceInstance serviceInstance = getServiceInstance(serviceInstanceName);
        return fetch(() -> createServiceKeyResource(serviceInstance, serviceKeyName, parameters),
                     resource -> ImmutableRawCloudServiceKey.builder()
                                                            .serviceInstance(serviceInstance)
                                                            .resource(resource)
                                                            .build());
    }

    @Override
    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance, Map<String, Object> credentials) {
        createUserProvidedServiceInstance(serviceInstance, credentials, "");
    }

    @Override
    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance, Map<String, Object> credentials,
                                                  String syslogDrainUrl) {
        assertSpaceProvided("create service instance");
        Assert.notNull(serviceInstance, "Service instance must not be null.");

        delegate.userProvidedServiceInstances()
                .create(CreateUserProvidedServiceInstanceRequest.builder()
                                                                .spaceId(getTargetSpaceGuid().toString())
                                                                .name(serviceInstance.getName())
                                                                .credentials(credentials)
                                                                .syslogDrainUrl(syslogDrainUrl)
                                                                .build())
                .block();
    }

    @Override
    public void deleteAllApplications() {
        List<UUID> applicationIds = getApplicationIds();
        for (UUID applicationGuid : applicationIds) {
            deleteApplication(applicationGuid);
        }
    }

    @Override
    public void deleteAllServiceInstances() {
        List<CloudServiceInstance> serviceInstances = getServiceInstances();
        for (CloudServiceInstance serviceInstance : serviceInstances) {
            doDeleteServiceInstance(serviceInstance);
        }
    }

    @Override
    public void deleteApplication(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        deleteApplication(applicationGuid);
    }

    private void deleteApplication(UUID applicationGuid) {
        List<UUID> serviceBindingGuids = getServiceBindingGuids(applicationGuid);
        for (UUID serviceBindingGuid : serviceBindingGuids) {
            doUnbindServiceInstance(serviceBindingGuid);
        }
        delegate.applicationsV3()
                .delete(DeleteApplicationRequest.builder()
                                                .applicationId(applicationGuid.toString())
                                                .build())
                .block();
    }

    @Override
    public void deleteDomain(String domainName) {
        assertSpaceProvided("delete domain");
        CloudDomain domain = findDomainByName(domainName, true);
        List<CloudRoute> routes = findRoutes(domain);
        if (!routes.isEmpty()) {
            throw new IllegalStateException("Unable to remove domain that is in use --" + " it has " + routes.size() + " routes.");
        }
        doDeleteDomain(getGuid(domain));
    }

    /**
     * Delete routes that do not have any application which is assigned to them.
     *
     * @return deleted routes or an empty list if no routes have been found
     */
    @Override
    public List<CloudRoute> deleteOrphanedRoutes() {
        List<CloudRoute> orphanRoutes = new ArrayList<>();
        for (CloudDomain domain : getDomainsForOrganization()) {
            orphanRoutes.addAll(fetchOrphanRoutes(domain.getName()));
        }

        List<CloudRoute> deletedRoutes = new ArrayList<>();
        for (CloudRoute orphanRoute : orphanRoutes) {
            deleteRoute(orphanRoute.getHost(), orphanRoute.getDomain()
                                                          .getName(),
                        orphanRoute.getPath());
            deletedRoutes.add(orphanRoute);
        }
        return deletedRoutes;
    }

    @Override
    public void deleteRoute(String host, String domainName, String path) {
        assertSpaceProvided("delete route for domain");
        UUID routeGuid = getRouteGuid(getRequiredDomainGuid(domainName), host, path);
        if (routeGuid == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND,
                                              "Not Found",
                                              "Host " + host + " not found for domain " + domainName + ".");
        }
        doDeleteRoute(routeGuid);
    }

    @Override
    public void deleteServiceInstance(String serviceInstanceName) {
        CloudServiceInstance serviceInstance = getServiceInstance(serviceInstanceName);
        doDeleteServiceInstance(serviceInstance);
    }

    @Override
    public void deleteServiceInstance(CloudServiceInstance serviceInstance) {
        doDeleteServiceInstance(serviceInstance);
    }

    @Override
    public void deleteServiceBroker(String name) {
        CloudServiceBroker broker = getServiceBroker(name);
        UUID guid = broker.getMetadata()
                          .getGuid();
        delegate.serviceBrokers()
                .delete(DeleteServiceBrokerRequest.builder()
                                                  .serviceBrokerId(guid.toString())
                                                  .build())
                .block();
    }

    @Override
    public void deleteServiceKey(String serviceInstanceName, String serviceKeyName) {
        List<CloudServiceKey> serviceKeys = getServiceKeys(serviceInstanceName);
        for (CloudServiceKey serviceKey : serviceKeys) {
            if (serviceKey.getName()
                          .equals(serviceKeyName)) {
                doDeleteServiceKey(serviceKey.getMetadata()
                                             .getGuid());
                return;
            }
        }
        throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service key " + serviceKeyName + " not found.");
    }

    @Override
    public void deleteServiceKey(CloudServiceKey serviceKey) {
        UUID serviceKeyGuid = serviceKey.getMetadata()
                                        .getGuid();
        doDeleteServiceKey(serviceKeyGuid);
    }

    @Override
    public CloudApplication getApplication(String applicationName) {
        return getApplication(applicationName, true);
    }

    @Override
    public CloudApplication getApplication(String applicationName, boolean required) {
        CloudApplication application = findApplicationByName(applicationName, required);
        return addMetadataIfNotNull(application);
    }

    @Override
    public CloudApplication getApplication(UUID applicationGuid) {
        final CloudApplication application = findApplication(applicationGuid);
        return addMetadataIfNotNull(application);
    }

    @Override
    public UUID getApplicationGuid(String applicationName) {
        return getRequiredApplicationGuid(applicationName);
    }

    @Override
    public Map<String, String> getApplicationEnvironment(UUID applicationGuid) {
        GetApplicationEnvironmentResponse applicationEnvironmentResponse = getApplicationEnvironmentResponse(applicationGuid);
        return EnvironmentUtil.parse(applicationEnvironmentResponse.getEnvironmentVariables());
    }

    private GetApplicationEnvironmentResponse getApplicationEnvironmentResponse(UUID applicationGuid) {
        return delegate.applicationsV3()
                       .getEnvironment(GetApplicationEnvironmentRequest.builder()
                                                                       .applicationId(applicationGuid.toString())
                                                                       .build())
                       .block();
    }

    @Override
    public Map<String, String> getApplicationEnvironment(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        return getApplicationEnvironment(applicationGuid);
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        return getEventsByTarget(applicationGuid);
    }

    @Override
    public List<CloudEvent> getEventsByTarget(UUID uuid) {
        return findEventsByTarget(uuid.toString());
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication application) {
        if (application.getState()
                       .equals(CloudApplication.State.STARTED)) {
            return findApplicationInstances(getGuid(application));
        }
        return ImmutableInstancesInfo.builder()
                                     .instances(Collections.emptyList())
                                     .build();
    }

    @Override
    public List<CloudApplication> getApplications() {
        List<CloudApplication> applications = fetchListWithAuxiliaryContent(this::getApplicationResources,
                                                                            this::zipWithAuxiliaryApplicationContent);
        return addMetadataIfNotEmpty(applications);
    }

    @Override
    public List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector) {
        Map<String, Metadata> applicationsMetadata = getApplicationsMetadataByLabelSelector(labelSelector);
        List<CloudApplication> cloudApplications = fetchListWithAuxiliaryContent(() -> getApplicationResourcesByNamesInBatches(applicationsMetadata.keySet()),
                                                                                 this::zipWithAuxiliaryApplicationContent);
        return addMetadata(cloudApplications, applicationsMetadata);
    }

    private List<UUID> getApplicationIds() {
        return getApplicationResources().map(this::getGuid)
                                        .collectList()
                                        .block();
    }

    private Map<String, Metadata> getApplicationsMetadataByLabelSelector(String labelSelector) {
        IntFunction<ListApplicationsRequest> pageRequestSupplier = page -> ListApplicationsRequest.builder()
                                                                                                  .spaceId(getTargetSpaceGuid().toString())
                                                                                                  .labelSelector(labelSelector)
                                                                                                  .page(page)
                                                                                                  .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.applicationsV3()
                                                                        .list(pageRequestSupplier.apply(page)))
                              .collectMap(ApplicationResource::getName, ApplicationResource::getMetadata)
                              .block();
    }

    @Override
    public CloudDomain getDefaultDomain() {
        return fetch(() -> getDefaultDomainResource(getTargetOrganizationGuid().toString()), ImmutableRawCloudDomain::of);
    }

    private Mono<? extends Domain> getDefaultDomainResource(String guid) {
        return delegate.organizationsV3()
                       .getDefaultDomain(GetOrganizationDefaultDomainRequest.builder()
                                                                            .organizationId(guid)
                                                                            .build());
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return fetchList(this::getSharedDomainResources, ImmutableRawCloudDomain::of);
    }

    @Override
    public List<CloudDomain> getDomains() {
        return fetchList(this::getDomainResources, ImmutableRawCloudDomain::of);
    }

    @Override
    public List<CloudDomain> getDomainsForOrganization() {
        assertSpaceProvided("access organization domains");
        return findDomainsByOrganizationGuid(getTargetOrganizationGuid());
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return fetchList(this::getPrivateDomainResources, ImmutableRawCloudDomain::of);
    }

    @Override
    public List<CloudEvent> getEvents() {
        return fetchList(this::getEventResources, ImmutableRawCloudEvent::of);
    }

    @Override
    public CloudOrganization getOrganization(String organizationName) {
        return getOrganization(organizationName, true);
    }

    /**
     * Get organization by given name.
     *
     * @param organizationName
     * @param required
     * @return CloudOrganization instance
     */
    @Override
    public CloudOrganization getOrganization(String organizationName, boolean required) {
        CloudOrganization organization = findOrganization(organizationName);
        if (organization == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Organization " + organizationName + " not found.");
        }
        return organization;
    }

    @Override
    public List<CloudOrganization> getOrganizations() {
        return fetchList(this::getOrganizationResources, ImmutableRawCloudOrganization::of);
    }

    @Override
    public List<ApplicationLog> getRecentLogs(String applicationName) {
        UUID appGuid = getRequiredApplicationGuid(applicationName);
        return getRecentLogs(appGuid);
    }

    @Override
    public List<ApplicationLog> getRecentLogs(UUID applicationGuid) {
        RecentLogsRequest request = RecentLogsRequest.builder()
                                                     .applicationId(applicationGuid.toString())
                                                     .build();
        return fetchFlux(() -> dopplerClient.recentLogs(request), ImmutableRawApplicationLog::of).collectSortedList()
                                                                                                 .block();
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        assertSpaceProvided("get routes for domain");
        CloudDomain domain = findDomainByName(domainName, true);
        return findRoutes(domain);
    }

    @Override
    public UUID getRequiredServiceInstanceGuid(String name) {
        org.cloudfoundry.client.v3.Resource serviceInstanceResource = getServiceInstanceByName(name).block();
        if (serviceInstanceResource == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service instance " + name + " not found.");
        }
        return UUID.fromString(serviceInstanceResource.getId());
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName) {
        return getServiceInstance(serviceInstanceName, true);
    }

    private CloudServiceInstance getServiceInstanceWithMetadata(CloudServiceInstance serviceInstance) {
        List<CloudServiceInstance> serviceInstances = Collections.singletonList(serviceInstance);
        Map<String, Metadata> serviceInstancesMetadata = getServiceInstancesMetadataInBatches(getServiceInstanceNames(serviceInstances));
        return getServiceInstancesWithMetadata(serviceInstances, serviceInstancesMetadata).get(0);
    }

    private List<CloudServiceInstance> getServiceInstancesWithMetadata(List<CloudServiceInstance> serviceInstances,
                                                                       Map<String, Metadata> serviceInstancesMetadata) {
        return serviceInstances.stream()
                               .map(serviceInstance -> getServiceInstanceWithMetadata(serviceInstance,
                                                                                      serviceInstancesMetadata.get(serviceInstance.getName())))
                               .collect(Collectors.toList());
    }

    private CloudServiceInstance getServiceInstanceWithMetadata(CloudServiceInstance serviceInstance, Metadata metadata) {
        return ImmutableCloudServiceInstance.builder()
                                            .from(serviceInstance)
                                            .v3Metadata(metadata)
                                            .build();
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName, boolean required) {
        CloudServiceInstance serviceInstance = findServiceInstanceByName(serviceInstanceName);
        if (serviceInstance == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service instance " + serviceInstanceName + " not found.");
        }
        return serviceInstance == null ? null : getServiceInstanceWithMetadata(serviceInstance);
    }

    @Override
    public List<CloudServiceBinding> getServiceBindings(UUID serviceInstanceGuid) {
        return fetchList(() -> getServiceBindingResourcesByServiceInstanceGuid(serviceInstanceGuid), ImmutableRawCloudServiceBinding::of);
    }

    @Override
    public CloudServiceBinding getServiceBindingForApplication(UUID applicationId, UUID serviceInstanceGuid) {
        return fetch(() -> getServiceBindingResourceByApplicationGuidAndServiceInstanceGuid(applicationId, serviceInstanceGuid),
                     ImmutableRawCloudServiceBinding::of);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name) {
        return getServiceBroker(name, true);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        CloudServiceBroker serviceBroker = findServiceBrokerByName(name);
        if (serviceBroker == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service broker " + name + " not found.");
        }
        return serviceBroker;
    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        return fetchList(this::getServiceBrokerResources, ImmutableRawCloudServiceBroker::of);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(String serviceInstanceName) {
        CloudServiceInstance serviceInstance = getServiceInstance(serviceInstanceName);
        return getServiceKeys(serviceInstance);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(CloudServiceInstance serviceInstance) {
        return fetchList(() -> getServiceKeyTuple(serviceInstance), tuple -> ImmutableRawCloudServiceKey.builder()
                                                                                                        .resource(tuple.getT1())
                                                                                                        .serviceInstance(tuple.getT2())
                                                                                                        .build());
    }

    @Override
    public Map<String, Object> getServiceInstanceParameters(UUID guid) {
        return delegate.serviceInstances()
                       .getParameters(GetServiceInstanceParametersRequest.builder()
                                                                         .serviceInstanceId(guid.toString())
                                                                         .build())
                       .map(GetServiceInstanceParametersResponse::getParameters)
                       .block();
    }

    @Override
    public Map<String, Object> getServiceBindingParameters(UUID guid) {
        return delegate.serviceBindingsV2()
                       .getParameters(GetServiceBindingParametersRequest.builder()
                                                                        .serviceBindingId(guid.toString())
                                                                        .build())
                       .map(GetServiceBindingParametersResponse::getParameters)
                       .block();
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        return fetchListWithAuxiliaryContent(this::getServiceResources, this::zipWithAuxiliaryServiceOfferingContent);
    }

    @Override
    public void updateServicePlan(String serviceName, String planName) {
        CloudServiceInstance service = getServiceInstance(serviceName);
        if (service.isUserProvided()) {
            return;
        }
        CloudServicePlan plan = findPlanForService(service, planName);
        delegate.serviceInstances()
                .update(org.cloudfoundry.client.v2.serviceinstances.UpdateServiceInstanceRequest.builder()
                                                                                                .serviceInstanceId(service.getGuid()
                                                                                                                          .toString())
                                                                                                .servicePlanId(plan.getGuid()
                                                                                                                   .toString())
                                                                                                .acceptsIncomplete(true)
                                                                                                .build())
                .block();
    }

    @Override
    public void updateServiceParameters(String serviceName, Map<String, Object> parameters) {
        CloudServiceInstance service = getServiceInstance(serviceName);
        if (service.isUserProvided()) {
            delegate.userProvidedServiceInstances()
                    .update(UpdateUserProvidedServiceInstanceRequest.builder()
                                                                    .userProvidedServiceInstanceId(service.getGuid()
                                                                                                          .toString())
                                                                    .credentials(parameters)
                                                                    .build())
                    .block();
            return;
        }
        delegate.serviceInstances()
                .update(org.cloudfoundry.client.v2.serviceinstances.UpdateServiceInstanceRequest.builder()
                                                                                                .serviceInstanceId(service.getGuid()
                                                                                                                          .toString())
                                                                                                .parameters(parameters)
                                                                                                .acceptsIncomplete(true)
                                                                                                .build())
                .block();
    }

    @Override
    public void updateServiceTags(String serviceName, List<String> tags) {
        CloudServiceInstance service = getServiceInstance(serviceName);
        if (service.isUserProvided()) {
            delegate.userProvidedServiceInstances()
                    .update(UpdateUserProvidedServiceInstanceRequest.builder()
                                                                    .userProvidedServiceInstanceId(service.getGuid()
                                                                                                          .toString())
                                                                    .tags(tags)
                                                                    .build())
                    .block();
            return;
        }
        delegate.serviceInstances()
                .update(org.cloudfoundry.client.v2.serviceinstances.UpdateServiceInstanceRequest.builder()
                                                                                                .serviceInstanceId(service.getGuid()
                                                                                                                          .toString())
                                                                                                .tags(tags)
                                                                                                .acceptsIncomplete(true)
                                                                                                .build())
                .block();
    }

    @Override
    public List<CloudServiceInstance> getServiceInstances() {
        List<CloudServiceInstance> serviceInstances = fetchListWithAuxiliaryContent(this::getServiceInstanceResources,
                                                                                    this::zipWithAuxiliaryServiceInstanceContent);
        Map<String, Metadata> serviceInstancesMetadata = getServiceInstancesMetadataInBatches(getServiceInstanceNames(serviceInstances));
        return getServiceInstancesWithMetadata(serviceInstances, serviceInstancesMetadata);
    }

    private List<String> getServiceInstanceNames(List<CloudServiceInstance> serviceInstances) {
        return serviceInstances.stream()
                               .map(CloudEntity::getName)
                               .collect(Collectors.toList());
    }

    private Map<String, Metadata> getServiceInstancesMetadataInBatches(List<String> serviceInstanceNames) {
        Map<String, Metadata> serviceInstancesMetadata = new HashMap<>();
        for (List<String> batchOfServiceInstanceNames : toBatches(serviceInstanceNames, MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST)) {
            serviceInstancesMetadata.putAll(getServiceInstancesMetadata(batchOfServiceInstanceNames));
        }
        return serviceInstancesMetadata;
    }

    private <T> List<List<T>> toBatches(Collection<T> largeList, int maxCharLength) {
        if (largeList.isEmpty()) {
            return Collections.emptyList();
        }

        List<List<T>> batches = new ArrayList<>();
        int currentBatchLength = 0, currentBatchIndex = 0;
        batches.add(new ArrayList<>());

        for (T element : largeList) {
            int elementLength = element.toString()
                                       .length();
            if (elementLength + currentBatchLength >= maxCharLength) {
                batches.add(new ArrayList<>());
                currentBatchIndex++;
                currentBatchLength = 0;
            }
            batches.get(currentBatchIndex)
                   .add(element);
            currentBatchLength += elementLength;
        }
        return batches;
    }

    private Map<String, Metadata> getServiceInstancesMetadata(List<String> serviceInstanceNames) {
        String spaceGuid = getTargetSpaceGuid().toString();
        IntFunction<ListServiceInstancesRequest> pageRequestSupplier = page -> ListServiceInstancesRequest.builder()
                                                                                                          .spaceId(spaceGuid)
                                                                                                          .addAllServiceInstanceNames(serviceInstanceNames)
                                                                                                          .page(page)
                                                                                                          .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.serviceInstancesV3()
                                                                        .list(pageRequestSupplier.apply(page)))
                              .collectMap(ServiceInstanceResource::getName, ServiceInstanceResource::getMetadata)
                              .block();
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector) {
        Map<String, Metadata> serviceInstancesMetadata = getServiceInstancesMetadataByLabelSelector(labelSelector);
        List<CloudServiceInstance> serviceInstances = fetchListWithAuxiliaryContent(() -> getServiceInstanceResourcesByNamesInBatches(serviceInstancesMetadata.keySet()),
                                                                                    this::zipWithAuxiliaryServiceInstanceContent);
        return getServiceInstancesWithMetadata(serviceInstances, serviceInstancesMetadata);
    }
    
    @Override
    public  List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(String labelSelector) {
        Map<String, Metadata> serviceInstancesMetadata = getServiceInstancesMetadataByLabelSelector(labelSelector);
        List<CloudServiceInstance> cloudServiceInstances = serviceInstancesMetadata.keySet()
                                                                                           .stream()
                                                                                           .map(serviceInstanceName -> ImmutableCloudServiceInstance.builder()
                                                                                                                                                    .name(serviceInstanceName)
                                                                                                                                                    .build())
                                                                                           .collect(Collectors.toList());
        return getServiceInstancesWithMetadata(cloudServiceInstances, serviceInstancesMetadata);
    }

    private Map<String, Metadata> getServiceInstancesMetadataByLabelSelector(String labelSelector) {
        IntFunction<ListServiceInstancesRequest> listServiceInstancesPageRequestSupplier = page -> ListServiceInstancesRequest.builder()
                                                                                                                              .labelSelector(labelSelector)
                                                                                                                              .spaceId(getTargetSpaceGuid().toString())
                                                                                                                              .page(page)
                                                                                                                              .build();

        return PaginationUtils.requestClientV3Resources(page -> delegate.serviceInstancesV3()
                                                                        .list(listServiceInstancesPageRequestSupplier.apply(page)))
                              .collectMap(ServiceInstanceResource::getName, ServiceInstanceResource::getMetadata)
                              .block();
    }

    @Override
    public CloudSpace getSpace(UUID spaceGuid) {
        return fetchWithAuxiliaryContent(() -> getSpaceResource(spaceGuid), this::zipWithAuxiliarySpaceContent);
    }

    @Override
    public CloudSpace getSpace(String organizationName, String spaceName) {
        return getSpace(organizationName, spaceName, true);
    }

    @Override
    public CloudSpace getSpace(String organizationName, String spaceName, boolean required) {
        UUID organizationGuid = getOrganizationGuid(organizationName, required);
        return findSpaceByOrganizationGuidAndName(organizationGuid, spaceName, required);
    }

    @Override
    public CloudSpace getSpace(String spaceName) {
        return getSpace(spaceName, true);
    }

    @Override
    public CloudSpace getSpace(String spaceName, boolean required) {
        UUID organizationGuid = getTargetOrganizationGuid();
        return findSpaceByOrganizationGuidAndName(organizationGuid, spaceName, required);
    }

    @Override
    public List<CloudSpace> getSpaces() {
        return fetchListWithAuxiliaryContent(this::getSpaceResources, this::zipWithAuxiliarySpaceContent);
    }

    @Override
    public List<CloudSpace> getSpaces(String organizationName) {
        UUID organizationGuid = getOrganizationGuid(organizationName, true);
        return findSpacesByOrganizationGuid(organizationGuid);
    }

    @Override
    public CloudStack getStack(String name) {
        return getStack(name, true);
    }

    @Override
    public CloudStack getStack(String name, boolean required) {
        CloudStack stack = findStackResource(name);
        if (stack == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Stack " + name + " not found.");
        }
        return stack;
    }

    @Override
    public List<CloudStack> getStacks() {
        return fetchList(this::getStackResources, ImmutableRawCloudStack::of);
    }

    @Override
    public OAuth2AccessTokenWithAdditionalInfo login() {
        oAuthClient.init(credentials);
        return oAuthClient.getToken();
    }

    @Override
    public void logout() {
        oAuthClient.clear();
    }

    @Override
    public void rename(String applicationName, String newName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        delegate.applicationsV3()
                .update(UpdateApplicationRequest.builder()
                                                .applicationId(applicationGuid.toString())
                                                .name(newName)
                                                .build())
                .block();
    }

    @Override
    public void restartApplication(String applicationName) {
        stopApplication(applicationName);
        startApplication(applicationName);
    }

    @Override
    public void startApplication(String applicationName) {
        Application application = getApplicationByName(applicationName).block();
        if (application.getState() == ApplicationState.STARTED) {
            return;
        }
        UUID applicationGuid = UUID.fromString(application.getId());
        delegate.applicationsV3()
                .start(StartApplicationRequest.builder()
                                              .applicationId(applicationGuid.toString())
                                              .build())
                .block();
    }

    @Override
    public void stopApplication(String applicationName) {
        Application application = getApplicationByName(applicationName).block();
        if (application.getState() == ApplicationState.STOPPED) {
            return;
        }
        UUID applicationGuid = UUID.fromString(application.getId());
        delegate.applicationsV3()
                .stop(StopApplicationRequest.builder()
                                            .applicationId(applicationGuid.toString())
                                            .build())
                .block();
    }

    @Override
    public void unbindServiceInstance(String applicationName, String serviceInstanceName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UUID serviceInstanceGuid = getRequiredServiceInstanceGuid(serviceInstanceName);

        doUnbindServiceInstance(applicationGuid, serviceInstanceGuid);
    }

    @Override
    public void unbindServiceInstance(CloudApplication application, CloudServiceInstance serviceInstance) {
        UUID applicationGuid = getGuid(application);
        UUID serviceInstanceGuid = getGuid(serviceInstance);

        doUnbindServiceInstance(applicationGuid, serviceInstanceGuid);
    }

    @Override
    public void updateApplicationDiskQuota(String applicationName, int diskQuota) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        delegate.applicationsV3()
                .scale(ScaleApplicationRequest.builder()
                                              .applicationId(applicationGuid.toString())
                                              .type("web")
                                              .diskInMb(diskQuota)
                                              .build())
                .block();
    }

    @Override
    public void updateApplicationEnv(String applicationName, Map<String, String> env) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        delegate.applicationsV3()
                .updateEnvironmentVariables(UpdateApplicationEnvironmentVariablesRequest.builder()
                                                                                        .applicationId(applicationGuid.toString())
                                                                                        .vars(env)
                                                                                        .build())
                .block();
    }

    @Override
    public void updateApplicationInstances(String applicationName, int instances) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        delegate.applicationsV3()
                .scale(ScaleApplicationRequest.builder()
                                              .applicationId(applicationGuid.toString())
                                              .type("web")
                                              .instances(instances)
                                              .build())
                .block();
    }

    @Override
    public void updateApplicationMemory(String applicationName, int memory) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        delegate.applicationsV3()
                .scale(ScaleApplicationRequest.builder()
                                              .applicationId(applicationGuid.toString())
                                              .type("web")
                                              .memoryInMb(memory)
                                              .build())
                .block();
    }

    @Override
    public void updateApplicationMetadata(UUID guid, org.cloudfoundry.client.v3.Metadata metadata) {
        delegate.applicationsV3()
                .update(org.cloudfoundry.client.v3.applications.UpdateApplicationRequest.builder()
                                                                                        .applicationId(guid.toString())
                                                                                        .metadata(metadata)
                                                                                        .build())
                .block();
    }

    @Override
    public void updateApplicationStaging(String applicationName, Staging staging) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UpdateApplicationRequest updateApplicationRequest = UpdateApplicationRequest.builder()
                                                                                    .applicationId(applicationGuid.toString())
                                                                                    .lifecycle(buildApplicationLifecycle(staging))
                                                                                    .build();
        delegate.applicationsV3()
                .update(updateApplicationRequest)
                .block();
        GetApplicationProcessResponse applicationProcess = getApplicationProcess(applicationGuid);
        updateApplicationProcess(applicationGuid, staging, applicationProcess);
    }

    @Override
    public void updateApplicationRoutes(String applicationName, Set<CloudRouteSummary> updatedRoutes) {
        CloudApplication application = getApplication(applicationName);
        Set<CloudRouteSummary> currentRoutes = application.getRoutes();

        Set<CloudRouteSummary> outdatedRoutes = selectRoutesOnlyInFirst(currentRoutes, updatedRoutes);

        Set<CloudRouteSummary> newRoutes = selectRoutesOnlyInFirst(updatedRoutes, currentRoutes);

        removeRoutes(outdatedRoutes, application.getMetadata()
                                                .getGuid());
        addRoutes(newRoutes, application.getMetadata()
                                        .getGuid());
    }

    protected Set<CloudRouteSummary> selectRoutesOnlyInFirst(Set<CloudRouteSummary> allRoutes, Set<CloudRouteSummary> routesToSubtract) {
        Set<CloudRouteSummary> selectedRoutes = new HashSet<>(allRoutes);
        selectedRoutes.removeAll(routesToSubtract);
        return selectedRoutes;
    }

    @Override
    public void updateServiceBroker(CloudServiceBroker serviceBroker) {
        Assert.notNull(serviceBroker, "Service broker must not be null.");

        CloudServiceBroker existingBroker = getServiceBroker(serviceBroker.getName());
        UUID brokerGuid = existingBroker.getMetadata()
                                        .getGuid();

        delegate.serviceBrokers()
                .update(UpdateServiceBrokerRequest.builder()
                                                  .serviceBrokerId(brokerGuid.toString())
                                                  .name(serviceBroker.getName())
                                                  .authenticationUsername(serviceBroker.getUsername())
                                                  .authenticationPassword(serviceBroker.getPassword())
                                                  .brokerUrl(serviceBroker.getUrl())
                                                  .build())
                .block();
    }

    @Override
    public void updateServiceInstanceMetadata(UUID guid, org.cloudfoundry.client.v3.Metadata metadata) {
        delegate.serviceInstancesV3()
                .update(UpdateServiceInstanceRequest.builder()
                                                    .serviceInstanceId(guid.toString())
                                                    .metadata(metadata)
                                                    .build())
                .block();
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, boolean visibility) {
        CloudServiceBroker broker = getServiceBroker(name);
        List<CloudServicePlan> servicePlans = findServicePlansByBrokerGuid(getGuid(broker));
        for (CloudServicePlan servicePlan : servicePlans) {
            updateServicePlanVisibility(servicePlan, visibility);
        }
    }

    @Override
    public CloudTask getTask(UUID taskGuid) {
        return fetch(() -> getTaskResource(taskGuid), ImmutableRawCloudTask::of);
    }

    @Override
    public List<CloudTask> getTasks(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        return fetchList(() -> getTaskResourcesByApplicationGuid(applicationGuid), ImmutableRawCloudTask::of);
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        return createTask(applicationGuid, task);
    }

    @Override
    public CloudTask cancelTask(UUID taskGuid) {
        return fetch(() -> cancelTaskResource(taskGuid), ImmutableRawCloudTask::of);
    }

    @Override
    public void uploadApplication(String applicationName, Path file, UploadStatusCallback callback) {
        CloudPackage cloudPackage = startUpload(applicationName, file);
        processAsyncUpload(cloudPackage, callback);
    }

    @Override
    public void uploadApplication(String applicationName, InputStream inputStream, UploadStatusCallback callback) throws IOException {
        Assert.notNull(inputStream, "InputStream must not be null");

        Path file = null;
        try {
            file = createTemporaryUploadFile(inputStream);
            uploadApplication(applicationName, file, callback);
        } finally {
            IOUtils.closeQuietly(inputStream);
            if (file != null) {
                Files.deleteIfExists(file);
            }
        }
    }

    @Override
    public CloudPackage asyncUploadApplication(String applicationName, Path file, UploadStatusCallback callback) {
        CloudPackage cloudPackage = startUpload(applicationName, file);
        processAsyncUploadInBackground(cloudPackage, callback);
        return cloudPackage;
    }

    @Override
    public Upload getUploadStatus(UUID packageGuid) {
        CloudPackage cloudPackage = getPackage(packageGuid);
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

    @Override
    public void bindDropletToApp(UUID dropletGuid, UUID applicationGuid) {
        delegate.applicationsV3()
                .setCurrentDroplet(SetApplicationCurrentDropletRequest.builder()
                                                                      .applicationId(applicationGuid.toString())
                                                                      .data(Relationship.builder()
                                                                                        .id(dropletGuid.toString())
                                                                                        .build())
                                                                      .build())
                .block();
    }

    @Override
    public DropletInfo getCurrentDropletForApplication(UUID applicationGuid) {
        GetApplicationCurrentDropletResponse getApplicationCurrentDropletResponse = delegate.applicationsV3()
                                                                                            .getCurrentDroplet(GetApplicationCurrentDropletRequest.builder()
                                                                                                                                                  .applicationId(applicationGuid.toString())
                                                                                                                                                  .build())
                                                                                            .block();
        return tryParseDropletInfo(getApplicationCurrentDropletResponse);
    }

    private DropletInfo tryParseDropletInfo(GetApplicationCurrentDropletResponse getApplicationCurrentDropletResponse) {
        try {
            return parseDropletInfo(getApplicationCurrentDropletResponse);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new CloudOperationException(HttpStatus.NOT_FOUND);
        }
    }

    private DropletInfo parseDropletInfo(GetApplicationCurrentDropletResponse getApplicationCurrentDropletResponse) {
        String packageUrl = getApplicationCurrentDropletResponse.getLinks()
                                                                .get(Constants.PACKAGE)
                                                                .getHref();
        if (packageUrl.endsWith("/")) {
            packageUrl = packageUrl.substring(0, packageUrl.lastIndexOf("/"));
        }
        String packageGuid = packageUrl.substring(packageUrl.lastIndexOf("/") + 1);
        return ImmutableDropletInfo.builder()
                                   .guid(UUID.fromString(getApplicationCurrentDropletResponse.getId()))
                                   .packageGuid(UUID.fromString(packageGuid))
                                   .build();
    }

    @Override
    public List<CloudPackage> getPackagesForApplication(UUID applicationGuid) {
        return fetchList(() -> getPackages(applicationGuid.toString()), ImmutableRawCloudPackage::of);
    }

    private Flux<? extends PackageResource> getPackages(String applicationGuid) {
        IntFunction<ListApplicationPackagesRequest> pageRequestSupplier = page -> ListApplicationPackagesRequest.builder()
                                                                                                                .page(page)
                                                                                                                .applicationId(applicationGuid)
                                                                                                                .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.applicationsV3()
                                                                        .listPackages(pageRequestSupplier.apply(page)));
    }

    @Override
    public List<UserRole> getUserRolesBySpaceAndUser(UUID spaceGuid, UUID userGuid) {
        return fetchList(() -> getRoles(spaceGuid, userGuid), ImmutableRawUserRole::of);
    }

    public CloudPackage createDockerPackage(UUID applicationGuid, DockerInfo dockerInfo) {
        org.cloudfoundry.client.v3.packages.DockerData.Builder dockerDataBuilder = org.cloudfoundry.client.v3.packages.DockerData.builder()
                                                                                                                                 .image(dockerInfo.getImage());
        if (dockerInfo.getCredentials() != null) {
            addNonNullDockerCredentials(dockerInfo.getCredentials(), dockerDataBuilder);
        }
        CreatePackageRequest packageRequest = CreatePackageRequest.builder()
                                                                  .type(PackageType.DOCKER)
                                                                  .data(dockerDataBuilder.build())
                                                                  .relationships(buildPackageRelationships(applicationGuid))
                                                                  .build();
        CreatePackageResponse packageResponse = delegate.packages()
                                                        .create(packageRequest)
                                                        .block();
        return getPackage(UUID.fromString(packageResponse.getId()));
    }

    private void addNonNullDockerCredentials(DockerCredentials dockerCredentials,
                                             org.cloudfoundry.client.v3.packages.DockerData.Builder dockerDataBuilder) {
        String username = dockerCredentials.getUsername();
        if (username != null) {
            dockerDataBuilder.username(username);
        }
        String password = dockerCredentials.getPassword();
        if (password != null) {
            dockerDataBuilder.password(password);
        }
    }

    private Flux<RoleResource> getRoles(UUID spaceGuid, UUID userGuid) {
        IntFunction<ListRolesRequest> pageRequestSupplier = page -> ListRolesRequest.builder()
                                                                                    .page(page)
                                                                                    .spaceId(spaceGuid.toString())
                                                                                    .userId(userGuid.toString())
                                                                                    .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.rolesV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private CloudApplication addMetadataIfNotNull(CloudApplication application) {
        return application == null ? null : addMetadata(application);
    }

    private CloudApplication addMetadata(CloudApplication application) {
        UUID appGuid = getGuid(application);
        Map<String, Metadata> applicationsMetadata = getApplicationsMetadataInBatches(Collections.singletonList(appGuid));
        return addMetadata(Collections.singletonList(application), applicationsMetadata).get(0);
    }

    private Map<String, Metadata> getApplicationsMetadataInBatches(List<UUID> appGuids) {
        Map<String, Metadata> applicationsMetadata = new HashMap<>();
        for (List<UUID> batchOfAppGuids : toBatches(appGuids, MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST)) {
            applicationsMetadata.putAll(getApplicationsMetadata(batchOfAppGuids));
        }
        return applicationsMetadata;
    }

    private Map<String, Metadata> getApplicationsMetadata(List<UUID> appGuids) {
        IntFunction<ListApplicationsRequest> pageRequestSupplier = page -> ListApplicationsRequest.builder()
                                                                                                  .spaceId(getTargetSpaceGuid().toString())
                                                                                                  .addAllApplicationIds(toString(appGuids))
                                                                                                  .page(page)
                                                                                                  .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.applicationsV3()
                                                                        .list(pageRequestSupplier.apply(page)))
                              .collectMap(ApplicationResource::getName, ApplicationResource::getMetadata)
                              .block();
    }

    private List<String> toString(List<UUID> guids) {
        return guids.stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());
    }

    private List<CloudApplication> addMetadataIfNotEmpty(List<CloudApplication> applications) {
        return applications.isEmpty() ? applications : addMetadata(applications);
    }

    private List<CloudApplication> addMetadata(List<CloudApplication> applications) {
        Map<String, Metadata> applicationsMetadata = getApplicationsMetadataInBatches(applications.stream()
                                                                                                  .map(CloudApplication::getMetadata)
                                                                                                  .map(CloudMetadata::getGuid)
                                                                                                  .collect(Collectors.toList()));
        return addMetadata(applications, applicationsMetadata);
    }

    private List<CloudApplication> addMetadata(List<CloudApplication> applications, Map<String, Metadata> applicationsMetadata) {
        return applications.stream()
                           .map(application -> addMetadata(application, applicationsMetadata))
                           .collect(Collectors.toList());
    }

    private CloudApplication addMetadata(CloudApplication application, Map<String, Metadata> applicationsMetadata) {
        String appName = application.getName();
        Metadata metadata = applicationsMetadata.get(appName);
        return ImmutableCloudApplication.copyOf(application)
                                        .withV3Metadata(metadata);
    }

    private CloudApplication findApplicationByName(String name, boolean required) {
        CloudApplication application = findApplicationByName(name);
        if (application == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application " + name + " not found.");
        }
        return application;
    }

    private CloudApplication findApplicationByName(String name) {
        return fetchWithAuxiliaryContent(() -> getApplicationByName(name), this::zipWithAuxiliaryApplicationContent);
    }

    private CloudApplication findApplication(UUID guid) {
        return fetchWithAuxiliaryContent(() -> getApplicationByGuid(guid), this::zipWithAuxiliaryApplicationContent);
    }

    private Flux<? extends Application> getApplicationResources() {
        IntFunction<ListApplicationsRequest> pageRequestSupplier = page -> ListApplicationsRequest.builder()
                                                                                                  .spaceId(getTargetSpaceGuid().toString())
                                                                                                  .page(page)
                                                                                                  .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.applicationsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Mono<? extends Application> getApplicationByGuid(UUID guid) {
        return delegate.applicationsV3()
                       .get(GetApplicationRequest.builder()
                                                 .applicationId(guid.toString())
                                                 .build());
    }

    private Mono<? extends Application> getApplicationByName(String name) {
        IntFunction<ListApplicationsRequest> pageRequestSupplier = page -> ListApplicationsRequest.builder()
                                                                                                  .spaceId(getTargetSpaceGuid().toString())
                                                                                                  .name(name)
                                                                                                  .page(page)
                                                                                                  .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.applicationsV3()
                                                                        .list(pageRequestSupplier.apply(page)))
                              .singleOrEmpty();
    }

    private Flux<? extends Application> getApplicationResourcesByNamesInBatches(Collection<String> names) {
        return Flux.fromIterable(toBatches(names, MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST))
                   .flatMap(this::getApplicationResourcesByNames);
    }

    private Flux<? extends Application> getApplicationResourcesByNames(Collection<String> names) {
        if (names.isEmpty()) {
            return Flux.empty();
        }
        IntFunction<ListApplicationsRequest> pageRequestSupplier = page -> ListApplicationsRequest.builder()
                                                                                                  .spaceId(getTargetSpaceGuid().toString())
                                                                                                  .addAllNames(names)
                                                                                                  .page(page)
                                                                                                  .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.applicationsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Mono<Derivable<CloudApplication>> zipWithAuxiliaryApplicationContent(Application application) {
        UUID applicationGuid = getGuid(application);
        return getApplicationSummary(applicationGuid).zipWhen(this::getApplicationStackResource)
                                                     .map(tuple -> ImmutableRawCloudApplication.builder()
                                                                                               .application(application)
                                                                                               .summary(tuple.getT1())
                                                                                               .stack(ImmutableRawCloudStack.of(tuple.getT2()))
                                                                                               .space(target)
                                                                                               .build());
    }

    // TODO: Use v3 if possible. Currently no summary endpoint, the "include" parameter
    // might be used if it supports all of the required resources
    // https://github.com/cloudfoundry/cloud_controller_ng/issues/1990
    @Deprecated
    private Mono<SummaryApplicationResponse> getApplicationSummary(UUID guid) {
        SummaryApplicationRequest request = SummaryApplicationRequest.builder()
                                                                     .applicationId(guid.toString())
                                                                     .build();
        return delegate.applicationsV2()
                       .summary(request);
    }

    private Mono<? extends Resource<StackEntity>> getApplicationStackResource(SummaryApplicationResponse summary) {
        UUID stackGuid = UUID.fromString(summary.getStackId());
        return getStackResource(stackGuid);
    }

    private CloudServiceInstance findServiceInstanceByName(String name) {
        return fetchWithAuxiliaryContent(() -> getServiceInstanceResourceByName(name), this::zipWithAuxiliaryServiceInstanceContent);
    }

    private Flux<? extends Resource<UnionServiceInstanceEntity>> getServiceInstanceResources() {
        IntFunction<ListSpaceServiceInstancesRequest> pageRequestSupplier = page -> ListSpaceServiceInstancesRequest.builder()
                                                                                                                    .returnUserProvidedServiceInstances(true)
                                                                                                                    .spaceId(getTargetSpaceGuid().toString())
                                                                                                                    .page(page)
                                                                                                                    .build();
        return getServiceInstanceResources(pageRequestSupplier);
    }

    private Mono<? extends Resource<UnionServiceInstanceEntity>> getServiceInstanceResourceByName(String name) {
        IntFunction<ListSpaceServiceInstancesRequest> pageRequestSupplier = page -> ListSpaceServiceInstancesRequest.builder()
                                                                                                                    .returnUserProvidedServiceInstances(true)
                                                                                                                    .spaceId(getTargetSpaceGuid().toString())
                                                                                                                    .name(name)
                                                                                                                    .page(page)
                                                                                                                    .build();
        return getServiceInstanceResources(pageRequestSupplier).singleOrEmpty();
    }

    private Flux<? extends Resource<UnionServiceInstanceEntity>> getServiceInstanceResourcesByNamesInBatches(Collection<String> names) {
        return Flux.fromIterable(toBatches(names, MAX_CHAR_LENGTH_FOR_PARAMS_IN_REQUEST))
                   .flatMap(this::getServiceInstanceResourcesByNames);
    }

    private Flux<? extends Resource<UnionServiceInstanceEntity>> getServiceInstanceResourcesByNames(Collection<String> names) {
        if (names.isEmpty()) {
            return Flux.empty();
        }
        IntFunction<ListSpaceServiceInstancesRequest> pageRequestSupplier = page -> ListSpaceServiceInstancesRequest.builder()
                                                                                                                    .returnUserProvidedServiceInstances(true)
                                                                                                                    .spaceId(getTargetSpaceGuid().toString())
                                                                                                                    .addAllNames(names)
                                                                                                                    .page(page)
                                                                                                                    .build();
        return getServiceInstanceResources(pageRequestSupplier);
    }

    private Flux<? extends Resource<UnionServiceInstanceEntity>>
            getServiceInstanceResources(IntFunction<ListSpaceServiceInstancesRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV2Resources(page -> delegate.spaces()
                                                                        .listServiceInstances(pageRequestSupplier.apply(page)));
    }

    private Mono<Derivable<CloudServiceInstance>>
            zipWithAuxiliaryServiceInstanceContent(Resource<UnionServiceInstanceEntity> serviceInstanceResource) {
        UnionServiceInstanceEntity serviceInstance = serviceInstanceResource.getEntity();
        if (isUserProvided(serviceInstance)) {
            return Mono.just(ImmutableRawCloudServiceInstance.of(serviceInstanceResource));
        }
        UUID serviceGuid = UUID.fromString(serviceInstance.getServiceId());
        UUID servicePlanGuid = UUID.fromString(serviceInstance.getServicePlanId());
        return Mono.zip(Mono.just(serviceInstanceResource), getServiceResource(serviceGuid), getServicePlanResource(servicePlanGuid))
                   .map(tuple -> ImmutableRawCloudServiceInstance.builder()
                                                                 .resource(tuple.getT1())
                                                                 .serviceResource(tuple.getT2())
                                                                 .servicePlanResource(tuple.getT3())
                                                                 .build());
    }

    private boolean isUserProvided(UnionServiceInstanceEntity serviceInstance) {
        return ServiceInstanceType.valueOfWithDefault(serviceInstance.getType())
                                  .equals(ServiceInstanceType.USER_PROVIDED);
    }

    private List<UUID> getServiceBindingGuids(UUID applicationGuid) {
        Flux<? extends Resource<ServiceBindingEntity>> bindings = getServiceBindingResourcesByApplicationGuid(applicationGuid);
        return getGuids(bindings);
    }

    private Flux<? extends Resource<ServiceBindingEntity>> getServiceBindingResourcesByServiceInstanceGuid(UUID serviceInstanceGuid) {
        IntFunction<ListServiceBindingsRequest> pageRequestSupplier = page -> ListServiceBindingsRequest.builder()
                                                                                                        .serviceInstanceId(serviceInstanceGuid.toString())
                                                                                                        .page(page)
                                                                                                        .build();
        return PaginationUtils.requestClientV2Resources(page -> delegate.serviceBindingsV2()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Mono<? extends Resource<ServiceBindingEntity>>
            getServiceBindingResourceByApplicationGuidAndServiceInstanceGuid(UUID applicationGuid, UUID serviceInstanceGuid) {
        IntFunction<ListApplicationServiceBindingsRequest> pageRequestSupplier = page -> ListApplicationServiceBindingsRequest.builder()
                                                                                                                              .applicationId(applicationGuid.toString())
                                                                                                                              .serviceInstanceId(serviceInstanceGuid.toString())
                                                                                                                              .page(page)
                                                                                                                              .build();
        return getApplicationServiceBindingResources(pageRequestSupplier).singleOrEmpty();
    }

    private Flux<? extends Resource<ServiceBindingEntity>> getServiceBindingResourcesByApplicationGuid(UUID applicationGuid) {
        IntFunction<ListApplicationServiceBindingsRequest> pageRequestSupplier = page -> ListApplicationServiceBindingsRequest.builder()
                                                                                                                              .applicationId(applicationGuid.toString())
                                                                                                                              .page(page)
                                                                                                                              .build();
        return getApplicationServiceBindingResources(pageRequestSupplier);
    }

    private Flux<? extends Resource<ServiceBindingEntity>>
            getApplicationServiceBindingResources(IntFunction<ListApplicationServiceBindingsRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV2Resources(page -> delegate.applicationsV2()
                                                                        .listServiceBindings(pageRequestSupplier.apply(page)));
    }

    private List<CloudServicePlan> findServicePlansByBrokerGuid(UUID brokerGuid) {
        List<CloudServiceOffering> offerings = findServiceOfferingsByBrokerGuid(brokerGuid);
        return getPlans(offerings);
    }

    private List<CloudServicePlan> getPlans(List<CloudServiceOffering> offerings) {
        return offerings.stream()
                        .map(CloudServiceOffering::getServicePlans)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toList());
    }

    private void updateServicePlanVisibility(CloudServicePlan servicePlan, boolean visibility) {
        updateServicePlanVisibility(getGuid(servicePlan), visibility);
    }

    private void updateServicePlanVisibility(UUID servicePlanGuid, boolean visibility) {
        UpdateServicePlanRequest request = UpdateServicePlanRequest.builder()
                                                                   .servicePlanId(servicePlanGuid.toString())
                                                                   .publiclyVisible(visibility)
                                                                   .build();
        delegate.servicePlans()
                .update(request)
                .block();
    }

    private Mono<? extends Task> getTaskResource(UUID guid) {
        GetTaskRequest request = GetTaskRequest.builder()
                                               .taskId(guid.toString())
                                               .build();
        return delegate.tasks()
                       .get(request);
    }

    private Flux<? extends Task> getTaskResourcesByApplicationGuid(UUID applicationGuid) {
        IntFunction<ListTasksRequest> pageRequestSupplier = page -> ListTasksRequest.builder()
                                                                                    .applicationId(applicationGuid.toString())
                                                                                    .page(page)
                                                                                    .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.tasks()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private CloudTask createTask(UUID applicationGuid, CloudTask task) {
        return fetch(() -> createTaskResource(applicationGuid, task), ImmutableRawCloudTask::of);
    }

    private Mono<? extends Task> createTaskResource(UUID applicationGuid, CloudTask task) {
        CreateTaskRequest request = CreateTaskRequest.builder()
                                                     .applicationId(applicationGuid.toString())
                                                     .command(task.getCommand())
                                                     .name(task.getName())
                                                     .memoryInMb(task.getLimits()
                                                                     .getMemory())
                                                     .diskInMb(task.getLimits()
                                                                   .getDisk())
                                                     .build();
        return delegate.tasks()
                       .create(request);
    }

    private Mono<? extends Task> cancelTaskResource(UUID taskGuid) {
        CancelTaskRequest request = CancelTaskRequest.builder()
                                                     .taskId(taskGuid.toString())
                                                     .build();
        return delegate.tasks()
                       .cancel(request);
    }

    private Mono<? extends Resource<ServiceKeyEntity>> createServiceKeyResource(CloudServiceInstance serviceInstance, String serviceKeyName,
                                                                                Map<String, Object> parameters) {
        UUID serviceInstanceGuid = getGuid(serviceInstance);

        return delegate.serviceKeys()
                       .create(CreateServiceKeyRequest.builder()
                                                      .serviceInstanceId(serviceInstanceGuid.toString())
                                                      .name(serviceKeyName)
                                                      .parameters(parameters)
                                                      .build());
    }

    private Path createTemporaryUploadFile(InputStream inputStream) throws IOException {
        Path file = Files.createTempFile("cfjava", null);
        Files.copy(inputStream, file);
        return file;
    }

    private CloudPackage startUpload(String applicationName, Path file) {
        Assert.notNull(applicationName, "AppName must not be null");
        Assert.notNull(file, "File must not be null");

        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UUID packageGuid = getGuid(createPackageForApplication(applicationGuid));

        delegate.packages()
                .upload(UploadPackageRequest.builder()
                                            .bits(file)
                                            .packageId(packageGuid.toString())
                                            .build())
                .block();

        return getPackage(packageGuid);
    }

    private CloudPackage createPackageForApplication(UUID applicationGuid) {
        return fetch(() -> createPackageResource(applicationGuid), ImmutableRawCloudPackage::of);
    }

    private Mono<? extends org.cloudfoundry.client.v3.packages.Package> createPackageResource(UUID applicationGuid) {
        CreatePackageRequest request = CreatePackageRequest.builder()
                                                           .type(PackageType.BITS)
                                                           .relationships(buildPackageRelationships(applicationGuid))
                                                           .build();
        return delegate.packages()
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

    private Mono<? extends Build> createBuildResource(UUID packageGuid) {
        CreateBuildRequest request = CreateBuildRequest.builder()
                                                       .getPackage(buildRelationship(packageGuid))
                                                       .build();
        return delegate.builds()
                       .create(request);
    }

    private Mono<? extends Build> getBuildResource(UUID buildGuid) {
        GetBuildRequest request = GetBuildRequest.builder()
                                                 .buildId(buildGuid.toString())
                                                 .build();
        return delegate.builds()
                       .get(request);
    }

    private Flux<? extends Build> getBuildResourcesByApplicationGuid(UUID applicationGuid) {
        IntFunction<ListApplicationBuildsRequest> pageRequestSupplier = page -> ListApplicationBuildsRequest.builder()
                                                                                                            .applicationId(applicationGuid.toString())
                                                                                                            .page(page)
                                                                                                            .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.applicationsV3()
                                                                        .listBuilds(pageRequestSupplier.apply(page)));
    }

    private Flux<? extends Build> getBuildResourcesByPackageGuid(UUID packageGuid) {
        IntFunction<ListBuildsRequest> pageRequestSupplier = page -> ListBuildsRequest.builder()
                                                                                      .packageId(packageGuid.toString())
                                                                                      .page(page)
                                                                                      .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.builds()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private void assertSpaceProvided(String operation) {
        Assert.notNull(target, "Unable to " + operation + " without specifying organization and space to use.");
    }

    private void removeRoutes(Set<CloudRouteSummary> routes, UUID applicationGuid) {
        for (CloudRouteSummary route : routes) {
            unbindRoute(route.getGuid(), applicationGuid);
        }
    }

    private void addRoutes(Set<CloudRouteSummary> routes, UUID applicationGuid) {
        Map<String, UUID> domains = getDomainGuids();
        for (CloudRouteSummary route : routes) {
            validateDomainForRoute(route, domains);
            UUID domainGuid = domains.get(route.getDomain());

            UUID routeGuid = getOrAddRoute(domainGuid, route.getHost(), route.getPath());

            bindRoute(routeGuid, applicationGuid);
        }
    }

    protected void validateDomainForRoute(CloudRouteSummary route, Map<String, UUID> existingDomains) {
        if (StringUtils.isEmpty(route.getDomain()) || !existingDomains.containsKey(route.getDomain())) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND,
                                              HttpStatus.NOT_FOUND.getReasonPhrase(),
                                              "Domain '" + route.getDomain() + "' not found for URI " + route.toUriString());
        }
    }

    private void unbindRoute(UUID routeGuid, UUID applicationGuid) {
        delegate.applicationsV2()
                .removeRoute(RemoveApplicationRouteRequest.builder()
                                                          .applicationId(applicationGuid.toString())
                                                          .routeId(routeGuid.toString())
                                                          .build())
                .block();
    }

    private void bindRoute(UUID routeGuid, UUID applicationGuid) {
        delegate.applicationsV2()
                .associateRoute(AssociateApplicationRouteRequest.builder()
                                                                .applicationId(applicationGuid.toString())
                                                                .routeId(routeGuid.toString())
                                                                .build())
                .block();
    }

    private UUID getOrAddRoute(UUID domainGuid, String host, String path) {
        UUID routeGuid = getRouteGuid(domainGuid, host, path);
        if (routeGuid == null) {
            routeGuid = doAddRoute(domainGuid, host, path);
        }
        return routeGuid;
    }

    private UUID doAddRoute(UUID domainGuid, String host, String path) {
        assertSpaceProvided("add route");
        CreateRouteResponse response = delegate.routes()
                                               .create(CreateRouteRequest.builder()
                                                                         .domainId(domainGuid.toString())
                                                                         .host(host)
                                                                         .path(path)
                                                                         .spaceId(getTargetSpaceGuid().toString())
                                                                         .build())
                                               .block();
        return getGuid(response);
    }

    private void doCreateDomain(String name) {
        delegate.domainsV3()
                .create(CreateDomainRequest.builder()
                                           .name(name)
                                           .relationships(DomainRelationships.builder()
                                                                             .organization(buildToOneRelationship(getTargetOrganizationGuid()))
                                                                             .build())
                                           .build())
                .block();
    }

    private void doDeleteDomain(UUID guid) {
        delegate.domainsV3()
                .delete(DeleteDomainRequest.builder()
                                           .domainId(guid.toString())
                                           .build())
                .block();
    }

    private void doDeleteRoute(UUID guid) {
        delegate.routes()
                .delete(DeleteRouteRequest.builder()
                                          .routeId(guid.toString())
                                          .build())
                .block();
    }

    private void doDeleteServiceInstance(CloudServiceInstance serviceInstance) {
        UUID serviceInstanceGuid = serviceInstance.getMetadata()
                                                  .getGuid();
        delegate.serviceInstances()
                .delete(DeleteServiceInstanceRequest.builder()
                                                    .acceptsIncomplete(true)
                                                    .serviceInstanceId(serviceInstanceGuid.toString())
                                                    .build())
                .block();
    }

    private void doUnbindServiceInstance(UUID applicationGuid, UUID serviceInstanceGuid) {
        UUID serviceBindingGuid = getServiceBindingGuid(applicationGuid, serviceInstanceGuid);
        if (serviceBindingGuid == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND,
                                              "Not Found",
                                              "Service binding between service with GUID " + serviceInstanceGuid + " and application with GUID " + applicationGuid + " not found.");
        }
        doUnbindServiceInstance(serviceBindingGuid);
    }

    private void doUnbindServiceInstance(UUID serviceBindingGuid) {
        delegate.serviceBindingsV2()
                .delete(DeleteServiceBindingRequest.builder()
                                                   .serviceBindingId(serviceBindingGuid.toString())
                                                   .build())
                .block();
    }

    private void doDeleteServiceKey(UUID guid) {
        delegate.serviceKeys()
                .delete(DeleteServiceKeyRequest.builder()
                                               .serviceKeyId(guid.toString())
                                               .build())
                .block();
    }

    private CloudDomain findDomainByName(String name, boolean required) {
        CloudDomain domain = findDomainByName(name);
        if (domain == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Domain " + name + " not found.");
        }
        return domain;
    }

    private CloudDomain findDomainByName(String name) {
        return fetch(() -> getDomainResourceByName(name), ImmutableRawCloudDomain::of);
    }

    private List<CloudDomain> findDomainsByOrganizationGuid(UUID organizationGuid) {
        return fetchList(() -> getDomainResourcesByOrganizationGuid(organizationGuid), ImmutableRawCloudDomain::of);
    }

    private Mono<DomainResource> getDomainResourceByName(String name) {
        IntFunction<ListDomainsRequest> pageRequestSupplier = page -> ListDomainsRequest.builder()
                                                                                        .name(name)
                                                                                        .page(page)
                                                                                        .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.domainsV3()
                                                                        .list(pageRequestSupplier.apply(page)))
                              .singleOrEmpty();
    }

    private Flux<DomainResource> getDomainResources() {
        IntFunction<ListDomainsRequest> pageRequestSupplier = page -> ListDomainsRequest.builder()
                                                                                        .page(page)
                                                                                        .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.domainsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Flux<DomainResource> getSharedDomainResources() {
        return getDomainResources().filter(domain -> domain.getRelationships()
                                                           .getOrganization()
                                                           .getData() != null);
    }

    private Flux<DomainResource> getPrivateDomainResources() {
        return getDomainResources().filter(domain -> domain.getRelationships()
                                                           .getOrganization()
                                                           .getData() == null);
    }

    private Flux<DomainResource> getDomainResourcesByOrganizationGuid(UUID organizationGuid) {
        IntFunction<ListOrganizationDomainsRequest> pageRequestSupplier = page -> ListOrganizationDomainsRequest.builder()
                                                                                                                .organizationId(organizationGuid.toString())
                                                                                                                .page(page)
                                                                                                                .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.organizationsV3()
                                                                        .listDomains(pageRequestSupplier.apply(page)));
    }

    private List<CloudSpace> findSpacesByOrganizationGuid(UUID organizationGuid) {
        return fetchListWithAuxiliaryContent(() -> getSpaceResourcesByOrganizationGuid(organizationGuid),
                                             this::zipWithAuxiliarySpaceContent);
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
                                         this::zipWithAuxiliarySpaceContent);
    }

    private Flux<SpaceResource> getSpaceResources() {
        IntFunction<ListSpacesRequest> pageRequestSupplier = page -> ListSpacesRequest.builder()
                                                                                      .page(page)
                                                                                      .build();
        return getSpaceResources(pageRequestSupplier);
    }

    private Mono<? extends Space> getSpaceResource(UUID guid) {
        GetSpaceRequest request = GetSpaceRequest.builder()
                                                 .spaceId(guid.toString())
                                                 .build();
        return delegate.spacesV3()
                       .get(request);
    }

    private Flux<SpaceResource> getSpaceResourcesByOrganizationGuid(UUID organizationGuid) {
        IntFunction<ListSpacesRequest> pageRequestSupplier = page -> ListSpacesRequest.builder()
                                                                                      .organizationId(organizationGuid.toString())
                                                                                      .page(page)
                                                                                      .build();
        return getSpaceResources(pageRequestSupplier);
    }

    private Mono<SpaceResource> getSpaceResourceByOrganizationGuidAndName(UUID organizationGuid, String name) {
        IntFunction<ListSpacesRequest> pageRequestSupplier = page -> ListSpacesRequest.builder()
                                                                                      .organizationId(organizationGuid.toString())
                                                                                      .name(name)
                                                                                      .page(page)
                                                                                      .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.spacesV3()
                                                                        .list(pageRequestSupplier.apply(page)))
                              .singleOrEmpty();
    }

    private Flux<SpaceResource> getSpaceResources(IntFunction<ListSpacesRequest> requestForPage) {
        return PaginationUtils.requestClientV3Resources(page -> delegate.spacesV3()
                                                                        .list(requestForPage.apply(page)));
    }

    private Mono<Derivable<CloudSpace>> zipWithAuxiliarySpaceContent(Space space) {
        UUID organizationGuid = UUID.fromString(space.getRelationships()
                                                     .getOrganization()
                                                     .getData()
                                                     .getId());
        return getOrganizationMono(organizationGuid).map(organization -> ImmutableRawCloudSpace.builder()
                                                                                               .space(space)
                                                                                               .organization(organization)
                                                                                               .build());
    }

    private Mono<? extends Derivable<CloudOrganization>> getOrganizationMono(UUID guid) {
        return fetchMono(() -> getOrganizationResource(guid), ImmutableRawCloudOrganization::of);
    }

    private CloudOrganization findOrganization(String name) {
        return fetch(() -> getOrganizationResourceByName(name), ImmutableRawCloudOrganization::of);
    }

    private Flux<OrganizationResource> getOrganizationResources() {
        IntFunction<ListOrganizationsRequest> pageRequestSupplier = page -> ListOrganizationsRequest.builder()
                                                                                                    .page(page)
                                                                                                    .build();
        return getOrganizationResources(pageRequestSupplier);
    }

    private Mono<? extends Organization> getOrganizationResource(UUID guid) {
        GetOrganizationRequest request = GetOrganizationRequest.builder()
                                                               .organizationId(guid.toString())
                                                               .build();
        return delegate.organizationsV3()
                       .get(request);
    }

    private Mono<OrganizationResource> getOrganizationResourceByName(String name) {
        IntFunction<ListOrganizationsRequest> pageRequestSupplier = page -> ListOrganizationsRequest.builder()
                                                                                                    .name(name)
                                                                                                    .page(page)
                                                                                                    .build();
        return getOrganizationResources(pageRequestSupplier).singleOrEmpty();
    }

    private Flux<OrganizationResource>
            getOrganizationResources(IntFunction<ListOrganizationsRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV3Resources(page -> delegate.organizationsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private List<CloudRoute> findRoutes(CloudDomain domain) {
        return fetchListWithAuxiliaryContent(() -> getRouteTuple(domain, getTargetSpaceGuid()), this::zipWithAuxiliaryRouteContent);
    }

    private Flux<Tuple2<? extends Resource<RouteEntity>, CloudDomain>> getRouteTuple(CloudDomain domain, UUID spaceGuid) {
        UUID domainGuid = getGuid(domain);
        return getRouteResourcesByDomainGuidAndSpaceGuid(domainGuid, spaceGuid).map(routeResource -> Tuples.of(routeResource, domain));
    }

    private Flux<? extends Resource<RouteEntity>> getRouteResourcesByDomainGuidAndSpaceGuid(UUID domainGuid, UUID spaceGuid) {
        IntFunction<ListSpaceRoutesRequest> pageRequestSupplier = page -> ListSpaceRoutesRequest.builder()
                                                                                                .domainId(domainGuid.toString())
                                                                                                .spaceId(spaceGuid.toString())
                                                                                                .page(page)
                                                                                                .build();
        return PaginationUtils.requestClientV2Resources(page -> delegate.spaces()
                                                                        .listRoutes(pageRequestSupplier.apply(page)));
    }

    private Flux<? extends Resource<RouteEntity>> getRouteResourcesByDomainGuidHostAndPath(UUID domainGuid, String host, String path) {
        ListSpaceRoutesRequest.Builder requestBuilder = ListSpaceRoutesRequest.builder();
        if (host != null) {
            requestBuilder.host(host);
        }
        if (path != null) {
            requestBuilder.path(path);
        }
        requestBuilder.spaceId(getTargetSpaceGuid().toString())
                      .domainId(domainGuid.toString());

        return PaginationUtils.requestClientV2Resources(page -> delegate.spaces()
                                                                        .listRoutes(requestBuilder.page(page)
                                                                                                  .build()));
    }

    private Mono<Derivable<CloudRoute>> zipWithAuxiliaryRouteContent(Tuple2<? extends Resource<RouteEntity>, CloudDomain> routeTuple) {
        UUID routeGuid = getGuid(routeTuple.getT1());
        return getRouteMappingResourcesByRouteGuid(routeGuid).collectList()
                                                             .map(routeMappingResources -> ImmutableRawCloudRoute.builder()
                                                                                                                 .resource(routeTuple.getT1())
                                                                                                                 .domain(routeTuple.getT2())
                                                                                                                 .routeMappingResources(routeMappingResources)
                                                                                                                 .build());
    }

    private Flux<? extends Resource<RouteMappingEntity>> getRouteMappingResourcesByRouteGuid(UUID routeGuid) {
        IntFunction<ListRouteMappingsRequest> pageRequestSupplier = page -> ListRouteMappingsRequest.builder()
                                                                                                    .routeId(routeGuid.toString())
                                                                                                    .page(page)
                                                                                                    .build();
        return PaginationUtils.requestClientV2Resources(page -> delegate.routeMappings()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private List<CloudServiceOffering> findServiceOfferingsByBrokerGuid(UUID brokerGuid) {
        return fetchListWithAuxiliaryContent(() -> getServiceResourcesByBrokerGuid(brokerGuid),
                                             this::zipWithAuxiliaryServiceOfferingContent);
    }

    private List<CloudServiceOffering> findServiceOfferingsByLabel(String label) {
        Assert.notNull(label, "Service label must not be null");
        return fetchListWithAuxiliaryContent(() -> getServiceResourcesByLabel(label), this::zipWithAuxiliaryServiceOfferingContent);
    }

    private Flux<? extends Resource<ServiceEntity>> getServiceResources() {
        IntFunction<ListSpaceServicesRequest> pageRequestSupplier = page -> ListSpaceServicesRequest.builder()
                                                                                                    .spaceId(getTargetSpaceGuid().toString())
                                                                                                    .page(page)
                                                                                                    .build();
        return getServiceResources(pageRequestSupplier);
    }

    protected Mono<? extends Resource<ServiceEntity>> getServiceResource(UUID serviceGuid) {
        GetServiceRequest request = GetServiceRequest.builder()
                                                     .serviceId(serviceGuid.toString())
                                                     .build();
        return delegate.services()
                       .get(request)
                       // The user may not be able to see this service, even though he created an instance from it, at some point in
                       // the past.
                       .onErrorResume(this::isForbidden, t -> Mono.empty());
    }

    private Flux<? extends Resource<ServiceEntity>> getServiceResourcesByBrokerGuid(UUID brokerGuid) {
        IntFunction<ListSpaceServicesRequest> pageRequestSupplier = page -> ListSpaceServicesRequest.builder()
                                                                                                    .serviceBrokerId(brokerGuid.toString())
                                                                                                    .spaceId(getTargetSpaceGuid().toString())
                                                                                                    .page(page)
                                                                                                    .build();
        return getServiceResources(pageRequestSupplier);
    }

    private Flux<? extends Resource<ServiceEntity>> getServiceResourcesByLabel(String label) {
        IntFunction<ListSpaceServicesRequest> pageRequestSupplier = page -> ListSpaceServicesRequest.builder()
                                                                                                    .label(label)
                                                                                                    .spaceId(getTargetSpaceGuid().toString())
                                                                                                    .page(page)
                                                                                                    .build();
        return getServiceResources(pageRequestSupplier);
    }

    private Flux<? extends Resource<ServiceEntity>> getServiceResources(IntFunction<ListSpaceServicesRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV2Resources(page -> delegate.spaces()
                                                                        .listServices(pageRequestSupplier.apply(page)));
    }

    private Mono<Derivable<CloudServiceOffering>> zipWithAuxiliaryServiceOfferingContent(Resource<ServiceEntity> resource) {
        UUID serviceGuid = getGuid(resource);
        return getServicePlansFlux(serviceGuid).collectList()
                                               .map(servicePlans -> ImmutableRawCloudServiceOffering.builder()
                                                                                                    .resource(resource)
                                                                                                    .servicePlans(servicePlans)
                                                                                                    .build());
    }

    private Flux<CloudServicePlan> getServicePlansFlux(UUID serviceGuid) {
        return fetchFlux(() -> getServicePlanResourcesByServiceGuid(serviceGuid), ImmutableRawCloudServicePlan::of);
    }

    protected Mono<? extends Resource<ServicePlanEntity>> getServicePlanResource(UUID servicePlanGuid) {
        GetServicePlanRequest request = GetServicePlanRequest.builder()
                                                             .servicePlanId(servicePlanGuid.toString())
                                                             .build();
        return delegate.servicePlans()
                       .get(request)
                       // The user may not be able to see this service plan, even though he created an instance from it, at some point in
                       // the past.
                       .onErrorResume(this::isForbidden, t -> Mono.empty());
    }

    private Flux<? extends Resource<ServicePlanEntity>> getServicePlanResourcesByServiceGuid(UUID serviceGuid) {
        IntFunction<ListServicePlansRequest> pageRequestSupplier = page -> ListServicePlansRequest.builder()
                                                                                                  .serviceId(serviceGuid.toString())
                                                                                                  .page(page)
                                                                                                  .build();
        return PaginationUtils.requestClientV2Resources(page -> delegate.servicePlans()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Flux<Tuple2<? extends Resource<ServiceKeyEntity>, CloudServiceInstance>>
            getServiceKeyTuple(CloudServiceInstance serviceInstance) {
        UUID serviceInstanceGuid = getGuid(serviceInstance);
        return getServiceKeyResourcesByServiceInstanceGuid(serviceInstanceGuid).map(serviceKeyResource -> Tuples.of(serviceKeyResource,
                                                                                                                    serviceInstance));
    }

    private Flux<? extends Resource<ServiceKeyEntity>> getServiceKeyResourcesByServiceInstanceGuid(UUID serviceInstanceGuid) {
        IntFunction<ListServiceKeysRequest> pageRequestSupplier = page -> ListServiceKeysRequest.builder()
                                                                                                .serviceInstanceId(serviceInstanceGuid.toString())
                                                                                                .page(page)
                                                                                                .build();
        return PaginationUtils.requestClientV2Resources(page -> delegate.serviceKeys()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private List<CloudRoute> fetchOrphanRoutes(String domainName) {
        return getRoutes(domainName).stream()
                                    .filter(route -> !route.isUsed())
                                    .collect(Collectors.toList());
    }

    private CloudStack findStackResource(String name) {
        return fetch(() -> getStackResourceByName(name), ImmutableRawCloudStack::of);
    }

    private Flux<? extends Resource<StackEntity>> getStackResources() {
        IntFunction<ListStacksRequest> pageRequestSupplier = page -> ListStacksRequest.builder()
                                                                                      .page(page)
                                                                                      .build();
        return getStackResources(pageRequestSupplier);
    }

    private Mono<? extends Resource<StackEntity>> getStackResource(UUID guid) {
        GetStackRequest request = GetStackRequest.builder()
                                                 .stackId(guid.toString())
                                                 .build();
        return delegate.stacks()
                       .get(request);
    }

    private Mono<? extends Resource<StackEntity>> getStackResourceByName(String name) {
        IntFunction<ListStacksRequest> pageRequestSupplier = page -> ListStacksRequest.builder()
                                                                                      .name(name)
                                                                                      .page(page)
                                                                                      .build();
        return getStackResources(pageRequestSupplier).singleOrEmpty();
    }

    private Flux<? extends Resource<StackEntity>> getStackResources(IntFunction<ListStacksRequest> requestForPage) {
        return PaginationUtils.requestClientV2Resources(page -> delegate.stacks()
                                                                        .list(requestForPage.apply(page)));
    }

    private List<CloudEvent> findEventsByTarget(String target) {
        return fetchList(() -> getEventResourcesByTarget(target), ImmutableRawCloudEvent::of);
    }

    private Flux<AuditEventResource> getEventResources() {
        IntFunction<ListAuditEventsRequest> pageRequestSupplier = page -> ListAuditEventsRequest.builder()
                                                                                                .page(page)
                                                                                                .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.auditEventsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private Flux<AuditEventResource> getEventResourcesByTarget(String target) {
        IntFunction<ListAuditEventsRequest> pageRequestSupplier = page -> ListAuditEventsRequest.builder()
                                                                                                .targetId(target)
                                                                                                .page(page)
                                                                                                .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.auditEventsV3()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private InstancesInfo findApplicationInstances(UUID applicationGuid) {
        return fetch(() -> getApplicationInstancesResource(applicationGuid), ImmutableRawInstancesInfo::of);
    }

    private Mono<ApplicationInstancesResponse> getApplicationInstancesResource(UUID applicationGuid) {
        ApplicationInstancesRequest request = ApplicationInstancesRequest.builder()
                                                                         .applicationId(applicationGuid.toString())
                                                                         .build();
        // TODO: Use v3 API when this issue is fixed: https://github.com/cloudfoundry/cf-java-client/issues/1086
        return delegate.applicationsV2()
                       .instances(request);
    }

    private CloudServiceBroker findServiceBrokerByName(String name) {
        return fetch(() -> getServiceBrokerResourceByName(name), ImmutableRawCloudServiceBroker::of);
    }

    private Flux<? extends Resource<ServiceBrokerEntity>> getServiceBrokerResources() {
        IntFunction<ListServiceBrokersRequest> pageRequestSupplier = page -> ListServiceBrokersRequest.builder()
                                                                                                      .page(page)
                                                                                                      .build();
        return getServiceBrokerResources(pageRequestSupplier);
    }

    private Mono<? extends Resource<ServiceBrokerEntity>> getServiceBrokerResourceByName(String name) {
        IntFunction<ListServiceBrokersRequest> pageRequestSupplier = page -> ListServiceBrokersRequest.builder()
                                                                                                      .page(page)
                                                                                                      .name(name)
                                                                                                      .build();
        return getServiceBrokerResources(pageRequestSupplier).singleOrEmpty();
    }

    private Flux<? extends Resource<ServiceBrokerEntity>>
            getServiceBrokerResources(IntFunction<ListServiceBrokersRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV2Resources(page -> delegate.serviceBrokers()
                                                                        .list(pageRequestSupplier.apply(page)));
    }

    private CloudServicePlan findPlanForService(CloudServiceInstance service, String planName) {
        List<CloudServiceOffering> offerings = findServiceOfferingsByLabel(service.getLabel());
        for (CloudServiceOffering offering : offerings) {
            if (service.getBroker() != null && !service.getBroker()
                                                       .equals(offering.getBrokerName())) {
                continue;
            }
            if (service.getVersion() == null || service.getVersion()
                                                       .equals(offering.getVersion())) {
                for (CloudServicePlan plan : offering.getServicePlans()) {
                    if (plan.getName()
                            .equals(planName)) {
                        return plan;
                    }
                }
            }
        }
        throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service plan " + service.getPlan() + " not found.");
    }

    private UUID getRequiredApplicationGuid(String name) {
        org.cloudfoundry.client.v3.Resource applicationResource = getApplicationByName(name).block();
        if (applicationResource == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application " + name + " not found.");
        }
        return UUID.fromString(applicationResource.getId());
    }

    private Mono<ServiceInstanceResource> getServiceInstanceByName(String name) {
        IntFunction<ListServiceInstancesRequest> pageRequestSupplier = page -> ListServiceInstancesRequest.builder()
                                                                                                          .spaceId(getTargetSpaceGuid().toString())
                                                                                                          .serviceInstanceName(name)
                                                                                                          .page(page)
                                                                                                          .build();
        return PaginationUtils.requestClientV3Resources(page -> delegate.serviceInstancesV3()
                                                                        .list(pageRequestSupplier.apply(page)))
                              .singleOrEmpty();
    }

    private UUID getRequiredDomainGuid(String name) {
        return getGuid(findDomainByName(name, true));
    }

    private UUID getOrganizationGuid(String organizationName, boolean required) {
        CloudOrganization organization = getOrganization(organizationName, required);
        return organization != null ? organization.getMetadata()
                                                  .getGuid()
            : null;
    }

    private Map<String, UUID> getDomainGuids() {
        List<CloudDomain> availableDomains = new ArrayList<>();
        availableDomains.addAll(getDomainsForOrganization());
        availableDomains.addAll(getSharedDomains());
        Map<String, UUID> domains = new HashMap<>(availableDomains.size());
        for (CloudDomain availableDomain : availableDomains) {
            domains.put(availableDomain.getName(), availableDomain.getMetadata()
                                                                  .getGuid());
        }
        return domains;
    }

    private UUID getRouteGuid(UUID domainGuid, String host, String path) {
        List<? extends Resource<RouteEntity>> routeEntitiesResource = getRouteResourcesByDomainGuidHostAndPath(domainGuid, host,
                                                                                                               path).collect(Collectors.toList())
                                                                                                                    .block();
        if (CollectionUtils.isEmpty(routeEntitiesResource)) {
            return null;
        }
        return getGuid(routeEntitiesResource.get(0));
    }

    private UUID getServiceBindingGuid(UUID applicationGuid, UUID serviceInstanceGuid) {
        return getServiceBindingResourceByApplicationGuidAndServiceInstanceGuid(applicationGuid, serviceInstanceGuid).map(this::getGuid)
                                                                                                                     .block();
    }

    private List<UUID> getGuids(Flux<? extends Resource<?>> resources) {
        return resources.map(this::getGuid)
                        .collectList()
                        .block();
    }

    private void processAsyncUploadInBackground(CloudPackage cloudPackage, UploadStatusCallback callback) {
        String threadName = String.format("App upload monitor: %s", cloudPackage.getGuid());
        new Thread(() -> processAsyncUpload(cloudPackage, callback), threadName).start();
    }

    private void processAsyncUpload(CloudPackage cloudPackage, UploadStatusCallback callback) {
        if (callback == null) {
            callback = UploadStatusCallback.NONE;
        }
        while (true) {
            Upload upload = getUploadStatus(cloudPackage.getGuid());
            Status uploadStatus = upload.getStatus();
            boolean unsubscribe = callback.onProgress(uploadStatus.toString());
            if (unsubscribe || isUploadReady(uploadStatus)) {
                return;
            }
            if (hasUploadFailed(uploadStatus)) {
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

    private boolean isUploadReady(Status status) {
        return status == Status.READY;
    }

    private boolean hasUploadFailed(Status status) {
        return status == Status.EXPIRED || status == Status.FAILED;
    }

    @Override
    public CloudPackage getPackage(UUID packageGuid) {
        return fetch(() -> getPackageResource(packageGuid), ImmutableRawCloudPackage::of);
    }

    private Mono<? extends org.cloudfoundry.client.v3.packages.Package> getPackageResource(UUID guid) {
        GetPackageRequest request = GetPackageRequest.builder()
                                                     .packageId(guid.toString())
                                                     .build();
        return delegate.packages()
                       .get(request);
    }

    private String getTargetOrganizationName() {
        return getName(target.getOrganization());
    }

    private String getName(CloudEntity entity) {
        return entity == null ? null : entity.getName();
    }

    private UUID getTargetOrganizationGuid() {
        return getGuid(target.getOrganization());
    }

    private UUID getTargetSpaceGuid() {
        return getGuid(target);
    }

    private UUID getGuid(org.cloudfoundry.client.v2.Resource<?> resource) {
        return Optional.ofNullable(resource)
                       .map(org.cloudfoundry.client.v2.Resource::getMetadata)
                       .map(org.cloudfoundry.client.v2.Metadata::getId)
                       .map(UUID::fromString)
                       .orElse(null);
    }

    private UUID getGuid(CloudEntity entity) {
        return Optional.ofNullable(entity)
                       .map(CloudEntity::getMetadata)
                       .map(CloudMetadata::getGuid)
                       .orElse(null);
    }

    private UUID getGuid(org.cloudfoundry.client.v3.Resource resource) {
        return Optional.ofNullable(resource)
                       .map(org.cloudfoundry.client.v3.Resource::getId)
                       .map(UUID::fromString)
                       .orElse(null);
    }

    private boolean isForbidden(Throwable t) {
        if (t instanceof AbstractCloudFoundryException) {
            AbstractCloudFoundryException e = (AbstractCloudFoundryException) t;
            return isForbidden(e);
        }
        return false;
    }

    private boolean isForbidden(AbstractCloudFoundryException e) {
        return e.getStatusCode() == HttpStatus.FORBIDDEN.value();
    }

    private <T, R, D extends Derivable<T>> Flux<T> fetchFluxWithAuxiliaryContent(Supplier<Flux<R>> resourceSupplier,
                                                                                 Function<R, Mono<D>> resourceMapper) {
        return resourceSupplier.get()
                               .flatMap(resourceMapper)
                               .map(Derivable::derive);
    }

    private <T, R, D extends Derivable<T>> List<T> fetchListWithAuxiliaryContent(Supplier<Flux<R>> resourceSupplier,
                                                                                 Function<R, Mono<D>> resourceMapper) {
        return fetchFluxWithAuxiliaryContent(resourceSupplier, resourceMapper).collectList()
                                                                              .block();
    }

    private <T, R, D extends Derivable<T>> List<T> fetchList(Supplier<Flux<R>> resourceSupplier, Function<R, D> resourceMapper) {
        return fetchFlux(resourceSupplier, resourceMapper).collectList()
                                                          .block();
    }

    private <T, R, D extends Derivable<T>> Flux<T> fetchFlux(Supplier<Flux<R>> resourceSupplier, Function<R, D> resourceMapper) {
        return resourceSupplier.get()
                               .map(resourceMapper)
                               .map(Derivable::derive);
    }

    private <T, R, D extends Derivable<T>> T fetchWithAuxiliaryContent(Supplier<Mono<R>> resourceSupplier,
                                                                       Function<R, Mono<D>> resourceMapper) {
        return fetchMonoWithAuxiliaryContent(resourceSupplier, resourceMapper).block();
    }

    private <T, R, D extends Derivable<T>> T fetch(Supplier<Mono<R>> resourceSupplier, Function<R, D> resourceMapper) {
        return fetchMono(resourceSupplier, resourceMapper).block();
    }

    private <T, R, D extends Derivable<T>> Mono<T> fetchMonoWithAuxiliaryContent(Supplier<Mono<R>> resourceSupplier,
                                                                                 Function<R, Mono<D>> resourceMapper) {
        return resourceSupplier.get()
                               .flatMap(resourceMapper)
                               .map(Derivable::derive);
    }

    private <T, R, D extends Derivable<T>> Mono<T> fetchMono(Supplier<Mono<R>> resourceSupplier, Function<R, D> resourceMapper) {
        return resourceSupplier.get()
                               .map(resourceMapper)
                               .map(Derivable::derive);
    }

}
