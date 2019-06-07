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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.AbstractCloudFoundryException;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.ApplicationServicesUpdateCallback;
import org.cloudfoundry.client.lib.ClientHttpResponseCallback;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudException;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.RestLogCallback;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudPackage;
import org.cloudfoundry.client.lib.domain.CloudQuota;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudSecurityGroup;
import org.cloudfoundry.client.lib.domain.CloudService;
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
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.ErrorDetails;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudInfo;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.client.lib.domain.ImmutableErrorDetails;
import org.cloudfoundry.client.lib.domain.ImmutableUpload;
import org.cloudfoundry.client.lib.domain.ImmutableUploadToken;
import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.client.lib.domain.InstanceState;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Status;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.cloudfoundry.client.lib.util.CloudUtil;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.cloudfoundry.client.v2.Metadata;
import org.cloudfoundry.client.v2.Resource;
import org.cloudfoundry.client.v2.applications.AssociateApplicationRouteRequest;
import org.cloudfoundry.client.v2.applications.DeleteApplicationRequest;
import org.cloudfoundry.client.v2.applications.ListApplicationServiceBindingsRequest;
import org.cloudfoundry.client.v2.applications.RemoveApplicationRouteRequest;
import org.cloudfoundry.client.v2.applications.UpdateApplicationRequest;
import org.cloudfoundry.client.v2.domains.CreateDomainRequest;
import org.cloudfoundry.client.v2.domains.DomainResource;
import org.cloudfoundry.client.v2.domains.ListDomainsRequest;
import org.cloudfoundry.client.v2.privatedomains.DeletePrivateDomainRequest;
import org.cloudfoundry.client.v2.routes.CreateRouteRequest;
import org.cloudfoundry.client.v2.routes.CreateRouteResponse;
import org.cloudfoundry.client.v2.routes.DeleteRouteRequest;
import org.cloudfoundry.client.v2.routes.RouteResource;
import org.cloudfoundry.client.v2.servicebindings.CreateServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.DeleteServiceBindingRequest;
import org.cloudfoundry.client.v2.servicebindings.ServiceBindingResource;
import org.cloudfoundry.client.v2.servicebrokers.CreateServiceBrokerRequest;
import org.cloudfoundry.client.v2.servicebrokers.DeleteServiceBrokerRequest;
import org.cloudfoundry.client.v2.servicebrokers.UpdateServiceBrokerRequest;
import org.cloudfoundry.client.v2.serviceinstances.CreateServiceInstanceRequest;
import org.cloudfoundry.client.v2.serviceinstances.DeleteServiceInstanceRequest;
import org.cloudfoundry.client.v2.serviceinstances.ListServiceInstanceServiceBindingsRequest;
import org.cloudfoundry.client.v2.servicekeys.CreateServiceKeyRequest;
import org.cloudfoundry.client.v2.servicekeys.DeleteServiceKeyRequest;
import org.cloudfoundry.client.v2.spaces.ListSpaceRoutesRequest;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.CreateUserProvidedServiceInstanceRequest;
import org.cloudfoundry.client.v2.userprovidedserviceinstances.ListUserProvidedServiceInstanceServiceBindingsRequest;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.applications.SetApplicationCurrentDropletRequest;
import org.cloudfoundry.client.v3.packages.UploadPackageRequest;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.util.PaginationUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import reactor.core.publisher.Flux;

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
    private static final long JOB_POLLING_PERIOD = TimeUnit.SECONDS.toMillis(5);

    private final Log logger = LogFactory.getLog(getClass().getName());
    private CloudCredentials credentials;
    private URL controllerUrl;
    private OAuthClient oAuthClient;
    private CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();
    private RestTemplate restTemplate;
    private CloudSpace target;

    @SuppressWarnings("unused")
    private CloudFoundryOperations v3OperationsClient;
    private CloudFoundryClient v3Client;

    /**
     * Only for unit tests. This works around the fact that the initialize method is called within the constructor and hence can not be
     * overloaded, making it impossible to write unit tests that don't trigger network calls.
     */
    protected CloudControllerRestClientImpl() {
    }

    public CloudControllerRestClientImpl(URL controllerUrl, CloudCredentials credentials, RestTemplate restTemplate,
        OAuthClient oAuthClient, CloudFoundryOperations v3OperationsClient, CloudFoundryClient v3Client) {
        this(controllerUrl, credentials, restTemplate, oAuthClient, v3OperationsClient, v3Client, null);
    }

    public CloudControllerRestClientImpl(URL controllerUrl, CloudCredentials credentials, RestTemplate restTemplate,
        OAuthClient oAuthClient, CloudFoundryOperations v3OperationsClient, CloudFoundryClient v3Client, CloudSpace target) {
        Assert.notNull(controllerUrl, "CloudControllerUrl cannot be null");
        Assert.notNull(restTemplate, "RestTemplate cannot be null");
        Assert.notNull(oAuthClient, "OAuthClient cannot be null");

        this.controllerUrl = controllerUrl;
        this.credentials = credentials;
        this.restTemplate = restTemplate;
        this.oAuthClient = oAuthClient;
        this.target = target;

        this.v3OperationsClient = v3OperationsClient;
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
        UUID domainGuid = getDomainGuid(domainName, false);
        if (domainGuid == null) {
            doCreateDomain(domainName);
        }
    }

    @Override
    public void addRoute(String host, String domainName) {
        assertSpaceProvided("add route for domain");
        UUID domainGuid = getDomainGuid(domainName, true);
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
        bindService(applicationName, serviceName, null, ApplicationServicesUpdateCallback.DEFAULT_APPLICATION_SERVICES_UPDATE_CALLBACK);
    }

    @Override
    public void bindService(String applicationName, String serviceName, Map<String, Object> parameters,
        ApplicationServicesUpdateCallback updateServicesCallback) {
        try {
            UUID applicationGuid = getRequiredApplicationGuid(applicationName);
            UUID serviceGuid = getService(serviceName).getMetadata()
                .getGuid();
            convertV3ClientExceptions(() -> v3Client.serviceBindingsV2()
                .create(CreateServiceBindingRequest.builder()
                    .applicationId(applicationGuid.toString())
                    .serviceInstanceId(serviceGuid.toString())
                    .parameters(parameters)
                    .build())
                .block());
        } catch (CloudOperationException e) {
            updateServicesCallback.onError(e, applicationName, serviceName);
        }
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
            updateApplicationServices(name, Collections.emptyMap(),
                ApplicationServicesUpdateCallback.DEFAULT_APPLICATION_SERVICES_UPDATE_CALLBACK);
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
        convertV3ClientExceptions(() -> v3Client.serviceInstances()
            .create(CreateServiceInstanceRequest.builder()
                .spaceId(getTargetSpaceId())
                .name(service.getName())
                .servicePlanId(servicePlanGuid.toString())
                .build())
            .block());
    }

    @Override
    public void createServiceBroker(CloudServiceBroker serviceBroker) {
        Assert.notNull(serviceBroker, "Service broker must not be null.");

        convertV3ClientExceptions(() -> v3Client.serviceBrokers()
            .create(CreateServiceBrokerRequest.builder()
                .name(serviceBroker.getName())
                .brokerUrl(serviceBroker.getUrl())
                .authenticationUsername(serviceBroker.getUsername())
                .authenticationPassword(serviceBroker.getPassword())
                .spaceId(serviceBroker.getSpaceGuid())
                .build())
            .block());
    }

    @Override
    public void createServiceKey(String serviceName, String name, Map<String, Object> parameters) {
        CloudService service = getService(serviceName);
        UUID serviceGuid = service.getMetadata()
            .getGuid();

        convertV3ClientExceptions(() -> v3Client.serviceKeys()
            .create(CreateServiceKeyRequest.builder()
                .serviceInstanceId(serviceGuid.toString())
                .name(name)
                .parameters(parameters)
                .build())
            .block());
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

        convertV3ClientExceptions(() -> v3Client.userProvidedServiceInstances()
            .create(CreateUserProvidedServiceInstanceRequest.builder()
                .spaceId(getTargetSpaceId())
                .name(service.getName())
                .credentials(credentials)
                .build())
            .block());
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
        convertV3ClientExceptions(() -> v3Client.applicationsV2()
            .delete(DeleteApplicationRequest.builder()
                .applicationId(applicationGuid.toString())
                .build())
            .block());
    }

    @Override
    public void deleteDomain(String domainName) {
        assertSpaceProvided("delete domain");
        UUID domainGuid = getDomainGuid(domainName, true);
        List<CloudRoute> routes = getRoutes(domainName);
        if (!routes.isEmpty()) {
            throw new IllegalStateException("Unable to remove domain that is in use --" + " it has " + routes.size() + " routes.");
        }
        doDeleteDomain(domainGuid);
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
        UUID routeGuid = getRouteGuid(getDomainGuid(domainName, true), host, null);
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
        convertV3ClientExceptions(() -> v3Client.serviceBrokers()
            .delete(DeleteServiceBrokerRequest.builder()
                .serviceBrokerId(guid.toString())
                .build())
            .block());
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

    private void doDeleteServiceKey(UUID guid) {
        convertV3ClientExceptions(() -> v3Client.serviceKeys()
            .delete(DeleteServiceKeyRequest.builder()
                .serviceKeyId(guid.toString())
                .build())
            .block());
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
        Map<String, Object> resource = findApplicationResource(applicationName, true);
        CloudApplication application = null;
        if (resource != null) {
            application = mapCloudApplication(resource);
        }
        if (application == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application " + applicationName + " not found.");
        }
        return application;
    }

    @Override
    public CloudApplication getApplication(UUID applicationGuid) {
        Map<String, Object> resource = findApplicationResource(applicationGuid);
        CloudApplication application = null;
        if (resource != null) {
            application = mapCloudApplication(resource);
        }
        if (application == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found",
                "Application with GUID " + applicationGuid + " not found.");
        }
        return application;
    }

    @Override
    public Map<String, Object> getApplicationEnvironment(UUID applicationGuid) {
        String url = getUrl("/v2/apps/{guid}/env");
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put("guid", applicationGuid);
        String response = restTemplate.getForObject(url, String.class, urlVars);
        return JsonUtil.convertJsonToMap(response);
    }

    @Override
    public Map<String, Object> getApplicationEnvironment(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        return getApplicationEnvironment(applicationGuid);
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String applicationName) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put("applicationGuid", applicationGuid);
        String urlPath = "/v2/events?q=actee:{applicationGuid}";
        return doGetEvents(urlPath, urlVars);
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
            return doGetApplicationInstances(application.getMetadata()
                .getGuid());
        }
        return null;
    }

    @Override
    public List<CloudApplication> getApplications() {
        return getApplications(true);
    }

    @Override
    public List<CloudApplication> getApplications(boolean fetchAdditionalInfo) {
        Map<String, Object> urlVars = new HashMap<>();
        String urlPath = "/v2";
        if (target != null) {
            urlVars.put("space", target.getMetadata()
                .getGuid());
            urlPath = urlPath + "/spaces/{space}";
        }
        urlPath = urlPath + "/apps";
        urlPath = fetchAdditionalInfo ? urlPath + "?inline-relations-depth=1" : urlPath;
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        List<CloudApplication> applications = new ArrayList<>();
        for (Map<String, Object> resource : resourceList) {
            resource = fetchAdditionalInfo ? processApplicationResource(resource) : resource;
            applications.add(mapCloudApplication(resource));
        }
        return applications;
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
    public List<CloudDomain> getDomains() {
        return doGetDomains((CloudOrganization) null);
    }

    @Override
    public List<CloudDomain> getDomainsForOrganization() {
        assertSpaceProvided("access organization domains");
        return doGetDomains(target.getOrganization());
    }

    @Override
    public List<CloudEvent> getEvents() {
        String urlPath = "/v2/events";
        return doGetEvents(urlPath, null);
    }

    @Override
    public String getFile(String applicationName, int instanceIndex, String filePath, int startPosition, int endPosition) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public CloudInfo getInfo() {
        String infoV2Json = getRestTemplate().getForObject(getUrl("/v2/info"), String.class);
        Map<String, Object> infoV2Map = JsonUtil.convertJsonToMap(infoV2Json);

        Map<String, Object> userMap = getUserInfo((String) infoV2Map.get("user"));

        String user = (String) userMap.get("user_name");
        String name = CloudUtil.parse(String.class, infoV2Map.get("name"));
        String support = CloudUtil.parse(String.class, infoV2Map.get("support"));
        String authorizationEndpoint = CloudUtil.parse(String.class, infoV2Map.get("authorization_endpoint"));
        String build = CloudUtil.parse(String.class, infoV2Map.get("build"));
        String version = "" + CloudUtil.parse(Number.class, infoV2Map.get("version"));
        String description = CloudUtil.parse(String.class, infoV2Map.get("description"));

        String loggingEndpoint = CloudUtil.parse(String.class, infoV2Map.get("doppler_logging_endpoint"));

        return ImmutableCloudInfo.builder()
            .name(name)
            .user(user)
            .support(support)
            .version(version)
            .authorizationEndpoint(authorizationEndpoint)
            .loggingEndpoint(loggingEndpoint)
            .build(build)
            .description(description)
            .build();
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
        Map<String, Object> urlVars = new HashMap<>();
        String urlPath = "/v2/organizations?inline-relations-depth=1&q=name:{name}";
        urlVars.put("name", organizationName);
        CloudOrganization organization = null;
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        if (!resourceList.isEmpty()) {
            Map<String, Object> resource = resourceList.get(0);
            organization = resourceMapper.mapResource(resource, CloudOrganization.class);
        }

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
        String urlPath = "/v2/organizations?inline-relations-depth=0";
        List<Map<String, Object>> resourceList = getAllResources(urlPath);
        List<CloudOrganization> organizations = new ArrayList<>();
        for (Map<String, Object> resource : resourceList) {
            organizations.add(resourceMapper.mapResource(resource, CloudOrganization.class));
        }
        return organizations;
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return doGetDomains("/v2/private_domains");
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
        UUID domainGuid = getDomainGuid(domainName, true);
        return doGetRoutes(domainGuid);
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
        Map<String, Object> resource = doGetServiceInstance(serviceName, 0);
        CloudService service = null;
        if (resource != null) {
            service = resourceMapper.mapResource(resource, CloudService.class);
        }
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
        Map<String, Object> resource = findServiceBrokerResource(name);
        CloudServiceBroker serviceBroker = null;
        if (resource != null) {
            serviceBroker = resourceMapper.mapResource(resource, CloudServiceBroker.class);
        }
        if (serviceBroker == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service broker " + name + " not found.");
        }
        return serviceBroker;
    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        String urlPath = "/v2/service_brokers?inline-relations-depth=1";
        List<Map<String, Object>> resourceList = getAllResources(urlPath);
        List<CloudServiceBroker> serviceBrokers = new ArrayList<>();
        for (Map<String, Object> resource : resourceList) {
            CloudServiceBroker broker = resourceMapper.mapResource(resource, CloudServiceBroker.class);
            serviceBrokers.add(broker);
        }
        return serviceBrokers;
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceName) {
        return getServiceInstance(serviceName, true);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceName, boolean required) {
        Map<String, Object> resource = doGetServiceInstance(serviceName, 1);
        CloudServiceInstance serviceInstance = null;
        if (resource != null) {
            serviceInstance = resourceMapper.mapResource(resource, CloudServiceInstance.class);
        }
        if (serviceInstance == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service instance " + serviceName + " not found.");
        }
        return serviceInstance;
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(String serviceName) {
        CloudService cloudService = getService(serviceName, true);
        return doGetServiceKeys(cloudService);
    }

    @Override
    public Map<String, Object> getServiceParameters(UUID guid) {
        String urlPath = "/v2/service_instances/{guid}/parameters";
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put("guid", guid);
        return getResponseMap(urlPath, urlVars);
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        String urlPath = "/v2/services?inline-relations-depth=1";
        List<Map<String, Object>> resourceList = getAllResources(urlPath);
        List<CloudServiceOffering> serviceOfferings = new ArrayList<>();
        for (Map<String, Object> resource : resourceList) {
            CloudServiceOffering serviceOffering = resourceMapper.mapResource(resource, CloudServiceOffering.class);
            serviceOfferings.add(serviceOffering);
        }
        return serviceOfferings;
    }

    @Override
    public List<CloudService> getServices() {
        Map<String, Object> urlVars = new HashMap<>();
        String urlPath = "/v2";
        if (target != null) {
            urlVars.put("space", target.getMetadata()
                .getGuid());
            urlPath = urlPath + "/spaces/{space}";
        }
        urlPath = urlPath + "/service_instances?inline-relations-depth=1&return_user_provided_service_instances=true";
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        List<CloudService> services = new ArrayList<>();
        for (Map<String, Object> resource : resourceList) {
            if (hasEmbeddedResource(resource, "service_plan")) {
                fillInEmbeddedResource(resource, "service_plan", "service");
            }
            services.add(resourceMapper.mapResource(resource, CloudService.class));
        }
        return services;
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return doGetDomains("/v2/shared_domains");
    }

    @Override
    public CloudSpace getSpace(UUID spaceGuid) {
        Map<String, Object> resource = findSpaceResource(spaceGuid);
        CloudSpace space = null;
        if (resource != null) {
            space = resourceMapper.mapResource(resource, CloudSpace.class);
        }
        if (space == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Space with GUID " + spaceGuid + " not found.");
        }
        return space;
    }

    @Override
    public CloudSpace getSpace(String organizationName, String spaceName) {
        return getSpace(organizationName, spaceName, true);
    }

    @Override
    public CloudSpace getSpace(String organizationName, String spaceName, boolean required) {
        UUID organizationGuid = getOrganizationGuid(organizationName, required);
        return getSpace(organizationGuid, spaceName, required);
    }

    @Override
    public CloudSpace getSpace(String spaceName) {
        return getSpace(spaceName, true);
    }

    @Override
    public CloudSpace getSpace(String spaceName, boolean required) {
        UUID organizationGuid = target.getOrganization()
            .getMetadata()
            .getGuid();
        return getSpace(organizationGuid, spaceName, required);
    }

    @Override
    public List<UUID> getSpaceAuditors(String organizationName, String spaceName) {
        String urlPath = "/v2/spaces/{guid}/auditors";
        return getSpaceUserGuids(organizationName, spaceName, urlPath);
    }

    @Override
    public List<UUID> getSpaceAuditors(UUID spaceGuid) {
        String urlPath = "/v2/spaces/{guid}/auditors";
        return getSpaceUserGuids(spaceGuid, urlPath);
    }

    @Override
    public List<UUID> getSpaceDevelopers(String organizationName, String spaceName) {
        String urlPath = "/v2/spaces/{guid}/developers";
        return getSpaceUserGuids(organizationName, spaceName, urlPath);
    }

    @Override
    public List<UUID> getSpaceDevelopers(UUID spaceGuid) {
        String urlPath = "/v2/spaces/{guid}/developers";
        return getSpaceUserGuids(spaceGuid, urlPath);
    }

    @Override
    public List<UUID> getSpaceManagers(String organizationName, String spaceName) {
        String urlPath = "/v2/spaces/{guid}/managers";
        return getSpaceUserGuids(organizationName, spaceName, urlPath);
    }

    @Override
    public List<UUID> getSpaceManagers(UUID spaceGuid) {
        String urlPath = "/v2/spaces/{guid}/managers";
        return getSpaceUserGuids(spaceGuid, urlPath);
    }

    @Override
    public List<CloudSpace> getSpaces() {
        return getSpacesByOrganizationGuid(null);
    }

    @Override
    public List<CloudSpace> getSpaces(String organizationName) {
        UUID organizationGuid = getOrganizationGuid(organizationName, true);
        return getSpacesByOrganizationGuid(organizationGuid);
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
        Map<String, Object> resource = findStackResource(name);
        CloudStack stack = null;
        if (resource != null) {
            stack = resourceMapper.mapResource(resource, CloudStack.class);
        }
        if (stack == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Stack " + name + " not found.");
        }
        return stack;
    }

    @Override
    public List<CloudStack> getStacks() {
        String urlPath = "/v2/stacks";
        List<Map<String, Object>> resources = getAllResources(urlPath);
        List<CloudStack> stacks = new ArrayList<>();
        for (Map<String, Object> resource : resources) {
            stacks.add(resourceMapper.mapResource(resource, CloudStack.class));
        }
        return stacks;
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
        convertV3ClientExceptions(() -> v3Client.applicationsV2()
            .update(UpdateApplicationRequest.builder()
                .applicationId(applicationGuid.toString())
                .name(newName)
                .build())
            .block());
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
        convertV3ClientExceptions(() -> v3Client.applicationsV2()
            .update(UpdateApplicationRequest.builder()
                .applicationId(applicationGuid.toString())
                .state(CloudApplication.State.STARTED.toString())
                .build())
            .block());
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
        convertV3ClientExceptions(() -> v3Client.applicationsV2()
            .update(UpdateApplicationRequest.builder()
                .applicationId(applicationGuid.toString())
                .state(CloudApplication.State.STOPPED.toString())
                .build())
            .block());
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
        unbindService(applicationName, serviceName, ApplicationServicesUpdateCallback.DEFAULT_APPLICATION_SERVICES_UPDATE_CALLBACK);
    }

    @Override
    public void unbindService(String applicationName, String serviceName,
        ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        try {
            UUID applicationGuid = getRequiredApplicationGuid(applicationName);
            UUID serviceGuid = getService(serviceName).getMetadata()
                .getGuid();
            doUnbindService(applicationGuid, serviceGuid);
        } catch (CloudOperationException e) {
            applicationServicesUpdateCallback.onError(e, applicationName, serviceName);
        }
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
        convertV3ClientExceptions(() -> v3Client.applicationsV2()
            .update(UpdateApplicationRequest.builder()
                .applicationId(applicationGuid.toString())
                .diskQuota(diskQuota)
                .build())
            .block());
    }

    @Override
    public void updateApplicationEnv(String applicationName, Map<String, String> env) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        convertV3ClientExceptions(() -> v3Client.applicationsV2()
            .update(UpdateApplicationRequest.builder()
                .applicationId(applicationGuid.toString())
                .environmentJsons(env)
                .build())
            .block());
    }

    @Override
    public void updateApplicationInstances(String applicationName, int instances) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        convertV3ClientExceptions(() -> v3Client.applicationsV2()
            .update(UpdateApplicationRequest.builder()
                .applicationId(applicationGuid.toString())
                .instances(instances)
                .build())
            .block());
    }

    @Override
    public void updateApplicationMemory(String applicationName, int memory) {
        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        convertV3ClientExceptions(() -> v3Client.applicationsV2()
            .update(UpdateApplicationRequest.builder()
                .applicationId(applicationGuid.toString())
                .memory(memory)
                .build())
            .block());
    }

    @Override
    public List<String> updateApplicationServices(String applicationName,
        Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
        ApplicationServicesUpdateCallback applicationServicesUpdateCallbaclk) {
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
        convertV3ClientExceptions(() -> v3Client.applicationsV2()
            .update(requestBuilder.build())
            .block());
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

        convertV3ClientExceptions(() -> v3Client.serviceBrokers()
            .update(UpdateServiceBrokerRequest.builder()
                .serviceBrokerId(brokerGuid.toString())
                .name(serviceBroker.getName())
                .authenticationUsername(serviceBroker.getUsername())
                .authenticationPassword(serviceBroker.getPassword())
                .brokerUrl(serviceBroker.getUrl())
                .build())
            .block());
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, boolean visibility) {
        CloudServiceBroker broker = getServiceBroker(name);

        String urlPath = "/v2/services?q={q}";
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put("q", "service_broker_guid:" + broker.getMetadata()
            .getGuid());
        List<Map<String, Object>> serviceResourceList = getAllResources(urlPath, urlVars);

        for (Map<String, Object> serviceResource : serviceResourceList) {
            Map<String, Object> metadata = (Map<String, Object>) serviceResource.get("metadata");
            String serviceGuid = (String) metadata.get("guid");

            urlPath = "/v2/service_plans?q={q}";
            urlVars = new HashMap<>();
            urlVars.put("q", "service_guid:" + serviceGuid);
            List<Map<String, Object>> planResourceList = getAllResources(urlPath, urlVars);
            for (Map<String, Object> planResource : planResourceList) {
                metadata = (Map<String, Object>) planResource.get("metadata");
                String planGuid = (String) metadata.get("guid");

                Map<String, Object> planUpdateRequest = new HashMap<>();
                planUpdateRequest.put("public", visibility);
                getRestTemplate().put(getUrl("/v2/service_plans/{guid}"), planUpdateRequest, planGuid);
            }
        }
    }

    @Override
    public CloudTask getTask(UUID taskGuid) {
        String urlPath = "/v3/tasks/{taskGuid}";
        Map<String, Object> urlVariables = new HashMap<>();
        urlVariables.put("taskGuid", taskGuid);

        @SuppressWarnings("unchecked")
        Map<String, Object> response = getRestTemplate().getForObject(getUrl(urlPath), Map.class, urlVariables);
        return resourceMapper.mapResource(response, CloudTask.class);
    }

    @Override
    public List<CloudTask> getTasks(String applicationName) {
        UUID applicationId = getRequiredApplicationGuid(applicationName);
        String urlPath = "/v3/apps/{applicationGuid}/tasks";
        Map<String, Object> urlVariables = new HashMap<>();
        urlVariables.put("applicationGuid", applicationId);

        return doGetTasks(urlPath, urlVariables);
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task) {
        UUID applicationId = getRequiredApplicationGuid(applicationName);
        String urlPath = "/v3/apps/{applicationGuid}/tasks";
        Map<String, Object> urlVariables = new HashMap<>();
        urlVariables.put("applicationGuid", applicationId);
        Map<String, Object> request = new HashMap<>();
        Assert.notNull(task.getCommand(), "Command must not be null");
        request.put("command", task.getCommand());

        addNonNullRequestParameter(request, "name", task.getName());
        addNonNullRequestParameter(request, "memory_in_mb", task.getMemory());
        addNonNullRequestParameter(request, "disk_in_mb", task.getDiskQuota());

        @SuppressWarnings("unchecked")
        Map<String, Object> resource = getRestTemplate().postForObject(getUrl(urlPath), request, Map.class, urlVariables);
        return resourceMapper.mapResource(resource, CloudTask.class);
    }

    private void addNonNullRequestParameter(Map<String, Object> request, String requestParameterName, Object value) {
        if (value != null) {
            request.put(requestParameterName, value);
        }
    }

    @Override
    public CloudTask cancelTask(UUID taskGuid) {
        String urlPath = "/v3/tasks/{taskGuid}/actions/cancel";
        Map<String, Object> urlVariables = new HashMap<>();
        urlVariables.put("taskGuid", taskGuid);
        Map<String, Object> request = null;

        @SuppressWarnings("unchecked")
        Map<String, Object> resource = getRestTemplate().postForObject(getUrl(urlPath), request, Map.class, urlVariables);
        return resourceMapper.mapResource(resource, CloudTask.class);
    }

    @Override
    public void uploadApplication(String applicationName, File file, UploadStatusCallback callback) throws IOException {
        UploadToken uploadToken = startUpload(applicationName, file);
        processAsyncUpload(uploadToken.getToken(), callback);
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
        processAsyncUploadInBackground(uploadToken.getToken(), callback);
        return uploadToken;
    }

    private UploadToken startUpload(String applicationName, File file) {
        Assert.notNull(applicationName, "AppName must not be null");
        Assert.notNull(file, "File must not be null");

        UUID applicationGuid = getRequiredApplicationGuid(applicationName);
        UUID packageGuid = createPackageForApplication(applicationGuid);

        convertV3ClientExceptions(() -> v3Client.packages()
            .upload(UploadPackageRequest.builder()
                .bits(file.toPath())
                .packageId(packageGuid.toString())
                .build())
            .block());

        return ImmutableUploadToken.builder()
            .token(getUrl("/v3/packages/" + packageGuid))
            .packageGuid(packageGuid)
            .build();
    }

    private UUID createPackageForApplication(UUID applicationGuid) {
        Map<String, Object> packageRequest = new HashMap<>();
        packageRequest.put("type", "bits");
        Map<String, Map<String, Map<String, Object>>> relationships = new HashMap<>();
        Map<String, Map<String, Object>> app = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("guid", applicationGuid);
        app.put("data", data);
        relationships.put("app", app);
        packageRequest.put("relationships", relationships);

        String packageResponse = getRestTemplate().postForObject(getUrl("/v3/packages"), packageRequest, String.class);
        Map<String, Object> packageEntity = JsonUtil.convertJsonToMap(packageResponse);

        return resourceMapper.mapResource(packageEntity, CloudPackage.class)
            .getMetadata()
            .getGuid();
    }

    @Override
    public CloudBuild createBuild(UUID packageGuid) {
        Map<String, Object> buildRequest = new HashMap<>();
        Map<String, Object> packageMap = new HashMap<>();
        packageMap.put("guid", packageGuid);
        buildRequest.put("package", packageMap);

        String buildResponse = getRestTemplate().postForObject(getUrl("/v3/builds"), buildRequest, String.class);
        Map<String, Object> buildEntity = JsonUtil.convertJsonToMap(buildResponse);

        return resourceMapper.mapResource(buildEntity, CloudBuild.class);
    }

    @Override
    public List<CloudBuild> getBuildsForPackage(UUID packageGuid) {
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put("packageGuid", packageGuid);
        return doGetResources("/v3/builds?package_guids={packageGuid}", urlVars, CloudBuild.class);
    }

    @Override
    public void bindDropletToApp(UUID dropletGuid, UUID applicationGuid) {
        convertV3ClientExceptions(() -> v3Client.applicationsV3()
            .setCurrentDroplet(SetApplicationCurrentDropletRequest.builder()
                .applicationId(applicationGuid.toString())
                .data(Relationship.builder()
                    .id(dropletGuid.toString())
                    .build())
                .build())
            .block());
    }

    @Override
    public CloudBuild getBuild(UUID buildGuid) {
        ResponseEntity<Map<String, Object>> responseEntity = getRestTemplate().exchange(getUrl("/v3/builds/{buildGuid}"), HttpMethod.GET,
            HttpEntity.EMPTY, new ParameterizedTypeReference<Map<String, Object>>() {
            }, buildGuid);

        return resourceMapper.mapResource(responseEntity.getBody(), CloudBuild.class);
    }

    @Override
    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid) {
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put("applicationGuid", applicationGuid);
        return doGetResources("/v3/apps/{applicationGuid}/builds", urlVars, CloudBuild.class);
    }

    @Override
    public Upload getUploadStatus(String uploadToken) {
        CloudPackage cloudPackage = getCloudPackage(uploadToken);
        ErrorDetails errorDetails = ImmutableErrorDetails.builder()
            .description(cloudPackage.getData()
                .getError())
            .build();

        return ImmutableUpload.builder()
            .status(cloudPackage.getStatus())
            .errorDetails(errorDetails)
            .build();
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

        for (String existingDomain : existingDomains.keySet()) {
            if (domain.equals(existingDomain)) {
                uriInfo.put("domainName", existingDomain);
                uriInfo.put("host", hostName);
                uriInfo.put("path", path);
            }
        }
        if (uriInfo.get("domainName") == null) {
            throw new IllegalArgumentException("Domain not found for URI " + uri);
        }
        if (uriInfo.get("host") == null) {
            throw new IllegalArgumentException("Invalid URI " + uri + " -- host not specified for domain " + uriInfo.get("domainName"));
        }
    }

    protected String getUrl(String path) {
        return controllerUrl + (path.startsWith(DEFAULT_PATH_SEPARATOR) ? path : DEFAULT_PATH_SEPARATOR + path);
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
        convertV3ClientExceptions(() -> v3Client.applicationsV2()
            .associateRoute(AssociateApplicationRouteRequest.builder()
                .applicationId(applicationGuid.toString())
                .routeId(routeGuid.toString())
                .build())
            .block());
    }

    private UUID getOrAddRoute(UUID domainGuid, String host, String path) {
        UUID routeGuid = getRouteGuid(domainGuid, host, path);
        if (routeGuid == null) {
            routeGuid = doAddRoute(domainGuid, host, path);
        }
        return routeGuid;
    }

    private File createTemporaryUploadFile(InputStream inputStream) throws IOException {
        File file = File.createTempFile("cfjava", null);
        FileOutputStream outputStream = new FileOutputStream(file);
        FileCopyUtils.copy(inputStream, outputStream);
        outputStream.close();
        return file;
    }

    private UUID doAddRoute(UUID domainGuid, String host, String path) {
        assertSpaceProvided("add route");
        CreateRouteResponse response = convertV3ClientExceptions(() -> v3Client.routes()
            .create(CreateRouteRequest.builder()
                .domainId(domainGuid.toString())
                .host(host)
                .path(path)
                .spaceId(getTargetSpaceId())
                .build())
            .block());
        return UUID.fromString(response.getMetadata()
            .getId());
    }

    private void doCreateDomain(String name) {
        convertV3ClientExceptions(() -> v3Client.domains()
            .create(CreateDomainRequest.builder()
                .wildcard(true)
                .owningOrganizationId(getTargetOrganizationGuid())
                .name(name)
                .build())
            .block());
    }

    private void doDeleteDomain(UUID domainGuid) {
        convertV3ClientExceptions(() -> v3Client.privateDomains()
            .delete(DeletePrivateDomainRequest.builder()
                .privateDomainId(domainGuid.toString())
                .build())
            .block());
    }

    private void doDeleteRoute(UUID routeGuid) {
        convertV3ClientExceptions(() -> v3Client.routes()
            .delete(DeleteRouteRequest.builder()
                .routeId(routeGuid.toString())
                .build())
            .block());
    }

    private void doDeleteService(CloudService service) {
        List<UUID> serviceBindingGuids = getServiceBindingGuids(service);
        for (UUID serviceBindingGuid : serviceBindingGuids) {
            doUnbindService(serviceBindingGuid);
        }
        UUID serviceGuid = service.getMetadata()
            .getGuid();
        convertV3ClientExceptions(() -> v3Client.serviceInstances()
            .delete(DeleteServiceInstanceRequest.builder()
                .acceptsIncomplete(true)
                .serviceInstanceId(serviceGuid.toString())
                .build())
            .block());
    }

    @SuppressWarnings("unchecked")
    private InstancesInfo doGetApplicationInstances(UUID applicationGuid) {
        List<Map<String, Object>> instanceList = new ArrayList<>();
        Map<String, Object> respMap = getInstanceInfoForApp(applicationGuid, "instances");
        if (respMap == null) {
            return null;
        }
        List<String> keys = new ArrayList<>(respMap.keySet());
        Collections.sort(keys);
        for (String instanceId : keys) {
            Integer index;
            try {
                index = Integer.valueOf(instanceId);
            } catch (NumberFormatException e) {
                index = -1;
            }
            Map<String, Object> instanceMap = (Map<String, Object>) respMap.get(instanceId);
            instanceMap.put("index", index);
            instanceList.add(instanceMap);
        }
        return new InstancesInfo(instanceList);
    }

    private List<CloudDomain> doGetDomains(CloudOrganization organization) {
        Map<String, Object> urlVars = new HashMap<>();
        String urlPath = "/v2";
        if (organization != null) {
            urlVars.put("organization", organization.getMetadata()
                .getGuid());
            urlPath = urlPath + "/organizations/{organization}";
        }
        urlPath = urlPath + "/private_domains";
        return doGetDomains(urlPath, urlVars);
    }

    private List<CloudDomain> doGetDomains(String urlPath) {
        return doGetDomains(urlPath, null);
    }

    private List<CloudDomain> doGetDomains(String urlPath, Map<String, Object> urlVariables) {
        return doGetResources(urlPath, urlVariables, CloudDomain.class);
    }

    private List<CloudEvent> doGetEvents(String urlPath, Map<String, Object> urlVariables) {
        return doGetResources(urlPath, urlVariables, CloudEvent.class);
    }

    private List<CloudTask> doGetTasks(String urlPath, Map<String, Object> urlVariables) {
        return doGetResources(urlPath, urlVariables, CloudTask.class);
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

    private List<CloudRoute> doGetRoutes(UUID domainGuid) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2";
        // TODO: NOT implemented ATM:
        // if (sessionSpace != null) {
        // urlVars.put("space", sessionSpace.getMetadata().getGuid());
        // urlPath = urlPath + "/spaces/{space}";
        // }
        urlPath = urlPath + "/routes?inline-relations-depth=1";
        List<Map<String, Object>> allRoutes = getAllResources(urlPath, urlVars);
        List<CloudRoute> routes = new ArrayList<CloudRoute>();
        for (Map<String, Object> route : allRoutes) {
            // TODO: move space_guid to path once implemented (see above):
            UUID space = CloudEntityResourceMapper.getV2ResourceAttribute(route, "space_guid", UUID.class);
            UUID domain = CloudEntityResourceMapper.getV2ResourceAttribute(route, "domain_guid", UUID.class);
            if (target.getMetadata()
                .getGuid()
                .equals(space) && domainGuid.equals(domain)) {
                // routes.add(CloudEntityResourceMapper.getEntityAttribute(route, "host", String.class));
                routes.add(resourceMapper.mapResource(route, CloudRoute.class));
            }
        }
        return routes;
    }

    private Map<String, Object> doGetServiceInstance(String serviceName, int inlineDepth) {
        String urlPath = "/v2";
        Map<String, Object> urlVars = new HashMap<>();
        if (target != null) {
            urlVars.put("space", target.getMetadata()
                .getGuid());
            urlPath = urlPath + "/spaces/{space}";
        }
        urlVars.put("q", "name:" + serviceName);
        urlPath = urlPath + "/service_instances?q={q}&return_user_provided_service_instances=true";
        if (inlineDepth > 0) {
            urlPath = urlPath + "&inline-relations-depth=" + inlineDepth;
        }

        List<Map<String, Object>> resources = getAllResources(urlPath, urlVars);

        if (!resources.isEmpty()) {
            Map<String, Object> serviceResource = resources.get(0);
            if (hasEmbeddedResource(serviceResource, "service_plan")) {
                fillInEmbeddedResource(serviceResource, false, "service_plan", "service");
            }
            // If the bindings are more than 50, then they will not be included in the response due to the way the 'inline-relations-depth'
            // parameter works. In this case, we have to make one additional request to the URL specified in the 'service_bindings_url'
            // field of the response.
            if (hasEmbeddedResource(serviceResource, "service_bindings")) {
                fillInEmbeddedResource(serviceResource, true, "service_bindings", "service_binding_parameters");
            }
            return serviceResource;
        }
        return null;
    }

    private List<CloudServiceKey> doGetServiceKeys(CloudService service) {
        String urlPath = "/v2/service_instances/{serviceId}/service_keys";
        Map<String, Object> pathVariables = new HashMap<>();
        pathVariables.put("serviceId", service.getMetadata()
            .getGuid());
        List<Map<String, Object>> resourceList = getAllResources(urlPath, pathVariables);
        List<CloudServiceKey> serviceKeys = new ArrayList<>();
        for (Map<String, Object> resource : resourceList) {
            CloudServiceKey serviceKey = resourceMapper.mapResource(resource, CloudServiceKey.class);
            serviceKey = ImmutableCloudServiceKey.copyOf(serviceKey)
                .withService(service);
            serviceKeys.add(serviceKey);
        }
        return serviceKeys;
    }

    private void doUnbindService(UUID applicationGuid, UUID serviceGuid) {
        UUID serviceBindingGuid = getServiceBindingGuid(applicationGuid, serviceGuid);
        doUnbindService(serviceBindingGuid);
    }

    private void doUnbindService(UUID serviceBindingGuid) {
        convertV3ClientExceptions(() -> v3Client.serviceBindingsV2()
            .delete(DeleteServiceBindingRequest.builder()
                .serviceBindingId(serviceBindingGuid.toString())
                .build())
            .block());
    }

    private List<CloudRoute> fetchOrphanRoutes(String domainName) {
        List<CloudRoute> orphanRoutes = new ArrayList<>();
        for (CloudRoute route : getRoutes(domainName)) {
            if (isOrphanRoute(route)) {
                orphanRoutes.add(route);
            }
        }
        return orphanRoutes;
    }

    private void fillInEmbeddedResource(Map<String, Object> resource, String... resourcePath) {
        fillInEmbeddedResource(resource, false, resourcePath);
    }

    @SuppressWarnings("unchecked")
    private void fillInEmbeddedResource(Map<String, Object> resource, boolean isFailSafe, String... resourcePath) {
        if (resourcePath.length == 0) {
            return;
        }
        Map<String, Object> entity = (Map<String, Object>) resource.get("entity");
        if (entity == null) {
            throw new IllegalStateException("Unable to find required element \"entity\" in the resource.");
        }

        String headKey = resourcePath[0];
        String[] tailPath = Arrays.copyOfRange(resourcePath, 1, resourcePath.length);

        if (!entity.containsKey(headKey)) {
            String pathUrl = entity.get(headKey + "_url")
                .toString();
            Object response = retrieveResponse(getUrl(pathUrl), isFailSafe);
            if (response == null) {
                fillInEmbeddedResource(resource, isFailSafe, tailPath);
            }
            if (response instanceof Map) {
                Map<String, Object> responseMap = (Map<String, Object>) response;
                List<Map<String, Object>> resources = (List<Map<String, Object>>) responseMap.get("resources");
                if (resources != null) {
                    addAllRemainingResources(responseMap, resources);
                    response = resources;
                }
            }
            entity.put(headKey, response);
        }
        Object embeddedResource = entity.get(headKey);

        if (embeddedResource instanceof Map) {
            Map<String, Object> embeddedResourceMap = (Map<String, Object>) embeddedResource;
            fillInEmbeddedResource(embeddedResourceMap, isFailSafe, tailPath);
        } else if (embeddedResource instanceof List) {
            List<Object> embeddedResourcesList = (List<Object>) embeddedResource;
            for (Object r : embeddedResourcesList) {
                fillInEmbeddedResource((Map<String, Object>) r, isFailSafe, tailPath);
            }
        }
    }

    private Object retrieveResponse(String url, boolean isFailSafe) {
        try {
            return getRestTemplate().getForObject(url, Object.class);
        } catch (CloudOperationException e) {
            if (!isFailSafe) {
                throw e;
            }
            return null;
        }
    }

    private Map<String, Object> findApplicationResource(UUID applicationGuid) {
        Map<String, Object> urlVars = new HashMap<>();
        String urlPath = "/v2/apps/{application}?inline-relations-depth=1";
        urlVars.put("application", applicationGuid);
        String response = getRestTemplate().getForObject(getUrl(urlPath), String.class, urlVars);
        Map<String, Object> resource = JsonUtil.convertJsonToMap(response);
        return processApplicationResource(resource);
    }

    private Map<String, Object> findApplicationResource(String applicationName, boolean fetchAdditionalInfo) {
        Map<String, Object> urlVars = new HashMap<>();
        String urlPath = "/v2";
        if (target != null) {
            urlVars.put("space", target.getMetadata()
                .getGuid());
            urlPath = urlPath + "/spaces/{space}";
        }
        urlVars.put("q", "name:" + applicationName);
        urlPath = urlPath + "/apps?q={q}";
        urlPath = fetchAdditionalInfo ? urlPath + "&inline-relations-depth=1" : urlPath;

        List<Map<String, Object>> allResources = getAllResources(urlPath, urlVars);
        if (allResources.isEmpty()) {
            return null;
        }
        Map<String, Object> resource = allResources.get(0);
        return fetchAdditionalInfo ? processApplicationResource(resource) : resource;
    }

    private List<String> findApplicationUris(UUID applicationGuid) {
        String urlPath = "/v2/apps/{application}/routes?inline-relations-depth=1";
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put("application", applicationGuid);
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        List<String> uris = new ArrayList<>();
        for (Map<String, Object> resource : resourceList) {
            Map<String, Object> domainResource = CloudEntityResourceMapper.getEmbeddedResource(resource, "domain");
            String host = CloudEntityResourceMapper.getV2ResourceAttribute(resource, "host", String.class);
            String domain = CloudEntityResourceMapper.getV2ResourceAttribute(domainResource, "name", String.class);
            if (host != null && host.length() > 0)
                uris.add(host + "." + domain);
            else
                uris.add(domain);
        }
        return uris;
    }

    private Map<String, Object> findServiceBrokerResource(String name) {
        String urlPath = "/v2/service_brokers?q={q}";
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put("q", "name:" + name);
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        if (!resourceList.isEmpty()) {
            return resourceList.get(0);
        }
        return null;
    }

    private List<Map<String, Object>> findSpaceResources(UUID organizationGuid, String spaceName) {
        String urlPath = "/v2/spaces?inline-relations-depth=1";
        Map<String, Object> request = new HashMap<>();
        if (organizationGuid != null) {
            urlPath += "&q=organization_guid:{organization_guid}";
            request.put("organization_guid", organizationGuid);
        }
        if (spaceName != null) {
            urlPath += "&q=name:{name}";
            request.put("name", spaceName);
        }
        return getAllResources(urlPath, request);
    }

    private Map<String, Object> findSpaceResource(UUID organizationGuid, String spaceName) {
        List<Map<String, Object>> resourceList = findSpaceResources(organizationGuid, spaceName);
        if (!resourceList.isEmpty()) {
            return resourceList.get(0);
        }
        return null;
    }

    private Map<String, Object> findSpaceResource(UUID spaceGuid) {
        String urlPath = "/v2/spaces/{space}?inline-relations-depth=1";
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put("space", spaceGuid);

        String response = getRestTemplate().getForObject(getUrl(urlPath), String.class, urlVars);
        return JsonUtil.convertJsonToMap(response);
    }

    private Map<String, Object> findStackResource(String name) {
        String urlPath = "/v2/stacks?q={q}";
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put("q", "name:" + name);
        List<Map<String, Object>> resources = getAllResources(urlPath, urlVars);
        if (!resources.isEmpty()) {
            return resources.get(0);
        }
        return null;
    }

    private CloudServicePlan findPlanForService(CloudService service) {
        List<CloudServiceOffering> offerings = getServiceOfferings(service.getLabel());
        for (CloudServiceOffering offering : offerings) {
            if (service.getVersion() == null || service.getVersion()
                .equals(offering.getVersion())) {
                for (CloudServicePlan plan : offering.getCloudServicePlans()) {
                    if (service.getPlan() != null && service.getPlan()
                        .equals(plan.getName())) {
                        return plan;
                    }
                }
            }
        }
        throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service plan " + service.getPlan() + " not found.");
    }

    private List<Map<String, Object>> getAllResources(String urlPath) {
        return getAllResources(urlPath, null);
    }

    @SuppressWarnings("unchecked")
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

    private UUID getRequiredApplicationGuid(String applicationName) {
        UUID applicationGuid = getApplicationGuid(applicationName);
        if (applicationGuid == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application " + applicationName + " not found.");
        }
        return applicationGuid;
    }

    @SuppressWarnings("unchecked")
    private UUID getApplicationGuid(String applicationName) {
        Map<String, Object> resource = findApplicationResource(applicationName, false);
        if (resource == null) {
            return null;
        }
        Map<String, Object> applicationMeta = (Map<String, Object>) resource.get("metadata");
        return UUID.fromString(String.valueOf(applicationMeta.get("guid")));
    }

    private UUID getOrganizationGuid(String organizationName, boolean required) {
        CloudOrganization organization = getOrganization(organizationName, required);
        return organization != null ? organization.getMetadata()
            .getGuid() : null;
    }

    private CloudSpace getSpace(UUID organizationGuid, String spaceName, boolean required) {
        Map<String, Object> resource = findSpaceResource(organizationGuid, spaceName);
        CloudSpace space = null;
        if (resource != null) {
            space = resourceMapper.mapResource(resource, CloudSpace.class);
        }
        if (space == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found",
                "Space " + spaceName + " not found in organization with GUID " + organizationGuid + ".");
        }
        return space;
    }

    private List<CloudSpace> getSpacesByOrganizationGuid(UUID organizationGuid) {
        List<Map<String, Object>> resourceList = findSpaceResources(organizationGuid, null);
        List<CloudSpace> spaces = new ArrayList<>();
        for (Map<String, Object> resource : resourceList) {
            spaces.add(resourceMapper.mapResource(resource, CloudSpace.class));
        }
        return spaces;
    }

    private List<UUID> getServiceBindingGuids(UUID applicationGuid) {
        Flux<ServiceBindingResource> bindings = convertV3ClientExceptions(
            () -> getServiceBindingResourceByApplicationGuid(applicationGuid));
        return getGuids(bindings);
    }

    private Flux<ServiceBindingResource> getServiceBindingResourceByApplicationGuid(UUID applicationGuid) {
        return convertV3ClientExceptions(() -> PaginationUtils.requestClientV2Resources(page -> v3Client.applicationsV2()
            .listServiceBindings(ListApplicationServiceBindingsRequest.builder()
                .applicationId(applicationGuid.toString())
                .page(page)
                .build())));
    }

    private List<UUID> getServiceBindingGuids(CloudService service) {
        Flux<ServiceBindingResource> serviceBindings = getServiceBindingResources(service);
        return getGuids(serviceBindings);
    }

    private Flux<ServiceBindingResource> getServiceBindingResources(CloudService service) {
        UUID serviceGuid = service.getMetadata()
            .getGuid();
        if (service.isUserProvided()) {
            return getUserProvidedServiceBindingResources(serviceGuid);
        }
        return getServiceBindingResources(serviceGuid);
    }

    private Flux<ServiceBindingResource> getUserProvidedServiceBindingResources(UUID serviceGuid) {
        return convertV3ClientExceptions(() -> PaginationUtils.requestClientV2Resources(page -> v3Client.userProvidedServiceInstances()
            .listServiceBindings(ListUserProvidedServiceInstanceServiceBindingsRequest.builder()
                .userProvidedServiceInstanceId(serviceGuid.toString())
                .page(page)
                .build())));
    }

    private Flux<ServiceBindingResource> getServiceBindingResources(UUID serviceGuid) {
        return convertV3ClientExceptions(() -> PaginationUtils.requestClientV2Resources(page -> v3Client.serviceInstances()
            .listServiceBindings(ListServiceInstanceServiceBindingsRequest.builder()
                .serviceInstanceId(serviceGuid.toString())
                .page(page)
                .build())));
    }

    private List<UUID> getGuids(Flux<? extends Resource<?>> resources) {
        return resources.map(Resource::getMetadata)
            .map(Metadata::getId)
            .map(UUID::fromString)
            .collectList()
            .block();
    }

    private UUID getDomainGuid(String name, boolean required) {
        List<DomainResource> domains = convertV3ClientExceptions(() -> getDomainResources(name)).collectList()
            .block();
        if (!domains.isEmpty()) {
            DomainResource domain = domains.get(0);
            return UUID.fromString(domain.getMetadata()
                .getId());
        }
        if (required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Domain " + name + " not found.");
        }
        return null;
    }

    private Flux<DomainResource> getDomainResources(String name) {
        return PaginationUtils.requestClientV2Resources(page -> v3Client.domains()
            .list(ListDomainsRequest.builder()
                .name(name)
                .page(page)
                .build()));
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

    private Map<String, Object> getInstanceInfoForApp(UUID applicationGuid, String path) {
        try {
            String url = getUrl("/v2/apps/{guid}/" + path);
            Map<String, Object> urlVars = new HashMap<>();
            urlVars.put("guid", applicationGuid);
            String response = getRestTemplate().getForObject(url, String.class, urlVars);
            return JsonUtil.convertJsonToMap(response);
        } catch (CloudOperationException e) {
            if (HttpStatus.BAD_REQUEST.equals(e.getStatusCode())) {
                // Application has been stopped before we could get the instance info
                logger.warn(e.getMessage(), e);
                return null;
            }
            throw e;
        }
    }

    private UUID getRouteGuid(UUID domainGuid, String host, String path) {
        List<RouteResource> routes = convertV3ClientExceptions(() -> getRouteResources(domainGuid, host, path).collectList()
            .block());

        if (!routes.isEmpty()) {
            RouteResource route = routes.get(0);
            return UUID.fromString(route.getMetadata()
                .getId());
        }
        return null;
    }

    private Flux<RouteResource> getRouteResources(UUID domainGuid, String host, String path) {
        ListSpaceRoutesRequest.Builder requestBuilder = ListSpaceRoutesRequest.builder();
        if (host != null) {
            requestBuilder.host(host);
        }
        if (path != null) {
            requestBuilder.path(path);
        }
        requestBuilder.spaceId(getTargetSpaceId())
            .domainId(domainGuid.toString());

        return PaginationUtils.requestClientV2Resources(page -> v3Client.spaces()
            .listRoutes(requestBuilder.page(page)
                .build()));
    }

    private int getRunningInstances(UUID applicationGuid, CloudApplication.State applicationState) {
        if (applicationState == CloudApplication.State.STOPPED) {
            return 0;
        }
        int running = 0;
        InstancesInfo instancesInfo = doGetApplicationInstances(applicationGuid);
        if (instancesInfo != null && instancesInfo.getInstances() != null) {
            for (InstanceInfo instanceInfo : instancesInfo.getInstances()) {
                if (InstanceState.RUNNING == instanceInfo.getState()) {
                    running++;
                }
            }
        }
        return running;
    }

    // Security Group operations

    @SuppressWarnings("unchecked")
    private UUID getServiceBindingGuid(UUID applicationGuid, UUID serviceId) {
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put("guid", applicationGuid);
        List<Map<String, Object>> resourceList = getAllResources("/v2/apps/{guid}/service_bindings", urlVars);
        UUID serviceBindingId = null;
        if (!resourceList.isEmpty()) {
            for (Map<String, Object> resource : resourceList) {
                Map<String, Object> bindingMeta = (Map<String, Object>) resource.get("metadata");
                Map<String, Object> bindingEntity = (Map<String, Object>) resource.get("entity");
                String serviceInstanceGuid = (String) bindingEntity.get("service_instance_guid");
                if (serviceInstanceGuid != null && serviceInstanceGuid.equals(serviceId.toString())) {
                    String bindingGuid = (String) bindingMeta.get("guid");
                    serviceBindingId = UUID.fromString(bindingGuid);
                    break;
                }
            }
        }
        return serviceBindingId;
    }

    private List<CloudServiceOffering> getServiceOfferings(String label) {
        Assert.notNull(label, "Service label must not be null");
        List<Map<String, Object>> resourceList = getAllResources("/v2/services?inline-relations-depth=1");
        List<CloudServiceOffering> results = new ArrayList<>();
        for (Map<String, Object> resource : resourceList) {
            CloudServiceOffering cloudServiceOffering = resourceMapper.mapResource(resource, CloudServiceOffering.class);
            if (cloudServiceOffering.getName() != null && label.equals(cloudServiceOffering.getName())) {
                results.add(cloudServiceOffering);
            }
        }
        return results;
    }

    private UUID getSpaceGuid(String spaceName, UUID organizationGuid) {
        Map<String, Object> urlVars = new HashMap<>();
        String urlPath = "/v2/organizations/{organizationGuid}/spaces?inline-relations-depth=1&q=name:{name}";
        urlVars.put("organizationGuid", organizationGuid);
        urlVars.put("name", spaceName);
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        if (!resourceList.isEmpty()) {
            Map<String, Object> resource = resourceList.get(0);
            return resourceMapper.getGuidOfV2Resource(resource);
        }
        return null;
    }

    private List<UUID> getSpaceUserGuids(String organizationName, String spaceName, String urlPath) {
        if (organizationName == null || spaceName == null) {
            assertSpaceProvided("get space users");
        }

        UUID spaceGuid;
        if (spaceName == null) {
            spaceGuid = target.getMetadata()
                .getGuid();
        } else {
            CloudOrganization organization = (organizationName == null ? target.getOrganization() : getOrganization(organizationName));
            spaceGuid = getSpaceGuid(spaceName, organization.getMetadata()
                .getGuid());
        }
        return getSpaceUserGuids(spaceGuid, urlPath);
    }

    private List<UUID> getSpaceUserGuids(UUID spaceGuid, String urlPath) {
        Assert.notNull(spaceGuid, "Unable to get space users without specifying space GUID.");
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put("guid", spaceGuid);

        List<UUID> managersGuid = new ArrayList<>();
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        for (Map<String, Object> resource : resourceList) {
            UUID userGuid = resourceMapper.getGuidOfV2Resource(resource);
            managersGuid.add(userGuid);
        }
        return managersGuid;
    }

    private Map<String, Object> getUserInfo(String user) {
        // String userJson = getRestTemplate().getForObject(getUrl("/v2/users/{guid}"), String.class, user);
        // Map<String, Object> userInfo = (Map<String, Object>) JsonUtil.convertJsonToMap(userJson);
        // return userInfo();
        // TODO: remove this temporary hack once the /v2/users/ uri can be accessed by mere mortals
        String userJson = "{}";
        OAuth2AccessToken accessToken = oAuthClient.getToken();
        if (accessToken != null) {
            String tokenString = accessToken.getValue();
            int x = tokenString.indexOf('.');
            int y = tokenString.indexOf('.', x + 1);
            String encodedString = tokenString.substring(x + 1, y);
            try {
                byte[] decodedBytes = new sun.misc.BASE64Decoder().decodeBuffer(encodedString);
                userJson = new String(decodedBytes, 0, decodedBytes.length, StandardCharsets.UTF_8);
            } catch (IOException e) {
            }
        }
        return (JsonUtil.convertJsonToMap(userJson));
    }

    @SuppressWarnings("unchecked")
    private boolean hasEmbeddedResource(Map<String, Object> resource, String resourceKey) {
        Map<String, Object> entity = (Map<String, Object>) resource.get("entity");
        return entity.containsKey(resourceKey) || entity.containsKey(resourceKey + "_url");
    }

    private boolean isOrphanRoute(CloudRoute cloudRoute) {
        return cloudRoute.getAppsUsingRoute() == 0;
    }

    @SuppressWarnings("unchecked")
    private CloudApplication mapCloudApplication(Map<String, Object> resource) {
        UUID applicationGuid = resourceMapper.getGuidOfV2Resource(resource);
        CloudApplication application = null;
        if (resource != null) {
            int running = getRunningInstances(applicationGuid,
                CloudApplication.State.valueOf(CloudEntityResourceMapper.getV2ResourceAttribute(resource, "state", String.class)));
            ((Map<String, Object>) resource.get("entity")).put("running_instances", running);
            application = resourceMapper.mapResource(resource, CloudApplication.class);
            List<String> uris = findApplicationUris(application.getMetadata()
                .getGuid());
            application = ImmutableCloudApplication.copyOf(application)
                .withUris(uris);
        }
        return application;
    }

    private Map<String, Object> processApplicationResource(Map<String, Object> resource) {
        try {
            fillInEmbeddedResource(resource, "service_bindings", "service_instance");
            fillInEmbeddedResource(resource, "stack");
            return resource;
        } catch (CloudOperationException e) {
            if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
                // Application has been deleted before we could fetch the embedded resource
                logger.warn(e.getMessage(), e);
                return null;
            }
            throw e;
        }
    }

    private void processAsyncUploadInBackground(final String packageUrl, final UploadStatusCallback callback) {
        String threadName = String.format("App upload monitor: %s", packageUrl);
        new Thread(() -> processAsyncUpload(packageUrl, callback), threadName).start();
    }

    private void processAsyncUpload(String packageUrl, UploadStatusCallback callback) {
        if (callback == null) {
            callback = UploadStatusCallback.NONE;
        }
        while (true) {
            Upload upload = getUploadStatus(packageUrl);
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

    private CloudPackage getCloudPackage(String packageUrl) {
        ResponseEntity<Map<String, Object>> cloudPackageEntity = getRestTemplate().exchange(packageUrl, HttpMethod.GET, HttpEntity.EMPTY,
            new ParameterizedTypeReference<Map<String, Object>>() {
            });

        return resourceMapper.mapResource(cloudPackageEntity.getBody(), CloudPackage.class);
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
        convertV3ClientExceptions(() -> v3Client.applicationsV2()
            .removeRoute(RemoveApplicationRouteRequest.builder()
                .applicationId(applicationGuid.toString())
                .routeId(routeGuid.toString())
                .build())
            .block());
    }

    private <T> T convertV3ClientExceptions(Supplier<T> runnable) {
        try {
            return runnable.get();
        } catch (AbstractCloudFoundryException e) {
            HttpStatus httpStatus = HttpStatus.valueOf(e.getStatusCode());
            throw new CloudOperationException(httpStatus, httpStatus.getReasonPhrase(), e.getMessage(), e);
        } catch (IllegalArgumentException e) { // Usually, the operations client throws such exceptions when an entity cannot be found. See
                                               // org.cloudfoundry.util.ExceptionUtils.
            throw new CloudOperationException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.getReasonPhrase(), e.getMessage(), e);
        } catch (IllegalStateException e) {
            throw new CloudException(e.getMessage(), e);
        }
    }

    private String getTargetOrganizationGuid() {
        return target.getOrganization()
            .getMetadata()
            .getGuid()
            .toString();
    }

    private String getTargetSpaceId() {
        return target.getMetadata()
            .getGuid()
            .toString();
    }

}
