/*
 * Copyright 2009-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cloudfoundry.client.lib.rest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.ClientHttpResponseCallback;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.RestLogCallback;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudApplication;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudBuild;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudDomain;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudEvent;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudInfo;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudOrganization;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudPackage;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudPrivateDomain;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudRoute;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudService;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudServiceBinding;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudServiceBroker;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudServiceInstance;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudServiceKey;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudServiceOffering;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudServicePlan;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudSharedDomain;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudSpace;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudStack;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudTask;
import org.cloudfoundry.client.lib.adapters.ImmutableRawInstancesInfo;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudPackage;
import org.cloudfoundry.client.lib.domain.CloudQuota;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudSecurityGroup;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.CloudUser;
import org.cloudfoundry.client.lib.domain.CrashesInfo;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.ErrorDetails;
import org.cloudfoundry.client.lib.domain.ImmutableErrorDetails;
import org.cloudfoundry.client.lib.domain.ImmutableUpload;
import org.cloudfoundry.client.lib.domain.ImmutableUploadToken;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Status;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.applications.ApplicationEntity;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesRequest;
import org.cloudfoundry.client.v2.applications.ApplicationInstancesResponse;
import org.cloudfoundry.client.v2.applications.AssociateApplicationRouteRequest;
import org.cloudfoundry.client.v2.applications.DeleteApplicationRequest;
import org.cloudfoundry.client.v2.applications.GetApplicationRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationServiceBindingsRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationsRequest;
import org.cloudfoundry.client.v2.applications.RemoveApplicationRouteRequest;
import org.cloudfoundry.client.v2.applications.SummaryApplicationRequest;
import org.cloudfoundry.client.v2.applications.SummaryApplicationResponse;
import org.cloudfoundry.client.v2.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v2.domains.CreateDomainRequest;
import org.cloudfoundry.client.v2.domains.DomainEntity;
import org.cloudfoundry.client.v2.domains.ListDomainsRequest;
import org.cloudfoundry.client.v2.events.EventEntity;
import org.cloudfoundry.client.v2.events.ListEventsRequest;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.client.v2.organizations.GetOrganizationRequest;
import org.cloudfoundry.client.v2.organizations.ListOrganizationPrivateDomainsRequest;
import org.cloudfoundry.client.v2.organizations.ListOrganizationSpacesRequest;
import org.cloudfoundry.client.v2.organizations.ListOrganizationsRequest;
import org.cloudfoundry.client.v2.organizations.OrganizationEntity;
import org.cloudfoundry.client.v2.privatedomains.DeletePrivateDomainRequest;
import org.cloudfoundry.client.v2.privatedomains.ListPrivateDomainsRequest;
import org.cloudfoundry.client.v2.privatedomains.PrivateDomainEntity;
import org.cloudfoundry.client.v2.routemappings.ListRouteMappingsRequest;
import org.cloudfoundry.client.v2.routemappings.RouteMappingEntity;
import org.cloudfoundry.client.v2.routes.CreateRouteRequest;
import org.cloudfoundry.client.v2.routes.CreateRouteResponse;
import org.cloudfoundry.client.v2.routes.DeleteRouteRequest;
import org.cloudfoundry.client.v2.routes.RouteEntity;
import org.cloudfoundry.client.v2.servicebindings.CreateServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.DeleteServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.ListServiceBindingsRequest;
import org.cloudfoundry.client.v2.servicebindings.ServiceBindingEntity;
import org.cloudfoundry.client.v2.servicebrokers.CreateServiceBrokerRequest;
import org.cloudfoundry.client.v2.servicebrokers.DeleteServiceBrokerRequest;
import org.cloudfoundry.client.v2.servicebrokers.ListServiceBrokersRequest;
import org.cloudfoundry.client.v2.servicebrokers.ServiceBrokerEntity;
import org.cloudfoundry.client.v2.servicebrokers.UpdateServiceBrokerRequest;
import org.cloudfoundry.client.v2.serviceinstances.CreateServiceInstanceRequest;
import org.cloudfoundry.client.v2.serviceinstances.DeleteServiceInstanceRequest;
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
import org.cloudfoundry.client.v2.services.ListServicesRequest;
import org.cloudfoundry.client.v2.services.ServiceEntity;
import org.cloudfoundry.client.v2.shareddomains.ListSharedDomainsRequest;
import org.cloudfoundry.client.v2.shareddomains.SharedDomainEntity;
import org.cloudfoundry.client.v2.spaces.GetSpaceRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceApplicationsRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceAuditorsRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceDevelopersRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceManagersRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceRoutesRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceServiceInstancesRequest;
import org.cloudfoundry.client.v2.spaces.ListSpacesRequest;
import org.cloudfoundry.client.v2.spaces.SpaceEntity;
import org.cloudfoundry.client.v2.stacks.GetStackRequest;
import org.cloudfoundry.client.v2.stacks.ListStacksRequest;
import org.cloudfoundry.client.v2.stacks.StackEntity;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.CreateUserProvidedServiceInstanceRequest;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.ListUserProvidedServiceInstanceServiceBindingsRequest;
import org.cloudfoundry.client.v2.users.UserEntity;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.applications.ListApplicationBuildsRequest;
import org.cloudfoundry.client.v3.applications.SetApplicationCurrentDropletRequest;
import org.cloudfoundry.client.v3.builds.Build;
import org.cloudfoundry.client.v3.builds.CreateBuildRequest;
import org.cloudfoundry.client.v3.builds.GetBuildRequest;
import org.cloudfoundry.client.v3.packages.CreatePackageRequest;
import org.cloudfoundry.client.v3.packages.GetPackageRequest;
import org.cloudfoundry.client.v3.packages.PackageRelationships;
import org.cloudfoundry.client.v3.packages.PackageType;
import org.cloudfoundry.client.v3.packages.UploadPackageRequest;
import org.cloudfoundry.client.v3.tasks.CancelTaskRequest;
import org.cloudfoundry.client.v3.tasks.CreateTaskRequest;
import org.cloudfoundry.client.v3.tasks.GetTaskRequest;
import org.cloudfoundry.client.v3.tasks.ListTasksRequest;
import org.cloudfoundry.client.v3.tasks.Task;
import org.cloudfoundry.util.PaginationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * Abstract implementation of the CloudControllerClient intended to serve as the base.
 *
 * @author Ramnivas Laddad
 * @author A.B.Srinivasan
 * @author Jennifer Hickey
 * @author Dave Syer
 * @author Thomas Risberg
 * @author Alexander Orlov
 * @author Scott Frederick
 */
public class CloudControllerRestClientImpl implements CloudControllerRestClient {

    private static final String MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED = "Feature is not yet implemented.";
    private static final String DEFAULT_HOST_DOMAIN_SEPARATOR = "\\.";
    private static final String DEFAULT_PATH_SEPARATOR = "/";
    private static final String USER_PROVIDED_SERVICE_INSTANCE_TYPE = "user_provided_service_instance";
    private static final long JOB_POLLING_PERIOD = TimeUnit.SECONDS.toMillis(5);

    private final Log logger = LogFactory.getLog(getClass().getName());
    private CloudCredentials credentials;
    private URL controllerUrl;
    private OAuthClient oAuthClient;
    private CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();
    private RestTemplate restTemplate;
    private CloudSpace target;

    private CloudFoundryClient v3Client;

    /**
     * Only for unit tests. This works around the fact that the initialize method is called within the constructor and hence can not be
     * overloaded, making it impossible to write unit tests that don't trigger network calls.
     */
    protected CloudControllerRestClientImpl() {
    }

    public CloudControllerRestClientImpl(URL controllerUrl, CloudCredentials credentials, RestTemplate restTemplate,
        OAuthClient oAuthClient, CloudFoundryClient v3Client) {
        this(controllerUrl, credentials, restTemplate, oAuthClient, v3Client, null);
    }

    public CloudControllerRestClientImpl(URL controllerUrl, CloudCredentials credentials, RestTemplate restTemplate,
        OAuthClient oAuthClient, CloudFoundryClient v3Client, CloudSpace target) {
        Assert.notNull(controllerUrl, "CloudControllerUrl cannot be null");
        Assert.notNull(restTemplate, "RestTemplate cannot be null");
        Assert.notNull(oAuthClient, "OAuthClient cannot be null");

        this.controllerUrl = controllerUrl;
        this.credentials = credentials;
        this.restTemplate = restTemplate;
        this.oAuthClient = oAuthClient;
        this.target = target;

        this.v3Client = v3Client;
    }

    @Override
    public RestTemplate getRestTemplate() {
        return restTemplate;
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
    public void addRoute(String host, String domainName) {
        assertSpaceProvided("add route for domain");
        UUID domainGuid = getRequiredDomainGuid(domainName);
        doAddRoute(domainGuid, host, null);
    }

    @Override
    public void associateAuditorWithSpace(String organizationName, String spaceName, String userGuid) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void associateDeveloperWithSpace(String organizationName, String spaceName, String userGuid) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void associateManagerWithSpace(String organizationName, String spaceName, String userGuid) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void bindRunningSecurityGroup(String securityGroupName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void bindSecurityGroup(String organizationName, String spaceName, String securityGroupName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void bindService(String applicationName, String serviceName) {
        bindService(applicationName, serviceName, null);
    }

    @Override
    public void bindService(String applicationName, String serviceName, Map<String, Object> parameters) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UUID serviceGuid = getService(serviceName).getMetadata()
            .getGuid();
        v3Client.serviceBindingsV2()
            .create(CreateServiceBindingRequest.builder()
                .applicationId(applicationGuid.toString())
                .serviceInstanceId(serviceGuid.toString())
                .parameters(parameters)
                .build())
            .block();
    }

    @Override
    public void bindStagingSecurityGroup(String securityGroupName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void createApplication(String name, Staging staging, Integer memory, List<String> uris, List<String> serviceNames) {
        createApplication(name, staging, null, memory, uris, serviceNames, null);
    }

    @Override
    public void createApplication(String name, Staging staging, Integer diskQuota, Integer memory, List<String> uris,
        List<String> serviceNames, DockerInfo dockerInfo) {
        // TODO: Refactor with version 3.16.0.RELEASE of the v3 client.
        Map<String, Object> appRequest = new HashMap<>();
        appRequest.put("space_guid", target.getMetadata()
            .getGuid());
        appRequest.put("name", name);
        appRequest.put("memory", memory);
        if (diskQuota != null) {
            appRequest.put("disk_quota", diskQuota);
        }
        if (dockerInfo != null) {
            appRequest.put("docker_image", dockerInfo.getImage());
            if (dockerInfo.getCredentials() != null) {
                appRequest.put("docker_credentials", dockerInfo.getCredentials());
            }
        }
        appRequest.put("instances", 1);
        addStagingToRequest(staging, appRequest);
        appRequest.put("state", CloudApplication.State.STOPPED);

        String appResp = getRestTemplate().postForObject(getUrl("/v2/apps"), appRequest, String.class);
        Map<String, Object> appEntity = JsonUtil.convertJsonToMap(appResp);
        UUID newAppGuid = CloudEntityResourceMapper.getV2Metadata(appEntity)
            .getGuid();

        if (!CollectionUtils.isEmpty(serviceNames)) {
            updateApplicationServices(name, Collections.emptyMap());
        }

        if (!CollectionUtils.isEmpty(uris)) {
            addUris(uris, newAppGuid);
        }
    }

    @Override
    public void createQuota(CloudQuota quota) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void createSecurityGroup(CloudSecurityGroup securityGroup) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void createSecurityGroup(String name, InputStream jsonRulesFile) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void createService(CloudService service) {
        assertSpaceProvided("create service");
        Assert.notNull(service, "Service must not be null.");

        CloudServicePlan servicePlan = findPlanForService(service);
        UUID servicePlanGuid = servicePlan.getMetadata()
            .getGuid();
        v3Client.serviceInstances()
            .create(CreateServiceInstanceRequest.builder()
                .spaceId(getTargetSpaceGuid().toString())
                .name(service.getName())
                .servicePlanId(servicePlanGuid.toString())
                .build())
            .block();
    }

    @Override
    public void createServiceBroker(CloudServiceBroker serviceBroker) {
        Assert.notNull(serviceBroker, "Service broker must not be null.");

        v3Client.serviceBrokers()
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
    public void createServiceKey(String serviceName, String name, Map<String, Object> parameters) {
        CloudService service = getService(serviceName);
        UUID serviceGuid = service.getMetadata()
            .getGuid();

        v3Client.serviceKeys()
            .create(CreateServiceKeyRequest.builder()
                .serviceInstanceId(serviceGuid.toString())
                .name(name)
                .parameters(parameters)
                .build())
            .block();
    }

    @Override
    public void createSpace(String spaceName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials) {
        createUserProvidedService(service, credentials, "");
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials, String syslogDrainUrl) {
        assertSpaceProvided("create service");
        Assert.notNull(service, "Service must not be null.");

        v3Client.userProvidedServiceInstances()
            .create(CreateUserProvidedServiceInstanceRequest.builder()
                .spaceId(getTargetSpaceGuid().toString())
                .name(service.getName())
                .credentials(credentials)
                .build())
            .block();
    }

    @Override
    public void deleteAllApplications() {
        List<CloudApplication> applications = getApplications();
        for (CloudApplication application : applications) {
            deleteApplication(application.getName());
        }
    }

    @Override
    public void deleteAllServices() {
        List<CloudService> services = getServices();
        for (CloudService service : services) {
            doDeleteService(service);
        }
    }

    @Override
    public void deleteApplication(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        List<UUID> serviceBindingGuids = getServiceBindingGuids(applicationGuid);
        for (UUID serviceBindingGuid : serviceBindingGuids) {
            doUnbindService(serviceBindingGuid);
        }
        v3Client.applicationsV2()
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
                .getName());
            deletedRoutes.add(orphanRoute);
        }
        return deletedRoutes;
    }

    @Override
    public void deleteQuota(String quotaName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void deleteRoute(String host, String domainName) {
        assertSpaceProvided("delete route for domain");
        UUID routeGuid = getRouteGuid(getRequiredDomainGuid(domainName), host, null);
        if (routeGuid == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found",
                "Host " + host + " not found for domain " + domainName + ".");
        }
        doDeleteRoute(routeGuid);
    }

    @Override
    public void deleteSecurityGroup(String securityGroupName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void deleteService(String serviceName) {
        CloudService service = getService(serviceName);
        doDeleteService(service);
    }

    @Override
    public void deleteServiceBroker(String name) {
        CloudServiceBroker broker = getServiceBroker(name);
        UUID guid = broker.getMetadata()
            .getGuid();
        v3Client.serviceBrokers()
            .delete(DeleteServiceBrokerRequest.builder()
                .serviceBrokerId(guid.toString())
                .build())
            .block();
    }

    @Override
    public void deleteServiceKey(String serviceName, String serviceKeyName) {
        List<CloudServiceKey> serviceKeys = getServiceKeys(serviceName);
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
    public void deleteSpace(String spaceName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public CloudApplication getApplication(String applicationName) {
        return getApplication(applicationName, true);
    }

    @Override
    public CloudApplication getApplication(String applicationName, boolean required) {
        return findApplicationByName(applicationName, required);
    }

    @Override
    public CloudApplication getApplication(UUID applicationGuid) {
        return findApplication(applicationGuid);
    }

    @Override
    public Map<String, String> getApplicationEnvironment(String applicationName) {
        return getApplication(applicationName).getEnv();
    }

    @Override
    public Map<String, String> getApplicationEnvironment(UUID applicationGuid) {
        return getApplication(applicationGuid).getEnv();
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        return getApplicationEvents(applicationGuid);
    }

    @Override
    public List<CloudEvent> getApplicationEvents(UUID applicationGuid) {
        return findEventsByActee(applicationGuid.toString());
    }

    @Override
    public InstancesInfo getApplicationInstances(String applicationName) {
        CloudApplication application = getApplication(applicationName);
        return getApplicationInstances(application);
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication application) {
        if (application.getState()
            .equals(CloudApplication.State.STARTED)) {
            return findApplicationInstances(getGuid(application));
        }
        return null;
    }

    @Override
    public List<CloudApplication> getApplications() {
        return fetchListWithAuxiliaryContent(this::getApplicationResources, this::zipWithAuxiliaryApplicationContent);
    }

    @Override
    public Map<String, String> getCrashLogs(String applicationName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public CrashesInfo getCrashes(String applicationName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public CloudDomain getDefaultDomain() {
        List<CloudDomain> sharedDomains = getSharedDomains();
        if (sharedDomains.isEmpty()) {
            return null;
        } else {
            return sharedDomains.get(0);
        }
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return fetchList(this::getSharedDomainResources, ImmutableRawCloudSharedDomain::of);
    }

    @Override
    public List<CloudDomain> getDomains() {
        return getPrivateDomains();
    }

    @Override
    public List<CloudDomain> getDomainsForOrganization() {
        assertSpaceProvided("access organization domains");
        return findDomainsByOrganizationGuid(getTargetOrganizationGuid());
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return fetchList(this::getPrivateDomainResources, ImmutableRawCloudPrivateDomain::of);
    }

    @Override
    public List<CloudEvent> getEvents() {
        return fetchList(this::getEventResources, ImmutableRawCloudEvent::of);
    }

    @Override
    public String getFile(String applicationName, int instanceIndex, String filePath, int startPosition, int endPosition) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public CloudInfo getInfo() {
        return fetch(this::getInfoResource, ImmutableRawCloudInfo::of);
    }

    @Override
    public Map<String, String> getLogs(String applicationName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
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
    public Map<String, CloudUser> getOrganizationUsers(String organizationName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public List<CloudOrganization> getOrganizations() {
        return fetchList(this::getOrganizationResources, ImmutableRawCloudOrganization::of);
    }

    @Override
    public CloudQuota getQuota(String quotaName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public CloudQuota getQuota(String quotaName, boolean required) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public List<CloudQuota> getQuotas() {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public List<ApplicationLog> getRecentLogs(String applicationName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        assertSpaceProvided("get routes for domain");
        CloudDomain domain = findDomainByName(domainName, true);
        return findRoutes(domain);
    }

    @Override
    public List<CloudSecurityGroup> getRunningSecurityGroups() {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public CloudSecurityGroup getSecurityGroup(String securityGroupName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public CloudSecurityGroup getSecurityGroup(String securityGroupName, boolean required) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public List<CloudSecurityGroup> getSecurityGroups() {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public CloudService getService(String serviceName) {
        return getService(serviceName, true);
    }

    @Override
    public CloudService getService(String serviceName, boolean required) {
        CloudService service = findServiceByName(serviceName);
        if (service == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service " + serviceName + " not found.");
        }
        return service;
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
    public CloudServiceInstance getServiceInstance(String serviceName) {
        return getServiceInstance(serviceName, true);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceName, boolean required) {
        CloudServiceInstance serviceInstance = findServiceInstanceByName(serviceName);
        if (serviceInstance == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service instance " + serviceName + " not found.");
        }
        return serviceInstance;
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(String serviceName) {
        CloudService service = getService(serviceName);
        return getServiceKeys(service);
    }

    @Override
    public Map<String, Object> getServiceParameters(UUID guid) {
        return fetchParameters(String.format("/v2/service_instances/%s/parameters", guid));
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        return fetchListWithAuxiliaryContent(this::getServiceResources, this::zipWithAuxiliaryServiceOfferingContent);
    }

    @Override
    public List<CloudService> getServices() {
        return fetchListWithAuxiliaryContent(this::getServiceInstanceResources, this::zipWithAuxiliaryServiceContent);
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
        return fetchListWithAuxiliaryContent(this::getSpaceResources, this::zipWithAuxiliarySpaceContent);
    }

    @Override
    public List<CloudSpace> getSpaces(String organizationName) {
        UUID organizationGuid = getOrganizationGuid(organizationName, true);
        return findSpacesByOrganizationGuid(organizationGuid);
    }

    @Override
    public List<CloudSpace> getSpacesBoundToSecurityGroup(String securityGroupName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
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

    /**
     * Returns null if no further content is available. Two errors that will lead to a null value are 404 Bad Request errors, which are
     * handled in the implementation, meaning that no further log file contents are available, or ResourceAccessException, also handled in
     * the implementation, indicating a possible timeout in the server serving the content. Note that any other
     * {@link org.cloudfoundry.client.lib.CloudOperationException}s not related to the two errors mentioned above may still be thrown (e.g.
     * 500 level errors, Unauthorized or Forbidden exceptions, etc..)
     *
     * @return content if available, which may contain multiple lines, or null if no further content is available.
     */
    @Override
    public String getStagingLogs(StartingInfo info, int offset) {
        String stagingFile = info.getStagingFile();
        if (stagingFile != null) {
            CloudControllerRestClientHttpRequestFactory cfRequestFactory = null;
            try {
                Map<String, Object> logsRequest = new HashMap<>();
                logsRequest.put("offset", offset);

                cfRequestFactory = getRestTemplate().getRequestFactory() instanceof CloudControllerRestClientHttpRequestFactory
                    ? (CloudControllerRestClientHttpRequestFactory) getRestTemplate().getRequestFactory()
                    : null;
                if (cfRequestFactory != null) {
                    cfRequestFactory.increaseReadTimeoutForStreamedTailedLogs(5 * 60 * 1000);
                }
                return getRestTemplate().getForObject(stagingFile + "&tail&tail_offset={offset}", String.class, logsRequest);
            } catch (CloudOperationException e) {
                if (e.getStatusCode()
                    .equals(HttpStatus.NOT_FOUND)) {
                    // Content is no longer available
                    return null;
                } else {
                    throw e;
                }
            } catch (ResourceAccessException e) {
                // Likely read timeout, the directory server won't serve
                // the content again
                logger.debug("Caught exception while fetching staging logs. Aborting. Caught:" + e, e);
            } finally {
                if (cfRequestFactory != null) {
                    cfRequestFactory.increaseReadTimeoutForStreamedTailedLogs(-1);
                }
            }
        }
        return null;
    }

    @Override
    public List<CloudSecurityGroup> getStagingSecurityGroups() {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public OAuth2AccessToken login() {
        oAuthClient.init(credentials);
        return oAuthClient.getToken();
    }

    @Override
    public void logout() {
        oAuthClient.clear();
    }

    @Override
    public void openFile(String applicationName, int instanceIndex, String filePath, ClientHttpResponseCallback callback) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void register(String email, String password) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void registerRestLogListener(RestLogCallback callBack) {
        if (getRestTemplate() instanceof LoggingRestTemplate) {
            ((LoggingRestTemplate) getRestTemplate()).registerRestLogListener(callBack);
        }
    }

    @Override
    public void removeDomain(String domainName) {
        deleteDomain(domainName);
    }

    @Override
    public void rename(String applicationName, String newName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        v3Client.applicationsV2()
            .update(UpdateApplicationRequest.builder()
                .applicationId(applicationGuid.toString())
                .name(newName)
                .build())
            .block();
    }

    @Override
    public StartingInfo restartApplication(String applicationName) {
        stopApplication(applicationName);
        return startApplication(applicationName);
    }

    @Override
    public void setQuotaToOrganization(String organizationName, String quotaName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void setResponseErrorHandler(ResponseErrorHandler errorHandler) {
        restTemplate.setErrorHandler(errorHandler);
    }

    @Override
    public StartingInfo startApplication(String applicationName) {
        CloudApplication application = getApplication(applicationName);
        if (application.getState() == CloudApplication.State.STARTED) {
            return null;
        }
        UUID applicationGuid = application.getMetadata()
            .getGuid();
        v3Client.applicationsV2()
            .update(UpdateApplicationRequest.builder()
                .applicationId(applicationGuid.toString())
                .state(CloudApplication.State.STARTED.toString())
                .build())
            .block();
        return null;
    }

    @Override
    public void stopApplication(String applicationName) {
        CloudApplication application = getApplication(applicationName);
        if (application.getState() == CloudApplication.State.STOPPED) {
            return;
        }
        UUID applicationGuid = application.getMetadata()
            .getGuid();
        v3Client.applicationsV2()
            .update(UpdateApplicationRequest.builder()
                .applicationId(applicationGuid.toString())
                .state(CloudApplication.State.STOPPED.toString())
                .build())
            .block();
    }

    @Override
    public StreamingLogToken streamLogs(String applicationName, ApplicationLogListener listener) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void unRegisterRestLogListener(RestLogCallback callBack) {
        if (getRestTemplate() instanceof LoggingRestTemplate) {
            ((LoggingRestTemplate) getRestTemplate()).unRegisterRestLogListener(callBack);
        }
    }

    @Override
    public void unbindRunningSecurityGroup(String securityGroupName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void unbindSecurityGroup(String organizationName, String spaceName, String securityGroupName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void unbindService(String applicationName, String serviceName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UUID serviceGuid = getService(serviceName).getMetadata()
            .getGuid();
        doUnbindService(applicationGuid, serviceGuid);
    }

    @Override
    public void unbindStagingSecurityGroup(String securityGroupName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void unregister() {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void updateApplicationDiskQuota(String applicationName, int diskQuota) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        v3Client.applicationsV2()
            .update(UpdateApplicationRequest.builder()
                .applicationId(applicationGuid.toString())
                .diskQuota(diskQuota)
                .build())
            .block();
    }

    @Override
    public void updateApplicationEnv(String applicationName, Map<String, String> env) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        v3Client.applicationsV2()
            .update(UpdateApplicationRequest.builder()
                .applicationId(applicationGuid.toString())
                .environmentJsons(env)
                .build())
            .block();
    }

    @Override
    public void updateApplicationInstances(String applicationName, int instances) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        v3Client.applicationsV2()
            .update(UpdateApplicationRequest.builder()
                .applicationId(applicationGuid.toString())
                .instances(instances)
                .build())
            .block();
    }

    @Override
    public void updateApplicationMemory(String applicationName, int memory) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        v3Client.applicationsV2()
            .update(UpdateApplicationRequest.builder()
                .applicationId(applicationGuid.toString())
                .memory(memory)
                .build())
            .block();
    }

    @Override
    public List<String> updateApplicationServices(String applicationName,
        Map<String, Map<String, Object>> serviceNamesWithBindingParameters) {
        // No implementation here is needed because the logic is moved in ApplicationServicesUpdater in order to be used in other
        // implementations of the client. Currently, the ApplicationServicesUpdater is used only in CloudControllerClientImpl. Check
        // CloudControllerClientImpl.updateApplicationServices
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void updateApplicationStaging(String applicationName, Staging staging) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UpdateApplicationRequest.Builder requestBuilder = UpdateApplicationRequest.builder();
        requestBuilder.applicationId(applicationGuid.toString());
        if (staging != null) {
            requestBuilder.buildpack(staging.getBuildpackUrl())
                .command(staging.getCommand())
                .healthCheckHttpEndpoint(staging.getHealthCheckHttpEndpoint())
                .healthCheckTimeout(staging.getHealthCheckTimeout())
                .healthCheckType(staging.getHealthCheckType())
                .enableSsh(staging.isSshEnabled());
            String stackName = staging.getStack();
            if (stackName != null) {
                UUID stackGuid = getStack(stackName).getMetadata()
                    .getGuid();
                requestBuilder.stackId(stackGuid.toString());
            }
        }
        v3Client.applicationsV2()
            .update(requestBuilder.build())
            .block();
    }

    @Override
    public void updateApplicationUris(String applicationName, List<String> uris) {
        CloudApplication application = getApplication(applicationName);
        List<String> newUris = new ArrayList<>(uris);
        newUris.removeAll(application.getUris());
        List<String> removeUris = new ArrayList<>(application.getUris());
        removeUris.removeAll(uris);
        removeUris(removeUris, application.getMetadata()
            .getGuid());
        addUris(newUris, application.getMetadata()
            .getGuid());
    }

    @Override
    public void updatePassword(String newPassword) {
        updatePassword(credentials, newPassword);
    }

    @Override
    public void updatePassword(CloudCredentials currentCredentials, String newPassword) {
        oAuthClient.changePassword(currentCredentials.getPassword(), newPassword);
        CloudCredentials newCloudCredentials = new CloudCredentials(currentCredentials.getEmail(), newPassword);
        if (credentials.getProxyUser() != null) {
            credentials = newCloudCredentials.proxyForUser(credentials.getProxyUser());
        } else {
            credentials = newCloudCredentials;
        }
    }

    @Override
    public void updateQuota(CloudQuota quota, String name) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void updateSecurityGroup(CloudSecurityGroup securityGroup) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void updateSecurityGroup(String name, InputStream jsonRulesFile) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void updateServiceBroker(CloudServiceBroker serviceBroker) {
        Assert.notNull(serviceBroker, "Service broker must not be null.");

        CloudServiceBroker existingBroker = getServiceBroker(serviceBroker.getName());
        UUID brokerGuid = existingBroker.getMetadata()
            .getGuid();

        v3Client.serviceBrokers()
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
    public void uploadApplication(String applicationName, File file, UploadStatusCallback callback) throws IOException {
        UploadToken uploadToken = startUpload(applicationName, file);
        processAsyncUpload(uploadToken, callback);
    }

    @Override
    public void uploadApplication(String applicationName, InputStream inputStream, UploadStatusCallback callback) throws IOException {
        Assert.notNull(inputStream, "InputStream must not be null");

        File file = null;
        try {
            file = createTemporaryUploadFile(inputStream);
            uploadApplication(applicationName, file, callback);
        } finally {
            IOUtils.closeQuietly(inputStream);
            if (file != null) {
                file.delete();
            }
        }
    }

    @Override
    public UploadToken asyncUploadApplication(String applicationName, File file, UploadStatusCallback callback) throws IOException {
        UploadToken uploadToken = startUpload(applicationName, file);
        processAsyncUploadInBackground(uploadToken, callback);
        return uploadToken;
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
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put("packageGuid", packageGuid);
        return doGetResources("/v3/builds?package_guids={packageGuid}", urlVars, CloudBuild.class);
    }

    @Override
    public CloudBuild createBuild(UUID packageGuid) {
        return fetch(() -> createBuildResource(packageGuid), ImmutableRawCloudBuild::of);
    }

    @Override
    public void bindDropletToApp(UUID dropletGuid, UUID applicationGuid) {
        v3Client.applicationsV3()
            .setCurrentDroplet(SetApplicationCurrentDropletRequest.builder()
                .applicationId(applicationGuid.toString())
                .data(Relationship.builder()
                    .id(dropletGuid.toString())
                    .build())
                .build())
            .block();
    }

    private <R> List<R> doGetResources(String urlPath, Map<String, Object> urlVariables, Class<R> resourceClass) {
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVariables);
        List<R> resources = new ArrayList<>();
        for (Map<String, Object> resource : resourceList) {
            if (resource != null) {
                resources.add(resourceMapper.mapResource(resource, resourceClass));
            }
        }
        return resources;
    }

    private List<Map<String, Object>> getAllResources(String urlPath, Map<String, Object> urlVars) {
        Map<String, Object> responseMap = getResponseMap(urlPath, urlVars);
        List<Map<String, Object>> allResources = new ArrayList<>();
        List<Map<String, Object>> newResources = (List<Map<String, Object>>) responseMap.get("resources");
        if (newResources != null && !newResources.isEmpty()) {
            allResources.addAll(newResources);
        }
        addAllRemainingResources(responseMap, allResources);
        return allResources;
    }

    private Map<String, Object> getResponseMap(String urlPath, Map<String, Object> urlVars) {
        String response = getResponse(urlPath, urlVars);
        return JsonUtil.convertJsonToMap(response);
    }

    private String getResponse(String urlPath, Map<String, Object> urlVars) {
        return urlVars == null ? getRestTemplate().getForObject(getUrl(urlPath), String.class)
            : getRestTemplate().getForObject(getUrl(urlPath), String.class, urlVars);
    }

    private void addAllRemainingResources(Map<String, Object> responseMap, List<Map<String, Object>> allResources) {
        String nextUrl = getNextUrl(responseMap);

        while (nextUrl != null && !nextUrl.isEmpty()) {
            nextUrl = addPageOfResources(nextUrl, allResources);
        }
    }

    @SuppressWarnings("unchecked")
    private String addPageOfResources(String nextUrl, List<Map<String, Object>> allResources) {
        String response = getRestTemplate().getForObject(nextUrl, String.class);
        Map<String, Object> respMap = JsonUtil.convertJsonToMap(response);
        List<Map<String, Object>> newResources = (List<Map<String, Object>>) respMap.get("resources");
        if (newResources != null && !newResources.isEmpty()) {
            allResources.addAll(newResources);
        }
        return getNextUrl(respMap);
    }

    private String getNextUrl(Map<String, Object> responseMap) {
        String nextUrl = getNextUrlValue(responseMap);
        if (nextUrl != null && !nextUrl.isEmpty()) {
            return getUrl(nextUrl);
        }
        Map<String, Object> paginationMap = getPaginationMap(responseMap);
        if (paginationMap == null) {
            return null;
        }
        return getNextUrlValueV3(paginationMap);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPaginationMap(Map<String, Object> responseMap) {
        return (Map<String, Object>) responseMap.get("pagination");
    }

    private String getNextUrlValue(Map<String, Object> map) {
        return (String) map.get("next_url");
    }

    private String getNextUrlValueV3(Map<String, Object> map) {
        Map<String, Object> next = (Map<String, Object>) map.get("next");
        return next == null ? null : (String) next.get("href");
    }

    private CloudApplication findApplicationByName(String name, boolean required) {
        CloudApplication application = findApplicationByName(name);
        if (application == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application " + name + " not found.");
        }
        return application;
    }

    private CloudApplication findApplicationByName(String name) {
        return fetchWithAuxiliaryContent(() -> getApplicationResourceByName(name), this::zipWithAuxiliaryApplicationContent);
    }

    private CloudApplication findApplication(UUID guid) {
        return fetchWithAuxiliaryContent(() -> getApplicationResource(guid), this::zipWithAuxiliaryApplicationContent);
    }

    private Flux<? extends Resource<ApplicationEntity>> getApplicationResources() {
        IntFunction<ListSpaceApplicationsRequest> pageRequestSupplier = page -> ListSpaceApplicationsRequest.builder()
            .spaceId(getTargetSpaceGuid().toString())
            .page(page)
            .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.spaces()
            .listApplications(pageRequestSupplier.apply(page)));
    }

    private Mono<? extends Resource<ApplicationEntity>> getApplicationResource(UUID guid) {
        GetApplicationRequest request = GetApplicationRequest.builder()
            .applicationId(guid.toString())
            .build();
        return v3Client.applicationsV2()
            .get(request);
    }

    private Mono<? extends Resource<ApplicationEntity>> getApplicationResourceByName(String name) {
        IntFunction<ListApplicationsRequest> pageRequestSupplier = page -> ListApplicationsRequest.builder()
            .spaceId(getTargetSpaceGuid().toString())
            .name(name)
            .page(page)
            .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.applicationsV2()
            .list(pageRequestSupplier.apply(page)))
            .singleOrEmpty();
    }

    private Mono<Derivable<CloudApplication>> zipWithAuxiliaryApplicationContent(Resource<ApplicationEntity> applicationResource) {
        UUID applicationGuid = getGuid(applicationResource);
        return getApplicationSummary(applicationGuid).zipWhen(this::getApplicationStackResource)
            .map(tuple -> ImmutableRawCloudApplication.builder()
                .resource(applicationResource)
                .summary(tuple.getT1())
                .stack(ImmutableRawCloudStack.of(tuple.getT2()))
                .space(target)
                .build());
    }

    private Mono<SummaryApplicationResponse> getApplicationSummary(UUID guid) {
        SummaryApplicationRequest request = SummaryApplicationRequest.builder()
            .applicationId(guid.toString())
            .build();
        return v3Client.applicationsV2()
            .summary(request);
    }

    private Mono<? extends Resource<StackEntity>> getApplicationStackResource(SummaryApplicationResponse summary) {
        UUID stackGuid = UUID.fromString(summary.getStackId());
        return getStackResource(stackGuid);
    }

    private CloudServiceInstance findServiceInstanceByName(String name) {
        return fetchWithAuxiliaryContent(() -> getServiceInstanceResourceByName(name), this::zipWithAuxiliaryServiceInstanceContent);
    }

    private CloudService findServiceByName(String name) {
        return fetchWithAuxiliaryContent(() -> getServiceInstanceResourceByName(name), this::zipWithAuxiliaryServiceContent);
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

    private Flux<? extends Resource<UnionServiceInstanceEntity>> getServiceInstanceResources(
        IntFunction<ListSpaceServiceInstancesRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV2Resources(page -> v3Client.spaces()
            .listServiceInstances(pageRequestSupplier.apply(page)));
    }

    private Mono<Derivable<CloudServiceInstance>> zipWithAuxiliaryServiceInstanceContent(
        Resource<UnionServiceInstanceEntity> serviceInstanceResource) {
        Mono<List<CloudServiceBinding>> serviceBindings = getServiceInstanceBindingsFlux(getGuid(serviceInstanceResource)).collectList();
        Mono<Derivable<CloudService>> service = zipWithAuxiliaryServiceContent(serviceInstanceResource);
        return Mono.zip(service, serviceBindings)
            .map(tuple -> ImmutableRawCloudServiceInstance.builder()
                .resource(serviceInstanceResource)
                .serviceBindings(tuple.getT2())
                .service(tuple.getT1())
                .build());
    }

    private Flux<CloudServiceBinding> getServiceInstanceBindingsFlux(UUID serviceInstanceGuid) {
        return fetchFluxWithAuxiliaryContent(() -> getServiceBindingResourcesByServiceInstanceGuid(serviceInstanceGuid),
            this::zipWithAuxiliaryServiceBindingContent);
    }

    private Mono<Derivable<CloudService>> zipWithAuxiliaryServiceContent(Resource<UnionServiceInstanceEntity> serviceInstanceResource) {
        UnionServiceInstanceEntity serviceInstance = serviceInstanceResource.getEntity();
        if (isUserProvided(serviceInstance)) {
            return Mono.just(ImmutableRawCloudService.of(serviceInstanceResource));
        }
        UUID serviceGuid = UUID.fromString(serviceInstance.getServiceId());
        UUID servicePlanGuid = UUID.fromString(serviceInstance.getServicePlanId());
        return Mono.zip(Mono.just(serviceInstanceResource), getServiceResource(serviceGuid), getServicePlanResource(servicePlanGuid))
            .map(tuple -> ImmutableRawCloudService.builder()
                .resource(tuple.getT1())
                .serviceResource(tuple.getT2())
                .servicePlanResource(tuple.getT3())
                .build());
    }

    private boolean isUserProvided(UnionServiceInstanceEntity serviceInstance) {
        return USER_PROVIDED_SERVICE_INSTANCE_TYPE.equals(serviceInstance.getType());
    }

    private List<UUID> getServiceBindingGuids(CloudService service) {
        Flux<? extends Resource<ServiceBindingEntity>> bindings = getServiceBindingResources(service);
        return getGuids(bindings);
    }

    private List<UUID> getServiceBindingGuids(UUID applicationGuid) {
        Flux<? extends Resource<ServiceBindingEntity>> bindings = getServiceBindingResourcesByApplicationGuid(applicationGuid);
        return getGuids(bindings);
    }

    private Flux<? extends Resource<ServiceBindingEntity>> getServiceBindingResources(CloudService service) {
        UUID serviceGuid = getGuid(service);
        if (service.isUserProvided()) {
            return getUserProvidedServiceBindingResourcesByServiceInstanceGuid(serviceGuid);
        }
        return getServiceBindingResourcesByServiceInstanceGuid(serviceGuid);
    }

    private Flux<? extends Resource<ServiceBindingEntity>> getUserProvidedServiceBindingResourcesByServiceInstanceGuid(
        UUID serviceInstanceGuid) {
        IntFunction<ListUserProvidedServiceInstanceServiceBindingsRequest> pageRequestSupplier = page -> ListUserProvidedServiceInstanceServiceBindingsRequest
            .builder()
            .userProvidedServiceInstanceId(serviceInstanceGuid.toString())
            .page(page)
            .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.userProvidedServiceInstances()
            .listServiceBindings(pageRequestSupplier.apply(page)));
    }

    private Flux<? extends Resource<ServiceBindingEntity>> getServiceBindingResourcesByServiceInstanceGuid(UUID serviceInstanceGuid) {
        IntFunction<ListServiceBindingsRequest> pageRequestSupplier = page -> ListServiceBindingsRequest.builder()
            .serviceInstanceId(serviceInstanceGuid.toString())
            .page(page)
            .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.serviceBindingsV2()
            .list(pageRequestSupplier.apply(page)));
    }

    private Mono<? extends Resource<ServiceBindingEntity>> getServiceBindingResourceByApplicationGuidAndServiceInstanceGuid(
        UUID applicationGuid, UUID serviceInstanceGuid) {
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

    private Flux<? extends Resource<ServiceBindingEntity>> getApplicationServiceBindingResources(
        IntFunction<ListApplicationServiceBindingsRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV2Resources(page -> v3Client.applicationsV2()
            .listServiceBindings(pageRequestSupplier.apply(page)));
    }

    private Mono<Derivable<CloudServiceBinding>> zipWithAuxiliaryServiceBindingContent(Resource<ServiceBindingEntity> resource) {
        Map<String, Object> parameters = fetchParametersQuietly(resource.getEntity()
            .getServiceBindingParametersUrl());
        return Mono.just(ImmutableRawCloudServiceBinding.builder()
            .resource(resource)
            .parameters(parameters)
            .build());
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
        v3Client.servicePlans()
            .update(request)
            .block();
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

    private Mono<? extends Task> getTaskResource(UUID guid) {
        GetTaskRequest request = GetTaskRequest.builder()
            .taskId(guid.toString())
            .build();
        return v3Client.tasks()
            .get(request);
    }

    private Flux<? extends Task> getTaskResourcesByApplicationGuid(UUID applicationGuid) {
        IntFunction<ListTasksRequest> pageRequestSupplier = page -> ListTasksRequest.builder()
            .applicationId(applicationGuid.toString())
            .page(page)
            .build();
        return PaginationUtils.requestClientV3Resources(page -> v3Client.tasks()
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
        return v3Client.tasks()
            .create(request);
    }

    private Mono<? extends Task> cancelTaskResource(UUID taskGuid) {
        CancelTaskRequest request = CancelTaskRequest.builder()
            .taskId(taskGuid.toString())
            .build();
        return v3Client.tasks()
            .cancel(request);
    }

    private File createTemporaryUploadFile(InputStream inputStream) throws IOException {
        File file = File.createTempFile("cfjava", null);
        FileOutputStream outputStream = new FileOutputStream(file);
        FileCopyUtils.copy(inputStream, outputStream);
        outputStream.close();
        return file;
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

    private Mono<? extends Build> createBuildResource(UUID packageGuid) {
        CreateBuildRequest request = CreateBuildRequest.builder()
            .getPackage(buildRelationship(packageGuid))
            .build();
        return v3Client.builds()
            .create(request);
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

    protected void extractUriInfo(Map<String, UUID> existingDomains, String uri, Map<String, String> uriInfo) {
        URI newUri = URI.create(uri);
        String host = newUri.getScheme() != null ? newUri.getHost() : newUri.getPath();
        String[] hostAndDomain = host.split(DEFAULT_HOST_DOMAIN_SEPARATOR, 2);
        if (hostAndDomain.length != 2) {
            throw new IllegalArgumentException("Invalid URI " + uri + " -- host or domain is not specified");
        }
        String hostName = hostAndDomain[0];
        int indexOfPathSeparator = hostAndDomain[1].indexOf(DEFAULT_PATH_SEPARATOR);
        String domain = hostAndDomain[1];
        String path = "";
        if (indexOfPathSeparator > 0) {
            domain = hostAndDomain[1].substring(0, indexOfPathSeparator);
            path = hostAndDomain[1].substring(indexOfPathSeparator);
        }

        extractDomainInfo(existingDomains, uriInfo, domain, hostName, path);

        if (uriInfo.get("domainName") == null) {
            domain = host.split(DEFAULT_PATH_SEPARATOR)[0];
            extractDomainInfo(existingDomains, uriInfo, domain, "", path);
        }
        if (uriInfo.get("domainName") == null) {
            throw new IllegalArgumentException("Domain not found for URI " + uri);
        }
    }

    private void extractDomainInfo(Map<String, UUID> existingDomains, Map<String, String> uriInfo, String domain, String hostName, String path) {
        for (String existingDomain : existingDomains.keySet()) {
            if (domain.equals(existingDomain)) {
                uriInfo.put("domainName", existingDomain);
                uriInfo.put("host", hostName);
                uriInfo.put("path", path);
            }
        }
    }

    protected String getUrl(String path) {
        return controllerUrl + (path.startsWith(DEFAULT_PATH_SEPARATOR) ? path : DEFAULT_PATH_SEPARATOR + path);
    }

    private void addStagingToRequest(Staging staging, Map<String, Object> appRequest) {
        if (staging.getBuildpackUrl() != null) {
            appRequest.put("buildpack", staging.getBuildpackUrl());
        }
        if (staging.getCommand() != null) {
            appRequest.put("command", staging.getCommand());
        }
        if (staging.getStack() != null) {
            appRequest.put("stack_guid", getStack(staging.getStack()).getMetadata()
                .getGuid());
        }
        if (staging.getHealthCheckTimeout() != null) {
            appRequest.put("health_check_timeout", staging.getHealthCheckTimeout());
        }
        if (staging.getHealthCheckType() != null) {
            appRequest.put("health_check_type", staging.getHealthCheckType());
        }
        if (staging.getHealthCheckHttpEndpoint() != null) {
            appRequest.put("health_check_http_endpoint", staging.getHealthCheckHttpEndpoint());
        }
        if (staging.isSshEnabled() != null) {
            appRequest.put("enable_ssh", staging.isSshEnabled());
        }
        if (staging.getDockerInfo() != null) {
            appRequest.put("docker_image", staging.getDockerInfo()
                .getImage());
            if (staging.getDockerInfo()
                .getCredentials() != null) {
                appRequest.put("docker_credentials", staging.getDockerInfo()
                    .getCredentials());
            }
        }
    }

    private void addUris(List<String> uris, UUID applicationGuid) {
        Map<String, UUID> domains = getDomainGuids();
        for (String uri : uris) {
            Map<String, String> uriInfo = new HashMap<>(2);
            extractUriInfo(domains, uri, uriInfo);
            UUID domainGuid = domains.get(uriInfo.get("domainName"));
            String host = uriInfo.get("host");
            String path = uriInfo.get("path");
            bindRoute(domainGuid, host, path, applicationGuid);
        }
    }

    private void assertSpaceProvided(String operation) {
        Assert.notNull(target, "Unable to " + operation + " without specifying organization and space to use.");
    }

    private void bindRoute(UUID domainGuid, String host, String path, UUID applicationGuid) {
        UUID routeGuid = getOrAddRoute(domainGuid, host, path);
        v3Client.applicationsV2()
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
        CreateRouteResponse response = v3Client.routes()
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
        v3Client.domains()
            .create(CreateDomainRequest.builder()
                .wildcard(true)
                .owningOrganizationId(getTargetOrganizationGuid().toString())
                .name(name)
                .build())
            .block();
    }

    private void doDeleteDomain(UUID guid) {
        v3Client.privateDomains()
            .delete(DeletePrivateDomainRequest.builder()
                .privateDomainId(guid.toString())
                .build())
            .block();
    }

    private void doDeleteRoute(UUID guid) {
        v3Client.routes()
            .delete(DeleteRouteRequest.builder()
                .routeId(guid.toString())
                .build())
            .block();
    }

    private void doDeleteService(CloudService service) {
        List<UUID> serviceBindingGuids = getServiceBindingGuids(service);
        for (UUID serviceBindingGuid : serviceBindingGuids) {
            doUnbindService(serviceBindingGuid);
        }
        UUID serviceGuid = service.getMetadata()
            .getGuid();
        v3Client.serviceInstances()
            .delete(DeleteServiceInstanceRequest.builder()
                .acceptsIncomplete(true)
                .serviceInstanceId(serviceGuid.toString())
                .build())
            .block();
    }

    private void doUnbindService(UUID applicationGuid, UUID serviceGuid) {
        UUID serviceBindingGuid = getServiceBindingGuid(applicationGuid, serviceGuid);
        doUnbindService(serviceBindingGuid);
    }

    private void doUnbindService(UUID serviceBindingGuid) {
        v3Client.serviceBindingsV2()
            .delete(DeleteServiceBindingRequest.builder()
                .serviceBindingId(serviceBindingGuid.toString())
                .build())
            .block();
    }

    private void doDeleteServiceKey(UUID guid) {
        v3Client.serviceKeys()
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
        return fetchList(() -> getPrivateDomainResourcesByOrganizationGuid(organizationGuid), ImmutableRawCloudPrivateDomain::of);
    }

    private Mono<? extends Resource<DomainEntity>> getDomainResourceByName(String name) {
        IntFunction<ListDomainsRequest> pageRequestSupplier = page -> ListDomainsRequest.builder()
            .name(name)
            .page(page)
            .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.domains()
            .list(pageRequestSupplier.apply(page)))
            .singleOrEmpty();
    }

    private Flux<? extends Resource<SharedDomainEntity>> getSharedDomainResources() {
        IntFunction<ListSharedDomainsRequest> pageRequestSupplier = page -> ListSharedDomainsRequest.builder()
            .page(page)
            .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.sharedDomains()
            .list(pageRequestSupplier.apply(page)));
    }

    private Flux<? extends Resource<PrivateDomainEntity>> getPrivateDomainResources() {
        IntFunction<ListPrivateDomainsRequest> pageRequestSupplier = page -> ListPrivateDomainsRequest.builder()
            .page(page)
            .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.privateDomains()
            .list(pageRequestSupplier.apply(page)));
    }

    private Flux<? extends Resource<PrivateDomainEntity>> getPrivateDomainResourcesByOrganizationGuid(UUID organizationGuid) {
        IntFunction<ListOrganizationPrivateDomainsRequest> pageRequestSupplier = page -> ListOrganizationPrivateDomainsRequest.builder()
            .organizationId(organizationGuid.toString())
            .page(page)
            .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.organizations()
            .listPrivateDomains(pageRequestSupplier.apply(page)));
    }

    private List<CloudSpace> findSpacesByOrganizationGuid(UUID organizationGuid) {
        return fetchListWithAuxiliaryContent(() -> getSpaceResourcesByOrganizationGuid(organizationGuid),
            this::zipWithAuxiliarySpaceContent);
    }

    private CloudSpace findSpaceByOrganizationGuidAndName(UUID organizationGuid, String spaceName, boolean required) {
        CloudSpace space = findSpaceByOrganizationGuidAndName(organizationGuid, spaceName);
        if (space == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found",
                "Space " + spaceName + " not found in organization with GUID " + organizationGuid + ".");
        }
        return space;
    }

    private CloudSpace findSpaceByOrganizationGuidAndName(UUID organizationGuid, String spaceName) {
        return fetchWithAuxiliaryContent(() -> getSpaceResourceByOrganizationGuidAndName(organizationGuid, spaceName),
            this::zipWithAuxiliarySpaceContent);
    }

    private Flux<? extends Resource<SpaceEntity>> getSpaceResources() {
        IntFunction<ListSpacesRequest> pageRequestSupplier = page -> ListSpacesRequest.builder()
            .page(page)
            .build();
        return getSpaceResources(pageRequestSupplier);
    }

    private Mono<? extends Resource<SpaceEntity>> getSpaceResource(UUID guid) {
        GetSpaceRequest request = GetSpaceRequest.builder()
            .spaceId(guid.toString())
            .build();
        return v3Client.spaces()
            .get(request);
    }

    private Flux<? extends Resource<SpaceEntity>> getSpaceResourcesByOrganizationGuid(UUID organizationGuid) {
        IntFunction<ListSpacesRequest> pageRequestSupplier = page -> ListSpacesRequest.builder()
            .organizationId(organizationGuid.toString())
            .page(page)
            .build();
        return getSpaceResources(pageRequestSupplier);
    }

    private Mono<? extends Resource<SpaceEntity>> getSpaceResourceByOrganizationGuidAndName(UUID organizationGuid, String name) {
        IntFunction<ListOrganizationSpacesRequest> pageRequestSupplier = page -> ListOrganizationSpacesRequest.builder()
            .organizationId(organizationGuid.toString())
            .name(name)
            .page(page)
            .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.organizations()
            .listSpaces(pageRequestSupplier.apply(page)))
            .singleOrEmpty();
    }

    private Flux<? extends Resource<SpaceEntity>> getSpaceResources(IntFunction<ListSpacesRequest> requestForPage) {
        return PaginationUtils.requestClientV2Resources(page -> v3Client.spaces()
            .list(requestForPage.apply(page)));
    }

    private Mono<Derivable<CloudSpace>> zipWithAuxiliarySpaceContent(Resource<SpaceEntity> resource) {
        UUID organizationGuid = UUID.fromString(resource.getEntity()
            .getOrganizationId());
        return getOrganizationMono(organizationGuid).map(organization -> ImmutableRawCloudSpace.builder()
            .resource(resource)
            .organization(organization)
            .build());
    }

    private Mono<? extends Derivable<CloudOrganization>> getOrganizationMono(UUID guid) {
        return fetchMono(() -> getOrganizationResource(guid), ImmutableRawCloudOrganization::of);
    }

    private CloudOrganization findOrganization(String name) {
        return fetch(() -> getOrganizationResourceByName(name), ImmutableRawCloudOrganization::of);
    }

    private Flux<? extends Resource<OrganizationEntity>> getOrganizationResources() {
        IntFunction<ListOrganizationsRequest> pageRequestSupplier = page -> ListOrganizationsRequest.builder()
            .page(page)
            .build();
        return getOrganizationResources(pageRequestSupplier);
    }

    private Mono<? extends Resource<OrganizationEntity>> getOrganizationResource(UUID guid) {
        GetOrganizationRequest request = GetOrganizationRequest.builder()
            .organizationId(guid.toString())
            .build();
        return v3Client.organizations()
            .get(request);
    }

    private Mono<? extends Resource<OrganizationEntity>> getOrganizationResourceByName(String name) {
        IntFunction<ListOrganizationsRequest> pageRequestSupplier = page -> ListOrganizationsRequest.builder()
            .name(name)
            .page(page)
            .build();
        return getOrganizationResources(pageRequestSupplier).singleOrEmpty();
    }

    private Flux<? extends Resource<OrganizationEntity>> getOrganizationResources(
        IntFunction<ListOrganizationsRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV2Resources(page -> v3Client.organizations()
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
        return PaginationUtils.requestClientV2Resources(page -> v3Client.spaces()
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

        return PaginationUtils.requestClientV2Resources(page -> v3Client.spaces()
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
        return PaginationUtils.requestClientV2Resources(page -> v3Client.routeMappings()
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
        IntFunction<ListServicesRequest> pageRequestSupplier = page -> ListServicesRequest.builder()
            .page(page)
            .build();
        return getServiceResources(pageRequestSupplier);
    }

    private Mono<? extends Resource<ServiceEntity>> getServiceResource(UUID serviceGuid) {
        GetServiceRequest request = GetServiceRequest.builder()
            .serviceId(serviceGuid.toString())
            .build();
        return v3Client.services()
            .get(request);
    }

    private Flux<? extends Resource<ServiceEntity>> getServiceResourcesByBrokerGuid(UUID brokerGuid) {
        IntFunction<ListServicesRequest> pageRequestSupplier = page -> ListServicesRequest.builder()
            .serviceBrokerId(brokerGuid.toString())
            .page(page)
            .build();
        return getServiceResources(pageRequestSupplier);
    }

    private Flux<? extends Resource<ServiceEntity>> getServiceResourcesByLabel(String label) {
        IntFunction<ListServicesRequest> pageRequestSupplier = page -> ListServicesRequest.builder()
            .label(label)
            .page(page)
            .build();
        return getServiceResources(pageRequestSupplier);
    }

    private Flux<? extends Resource<ServiceEntity>> getServiceResources(IntFunction<ListServicesRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV2Resources(page -> v3Client.services()
            .list(pageRequestSupplier.apply(page)));
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

    private Mono<? extends Resource<ServicePlanEntity>> getServicePlanResource(UUID servicePlanGuid) {
        GetServicePlanRequest request = GetServicePlanRequest.builder()
            .servicePlanId(servicePlanGuid.toString())
            .build();
        return v3Client.servicePlans()
            .get(request);
    }

    private Flux<? extends Resource<ServicePlanEntity>> getServicePlanResourcesByServiceGuid(UUID serviceGuid) {
        IntFunction<ListServicePlansRequest> pageRequestSupplier = page -> ListServicePlansRequest.builder()
            .serviceId(serviceGuid.toString())
            .page(page)
            .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.servicePlans()
            .list(pageRequestSupplier.apply(page)));
    }

    private Map<String, Object> fetchParametersQuietly(String url) {
        try {
            return fetchParameters(url);
        } catch (CloudOperationException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchParameters(String url) {
        return getRestTemplate().getForObject(getUrl(url), Map.class);
    }

    private List<CloudServiceKey> getServiceKeys(CloudService service) {
        return fetchList(() -> getServiceKeyTuple(service), tuple -> ImmutableRawCloudServiceKey.builder()
            .resource(tuple.getT1())
            .service(tuple.getT2())
            .build());
    }

    private Flux<Tuple2<? extends Resource<ServiceKeyEntity>, CloudService>> getServiceKeyTuple(CloudService service) {
        UUID serviceInstanceGuid = getGuid(service);
        return getServiceKeyResourcesByServiceInstanceGuid(serviceInstanceGuid)
            .map(serviceKeyResource -> Tuples.of(serviceKeyResource, service));
    }

    private Flux<? extends Resource<ServiceKeyEntity>> getServiceKeyResourcesByServiceInstanceGuid(UUID serviceInstanceGuid) {
        IntFunction<ListServiceKeysRequest> pageRequestSupplier = page -> ListServiceKeysRequest.builder()
            .serviceInstanceId(serviceInstanceGuid.toString())
            .page(page)
            .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.serviceKeys()
            .list(pageRequestSupplier.apply(page)));
    }

    private List<CloudRoute> fetchOrphanRoutes(String domainName) {
        List<CloudRoute> orphanRoutes = new ArrayList<>();
        for (CloudRoute route : getRoutes(domainName)) {
            if (!route.isUsed()) {
                orphanRoutes.add(route);
            }
        }
        return orphanRoutes;
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
        return v3Client.stacks()
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
        return PaginationUtils.requestClientV2Resources(page -> v3Client.stacks()
            .list(requestForPage.apply(page)));
    }

    private List<CloudEvent> findEventsByActee(String actee) {
        return fetchList(() -> getEventResourcesByActee(actee), ImmutableRawCloudEvent::of);
    }

    private Flux<? extends Resource<EventEntity>> getEventResources() {
        IntFunction<ListEventsRequest> pageRequestSupplier = page -> ListEventsRequest.builder()
            .page(page)
            .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.events()
            .list(pageRequestSupplier.apply(page)));
    }

    private Flux<? extends Resource<EventEntity>> getEventResourcesByActee(String actee) {
        IntFunction<ListEventsRequest> pageRequestSupplier = page -> ListEventsRequest.builder()
            .actee(actee)
            .page(page)
            .build();
        return PaginationUtils.requestClientV2Resources(page -> v3Client.events()
            .list(pageRequestSupplier.apply(page)));
    }

    private InstancesInfo findApplicationInstances(UUID applicationGuid) {
        return fetch(() -> getApplicationInstances(applicationGuid), ImmutableRawInstancesInfo::of);
    }

    private Mono<ApplicationInstancesResponse> getApplicationInstances(UUID applicationGuid) {
        ApplicationInstancesRequest request = ApplicationInstancesRequest.builder()
            .applicationId(applicationGuid.toString())
            .build();
        return v3Client.applicationsV2()
            .instances(request);
    }

    private Mono<GetInfoResponse> getInfoResource() {
        return v3Client.info()
            .get(GetInfoRequest.builder()
                .build());
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

    private Flux<? extends Resource<ServiceBrokerEntity>> getServiceBrokerResources(
        IntFunction<ListServiceBrokersRequest> pageRequestSupplier) {
        return PaginationUtils.requestClientV2Resources(page -> v3Client.serviceBrokers()
            .list(pageRequestSupplier.apply(page)));
    }

    private List<UUID> findSpaceUsers(String organizationName, String spaceName, Function<UUID, List<UUID>> usersRetriever) {
        CloudSpace space = getSpace(organizationName, spaceName);
        return usersRetriever.apply(getGuid(space));
    }

    private CloudServicePlan findPlanForService(CloudService service) {
        List<CloudServiceOffering> offerings = findServiceOfferingsByLabel(service.getLabel());
        for (CloudServiceOffering offering : offerings) {
            if (service.getVersion() == null || service.getVersion()
                .equals(offering.getVersion())) {
                for (CloudServicePlan plan : offering.getServicePlans()) {
                    if (service.getPlan() != null && service.getPlan()
                        .equals(plan.getName())) {
                        return plan;
                    }
                }
            }
        }
        throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service plan " + service.getPlan() + " not found.");
    }

    private UUID getRequiredApplicationGuid(String name) {
        return getGuid(findApplicationByName(name, true));
    }

    private UUID getRequiredDomainGuid(String name) {
        return getGuid(findDomainByName(name, true));
    }

    private UUID getOrganizationGuid(String organizationName, boolean required) {
        CloudOrganization organization = getOrganization(organizationName, required);
        return organization != null ? organization.getMetadata()
            .getGuid() : null;
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
        List<UUID> routeGuids = getRouteResourcesByDomainGuidHostAndPath(domainGuid, host, path).map(this::getGuid)
            .collectList()
            .block();
        return routeGuids.isEmpty() ? null : routeGuids.get(0);
    }

    private UUID getServiceBindingGuid(UUID applicationGuid, UUID serviceGuid) {
        return getServiceBindingResourceByApplicationGuidAndServiceInstanceGuid(applicationGuid, serviceGuid).map(this::getGuid)
            .block();
    }

    private List<UUID> getGuids(Flux<? extends Resource<?>> resources) {
        return resources.map(this::getGuid)
            .collectList()
            .block();
    }

    private void processAsyncUploadInBackground(UploadToken uploadToken, UploadStatusCallback callback) {
        String threadName = String.format("App upload monitor: %s", uploadToken.getPackageGuid());
        new Thread(() -> processAsyncUpload(uploadToken, callback), threadName).start();
    }

    private void processAsyncUpload(UploadToken uploadToken, UploadStatusCallback callback) {
        if (callback == null) {
            callback = UploadStatusCallback.NONE;
        }
        while (true) {
            Upload upload = getUploadStatus(uploadToken.getPackageGuid());
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

    private void removeUris(List<String> uris, UUID applicationGuid) {
        Map<String, UUID> domains = getDomainGuids();
        for (String uri : uris) {
            Map<String, String> uriInfo = new HashMap<>(2);
            extractUriInfo(domains, uri, uriInfo);
            UUID domainGuid = domains.get(uriInfo.get("domainName"));
            String host = uriInfo.get("host");
            String path = uriInfo.get("path");
            unbindRoute(domainGuid, host, path, applicationGuid);
        }
    }

    private void unbindRoute(UUID domainGuid, String host, String path, UUID applicationGuid) {
        UUID routeGuid = getRouteGuid(domainGuid, host, path);
        if (routeGuid == null) {
            return;
        }
        v3Client.applicationsV2()
            .removeRoute(RemoveApplicationRouteRequest.builder()
                .applicationId(applicationGuid.toString())
                .routeId(routeGuid.toString())
                .build())
            .block();
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
