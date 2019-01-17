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
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;

import javax.websocket.ClientEndpointConfig;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.ClientHttpResponseCallback;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudOperationException;
import org.cloudfoundry.client.lib.RestLogCallback;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.archive.DirectoryApplicationArchive;
import org.cloudfoundry.client.lib.archive.ZipApplicationArchive;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationLogs;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudEntity.Meta;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.CloudJob;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudPackage;
import org.cloudfoundry.client.lib.domain.CloudQuota;
import org.cloudfoundry.client.lib.domain.CloudResource;
import org.cloudfoundry.client.lib.domain.CloudResources;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudSecurityGroup;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.CloudUser;
import org.cloudfoundry.client.lib.domain.CrashInfo;
import org.cloudfoundry.client.lib.domain.CrashesInfo;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.ErrorDetails;
import org.cloudfoundry.client.lib.domain.InstanceState;
import org.cloudfoundry.client.lib.domain.InstanceStats;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.SecurityGroupRule;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Status;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadApplicationPayload;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.cloudfoundry.client.lib.oauth2.OauthClient;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.cloudfoundry.client.lib.util.CloudUtil;
import org.cloudfoundry.client.lib.util.JsonUtil;
import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;

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

    private static final String DEFAULT_HOST_DOMAIN_SEPARATOR = "\\.";

    private static final String DEFAULT_PATH_SEPARATOR = "/";

    private static final String AUTHORIZATION_HEADER_KEY = "Authorization";

    private static final long JOB_POLLING_PERIOD = TimeUnit.SECONDS.toMillis(5);

    private static final String LOGS_LOCATION = "logs";

    private static final String PROXY_USER_HEADER_KEY = "Proxy-User";

    private final Log logger;

    protected CloudCredentials cloudCredentials;

    private URL cloudControllerUrl;

    private LoggregatorClient loggregatorClient;

    private OauthClient oauthClient;

    private CloudEntityResourceMapper resourceMapper = new CloudEntityResourceMapper();

    private RestTemplate restTemplate;

    private CloudSpace sessionSpace;

    public CloudControllerRestClientImpl(URL cloudControllerUrl, RestTemplate restTemplate, OauthClient oauthClient,
        LoggregatorClient loggregatorClient, CloudCredentials cloudCredentials, CloudSpace sessionSpace) {
        logger = LogFactory.getLog(getClass().getName());

        initialize(cloudControllerUrl, restTemplate, oauthClient, loggregatorClient, cloudCredentials);

        this.sessionSpace = sessionSpace;
    }

    public CloudControllerRestClientImpl(URL cloudControllerUrl, RestTemplate restTemplate, OauthClient oauthClient,
        LoggregatorClient loggregatorClient, CloudCredentials cloudCredentials, String orgName, String spaceName) {
        logger = LogFactory.getLog(getClass().getName());
        CloudControllerRestClientImpl tempClient = new CloudControllerRestClientImpl(cloudControllerUrl, restTemplate, oauthClient,
            loggregatorClient, cloudCredentials, null);

        initialize(cloudControllerUrl, restTemplate, oauthClient, loggregatorClient, cloudCredentials);

        this.sessionSpace = validateSpaceAndOrg(spaceName, orgName, tempClient);
    }

    /**
     * Only for unit tests. This works around the fact that the initialize method is called within the constructor and hence can not be
     * overloaded, making it impossible to write unit tests that don't trigger network calls.
     */
    protected CloudControllerRestClientImpl() {
        logger = LogFactory.getLog(getClass().getName());
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
        Map<String, String> uriInfo = new HashMap<String, String>();
        uriInfo.put("host", host);
        doAddRoute(uriInfo, domainGuid);
    }

    @Override
    public void associateAuditorWithSpace(String orgName, String spaceName, String userGuid) {
        String urlPath = "/v2/spaces/{guid}/auditors/{userGuid}";
        associateRoleWithSpace(orgName, spaceName, userGuid, urlPath);
    }

    @Override
    public void associateDeveloperWithSpace(String orgName, String spaceName, String userGuid) {
        String urlPath = "/v2/spaces/{guid}/developers/{userGuid}";
        associateRoleWithSpace(orgName, spaceName, userGuid, urlPath);
    }

    @Override
    public void associateManagerWithSpace(String orgName, String spaceName, String userGuid) {
        String urlPath = "/v2/spaces/{guid}/managers/{userGuid}";
        associateRoleWithSpace(orgName, spaceName, userGuid, urlPath);
    }

    @Override
    public void bindRunningSecurityGroup(String securityGroupName) {
        CloudSecurityGroup group = getSecurityGroup(securityGroupName);

        String path = "/v2/config/running_security_groups/{guid}";

        Map<String, Object> pathVariables = new HashMap<String, Object>();
        pathVariables.put("guid", group.getMeta()
            .getGuid());

        getRestTemplate().put(getUrl(path), null, pathVariables);
    }

    @Override
    public void bindSecurityGroup(String orgName, String spaceName, String securityGroupName) {
        UUID spaceGuid = getSpaceGuid(orgName, spaceName);
        CloudSecurityGroup group = getSecurityGroup(securityGroupName);

        String path = "/v2/security_groups/{group_guid}/spaces/{space_guid}";

        Map<String, Object> pathVariables = new HashMap<String, Object>();
        pathVariables.put("group_guid", group.getMeta()
            .getGuid());
        pathVariables.put("space_guid", spaceGuid);

        getRestTemplate().put(getUrl(path), null, pathVariables);
    }

    @Override
    public void bindService(String appName, String serviceName) {
        CloudService cloudService = getService(serviceName);
        UUID appId = getApplicationId(appName);
        doBindService(appId, cloudService.getMeta()
            .getGuid());
    }

    @Override
    public void bindStagingSecurityGroup(String securityGroupName) {
        CloudSecurityGroup group = getSecurityGroup(securityGroupName);

        String path = "/v2/config/staging_security_groups/{guid}";

        Map<String, Object> pathVariables = new HashMap<String, Object>();
        pathVariables.put("guid", group.getMeta()
            .getGuid());

        getRestTemplate().put(getUrl(path), null, pathVariables);
    }

    @Override
    public void createApplication(String appName, Staging staging, Integer memory, List<String> uris, List<String> serviceNames) {
        createApplication(appName, staging, null, memory, uris, serviceNames, null);
    }

    @Override
    public void createApplication(String appName, Staging staging, Integer disk, Integer memory, List<String> uris,
        List<String> serviceNames, DockerInfo dockerInfo) {
        HashMap<String, Object> appRequest = new HashMap<String, Object>();
        appRequest.put("space_guid", sessionSpace.getMeta()
            .getGuid());
        appRequest.put("name", appName);
        appRequest.put("memory", memory);
        if (disk != null) {
            appRequest.put("disk_quota", disk);
        }
        if (dockerInfo != null) {
            appRequest.put("docker_image", dockerInfo.getImage());
            if (dockerInfo.getDockerCredentials() != null) {
                appRequest.put("docker_credentials", dockerInfo.getDockerCredentials());
            }
        }
        appRequest.put("instances", 1);
        addStagingToRequest(staging, appRequest);
        appRequest.put("state", CloudApplication.AppState.STOPPED);

        String appResp = getRestTemplate().postForObject(getUrl("/v2/apps"), appRequest, String.class);
        Map<String, Object> appEntity = JsonUtil.convertJsonToMap(appResp);
        UUID newAppGuid = CloudEntityResourceMapper.getV2Meta(appEntity)
            .getGuid();

        if (serviceNames != null && serviceNames.size() > 0) {
            updateApplicationServices(appName, serviceNames);
        }

        if (uris != null && uris.size() > 0) {
            addUris(uris, newAppGuid);
        }
    }

    /**
     * Create quota from a CloudQuota instance (Quota Plan)
     *
     * @param quota
     */
    @Override
    public void createQuota(CloudQuota quota) {
        String setPath = "/v2/quota_definitions";
        HashMap<String, Object> setRequest = new HashMap<String, Object>();
        setRequest.put("name", quota.getName());
        setRequest.put("memory_limit", quota.getMemoryLimit());
        setRequest.put("total_routes", quota.getTotalRoutes());
        setRequest.put("total_services", quota.getTotalServices());
        setRequest.put("non_basic_services_allowed", quota.isNonBasicServicesAllowed());
        getRestTemplate().postForObject(getUrl(setPath), setRequest, String.class);
    }

    @Override
    public void createSecurityGroup(CloudSecurityGroup securityGroup) {
        doCreateSecurityGroup(securityGroup.getName(), convertToList(securityGroup.getRules()));
    }

    @Override
    public void createSecurityGroup(String name, InputStream jsonRulesFile) {
        doCreateSecurityGroup(name, JsonUtil.convertToJsonList(jsonRulesFile));
    }

    @Override
    public void createService(CloudService service) {
        assertSpaceProvided("create service");
        Assert.notNull(service, "Service must not be null");
        Assert.notNull(service.getName(), "Service name must not be null");
        Assert.notNull(service.getLabel(), "Service label must not be null");
        Assert.notNull(service.getPlan(), "Service plan must not be null");

        CloudServicePlan cloudServicePlan = findPlanForService(service);

        HashMap<String, Object> serviceRequest = new HashMap<String, Object>();
        serviceRequest.put("space_guid", sessionSpace.getMeta()
            .getGuid());
        serviceRequest.put("name", service.getName());
        serviceRequest.put("service_plan_guid", cloudServicePlan.getMeta()
            .getGuid());
        getRestTemplate().postForObject(getUrl("/v2/service_instances"), serviceRequest, String.class);
    }

    @Override
    public void createServiceBroker(CloudServiceBroker serviceBroker) {
        Assert.notNull(serviceBroker, "Service Broker must not be null");
        Assert.notNull(serviceBroker.getName(), "Service Broker name must not be null");
        Assert.notNull(serviceBroker.getUrl(), "Service Broker URL must not be null");
        Assert.notNull(serviceBroker.getUsername(), "Service Broker username must not be null");
        Assert.notNull(serviceBroker.getPassword(), "Service Broker password must not be null");

        HashMap<String, Object> serviceRequest = new HashMap<>();
        serviceRequest.put("name", serviceBroker.getName());
        serviceRequest.put("broker_url", serviceBroker.getUrl());
        serviceRequest.put("auth_username", serviceBroker.getUsername());
        serviceRequest.put("auth_password", serviceBroker.getPassword());
        getRestTemplate().postForObject(getUrl("/v2/service_brokers"), serviceRequest, String.class);
    }

    @Override
    public void createServiceKey(String serviceName, String serviceKeyName, Map<String, Object> parameters) {
        Assert.notNull(serviceName, "Service name must not be null");
        Assert.notNull(serviceKeyName, "Service Key name must not be null");
        Assert.notNull(parameters, "Parameters must not be null");
        CloudService service = getService(serviceName);

        HashMap<String, Object> serviceKeyRequest = new HashMap<>();
        serviceKeyRequest.put("service_instance_guid", service.getMeta()
            .getGuid()
            .toString());
        serviceKeyRequest.put("name", serviceKeyName);
        serviceKeyRequest.put("parameters", parameters);
        getRestTemplate().postForObject(getUrl("/v2/service_keys"), serviceKeyRequest, String.class);
    }

    @Override
    public void createSpace(String spaceName) {
        assertSpaceProvided("create a new space");
        UUID orgGuid = sessionSpace.getOrganization()
            .getMeta()
            .getGuid();
        UUID spaceGuid = getSpaceGuid(spaceName, orgGuid);
        if (spaceGuid == null) {
            doCreateSpace(spaceName, orgGuid);
        }
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials) {
        createUserProvidedServiceDelegate(service, credentials, "");
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials, String syslogDrainUrl) {
        createUserProvidedServiceDelegate(service, credentials, syslogDrainUrl);
    }

    @Override
    public void debugApplication(String appName, CloudApplication.DebugMode mode) {
        throw new UnsupportedOperationException("Feature is not yet implemented.");
    }

    @Override
    public void deleteAllApplications() {
        List<CloudApplication> cloudApps = getApplications();
        for (CloudApplication cloudApp : cloudApps) {
            deleteApplication(cloudApp.getName());
        }
    }

    @Override
    public void deleteAllServices() {
        List<CloudService> cloudServices = getServices();
        for (CloudService cloudService : cloudServices) {
            doDeleteService(cloudService);
        }
    }

    @Override
    public void deleteApplication(String appName) {
        UUID appId = getApplicationId(appName);
        doDeleteApplication(appId);
    }

    @Override
    public void deleteDomain(String domainName) {
        assertSpaceProvided("delete domain");
        UUID domainGuid = getDomainGuid(domainName, true);
        List<CloudRoute> routes = getRoutes(domainName);
        if (routes.size() > 0) {
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
        for (CloudDomain cloudDomain : getDomainsForOrg()) {
            orphanRoutes.addAll(fetchOrphanRoutes(cloudDomain.getName()));
        }

        List<CloudRoute> deletedCloudRoutes = new ArrayList<>();
        for (CloudRoute orphanRoute : orphanRoutes) {
            deleteRoute(orphanRoute.getHost(), orphanRoute.getDomain()
                .getName());
            deletedCloudRoutes.add(orphanRoute);
        }

        return deletedCloudRoutes;
    }

    @Override
    public void deleteQuota(String quotaName) {
        CloudQuota quota = this.getQuota(quotaName);
        String setPath = "/v2/quota_definitions/{quotaGuid}";
        Map<String, Object> setVars = new HashMap<String, Object>();
        setVars.put("quotaGuid", quota.getMeta()
            .getGuid());
        getRestTemplate().delete(getUrl(setPath), setVars);
    }

    @Override
    public void deleteRoute(String host, String domainName) {
        assertSpaceProvided("delete route for domain");
        UUID domainGuid = getDomainGuid(domainName, true);
        Map<String, String> uriInfo = new HashMap<String, String>();
        uriInfo.put("host", host);
        UUID routeGuid = getRouteGuid(uriInfo, domainGuid);
        if (routeGuid == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found",
                "Host '" + host + "' not found for domain '" + domainName + "'.");
        }
        doDeleteRoute(routeGuid);
    }

    @Override
    public void deleteSecurityGroup(String securityGroupName) {
        CloudSecurityGroup group = getSecurityGroup(securityGroupName);

        String path = "/v2/security_groups/{guid}";
        Map<String, Object> pathVariables = new HashMap<String, Object>();
        pathVariables.put("guid", group.getMeta()
            .getGuid());

        getRestTemplate().delete(getUrl(path), pathVariables);
    }

    @Override
    public void deleteService(String serviceName) {
        CloudService cloudService = getService(serviceName);
        doDeleteService(cloudService);
    }

    @Override
    public void deleteServiceBroker(String name) {
        CloudServiceBroker existingBroker = getServiceBroker(name);
        getRestTemplate().delete(getUrl("/v2/service_brokers/{guid}"), existingBroker.getMeta()
            .getGuid());
    }

    @Override
    public void deleteServiceKey(String serviceName, final String serviceKeyName) {
        List<ServiceKey> serviceKeys = getServiceKeys(serviceName);

        for (ServiceKey serviceKey : serviceKeys) {
            if (serviceKey.getName()
                .equals(serviceKeyName)) {
                getRestTemplate().delete(getUrl("/v2/service_keys/{guid}"), serviceKey.getMeta()
                    .getGuid());
                return;
            }
        }

        throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service key '" + serviceKeyName + "' not found.");
    }

    @Override
    public void deleteSpace(String spaceName) {
        assertSpaceProvided("delete a space");
        UUID orgGuid = sessionSpace.getOrganization()
            .getMeta()
            .getGuid();
        UUID spaceGuid = getSpaceGuid(spaceName, orgGuid);
        if (spaceGuid != null) {
            doDeleteSpace(spaceGuid);
        }
    }

    @Override
    public CloudApplication getApplication(String appName) {
        return getApplication(appName, true);
    }

    @Override
    public CloudApplication getApplication(String appName, boolean required) {
        Map<String, Object> resource = findApplicationResource(appName, true);
        CloudApplication application = null;
        if (resource != null) {
            application = mapCloudApplication(resource);
        }
        if (application == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application '" + appName + "' not found.");
        }
        return application;
    }

    @Override
    public CloudApplication getApplication(UUID appGuid) {
        return getApplication(appGuid, true);
    }

    @Override
    public CloudApplication getApplication(UUID appGuid, boolean required) {
        Map<String, Object> resource = findApplicationResource(appGuid, true);
        CloudApplication application = null;
        if (resource != null) {
            application = mapCloudApplication(resource);
        }
        if (application == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application '" + appGuid + "' not found.");
        }
        return application;
    }

    @Override
    public Map<String, Object> getApplicationEnvironment(UUID appGuid) {
        String url = getUrl("/v2/apps/{guid}/env");
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put("guid", appGuid);
        String resp = restTemplate.getForObject(url, String.class, urlVars);
        return JsonUtil.convertJsonToMap(resp);
    }

    @Override
    public Map<String, Object> getApplicationEnvironment(String appName) {
        UUID appId = getApplicationId(appName);
        return getApplicationEnvironment(appId);
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String appName) {
        UUID appId = getApplicationId(appName);
        Map<String, Object> urlVars = new HashMap<String, Object>();
        urlVars.put("appId", appId);
        String urlPath = "/v2/events?q=actee:{appId}";
        return doGetEvents(urlPath, urlVars);
    }

    @Override
    public InstancesInfo getApplicationInstances(String appName) {
        CloudApplication app = getApplication(appName);
        return getApplicationInstances(app);
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication app) {
        if (app.getState()
            .equals(CloudApplication.AppState.STARTED)) {
            return doGetApplicationInstances(app.getMeta()
                .getGuid());
        }
        return null;
    }

    @Override
    public ApplicationStats getApplicationStats(String appName) {
        CloudApplication app = getApplication(appName);
        return doGetApplicationStats(app.getMeta()
            .getGuid(), app.getState());
    }

    @Override
    public List<CloudApplication> getApplications() {
        return getApplicationsWithCustomDepth("1");
    }

    @Override
    public List<CloudApplication> getApplications(String inlineDepth) {
        return getApplicationsWithCustomDepth(inlineDepth);
    }

    private List<CloudApplication> getApplicationsWithCustomDepth(String inlineDepth) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2";
        if (sessionSpace != null) {
            urlVars.put("space", sessionSpace.getMeta()
                .getGuid());
            urlPath = urlPath + "/spaces/{space}";
        }
        urlPath = urlPath + "/apps?inline-relations-depth=" + inlineDepth;
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        List<CloudApplication> apps = new ArrayList<CloudApplication>();
        for (Map<String, Object> resource : resourceList) {
            processApplicationResource(resource, true);
            apps.add(mapCloudApplication(resource));
        }
        return apps;
    }

    @Override
    public URL getCloudControllerUrl() {
        return this.cloudControllerUrl;
    }

    @Override
    public Map<String, String> getCrashLogs(String appName) {
        String urlPath = getFileUrlPath();
        CrashesInfo crashes = getCrashes(appName);
        if (crashes.getCrashes()
            .isEmpty()) {
            return Collections.emptyMap();
        }
        TreeMap<Date, String> crashInstances = new TreeMap<Date, String>();
        for (CrashInfo crash : crashes.getCrashes()) {
            crashInstances.put(crash.getSince(), crash.getInstance());
        }
        String instance = crashInstances.get(crashInstances.lastKey());
        return doGetLogs(urlPath, appName, instance);
    }

    @SuppressWarnings("unchecked")
    @Override
    public CrashesInfo getCrashes(String appName) {
        UUID appId = getApplicationId(appName);
        if (appId == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application '" + appName + "' not found.");
        }
        Map<String, Object> urlVars = new HashMap<String, Object>();
        urlVars.put("guid", appId);
        String resp = getRestTemplate().getForObject(getUrl("/v2/apps/{guid}/crashes"), String.class, urlVars);
        Map<String, Object> respMap = JsonUtil.convertJsonToMap("{ \"crashes\" : " + resp + " }");
        List<Map<String, Object>> attributes = (List<Map<String, Object>>) respMap.get("crashes");
        return new CrashesInfo(attributes);
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
    public List<CloudDomain> getDomainsForOrg() {
        assertSpaceProvided("access organization domains");
        return doGetDomains(sessionSpace.getOrganization());
    }

    @Override
    public List<CloudEvent> getEvents() {
        String urlPath = "/v2/events";
        return doGetEvents(urlPath, null);
    }

    @Override
    public String getFile(String appName, int instanceIndex, String filePath, int startPosition, int endPosition) {
        String urlPath = getFileUrlPath();
        Object appId = getFileAppId(appName);
        return doGetFile(urlPath, appId, instanceIndex, filePath, startPosition, endPosition);
    }

    @Override
    public CloudInfo getInfo() {
        String infoV2Json = getRestTemplate().getForObject(getUrl("/v2/info"), String.class);
        Map<String, Object> infoV2Map = JsonUtil.convertJsonToMap(infoV2Json);

        Map<String, Object> userMap = getUserInfo((String) infoV2Map.get("user"));

        String name = CloudUtil.parse(String.class, infoV2Map.get("name"));
        String support = CloudUtil.parse(String.class, infoV2Map.get("support"));
        String authorizationEndpoint = CloudUtil.parse(String.class, infoV2Map.get("authorization_endpoint"));
        String build = CloudUtil.parse(String.class, infoV2Map.get("build"));
        String version = "" + CloudUtil.parse(Number.class, infoV2Map.get("version"));
        String description = CloudUtil.parse(String.class, infoV2Map.get("description"));

        CloudInfo.Limits limits = null;
        CloudInfo.Usage usage = null;
        boolean debug = false;

        String loggregatorEndpoint = CloudUtil.parse(String.class, infoV2Map.get("logging_endpoint"));

        return new CloudInfo(name, support, authorizationEndpoint, build, version, (String) userMap.get("user_name"), description, limits,
            usage, debug, loggregatorEndpoint);
    }

    @Override
    public Map<String, String> getLogs(String appName) {
        String urlPath = getFileUrlPath();
        String instance = String.valueOf(0);
        return doGetLogs(urlPath, appName, instance);
    }

    @Override
    public CloudOrganization getOrganization(String orgName) {
        return getOrganization(orgName, true);
    }

    /**
     * Get organization by given name.
     *
     * @param orgName
     * @param required
     * @return CloudOrganization instance
     */
    public CloudOrganization getOrganization(String orgName, boolean required) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2/organizations?inline-relations-depth=1&q=name:{name}";
        urlVars.put("name", orgName);
        CloudOrganization org = null;
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        if (resourceList.size() > 0) {
            Map<String, Object> resource = resourceList.get(0);
            org = resourceMapper.mapResource(resource, CloudOrganization.class);
        }

        if (org == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Organization '" + orgName + "' not found.");
        }

        return org;
    }

    @Override
    public Map<String, CloudUser> getOrganizationUsers(String orgName) {
        String urlPath = "/v2/organizations/{guid}/users";
        CloudOrganization organization = getOrganization(orgName);

        UUID orgGuid = organization.getMeta()
            .getGuid();
        Map<String, Object> urlVars = new HashMap<String, Object>();
        urlVars.put("guid", orgGuid);

        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        Map<String, CloudUser> orgUsers = new HashMap<String, CloudUser>();
        for (Map<String, Object> resource : resourceList) {
            CloudUser user = resourceMapper.mapResource(resource, CloudUser.class);
            orgUsers.put(user.getUsername(), user);
        }
        return orgUsers;
    }

    @Override
    public List<CloudOrganization> getOrganizations() {
        String urlPath = "/v2/organizations?inline-relations-depth=0";
        List<Map<String, Object>> resourceList = getAllResources(urlPath);
        List<CloudOrganization> orgs = new ArrayList<CloudOrganization>();
        for (Map<String, Object> resource : resourceList) {
            orgs.add(resourceMapper.mapResource(resource, CloudOrganization.class));
        }
        return orgs;
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return doGetDomains("/v2/private_domains");
    }

    @Override
    public CloudQuota getQuota(String quotaName) {
        return getQuota(quotaName, true);
    }

    @Override
    public CloudQuota getQuota(String quotaName, boolean required) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2/quota_definitions?q=name:{name}";
        urlVars.put("name", quotaName);
        CloudQuota quota = null;
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        if (resourceList.size() > 0) {
            Map<String, Object> resource = resourceList.get(0);
            quota = resourceMapper.mapResource(resource, CloudQuota.class);
        }

        if (quota == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Quota '" + quotaName + "' not found.");
        }

        return quota;
    }

    @Override
    public List<CloudQuota> getQuotas() {
        String urlPath = "/v2/quota_definitions";
        List<Map<String, Object>> resourceList = getAllResources(urlPath);
        List<CloudQuota> quotas = new ArrayList<CloudQuota>();
        for (Map<String, Object> resource : resourceList) {
            quotas.add(resourceMapper.mapResource(resource, CloudQuota.class));
        }
        return quotas;
    }

    @Override
    public List<ApplicationLog> getRecentLogs(String appName) {
        UUID appId = getApplicationId(appName);

        String endpoint = getInfo().getLoggregatorEndpoint();
        String uri = loggregatorClient.getRecentHttpEndpoint(endpoint);

        ApplicationLogs logs = getRestTemplate().getForObject(uri + "?app={guid}", ApplicationLogs.class, appId);

        Collections.sort(logs);

        return logs;
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        assertSpaceProvided("get routes for domain");
        UUID domainGuid = getDomainGuid(domainName, true);
        return doGetRoutes(domainGuid);
    }

    @Override
    public List<CloudSecurityGroup> getRunningSecurityGroups() {
        String urlPath = "/v2/config/running_security_groups";
        List<Map<String, Object>> resourceList = getAllResources(urlPath);
        List<CloudSecurityGroup> groups = new ArrayList<CloudSecurityGroup>();
        for (Map<String, Object> resource : resourceList) {
            groups.add(resourceMapper.mapResource(resource, CloudSecurityGroup.class));
        }
        return groups;
    }

    @Override
    public CloudSecurityGroup getSecurityGroup(String securityGroupName) {
        return getSecurityGroup(securityGroupName, true);
    }

    @Override
    public CloudSecurityGroup getSecurityGroup(String securityGroupName, boolean required) {
        Map<String, Object> resource = findSecurityGroupResource(securityGroupName);
        CloudSecurityGroup securityGroup = null;
        if (resource != null) {
            securityGroup = resourceMapper.mapResource(resource, CloudSecurityGroup.class);
        }
        if (securityGroup == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found",
                "Security group named '" + securityGroupName + "' not found.");
        }
        return securityGroup;
    }

    @Override
    public List<CloudSecurityGroup> getSecurityGroups() {
        String urlPath = "/v2/security_groups";
        List<Map<String, Object>> resourceList = getAllResources(urlPath);
        List<CloudSecurityGroup> groups = new ArrayList<CloudSecurityGroup>();
        for (Map<String, Object> resource : resourceList) {
            groups.add(resourceMapper.mapResource(resource, CloudSecurityGroup.class));
        }
        return groups;
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
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service '" + serviceName + "' not found.");
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
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service broker '" + name + "' not found.");
        }
        return serviceBroker;
    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        String urlPath = "/v2/service_brokers?inline-relations-depth=1";
        List<Map<String, Object>> resourceList = getAllResources(urlPath);
        List<CloudServiceBroker> serviceBrokers = new ArrayList<CloudServiceBroker>();
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
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Service instance '" + serviceName + "' not found.");
        }
        return serviceInstance;
    }

    @Override
    public List<ServiceKey> getServiceKeys(String serviceName) {
        CloudService cloudService = getService(serviceName, true);
        return doGetServiceKeys(cloudService);
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        String urlPath = "/v2/services?inline-relations-depth=1";
        List<Map<String, Object>> resourceList = getAllResources(urlPath);
        List<CloudServiceOffering> serviceOfferings = new ArrayList<CloudServiceOffering>();
        for (Map<String, Object> resource : resourceList) {
            CloudServiceOffering serviceOffering = resourceMapper.mapResource(resource, CloudServiceOffering.class);
            serviceOfferings.add(serviceOffering);
        }
        return serviceOfferings;
    }

    @Override
    public List<CloudService> getServices() {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2";
        if (sessionSpace != null) {
            urlVars.put("space", sessionSpace.getMeta()
                .getGuid());
            urlPath = urlPath + "/spaces/{space}";
        }
        urlPath = urlPath + "/service_instances?inline-relations-depth=1&return_user_provided_service_instances=true";
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        List<CloudService> services = new ArrayList<CloudService>();
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
    public CloudSpace getSpace(String spaceName) {
        return getSpace(spaceName, true);
    }

    @Override
    public CloudSpace getSpace(String spaceName, boolean required) {
        Map<String, Object> resource = findSpaceResource(spaceName);
        CloudSpace space = null;
        if (resource != null) {
            space = resourceMapper.mapResource(resource, CloudSpace.class);
        }
        if (space == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Space '" + spaceName + "' not found.");
        }
        return space;
    }

    @Override
    public List<UUID> getSpaceAuditors(String orgName, String spaceName) {
        String urlPath = "/v2/spaces/{guid}/auditors";
        return getSpaceUserGuids(orgName, spaceName, urlPath);
    }

    @Override
    public List<UUID> getSpaceAuditors(UUID spaceGuid) {
        String urlPath = "/v2/spaces/{guid}/auditors";
        return getSpaceUserGuids(spaceGuid, urlPath);
    }

    @Override
    public List<UUID> getSpaceDevelopers(String orgName, String spaceName) {
        String urlPath = "/v2/spaces/{guid}/developers";
        return getSpaceUserGuids(orgName, spaceName, urlPath);
    }

    @Override
    public List<UUID> getSpaceDevelopers(UUID spaceGuid) {
        String urlPath = "/v2/spaces/{guid}/developers";
        return getSpaceUserGuids(spaceGuid, urlPath);
    }

    @Override
    public List<UUID> getSpaceManagers(String orgName, String spaceName) {
        String urlPath = "/v2/spaces/{guid}/managers";
        return getSpaceUserGuids(orgName, spaceName, urlPath);
    }

    @Override
    public List<UUID> getSpaceManagers(UUID spaceGuid) {
        String urlPath = "/v2/spaces/{guid}/managers";
        return getSpaceUserGuids(spaceGuid, urlPath);
    }

    @Override
    public List<CloudSpace> getSpaces() {
        String urlPath = "/v2/spaces?inline-relations-depth=1";
        List<Map<String, Object>> resourceList = getAllResources(urlPath);
        List<CloudSpace> spaces = new ArrayList<CloudSpace>();
        for (Map<String, Object> resource : resourceList) {
            spaces.add(resourceMapper.mapResource(resource, CloudSpace.class));
        }
        return spaces;
    }

    @Override
    public List<CloudSpace> getSpacesBoundToSecurityGroup(String securityGroupName) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        // Need to go a few levels out to get the Organization that Spaces needs
        String urlPath = "/v2/security_groups?q=name:{name}&inline-relations-depth=2";
        urlVars.put("name", securityGroupName);
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        List<CloudSpace> spaces = new ArrayList<CloudSpace>();
        if (resourceList.size() > 0) {
            Map<String, Object> resource = resourceList.get(0);

            Map<String, Object> securityGroupResource = CloudEntityResourceMapper.getEntity(resource);
            List<Map<String, Object>> spaceResources = CloudEntityResourceMapper.getEmbeddedResourceList(securityGroupResource, "spaces");
            for (Map<String, Object> spaceResource : spaceResources) {
                spaces.add(resourceMapper.mapResource(spaceResource, CloudSpace.class));
            }
        } else {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Security group '" + securityGroupName + "' not found.");
        }
        return spaces;
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
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Stack '" + name + "' not found.");
        }
        return stack;
    }

    @Override
    public List<CloudStack> getStacks() {
        String urlPath = "/v2/stacks";
        List<Map<String, Object>> resources = getAllResources(urlPath);
        List<CloudStack> stacks = new ArrayList<CloudStack>();
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
                HashMap<String, Object> logsRequest = new HashMap<String, Object>();
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
        String urlPath = "/v2/config/staging_security_groups";
        List<Map<String, Object>> resourceList = getAllResources(urlPath);
        List<CloudSecurityGroup> groups = new ArrayList<CloudSecurityGroup>();
        for (Map<String, Object> resource : resourceList) {
            groups.add(resourceMapper.mapResource(resource, CloudSecurityGroup.class));
        }
        return groups;
    }

    @Override
    public OAuth2AccessToken login() {
        oauthClient.init(cloudCredentials);
        return oauthClient.getToken();
    }

    @Override
    public void logout() {
        oauthClient.clear();
    }

    @Override
    public void openFile(String appName, int instanceIndex, String filePath, ClientHttpResponseCallback callback) {
        String urlPath = getFileUrlPath();
        Object appId = getFileAppId(appName);
        doOpenFile(urlPath, appId, instanceIndex, filePath, callback);
    }

    @Override
    public void register(String email, String password) {
        throw new UnsupportedOperationException("Feature is not yet implemented.");
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
    public void rename(String appName, String newName) {
        UUID appId = getApplicationId(appName);
        HashMap<String, Object> appRequest = new HashMap<String, Object>();
        appRequest.put("name", newName);
        getRestTemplate().put(getUrl("/v2/apps/{guid}"), appRequest, appId);
    }

    @Override
    public StartingInfo restartApplication(String appName) {
        stopApplication(appName);
        return startApplication(appName);
    }

    /**
     * Set quota to organization
     *
     * @param orgName
     * @param quotaName
     */
    @Override
    public void setQuotaToOrg(String orgName, String quotaName) {
        CloudQuota quota = this.getQuota(quotaName);
        CloudOrganization org = this.getOrganization(orgName);

        doSetQuotaToOrg(org.getMeta()
            .getGuid(),
            quota.getMeta()
                .getGuid());
    }

    @Override
    public void setResponseErrorHandler(ResponseErrorHandler errorHandler) {
        this.restTemplate.setErrorHandler(errorHandler);
    }

    @Override
    public StartingInfo startApplication(String appName) {
        CloudApplication app = getApplication(appName);
        if (app.getState() != CloudApplication.AppState.STARTED) {
            HashMap<String, Object> appRequest = new HashMap<String, Object>();
            appRequest.put("state", CloudApplication.AppState.STARTED);

            HttpEntity<Object> requestEntity = new HttpEntity<Object>(appRequest);
            ResponseEntity<String> entity = getRestTemplate().exchange(getUrl("/v2/apps/{guid}?stage_async=true"), HttpMethod.PUT,
                requestEntity, String.class, app.getMeta()
                    .getGuid());

            HttpHeaders headers = entity.getHeaders();

            // Return a starting info, even with a null staging log value, as a non-null starting info
            // indicates that the response entity did have headers. The API contract is to return starting info
            // if there are headers in the response, null otherwise.
            if (headers != null && !headers.isEmpty()) {
                String stagingFile = headers.getFirst("x-app-staging-log");

                if (stagingFile != null) {
                    try {
                        stagingFile = URLDecoder.decode(stagingFile, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        logger.error("unexpected inability to UTF-8 decode", e);
                    }
                }
                // Return the starting info even if decoding failed or staging file is null
                return new StartingInfo(stagingFile);
            }
        }
        return null;
    }

    @Override
    public void stopApplication(String appName) {
        CloudApplication app = getApplication(appName);
        if (app.getState() != CloudApplication.AppState.STOPPED) {
            HashMap<String, Object> appRequest = new HashMap<String, Object>();
            appRequest.put("state", CloudApplication.AppState.STOPPED);
            getRestTemplate().put(getUrl("/v2/apps/{guid}"), appRequest, app.getMeta()
                .getGuid());
        }
    }

    @Override
    public StreamingLogToken streamLogs(String appName, ApplicationLogListener listener) {
        return streamLoggregatorLogs(appName, listener, false);
    }

    @Override
    public void unRegisterRestLogListener(RestLogCallback callBack) {
        if (getRestTemplate() instanceof LoggingRestTemplate) {
            ((LoggingRestTemplate) getRestTemplate()).unRegisterRestLogListener(callBack);
        }
    }

    @Override
    public void unbindRunningSecurityGroup(String securityGroupName) {
        CloudSecurityGroup group = getSecurityGroup(securityGroupName);

        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2/config/running_security_groups/{guid}";
        urlVars.put("guid", group.getMeta()
            .getGuid());
        getRestTemplate().delete(getUrl(urlPath), urlVars);
    }

    @Override
    public void unbindSecurityGroup(String orgName, String spaceName, String securityGroupName) {
        UUID spaceGuid = getSpaceGuid(orgName, spaceName);
        CloudSecurityGroup group = getSecurityGroup(securityGroupName);

        String path = "/v2/security_groups/{group_guid}/spaces/{space_guid}";

        Map<String, Object> pathVariables = new HashMap<String, Object>();
        pathVariables.put("group_guid", group.getMeta()
            .getGuid());
        pathVariables.put("space_guid", spaceGuid);

        getRestTemplate().delete(getUrl(path), pathVariables);
    }

    @Override
    public void unbindService(String appName, String serviceName) {
        CloudService cloudService = getService(serviceName);
        UUID appId = getApplicationId(appName);
        doUnbindService(appId, cloudService.getMeta()
            .getGuid());
    }

    @Override
    public void unbindStagingSecurityGroup(String securityGroupName) {
        CloudSecurityGroup group = getSecurityGroup(securityGroupName);

        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2/config/staging_security_groups/{guid}";
        urlVars.put("guid", group.getMeta()
            .getGuid());
        getRestTemplate().delete(getUrl(urlPath), urlVars);
    }

    @Override
    public void unregister() {
        throw new UnsupportedOperationException("Feature is not yet implemented.");
    }

    @Override
    public void updateApplicationDiskQuota(String appName, int disk) {
        UUID appId = getApplicationId(appName);
        HashMap<String, Object> appRequest = new HashMap<String, Object>();
        appRequest.put("disk_quota", disk);
        getRestTemplate().put(getUrl("/v2/apps/{guid}"), appRequest, appId);
    }

    @Override
    public void updateApplicationEnv(String appName, Map<String, String> env) {
        UUID appId = getApplicationId(appName);
        HashMap<String, Object> appRequest = new HashMap<String, Object>();
        appRequest.put("environment_json", env);
        getRestTemplate().put(getUrl("/v2/apps/{guid}"), appRequest, appId);
    }

    @Override
    public void updateApplicationEnv(String appName, List<String> env) {
        Map<String, String> envHash = new HashMap<String, String>();
        for (String s : env) {
            if (!s.contains("=")) {
                throw new IllegalArgumentException("Environment setting without '=' is invalid: " + s);
            }
            String key = s.substring(0, s.indexOf('='))
                .trim();
            String value = s.substring(s.indexOf('=') + 1)
                .trim();
            envHash.put(key, value);
        }
        updateApplicationEnv(appName, envHash);
    }

    @Override
    public void updateApplicationInstances(String appName, int instances) {
        UUID appId = getApplicationId(appName);
        HashMap<String, Object> appRequest = new HashMap<String, Object>();
        appRequest.put("instances", instances);
        getRestTemplate().put(getUrl("/v2/apps/{guid}"), appRequest, appId);
    }

    @Override
    public void updateApplicationMemory(String appName, int memory) {
        UUID appId = getApplicationId(appName);
        HashMap<String, Object> appRequest = new HashMap<String, Object>();
        appRequest.put("memory", memory);
        getRestTemplate().put(getUrl("/v2/apps/{guid}"), appRequest, appId);
    }

    @Override
    public void updateApplicationServices(String appName, List<String> services) {
        CloudApplication app = getApplication(appName);
        List<UUID> addServices = new ArrayList<UUID>();
        List<UUID> deleteServices = new ArrayList<UUID>();
        // services to add
        for (String serviceName : services) {
            if (!app.getServices()
                .contains(serviceName)) {
                CloudService cloudService = getService(serviceName);
                addServices.add(cloudService.getMeta()
                    .getGuid());
            }
        }
        // services to delete
        for (String serviceName : app.getServices()) {
            if (!services.contains(serviceName)) {
                CloudService cloudService = getService(serviceName, false);
                if (cloudService != null) {
                    deleteServices.add(cloudService.getMeta()
                        .getGuid());
                }
            }
        }
        for (UUID serviceId : addServices) {
            doBindService(app.getMeta()
                .getGuid(), serviceId);
        }
        for (UUID serviceId : deleteServices) {
            doUnbindService(app.getMeta()
                .getGuid(), serviceId);
        }
    }

    @Override
    public void updateApplicationStaging(String appName, Staging staging) {
        UUID appId = getApplicationId(appName);
        HashMap<String, Object> appRequest = new HashMap<String, Object>();
        addStagingToRequest(staging, appRequest);
        getRestTemplate().put(getUrl("/v2/apps/{guid}"), appRequest, appId);
    }

    @Override
    public void updateApplicationUris(String appName, List<String> uris) {
        CloudApplication app = getApplication(appName);
        List<String> newUris = new ArrayList<String>(uris);
        newUris.removeAll(app.getUris());
        List<String> removeUris = app.getUris();
        removeUris.removeAll(uris);
        removeUris(removeUris, app.getMeta()
            .getGuid());
        addUris(newUris, app.getMeta()
            .getGuid());
    }

    @Override
    public void updatePassword(String newPassword) {
        updatePassword(cloudCredentials, newPassword);
    }

    @Override
    public void updatePassword(CloudCredentials credentials, String newPassword) {
        oauthClient.changePassword(credentials.getPassword(), newPassword);
        CloudCredentials newCloudCredentials = new CloudCredentials(credentials.getEmail(), newPassword);
        if (cloudCredentials.getProxyUser() != null) {
            cloudCredentials = newCloudCredentials.proxyForUser(cloudCredentials.getProxyUser());
        } else {
            cloudCredentials = newCloudCredentials;
        }
    }

    @Override
    public void updateQuota(CloudQuota quota, String name) {
        CloudQuota oldQuota = this.getQuota(name);

        String setPath = "/v2/quota_definitions/{quotaGuid}";

        Map<String, Object> setVars = new HashMap<String, Object>();
        setVars.put("quotaGuid", oldQuota.getMeta()
            .getGuid());

        HashMap<String, Object> setRequest = new HashMap<String, Object>();
        setRequest.put("name", quota.getName());
        setRequest.put("memory_limit", quota.getMemoryLimit());
        setRequest.put("total_routes", quota.getTotalRoutes());
        setRequest.put("total_services", quota.getTotalServices());
        setRequest.put("non_basic_services_allowed", quota.isNonBasicServicesAllowed());

        getRestTemplate().put(getUrl(setPath), setRequest, setVars);
    }

    @Override
    public void updateSecurityGroup(CloudSecurityGroup securityGroup) {
        CloudSecurityGroup oldGroup = getSecurityGroup(securityGroup.getName());
        doUpdateSecurityGroup(oldGroup, securityGroup.getName(), convertToList(securityGroup.getRules()));
    }

    @Override
    public void updateSecurityGroup(String name, InputStream jsonRulesFile) {
        CloudSecurityGroup oldGroup = getSecurityGroup(name);
        doUpdateSecurityGroup(oldGroup, name, JsonUtil.convertToJsonList(jsonRulesFile));
    }

    @Override
    public void updateServiceBroker(CloudServiceBroker serviceBroker) {
        Assert.notNull(serviceBroker, "Service Broker must not be null");
        Assert.notNull(serviceBroker.getName(), "Service Broker name must not be null");
        Assert.notNull(serviceBroker.getUrl(), "Service Broker URL must not be null");
        Assert.notNull(serviceBroker.getUsername(), "Service Broker username must not be null");
        Assert.notNull(serviceBroker.getPassword(), "Service Broker password must not be null");

        CloudServiceBroker existingBroker = getServiceBroker(serviceBroker.getName());

        HashMap<String, Object> serviceRequest = new HashMap<>();
        serviceRequest.put("name", serviceBroker.getName());
        serviceRequest.put("broker_url", serviceBroker.getUrl());
        serviceRequest.put("auth_username", serviceBroker.getUsername());
        serviceRequest.put("auth_password", serviceBroker.getPassword());
        getRestTemplate().put(getUrl("/v2/service_brokers/{guid}"), serviceRequest, existingBroker.getMeta()
            .getGuid());
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, boolean visibility) {
        CloudServiceBroker broker = getServiceBroker(name);

        String urlPath = "/v2/services?q={q}";
        Map<String, Object> urlVars = new HashMap<>();
        urlVars.put("q", "service_broker_guid:" + broker.getMeta()
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

                HashMap<String, Object> planUpdateRequest = new HashMap<>();
                planUpdateRequest.put("public", visibility);
                getRestTemplate().put(getUrl("/v2/service_plans/{guid}"), planUpdateRequest, planGuid);
            }
        }
    }

    @Override
    public boolean areTasksSupported() {
        return true;
    }

    @Override
    public List<CloudTask> getTasks(String applicationName) {
        UUID applicationId = getRequiredApplicationId(applicationName);
        String urlPath = "/v3/apps/{applicationGuid}/tasks";
        Map<String, Object> urlVariables = new HashMap<>();
        urlVariables.put("applicationGuid", applicationId);

        return doGetTasks(urlPath, urlVariables);
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task) {
        UUID applicationId = getRequiredApplicationId(applicationName);
        String urlPath = "/v3/apps/{applicationGuid}/tasks";
        Map<String, Object> urlVariables = new HashMap<>();
        urlVariables.put("applicationGuid", applicationId);
        Map<String, Object> request = new HashMap<>();
        request.put("name", task.getName());
        request.put("command", task.getCommand());
        request.put("memory_in_mb", task.getMemory());
        request.put("disk_in_mb", task.getDiskQuota());

        @SuppressWarnings("unchecked")
        Map<String, Object> resource = getRestTemplate().postForObject(getUrl(urlPath), request, Map.class, urlVariables);
        return resourceMapper.mapResource(resource, CloudTask.class);
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
    public void uploadApplication(String appName, File file, UploadStatusCallback callback) throws IOException {
        UploadToken uploadToken = startUpload(appName, file, callback);
        waitUntilPackageUploadFinish(uploadToken);
    }

    @Override
    public void uploadApplication(String appName, InputStream inputStream, UploadStatusCallback callback) throws IOException {
        Assert.notNull(inputStream, "InputStream must not be null");

        File file = null;
        ZipFile zipFile = null;
        try {
            file = createTemporaryUploadFile(inputStream);
            zipFile = new ZipFile(file);
            ApplicationArchive archive = new ZipApplicationArchive(zipFile);
            uploadApplication(appName, archive, callback);
        } finally {
            IOUtils.closeQuietly(zipFile);
            if (file != null) {
                file.delete();
            }
        }
    }

    @Override
    public void uploadApplication(String appName, ApplicationArchive archive, UploadStatusCallback callback) throws IOException {
        UploadToken uploadToken = startUpload(appName, archive, callback);
        waitUntilPackageUploadFinish(uploadToken);
    }

    @Override
    public UploadToken asyncUploadApplication(String appName, File file, UploadStatusCallback callback) throws IOException {
        return startUpload(appName, file, callback);
    }

    @Override
    public UploadToken asyncUploadApplication(String appName, ApplicationArchive archive, UploadStatusCallback callback)
        throws IOException {
        return startUpload(appName, archive, callback);
    }

    private UploadToken startUpload(String appName, File file, UploadStatusCallback callback) throws IOException {
        Assert.notNull(file, "File must not be null");
        if (file.isDirectory()) {
            ApplicationArchive archive = new DirectoryApplicationArchive(file);
            return startUpload(appName, archive, callback);
        }
        try (ZipFile zipFile = new ZipFile(file)) {
            ApplicationArchive archive = new ZipApplicationArchive(zipFile);
            return startUpload(appName, archive, callback);
        }
    }

    private UploadToken startUpload(String appName, ApplicationArchive archive, UploadStatusCallback callback) throws IOException {
        Assert.notNull(appName, "AppName must not be null");
        Assert.notNull(archive, "Archive must not be null");
        UUID appGuid = getApplicationId(appName);

        if (callback == null) {
            callback = UploadStatusCallback.NONE;
        }
        CloudResources knownRemoteResources = getKnownRemoteResources(archive);
        callback.onCheckResources();
        callback.onMatchedFileNames(knownRemoteResources.getFilenames());
        UploadApplicationPayload payload = new UploadApplicationPayload(archive, knownRemoteResources);
        callback.onProcessMatchedResources(payload.getTotalUncompressedSize());
        HttpEntity<?> entity = generatePartialResourceRequest(payload, knownRemoteResources);

        UUID packageGuid = createPackageForApplication(appGuid);

        ResponseEntity<Map<String, Object>> responseEntity = getRestTemplate().exchange(getUrl("/v3/packages/{packageGuid}/upload"),
            HttpMethod.POST, entity, new ParameterizedTypeReference<Map<String, Object>>() {
            }, packageGuid);

        CloudPackage cloudPackage = resourceMapper.mapResource(responseEntity.getBody(), CloudPackage.class);

        return new UploadToken(getUrl("/v3/packages/" + cloudPackage.getMeta()
            .getGuid()), cloudPackage.getMeta()
                .getGuid());
    }

    private UUID createPackageForApplication(UUID appGuid) {
        Map<String, Object> packageRequest = new HashMap<>();
        packageRequest.put("type", "bits");
        Map<String, Map<String, Map<String, Object>>> relationships = new HashMap<>();
        Map<String, Map<String, Object>> app = new HashMap<>();
        Map<String, Object> data = new HashMap<>();
        data.put("guid", appGuid);
        app.put("data", data);
        relationships.put("app", app);
        packageRequest.put("relationships", relationships);

        String packageResponse = getRestTemplate().postForObject(getUrl("/v3/packages"), packageRequest, String.class);
        Map<String, Object> packageEntity = JsonUtil.convertJsonToMap(packageResponse);

        return resourceMapper.mapResource(packageEntity, CloudPackage.class)
            .getMeta()
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
    public void bindDropletToApp(UUID dropletGuid, UUID appGuid) {
        Map<String, Object> bindDropletRequest = new HashMap<>();
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("guid", dropletGuid);
        bindDropletRequest.put("data", dataMap);

        getRestTemplate().patchForObject(getUrl("/v3/apps/{appGuid}/relationships/current_droplet"), bindDropletRequest, String.class,
            appGuid);
    }

    @Override
    public CloudBuild getBuild(UUID buildGuid) {
        ResponseEntity<Map<String, Object>> responseEntity = getRestTemplate().exchange(getUrl("/v3/builds/{buildGuid}"), HttpMethod.GET,
            HttpEntity.EMPTY, new ParameterizedTypeReference<Map<String, Object>>() {
            }, buildGuid);

        return resourceMapper.mapResource(responseEntity.getBody(), CloudBuild.class);
    }

    @Override
    public Upload getUploadStatus(String uploadToken) {
        CloudPackage cloudPackage = getCloudPackage(uploadToken);
        ErrorDetails errorDetails = new ErrorDetails(0, cloudPackage.getData()
            .getError(), null);

        return new Upload(cloudPackage.getStatus(), errorDetails);
    }

    protected void configureRequestFactory(RestTemplate restTemplate) {
        ClientHttpRequestFactory requestFactory = restTemplate.getRequestFactory();
        if (!(requestFactory instanceof CloudControllerRestClientHttpRequestFactory)) {
            restTemplate.setRequestFactory(new CloudControllerRestClientHttpRequestFactory(requestFactory));
        }
    }

    protected String doGetFile(String urlPath, Object app, int instanceIndex, String filePath, int startPosition, int endPosition) {
        return doGetFile(urlPath, app, String.valueOf(instanceIndex), filePath, startPosition, endPosition);
    }

    protected String doGetFile(String urlPath, Object app, String instance, String filePath, int startPosition, int endPosition) {
        Assert.isTrue(startPosition >= -1, "Invalid start position value: " + startPosition);
        Assert.isTrue(endPosition >= -1, "Invalid end position value: " + endPosition);
        Assert.isTrue(startPosition < 0 || endPosition < 0 || endPosition >= startPosition,
            "The end position (" + endPosition + ") can't be less than the start position (" + startPosition + ")");

        int start, end;
        if (startPosition == -1 && endPosition == -1) {
            start = 0;
            end = -1;
        } else {
            start = startPosition;
            end = endPosition;
        }

        final String range = "bytes=" + (start == -1 ? "" : start) + "-" + (end == -1 ? "" : end);

        return doGetFileByRange(urlPath, app, instance, filePath, start, end, range);
    }

    protected Map<String, String> doGetLogs(String urlPath, String appName, String instance) {
        Object appId = getFileAppId(appName);
        String logFiles = doGetFile(urlPath, appId, instance, LOGS_LOCATION, -1, -1);
        String[] lines = logFiles.split("\n");
        List<String> fileNames = new ArrayList<String>();
        for (String line : lines) {
            String[] parts = line.split("\\s");
            if (parts.length > 0 && parts[0] != null) {
                fileNames.add(parts[0]);
            }
        }
        Map<String, String> logs = new HashMap<String, String>(fileNames.size());
        for (String fileName : fileNames) {
            String logFile = LOGS_LOCATION + DEFAULT_PATH_SEPARATOR + fileName;
            logs.put(logFile, doGetFile(urlPath, appId, instance, logFile, -1, -1));
        }
        return logs;
    }

    @SuppressWarnings("unchecked")
    protected void doOpenFile(String urlPath, Object app, int instanceIndex, String filePath, ClientHttpResponseCallback callback) {
        getRestTemplate().execute(getUrl(urlPath), HttpMethod.GET, null, new ResponseExtractorWrapper(callback), app,
            String.valueOf(instanceIndex), filePath);
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
            if (host != null && domain.equals(existingDomain)) {
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

    protected Object getFileAppId(String appName) {
        return getApplicationId(appName);
    }

    protected String getFileUrlPath() {
        return "/v2/apps/{appId}/instances/{instance}/files/{filePath}";
    }

    protected RestTemplate getRestTemplate() {
        return this.restTemplate;
    }

    protected String getUrl(CloudJob job) {
        Meta meta = job.getMeta();
        return getUrl(meta.getUrl());
    }

    protected String getUrl(String path) {
        return cloudControllerUrl + (path.startsWith(DEFAULT_PATH_SEPARATOR) ? path : DEFAULT_PATH_SEPARATOR + path);
    }

    @SuppressWarnings("unchecked")
    private String addPageOfResources(String nextUrl, List<Map<String, Object>> allResources) {
        String resp = getRestTemplate().getForObject(getUrl(nextUrl), String.class);
        Map<String, Object> respMap = JsonUtil.convertJsonToMap(resp);
        List<Map<String, Object>> newResources = (List<Map<String, Object>>) respMap.get("resources");
        if (newResources != null && newResources.size() > 0) {
            allResources.addAll(newResources);
        }
        return (String) respMap.get("next_url");
    }

    private void addStagingToRequest(Staging staging, HashMap<String, Object> appRequest) {
        if (staging.getBuildpackUrl() != null) {
            appRequest.put("buildpack", staging.getBuildpackUrl());
        }
        if (staging.getCommand() != null) {
            appRequest.put("command", staging.getCommand());
        }
        if (staging.getStack() != null) {
            appRequest.put("stack_guid", getStack(staging.getStack()).getMeta()
                .getGuid());
        }
        if (staging.getHealthCheckTimeout() != null) {
            appRequest.put("health_check_timeout", staging.getHealthCheckTimeout());
        }
    }

    private void addUris(List<String> uris, UUID appGuid) {
        Map<String, UUID> domains = getDomainGuids();
        for (String uri : uris) {
            Map<String, String> uriInfo = new HashMap<String, String>(2);
            extractUriInfo(domains, uri, uriInfo);
            UUID domainGuid = domains.get(uriInfo.get("domainName"));
            bindRoute(uriInfo, domainGuid, appGuid);
        }
    }

    private void assertSpaceProvided(String operation) {
        Assert.notNull(sessionSpace, "Unable to " + operation + " without specifying organization and space to use.");
    }

    private void associateRoleWithSpace(String orgName, String spaceName, String userGuid, String urlPath) {
        assertSpaceProvided("associate roles");

        CloudOrganization organization = (orgName == null ? sessionSpace.getOrganization() : getOrganization(orgName));
        UUID orgGuid = organization.getMeta()
            .getGuid();

        UUID spaceGuid = getSpaceGuid(spaceName, orgGuid);
        HashMap<String, Object> spaceRequest = new HashMap<String, Object>();
        spaceRequest.put("guid", spaceGuid);

        String userId = (userGuid == null ? getCurrentUserId() : userGuid);

        getRestTemplate().put(getUrl(urlPath), spaceRequest, spaceGuid, userId);
    }

    private void bindRoute(Map<String, String> uriInfo, UUID domainGuid, UUID appGuid) {
        UUID routeGuid = getRouteGuid(uriInfo, domainGuid);
        if (routeGuid == null) {
            routeGuid = doAddRoute(uriInfo, domainGuid);
        }
        String bindPath = "/v2/apps/{app}/routes/{route}";
        Map<String, Object> bindVars = new HashMap<String, Object>();
        bindVars.put("app", appGuid);
        bindVars.put("route", routeGuid);
        HashMap<String, Object> bindRequest = new HashMap<String, Object>();
        getRestTemplate().put(getUrl(bindPath), bindRequest, bindVars);
    }

    private List<Map<String, Object>> convertToList(List<SecurityGroupRule> rules) {
        List<Map<String, Object>> ruleList = new ArrayList<Map<String, Object>>();
        for (SecurityGroupRule rule : rules) {
            Map<String, Object> ruleMap = new HashMap<String, Object>();
            ruleMap.put("protocol", rule.getProtocol());
            ruleMap.put("destination", rule.getDestination());
            if (rule.getPorts() != null) {
                ruleMap.put("ports", rule.getPorts());
            }
            if (rule.getLog() != null) {
                ruleMap.put("log", rule.getLog());
            }
            if (rule.getType() != null) {
                ruleMap.put("type", rule.getType());
            }
            if (rule.getCode() != null) {
                ruleMap.put("code", rule.getCode());
            }
            ruleList.add(ruleMap);
        }
        return ruleList;
    }

    private File createTemporaryUploadFile(InputStream inputStream) throws IOException {
        File file = File.createTempFile("cfjava", null);
        FileOutputStream outputStream = new FileOutputStream(file);
        FileCopyUtils.copy(inputStream, outputStream);
        outputStream.close();
        return file;
    }

    private void createUserProvidedServiceDelegate(CloudService service, Map<String, Object> credentials, String syslogDrainUrl) {
        assertSpaceProvided("create service");
        Assert.notNull(credentials, "Service credentials must not be null");
        Assert.notNull(service, "Service must not be null");
        Assert.notNull(service.getName(), "Service name must not be null");
        Assert.isNull(service.getLabel(), "Service label is not valid for user-provided services");
        Assert.isNull(service.getProvider(), "Service provider is not valid for user-provided services");
        Assert.isNull(service.getVersion(), "Service version is not valid for user-provided services");
        Assert.isNull(service.getPlan(), "Service plan is not valid for user-provided services");

        HashMap<String, Object> serviceRequest = new HashMap<>();
        serviceRequest.put("space_guid", sessionSpace.getMeta()
            .getGuid());
        serviceRequest.put("name", service.getName());
        serviceRequest.put("credentials", credentials);
        if (syslogDrainUrl != null && !syslogDrainUrl.equals("")) {
            serviceRequest.put("syslog_drain_url", syslogDrainUrl);
        }

        getRestTemplate().postForObject(getUrl("/v2/user_provided_service_instances"), serviceRequest, String.class);
    }

    private UUID doAddRoute(Map<String, String> uriInfo, UUID domainGuid) {
        assertSpaceProvided("add route");

        HashMap<String, Object> routeRequest = new HashMap<String, Object>();
        routeRequest.put("host", uriInfo.get("host"));
        routeRequest.put("path", uriInfo.get("path"));
        routeRequest.put("domain_guid", domainGuid);
        routeRequest.put("space_guid", sessionSpace.getMeta()
            .getGuid());
        String routeResp = getRestTemplate().postForObject(getUrl("/v2/routes"), routeRequest, String.class);
        Map<String, Object> routeEntity = JsonUtil.convertJsonToMap(routeResp);
        return CloudEntityResourceMapper.getV2Meta(routeEntity)
            .getGuid();
    }

    private void doBindService(UUID appId, UUID serviceId) {
        HashMap<String, Object> serviceRequest = new HashMap<String, Object>();
        serviceRequest.put("service_instance_guid", serviceId);
        serviceRequest.put("app_guid", appId);
        getRestTemplate().postForObject(getUrl("/v2/service_bindings"), serviceRequest, String.class);
    }

    private UUID doCreateDomain(String domainName) {
        String urlPath = "/v2/private_domains";
        HashMap<String, Object> domainRequest = new HashMap<String, Object>();
        domainRequest.put("owning_organization_guid", sessionSpace.getOrganization()
            .getMeta()
            .getGuid());
        domainRequest.put("name", domainName);
        domainRequest.put("wildcard", true);
        String resp = getRestTemplate().postForObject(getUrl(urlPath), domainRequest, String.class);
        Map<String, Object> respMap = JsonUtil.convertJsonToMap(resp);
        return resourceMapper.getGuidOfV2Resource(respMap);
    }

    private void doCreateSecurityGroup(String name, List<Map<String, Object>> rules) {
        String path = "/v2/security_groups";
        HashMap<String, Object> request = new HashMap<String, Object>();
        request.put("name", name);
        request.put("rules", rules);
        getRestTemplate().postForObject(getUrl(path), request, String.class);
    }

    private UUID doCreateSpace(String spaceName, UUID orgGuid) {
        String urlPath = "/v2/spaces";
        HashMap<String, Object> spaceRequest = new HashMap<String, Object>();
        spaceRequest.put("organization_guid", orgGuid);
        spaceRequest.put("name", spaceName);
        String resp = getRestTemplate().postForObject(getUrl(urlPath), spaceRequest, String.class);
        Map<String, Object> respMap = JsonUtil.convertJsonToMap(resp);
        return resourceMapper.getGuidOfV2Resource(respMap);
    }

    private void doDeleteApplication(UUID appId) {
        getRestTemplate().delete(getUrl("/v2/apps/{guid}?recursive=true"), appId);
    }

    private void doDeleteDomain(UUID domainGuid) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2/private_domains/{domain}";
        urlVars.put("domain", domainGuid);
        getRestTemplate().delete(getUrl(urlPath), urlVars);
    }

    private void doDeleteRoute(UUID routeGuid) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2/routes/{route}";
        urlVars.put("route", routeGuid);
        getRestTemplate().delete(getUrl(urlPath), urlVars);
    }

    private void doDeleteService(CloudService cloudService) {
        List<UUID> appIds = getAppsBoundToService(cloudService);
        if (appIds.size() > 0) {
            for (UUID appId : appIds) {
                doUnbindService(appId, cloudService.getMeta()
                    .getGuid());
            }
        }
        getRestTemplate().exchange(getUrl("/v2/service_instances/{guid}?accepts_incomplete=true"), HttpMethod.DELETE, HttpEntity.EMPTY,
            new ParameterizedTypeReference<Map<String, Object>>() {
            }, cloudService.getMeta()
                .getGuid());
    }

    private void doDeleteSpace(UUID spaceGuid) {
        getRestTemplate().delete(getUrl("/v2/spaces/{guid}?async=false"), spaceGuid);
    }

    @SuppressWarnings("unchecked")
    private InstancesInfo doGetApplicationInstances(UUID appId) {
        try {
            List<Map<String, Object>> instanceList = new ArrayList<Map<String, Object>>();
            Map<String, Object> respMap = getInstanceInfoForApp(appId, "instances");
            List<String> keys = new ArrayList<String>(respMap.keySet());
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
        } catch (CloudOperationException e) {
            if (e.getStatusCode()
                .equals(HttpStatus.BAD_REQUEST)) {
                return null;
            } else {
                throw e;
            }

        }
    }

    @SuppressWarnings("unchecked")
    private ApplicationStats doGetApplicationStats(UUID appId, CloudApplication.AppState appState) {
        List<InstanceStats> instanceList = new ArrayList<InstanceStats>();
        if (appState.equals(CloudApplication.AppState.STARTED)) {
            Map<String, Object> respMap = getInstanceInfoForApp(appId, "stats");
            for (String instanceId : respMap.keySet()) {
                InstanceStats instanceStats = new InstanceStats(instanceId, (Map<String, Object>) respMap.get(instanceId));
                instanceList.add(instanceStats);
            }
        }
        return new ApplicationStats(instanceList);
    }

    private List<CloudDomain> doGetDomains(CloudOrganization org) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2";
        if (org != null) {
            urlVars.put("org", org.getMeta()
                .getGuid());
            urlPath = urlPath + "/organizations/{org}";
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

    private String doGetFileByRange(String urlPath, Object app, String instance, String filePath, int start, int end, String range) {

        boolean supportsRanges;
        try {
            supportsRanges = getRestTemplate().execute(getUrl(urlPath), HttpMethod.HEAD, new RequestCallback() {
                @Override
                public void doWithRequest(ClientHttpRequest request) throws IOException {
                    request.getHeaders()
                        .set("Range", "bytes=0-");
                }
            }, new ResponseExtractor<Boolean>() {
                @Override
                public Boolean extractData(ClientHttpResponse response) throws IOException {
                    return response.getStatusCode()
                        .equals(HttpStatus.PARTIAL_CONTENT);
                }
            }, app, instance, filePath);
        } catch (CloudOperationException e) {
            if (e.getStatusCode()
                .equals(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)) {
                // must be a 0 byte file
                return "";
            } else {
                throw e;
            }
        }
        HttpHeaders headers = new HttpHeaders();
        if (supportsRanges) {
            headers.set("Range", range);
        }
        HttpEntity<Object> requestEntity = new HttpEntity<Object>(headers);
        ResponseEntity<String> responseEntity = getRestTemplate().exchange(getUrl(urlPath), HttpMethod.GET, requestEntity, String.class,
            app, instance, filePath);
        String response = responseEntity.getBody();
        boolean partialFile = false;
        if (responseEntity.getStatusCode()
            .equals(HttpStatus.PARTIAL_CONTENT)) {
            partialFile = true;
        }
        if (!partialFile && response != null) {
            if (start == -1) {
                return response.substring(response.length() - end);
            } else {
                if (start >= response.length()) {
                    if (response.length() == 0) {
                        return "";
                    }
                    throw new CloudOperationException(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE,
                        "The starting position " + start + " is past the end of the file content.");
                }
                if (end != -1) {
                    if (end >= response.length()) {
                        end = response.length() - 1;
                    }
                    return response.substring(start, end + 1);
                } else {
                    return response.substring(start);
                }
            }
        }
        return response;
    }

    private List<CloudRoute> doGetRoutes(UUID domainGuid) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2";
        // TODO: NOT implemented ATM:
        // if (sessionSpace != null) {
        // urlVars.put("space", sessionSpace.getMeta().getGuid());
        // urlPath = urlPath + "/spaces/{space}";
        // }
        urlPath = urlPath + "/routes?inline-relations-depth=1";
        List<Map<String, Object>> allRoutes = getAllResources(urlPath, urlVars);
        List<CloudRoute> routes = new ArrayList<CloudRoute>();
        for (Map<String, Object> route : allRoutes) {
            // TODO: move space_guid to path once implemented (see above):
            UUID space = CloudEntityResourceMapper.getAttributeOfV2Resource(route, "space_guid", UUID.class);
            UUID domain = CloudEntityResourceMapper.getAttributeOfV2Resource(route, "domain_guid", UUID.class);
            if (sessionSpace.getMeta()
                .getGuid()
                .equals(space) && domainGuid.equals(domain)) {
                // routes.add(CloudEntityResourceMapper.getEntityAttribute(route, "host", String.class));
                routes.add(resourceMapper.mapResource(route, CloudRoute.class));
            }
        }
        return routes;
    }

    private Map<String, Object> findSecurityGroupResource(String securityGroupName) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2/security_groups?q=name:{name}";
        urlVars.put("name", securityGroupName);
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        if (resourceList.size() > 0) {
            return resourceList.get(0);
        }
        return null;
    }

    private Map<String, Object> doGetServiceInstance(String serviceName, int inlineDepth) {
        String urlPath = "/v2";
        Map<String, Object> urlVars = new HashMap<String, Object>();
        if (sessionSpace != null) {
            urlVars.put("space", sessionSpace.getMeta()
                .getGuid());
            urlPath = urlPath + "/spaces/{space}";
        }
        urlVars.put("q", "name:" + serviceName);
        urlPath = urlPath + "/service_instances?q={q}&return_user_provided_service_instances=true";
        if (inlineDepth > 0) {
            urlPath = urlPath + "&inline-relations-depth=" + inlineDepth;
        }

        List<Map<String, Object>> resources = getAllResources(urlPath, urlVars);

        if (resources.size() > 0) {
            Map<String, Object> serviceResource = resources.get(0);
            if (hasEmbeddedResource(serviceResource, "service_plan")) {
                fillInEmbeddedResource(serviceResource, "service_plan", "service");
            }
            // If the bindings are more than 50, then they will not be included in the response due to the way the 'inline-relations-depth'
            // parameter works. In this case, we have to make one additional request to the URL specified in the 'service_bindings_url'
            // field of the response.
            if (hasEmbeddedResource(serviceResource, "service_bindings")) {
                fillInEmbeddedResource(serviceResource, "service_bindings");
            }
            return serviceResource;
        }
        return null;
    }

    private List<ServiceKey> doGetServiceKeys(CloudService cloudService) {
        String urlPath = "/v2/service_instances/{serviceId}/service_keys";
        Map<String, Object> pathVariables = new HashMap<String, Object>();
        pathVariables.put("serviceId", cloudService.getMeta()
            .getGuid());
        List<Map<String, Object>> resourceList = getAllResources(urlPath, pathVariables);
        List<ServiceKey> serviceKeys = new ArrayList<ServiceKey>();
        for (Map<String, Object> resource : resourceList) {
            ServiceKey serviceKey = resourceMapper.mapResource(resource, ServiceKey.class);
            serviceKey.setService(cloudService);
            serviceKeys.add(serviceKey);
        }
        return serviceKeys;
    }

    private void doSetQuotaToOrg(UUID orgGuid, UUID quotaGuid) {
        String setPath = "/v2/organizations/{org}";
        Map<String, Object> setVars = new HashMap<String, Object>();
        setVars.put("org", orgGuid);
        HashMap<String, Object> setRequest = new HashMap<String, Object>();
        setRequest.put("quota_definition_guid", quotaGuid);

        getRestTemplate().put(getUrl(setPath), setRequest, setVars);
    }

    private void doUnbindService(UUID appId, UUID serviceId) {
        UUID serviceBindingId = getServiceBindingId(appId, serviceId);
        getRestTemplate().delete(getUrl("/v2/service_bindings/{guid}"), serviceBindingId);
    }

    private void doUpdateSecurityGroup(CloudSecurityGroup currentGroup, String name, List<Map<String, Object>> rules) {
        String path = "/v2/security_groups/{guid}";

        Map<String, Object> pathVariables = new HashMap<String, Object>();
        pathVariables.put("guid", currentGroup.getMeta()
            .getGuid());

        HashMap<String, Object> request = new HashMap<String, Object>();
        request.put("name", name);
        request.put("rules", rules);
        // Updates of bindings to spaces and default staging/running groups must be done
        // through explicit calls to those methods and not through this generic update

        getRestTemplate().put(getUrl(path), request, pathVariables);
    }

    private List<CloudRoute> fetchOrphanRoutes(String domainName) {
        List<CloudRoute> orphanRoutes = new ArrayList<>();
        for (CloudRoute cloudRoute : getRoutes(domainName)) {
            if (isOrphanRoute(cloudRoute)) {
                orphanRoutes.add(cloudRoute);
            }
        }

        return orphanRoutes;
    }

    @SuppressWarnings("unchecked")
    private void fillInEmbeddedResource(Map<String, Object> resource, String... resourcePath) {
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
            Object response = getRestTemplate().getForObject(getUrl(pathUrl), Object.class);
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
            // entity = (Map<String, Object>) embeddedResourceMap.get("entity");
            fillInEmbeddedResource(embeddedResourceMap, tailPath);
        } else if (embeddedResource instanceof List) {
            List<Object> embeddedResourcesList = (List<Object>) embeddedResource;
            for (Object r : embeddedResourcesList) {
                fillInEmbeddedResource((Map<String, Object>) r, tailPath);
            }
        } else {
            // no way to proceed
            return;
        }
    }

    private Map<String, Object> findApplicationResource(UUID appGuid, boolean fetchServiceInfo) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2/apps/{app}?inline-relations-depth=1";
        urlVars.put("app", appGuid);
        String resp = getRestTemplate().getForObject(getUrl(urlPath), String.class, urlVars);

        return processApplicationResource(JsonUtil.convertJsonToMap(resp), fetchServiceInfo);
    }

    private Map<String, Object> findApplicationResource(String appName, boolean fetchServiceInfo) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2";
        if (sessionSpace != null) {
            urlVars.put("space", sessionSpace.getMeta()
                .getGuid());
            urlPath = urlPath + "/spaces/{space}";
        }
        urlVars.put("q", "name:" + appName);
        urlPath = urlPath + "/apps?inline-relations-depth=1&q={q}";

        List<Map<String, Object>> allResources = getAllResources(urlPath, urlVars);
        if (!allResources.isEmpty()) {
            return processApplicationResource(allResources.get(0), fetchServiceInfo);
        }
        return null;
    }

    private List<String> findApplicationUris(UUID appGuid) {
        String urlPath = "/v2/apps/{app}/routes?inline-relations-depth=1";
        Map<String, Object> urlVars = new HashMap<String, Object>();
        urlVars.put("app", appGuid);
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        List<String> uris = new ArrayList<String>();
        for (Map<String, Object> resource : resourceList) {
            Map<String, Object> domainResource = CloudEntityResourceMapper.getEmbeddedResource(resource, "domain");
            String host = CloudEntityResourceMapper.getAttributeOfV2Resource(resource, "host", String.class);
            String domain = CloudEntityResourceMapper.getAttributeOfV2Resource(domainResource, "name", String.class);
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
        if (resourceList.size() > 0) {
            return resourceList.get(0);
        }
        return null;
    }

    private Map<String, Object> findSpaceResource(String spaceName) {
        String urlPath = "/v2/spaces?inline-relations-depth=1&q=name:{name}";
        HashMap<String, Object> spaceRequest = new HashMap<String, Object>();
        spaceRequest.put("name", spaceName);
        List<Map<String, Object>> resourceList = getAllResources(urlPath, spaceRequest);
        if (resourceList.size() > 0) {
            return resourceList.get(0);
        }
        return null;
    }

    private Map<String, Object> findStackResource(String name) {
        String urlPath = "/v2/stacks?q={q}";
        Map<String, Object> urlVars = new HashMap<String, Object>();
        urlVars.put("q", "name:" + name);
        List<Map<String, Object>> resources = getAllResources(urlPath, urlVars);
        if (resources.size() > 0) {
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

    private HttpEntity<MultiValueMap<String, ?>> generatePartialResourceRequest(UploadApplicationPayload application,
        CloudResources knownRemoteResources) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<String, Object>(2);
        body.add("bits", application);
        ObjectMapper mapper = new ObjectMapper();
        String knownRemoteResourcesPayload = mapper.writeValueAsString(knownRemoteResources);
        body.add("resources", knownRemoteResourcesPayload);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return new HttpEntity<MultiValueMap<String, ?>>(body, headers);
    }

    private List<Map<String, Object>> getAllResources(String urlPath) {
        return getAllResources(urlPath, null);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getAllResources(String urlPath, Map<String, Object> urlVars) {
        List<Map<String, Object>> allResources = new ArrayList<Map<String, Object>>();
        String response;
        if (urlVars != null) {
            response = getRestTemplate().getForObject(getUrl(urlPath), String.class, urlVars);
        } else {
            response = getRestTemplate().getForObject(getUrl(urlPath), String.class);
        }
        Map<String, Object> responseMap = JsonUtil.convertJsonToMap(response);
        List<Map<String, Object>> newResources = (List<Map<String, Object>>) responseMap.get("resources");
        if (newResources != null && newResources.size() > 0) {
            allResources.addAll(newResources);
        }
        addAllRemainingResources(responseMap, allResources);
        return allResources;
    }

    private void addAllRemainingResources(Map<String, Object> responseMap, List<Map<String, Object>> allResources) {
        String nextUrl = (String) responseMap.get("next_url");
        while (nextUrl != null && nextUrl.length() > 0) {
            nextUrl = addPageOfResources(nextUrl, allResources);
        }
    }

    private UUID getRequiredApplicationId(String applicationName) {
        UUID applicationId = getApplicationId(applicationName);
        if (applicationId == null) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Application '" + applicationName + "' not found.");
        }
        return applicationId;
    }

    @SuppressWarnings("unchecked")
    private UUID getApplicationId(String applicationName) {
        Map<String, Object> resource = findApplicationResource(applicationName, false);
        if (resource == null) {
            return null;
        }
        Map<String, Object> applicationMeta = (Map<String, Object>) resource.get("metadata");
        return UUID.fromString(String.valueOf(applicationMeta.get("guid")));
    }

    @SuppressWarnings("unchecked")
    private List<UUID> getAppsBoundToService(CloudService cloudService) {
        List<UUID> appGuids = new ArrayList<UUID>();
        String urlPath = "/v2";
        Map<String, Object> urlVars = new HashMap<String, Object>();
        if (sessionSpace != null) {
            urlVars.put("space", sessionSpace.getMeta()
                .getGuid());
            urlPath = urlPath + "/spaces/{space}";
        }
        urlVars.put("q", "name:" + cloudService.getName());
        urlPath = urlPath + "/service_instances?q={q}";
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        for (Map<String, Object> resource : resourceList) {
            fillInEmbeddedResource(resource, "service_bindings");
            List<Map<String, Object>> bindings = CloudEntityResourceMapper.getAttributeOfV2Resource(resource, "service_bindings",
                List.class);
            for (Map<String, Object> binding : bindings) {
                String appId = CloudEntityResourceMapper.getAttributeOfV2Resource(binding, "app_guid", String.class);
                if (appId != null) {
                    appGuids.add(UUID.fromString(appId));
                }
            }
        }
        return appGuids;
    }

    private String getCurrentUserId() {
        String username = getInfo().getUser();
        Map<String, Object> userMap = getUserInfo(username);
        String userId = (String) userMap.get("user_id");
        return userId;
    }

    private UUID getDomainGuid(String domainName, boolean required) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2/domains?inline-relations-depth=1&q=name:{name}";
        urlVars.put("name", domainName);
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        UUID domainGuid = null;
        if (resourceList.size() > 0) {
            Map<String, Object> resource = resourceList.get(0);
            domainGuid = resourceMapper.getGuidOfV2Resource(resource);
        }
        if (domainGuid == null && required) {
            throw new CloudOperationException(HttpStatus.NOT_FOUND, "Not Found", "Domain '" + domainName + "' not found.");
        }
        return domainGuid;
    }

    private Map<String, UUID> getDomainGuids() {
        List<CloudDomain> availableDomains = new ArrayList<CloudDomain>();
        availableDomains.addAll(getDomainsForOrg());
        availableDomains.addAll(getSharedDomains());
        Map<String, UUID> domains = new HashMap<String, UUID>(availableDomains.size());
        for (CloudDomain availableDomain : availableDomains) {
            domains.put(availableDomain.getName(), availableDomain.getMeta()
                .getGuid());
        }
        return domains;
    }

    private Map<String, Object> getInstanceInfoForApp(UUID appId, String path) {
        String url = getUrl("/v2/apps/{guid}/" + path);
        Map<String, Object> urlVars = new HashMap<String, Object>();
        urlVars.put("guid", appId);
        String resp = getRestTemplate().getForObject(url, String.class, urlVars);
        return JsonUtil.convertJsonToMap(resp);
    }

    private CloudResources getKnownRemoteResources(ApplicationArchive archive) throws IOException {
        CloudResources archiveResources = new CloudResources(archive);
        String json = JsonUtil.convertToJson(archiveResources);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(JsonUtil.JSON_MEDIA_TYPE);
        HttpEntity<String> requestEntity = new HttpEntity<String>(json, headers);
        ResponseEntity<String> responseEntity = getRestTemplate().exchange(getUrl("/v2/resource_match"), HttpMethod.PUT, requestEntity,
            String.class);
        List<CloudResource> cloudResources = JsonUtil.convertJsonToCloudResourceList(responseEntity.getBody());
        return new CloudResources(cloudResources);
    }

    private UUID getRouteGuid(Map<String, String> uriInfo, UUID domainGuid) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2";
        urlPath = urlPath + "/routes?inline-relations-depth=0&q=host:{host}";
        urlVars.put("host", uriInfo.get("host"));
        String path = uriInfo.get("path");
        if (!StringUtils.isEmpty(path)) {
            urlPath = urlPath + "&q=path:{path}";
            urlVars.put("path", path);
        }
        List<Map<String, Object>> allRoutes = getAllResources(urlPath, urlVars);
        UUID routeGuid = null;
        for (Map<String, Object> route : allRoutes) {
            UUID routeSpace = CloudEntityResourceMapper.getAttributeOfV2Resource(route, "space_guid", UUID.class);
            UUID routeDomain = CloudEntityResourceMapper.getAttributeOfV2Resource(route, "domain_guid", UUID.class);
            if (sessionSpace.getMeta()
                .getGuid()
                .equals(routeSpace) && domainGuid.equals(routeDomain)) {
                routeGuid = CloudEntityResourceMapper.getV2Meta(route)
                    .getGuid();
            }
        }
        return routeGuid;
    }

    private int getRunningInstances(UUID appId, CloudApplication.AppState appState) {
        int running = 0;
        ApplicationStats appStats = doGetApplicationStats(appId, appState);
        if (appStats != null && appStats.getRecords() != null) {
            for (InstanceStats inst : appStats.getRecords()) {
                if (InstanceState.RUNNING == inst.getState()) {
                    running++;
                }
            }
        }
        return running;
    }

    // Security Group operations

    @SuppressWarnings("unchecked")
    private UUID getServiceBindingId(UUID appId, UUID serviceId) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        urlVars.put("guid", appId);
        List<Map<String, Object>> resourceList = getAllResources("/v2/apps/{guid}/service_bindings", urlVars);
        UUID serviceBindingId = null;
        if (resourceList != null && resourceList.size() > 0) {
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
        List<CloudServiceOffering> results = new ArrayList<CloudServiceOffering>();
        for (Map<String, Object> resource : resourceList) {
            CloudServiceOffering cloudServiceOffering = resourceMapper.mapResource(resource, CloudServiceOffering.class);
            if (cloudServiceOffering.getLabel() != null && label.equals(cloudServiceOffering.getLabel())) {
                results.add(cloudServiceOffering);
            }
        }
        return results;
    }

    private UUID getSpaceGuid(String spaceName, UUID orgGuid) {
        Map<String, Object> urlVars = new HashMap<String, Object>();
        String urlPath = "/v2/organizations/{orgGuid}/spaces?inline-relations-depth=1&q=name:{name}";
        urlVars.put("orgGuid", orgGuid);
        urlVars.put("name", spaceName);
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        if (resourceList.size() > 0) {
            Map<String, Object> resource = resourceList.get(0);
            return resourceMapper.getGuidOfV2Resource(resource);
        }
        return null;
    }

    private UUID getSpaceGuid(String orgName, String spaceName) {
        CloudOrganization org = getOrganization(orgName);
        return getSpaceGuid(spaceName, org.getMeta()
            .getGuid());
    }

    private List<UUID> getSpaceUserGuids(String orgName, String spaceName, String urlPath) {
        if (orgName == null || spaceName == null) {
            assertSpaceProvided("get space users");
        }

        UUID spaceGuid;
        if (spaceName == null) {
            spaceGuid = sessionSpace.getMeta()
                .getGuid();
        } else {
            CloudOrganization organization = (orgName == null ? sessionSpace.getOrganization() : getOrganization(orgName));
            spaceGuid = getSpaceGuid(spaceName, organization.getMeta()
                .getGuid());
        }
        return getSpaceUserGuids(spaceGuid, urlPath);
    }

    private List<UUID> getSpaceUserGuids(UUID spaceGuid, String urlPath) {
        Assert.notNull(spaceGuid, "Unable to get space users without specifying space GUID.");
        Map<String, Object> urlVars = new HashMap<String, Object>();
        urlVars.put("guid", spaceGuid);

        List<UUID> managersGuid = new ArrayList<UUID>();
        List<Map<String, Object>> resourceList = getAllResources(urlPath, urlVars);
        for (Map<String, Object> resource : resourceList) {
            UUID userGuid = resourceMapper.getGuidOfV2Resource(resource);
            managersGuid.add(userGuid);
        }
        return managersGuid;
    }

    @SuppressWarnings("restriction")
    private Map<String, Object> getUserInfo(String user) {
        // String userJson = getRestTemplate().getForObject(getUrl("/v2/users/{guid}"), String.class, user);
        // Map<String, Object> userInfo = (Map<String, Object>) JsonUtil.convertJsonToMap(userJson);
        // return userInfo();
        // TODO: remove this temporary hack once the /v2/users/ uri can be accessed by mere mortals
        String userJson = "{}";
        OAuth2AccessToken accessToken = oauthClient.getToken();
        if (accessToken != null) {
            String tokenString = accessToken.getValue();
            int x = tokenString.indexOf('.');
            int y = tokenString.indexOf('.', x + 1);
            String encodedString = tokenString.substring(x + 1, y);
            try {
                byte[] decodedBytes = new sun.misc.BASE64Decoder().decodeBuffer(encodedString);
                userJson = new String(decodedBytes, 0, decodedBytes.length, "UTF-8");
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

    private void initialize(URL cloudControllerUrl, RestTemplate restTemplate, OauthClient oauthClient, LoggregatorClient loggregatorClient,
        CloudCredentials cloudCredentials) {
        Assert.notNull(cloudControllerUrl, "CloudControllerUrl cannot be null");
        Assert.notNull(restTemplate, "RestTemplate cannot be null");
        Assert.notNull(oauthClient, "OauthClient cannot be null");

        oauthClient.init(cloudCredentials);

        this.cloudCredentials = cloudCredentials;

        this.cloudControllerUrl = cloudControllerUrl;

        this.restTemplate = restTemplate;
        configureRequestFactory(restTemplate);

        this.oauthClient = oauthClient;

        this.loggregatorClient = loggregatorClient;
    }

    private boolean isOrphanRoute(CloudRoute cloudRoute) {
        return cloudRoute.getAppsUsingRoute() == 0;
    }

    @SuppressWarnings("unchecked")
    private CloudApplication mapCloudApplication(Map<String, Object> resource) {
        UUID appId = resourceMapper.getGuidOfV2Resource(resource);
        CloudApplication cloudApp = null;
        if (resource != null) {
            int running = getRunningInstances(appId,
                CloudApplication.AppState.valueOf(CloudEntityResourceMapper.getAttributeOfV2Resource(resource, "state", String.class)));
            ((Map<String, Object>) resource.get("entity")).put("running_instances", running);
            cloudApp = resourceMapper.mapResource(resource, CloudApplication.class);
            cloudApp.setUris(findApplicationUris(cloudApp.getMeta()
                .getGuid()));
        }
        return cloudApp;
    }

    private Map<String, Object> processApplicationResource(Map<String, Object> resource, boolean fetchServiceInfo) {
        if (fetchServiceInfo) {
            fillInEmbeddedResource(resource, "service_bindings", "service_instance");
        }
        fillInEmbeddedResource(resource, "stack");
        return resource;
    }

    private void processAsyncJobInBackground(final String jobUrl, final UploadStatusCallback callback) {
        String threadName = String.format("App upload monitor: %s", jobUrl);
        new Thread(new Runnable() {

            @Override
            public void run() {
                processAsyncJob(jobUrl, callback);
            }

        }, threadName).start();
    }

    private void processAsyncJob(String jobUrl, UploadStatusCallback callback) {
        while (true) {
            CloudJob job = getJob(jobUrl);
            boolean unsubscribe = callback.onProgress(job.getStatus()
                .toString());
            if (unsubscribe || job.getStatus() == CloudJob.Status.FINISHED) {
                return;
            }
            if (job.getStatus() == CloudJob.Status.FAILED) {
                callback.onError(job.getErrorDetails()
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

    private CloudJob getJob(String jobUrl) {
        ResponseEntity<Map<String, Object>> jobProgressEntity = getRestTemplate().exchange(jobUrl, HttpMethod.GET, HttpEntity.EMPTY,
            new ParameterizedTypeReference<Map<String, Object>>() {
            });
        return resourceMapper.mapResource(jobProgressEntity.getBody(), CloudJob.class);
    }

    private CloudPackage getCloudPackage(String packageUrl) {
        ResponseEntity<Map<String, Object>> cloudPackageEntity = getRestTemplate().exchange(packageUrl, HttpMethod.GET, HttpEntity.EMPTY,
            new ParameterizedTypeReference<Map<String, Object>>() {
            });

        return resourceMapper.mapResource(cloudPackageEntity.getBody(), CloudPackage.class);
    }

    private void removeUris(List<String> uris, UUID appGuid) {
        Map<String, UUID> domains = getDomainGuids();
        for (String uri : uris) {
            Map<String, String> uriInfo = new HashMap<String, String>(2);
            extractUriInfo(domains, uri, uriInfo);
            UUID domainGuid = domains.get(uriInfo.get("domainName"));
            unbindRoute(uriInfo, domainGuid, appGuid);
        }
    }

    private StreamingLogToken streamLoggregatorLogs(String appName, ApplicationLogListener listener, boolean recent) {
        ClientEndpointConfig.Configurator configurator = new ClientEndpointConfig.Configurator() {
            @Override
            public void beforeRequest(Map<String, List<String>> headers) {
                String authorizationHeader = oauthClient.getAuthorizationHeader();
                if (authorizationHeader != null) {
                    headers.put(AUTHORIZATION_HEADER_KEY, Arrays.asList(authorizationHeader));
                }
            }
        };

        String endpoint = getInfo().getLoggregatorEndpoint();
        String mode = recent ? "dump" : "tail";
        UUID appId = getApplicationId(appName);
        return loggregatorClient.connectToLoggregator(endpoint, mode, appId, listener, configurator);
    }

    private void unbindRoute(Map<String, String> uriInfo, UUID domainGuid, UUID appGuid) {
        UUID routeGuid = getRouteGuid(uriInfo, domainGuid);
        if (routeGuid != null) {
            String bindPath = "/v2/apps/{app}/routes/{route}";
            Map<String, Object> bindVars = new HashMap<String, Object>();
            bindVars.put("app", appGuid);
            bindVars.put("route", routeGuid);
            getRestTemplate().delete(getUrl(bindPath), bindVars);
        }
    }

    private CloudSpace validateSpaceAndOrg(String spaceName, String orgName, CloudControllerRestClientImpl client) {
        List<CloudSpace> spaces = client.getSpaces();

        for (CloudSpace space : spaces) {
            if (space.getName()
                .equals(spaceName)) {
                CloudOrganization org = space.getOrganization();
                if (orgName == null || org.getName()
                    .equals(orgName)) {
                    return space;
                }
            }
        }

        throw new IllegalArgumentException("No matching organization and space found for org: " + orgName + " space: " + "" + spaceName);
    }

    private static class ResponseExtractorWrapper implements ResponseExtractor {

        private ClientHttpResponseCallback callback;

        public ResponseExtractorWrapper(ClientHttpResponseCallback callback) {
            this.callback = callback;
        }

        @Override
        public Object extractData(ClientHttpResponse clientHttpResponse) throws IOException {
            callback.onClientHttpResponse(clientHttpResponse);
            return null;
        }

    }

    private class CloudControllerRestClientHttpRequestFactory implements ClientHttpRequestFactory {

        private Integer defaultSocketTimeout = 0;

        private ClientHttpRequestFactory delegate;

        public CloudControllerRestClientHttpRequestFactory(ClientHttpRequestFactory delegate) {
            this.delegate = delegate;
            captureDefaultReadTimeout();
        }

        @Override
        public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
            ClientHttpRequest request = delegate.createRequest(uri, httpMethod);

            String authorizationHeader = oauthClient.getAuthorizationHeader();
            if (authorizationHeader != null) {
                request.getHeaders()
                    .add(AUTHORIZATION_HEADER_KEY, authorizationHeader);
            }

            if (cloudCredentials != null && cloudCredentials.getProxyUser() != null) {
                request.getHeaders()
                    .add(PROXY_USER_HEADER_KEY, cloudCredentials.getProxyUser());
            }

            return request;
        }

        public void increaseReadTimeoutForStreamedTailedLogs(int timeout) {
            // May temporary increase read timeout on other unrelated concurrent
            // threads, but per-request read timeout don't seem easily
            // accessible
            if (delegate instanceof HttpComponentsClientHttpRequestFactory) {
                HttpComponentsClientHttpRequestFactory httpRequestFactory = (HttpComponentsClientHttpRequestFactory) delegate;

                if (timeout > 0) {
                    httpRequestFactory.setReadTimeout(timeout);
                } else {
                    httpRequestFactory.setReadTimeout(defaultSocketTimeout);
                }
            }
        }

        private void captureDefaultReadTimeout() {
            // As of HttpClient 4.3.x, obtaining the default parameters is deprecated and removed,
            // so we fallback to java.net.Socket.

            if (defaultSocketTimeout == null) {
                try {
                    defaultSocketTimeout = new Socket().getSoTimeout();
                } catch (SocketException e) {
                    defaultSocketTimeout = 0;
                }
            }
        }
    }

    private void waitUntilPackageUploadFinish(UploadToken uploadToken) {
        Upload upload = getUploadStatus(uploadToken.getToken());

        while (!upload.getStatus()
            .equals(Status.EXPIRED)
            && !upload.getStatus()
                .equals(Status.READY)) {
            upload = getUploadStatus(uploadToken.getToken());
        }
    }

}
