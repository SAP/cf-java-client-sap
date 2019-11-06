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

import static org.cloudfoundry.client.lib.rest.clients.util.ReactorResourcesFetcher.fetch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.ApplicationLogListener;
import org.cloudfoundry.client.lib.ClientHttpResponseCallback;
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.RestLogCallback;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.StreamingLogToken;
import org.cloudfoundry.client.lib.UploadStatusCallback;
import org.cloudfoundry.client.lib.adapters.ImmutableRawCloudInfo;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudInfo;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudQuota;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudSecurityGroup;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.CloudUser;
import org.cloudfoundry.client.lib.domain.CrashesInfo;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerCompositeClient;
import org.cloudfoundry.client.lib.rest.clients.ImmutableCloudControllerCompositeClient;
import org.cloudfoundry.client.lib.rest.clients.apps.CloudControllerApplicationsClient;
import org.cloudfoundry.client.lib.rest.clients.apps.impl.CloudControllerApplicationsClientImpl;
import org.cloudfoundry.client.lib.rest.clients.builds.CloudControllerBuildsClient;
import org.cloudfoundry.client.lib.rest.clients.builds.CloudControllerBuildsClientImpl;
import org.cloudfoundry.client.lib.rest.clients.domains.CloudControllerDomainsClient;
import org.cloudfoundry.client.lib.rest.clients.domains.CloudControllerDomainsClientImpl;
import org.cloudfoundry.client.lib.rest.clients.droplets.CloudControllerDropletsClientImpl;
import org.cloudfoundry.client.lib.rest.clients.events.CloudControllerEventsClient;
import org.cloudfoundry.client.lib.rest.clients.events.CloudControllerEventsClientImpl;
import org.cloudfoundry.client.lib.rest.clients.logs.CloudControllerLogsClient;
import org.cloudfoundry.client.lib.rest.clients.logs.CloudControllerLogsClientImpl;
import org.cloudfoundry.client.lib.rest.clients.organizations.CloudControllerOrganizationsClient;
import org.cloudfoundry.client.lib.rest.clients.organizations.CloudControllerOrganizationsClientImpl;
import org.cloudfoundry.client.lib.rest.clients.packages.CloudControllerPackagesClient;
import org.cloudfoundry.client.lib.rest.clients.packages.CloudControllerPackagesClientImpl;
import org.cloudfoundry.client.lib.rest.clients.routes.CloudControllerRoutesClient;
import org.cloudfoundry.client.lib.rest.clients.routes.CloudControllerRoutesClientImpl;
import org.cloudfoundry.client.lib.rest.clients.servicebrokers.CloudControllerServiceBrokersClient;
import org.cloudfoundry.client.lib.rest.clients.servicebrokers.CloudControllerServiceBrokersClientImpl;
import org.cloudfoundry.client.lib.rest.clients.servicekeys.CloudControllerServiceKeysClient;
import org.cloudfoundry.client.lib.rest.clients.servicekeys.CloudControllerServiceKeysClientImpl;
import org.cloudfoundry.client.lib.rest.clients.services.CloudControllerServicesClient;
import org.cloudfoundry.client.lib.rest.clients.services.CloudControllerServicesClientImpl;
import org.cloudfoundry.client.lib.rest.clients.spaces.CloudControllerSpacesClient;
import org.cloudfoundry.client.lib.rest.clients.spaces.CloudControllerSpacesClientImpl;
import org.cloudfoundry.client.lib.rest.clients.stacks.CloudControllerStacksClient;
import org.cloudfoundry.client.lib.rest.clients.stacks.CloudControllerStacksClientImpl;
import org.cloudfoundry.client.lib.rest.clients.tasks.CloudControllerTasksClient;
import org.cloudfoundry.client.lib.rest.clients.tasks.CloudControllerTasksClientImpl;
import org.cloudfoundry.client.v2.info.GetInfoRequest;
import org.cloudfoundry.client.v2.info.GetInfoResponse;
import org.cloudfoundry.doppler.DopplerClient;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import reactor.core.publisher.Mono;

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

    private CloudCredentials credentials;
    private URL controllerUrl;
    private OAuthClient oAuthClient;
    private RestTemplate restTemplate;

    private CloudFoundryClient v3Client;
    private CloudControllerCompositeClient client;

    /**
     * Only for unit tests. This works around the fact that the initialize method is called within the constructor and hence can not be
     * overloaded, making it impossible to write unit tests that don't trigger network calls.
     */
    protected CloudControllerRestClientImpl() {
    }

    public CloudControllerRestClientImpl(URL controllerUrl, CloudCredentials credentials, RestTemplate restTemplate,
                                         OAuthClient oAuthClient, CloudFoundryClient v3Client) {
        this(controllerUrl, credentials, restTemplate, oAuthClient, v3Client, null, null);
    }

    public CloudControllerRestClientImpl(URL controllerUrl, CloudCredentials credentials, RestTemplate restTemplate,
                                         OAuthClient oAuthClient, CloudFoundryClient v3Client, DopplerClient dopplerClient,
                                         CloudSpace target) {
        Assert.notNull(controllerUrl, "CloudControllerUrl cannot be null");
        Assert.notNull(restTemplate, "RestTemplate cannot be null");
        Assert.notNull(oAuthClient, "OAuthClient cannot be null");

        CloudControllerDomainsClient domainsClient = new CloudControllerDomainsClientImpl(target, v3Client);
        CloudControllerRoutesClient routesClient = new CloudControllerRoutesClientImpl(target, v3Client, domainsClient);
        CloudControllerApplicationsClient applicationsClient = new CloudControllerApplicationsClientImpl(target, v3Client, domainsClient);
        CloudControllerTasksClient tasksClient = new CloudControllerTasksClientImpl(target, v3Client, applicationsClient);
        CloudControllerServicesClient servicesClient = new CloudControllerServicesClientImpl(target, v3Client, applicationsClient);
        CloudControllerServiceBrokersClient serviceBrokersClient = new CloudControllerServiceBrokersClientImpl(target, v3Client);
        CloudControllerServiceKeysClient serviceKeysClient = new CloudControllerServiceKeysClientImpl(target, v3Client, servicesClient);
        CloudControllerOrganizationsClient organizationsClient = new CloudControllerOrganizationsClientImpl(target, v3Client);
        CloudControllerSpacesClient spacesClient = new CloudControllerSpacesClientImpl(target, v3Client, organizationsClient);
        CloudControllerBuildsClient buildsClient = new CloudControllerBuildsClientImpl(target, v3Client);
        CloudControllerStacksClient stacksClient = new CloudControllerStacksClientImpl(target, v3Client);
        CloudControllerEventsClient eventsClient = new CloudControllerEventsClientImpl(target, v3Client);
        CloudControllerPackagesClient packagesClient = new CloudControllerPackagesClientImpl(target, v3Client);
        CloudControllerLogsClient logsClient = new CloudControllerLogsClientImpl(target,
                                                                                 v3Client,
                                                                                 applicationsClient,
                                                                                 dopplerClient,
                                                                                 restTemplate);
        CloudControllerDropletsClientImpl dropletsClient = new CloudControllerDropletsClientImpl(target, v3Client);
        client = ImmutableCloudControllerCompositeClient.builder()
                                                             .domains(domainsClient)
                                                             .routes(routesClient)
                                                             .applications(applicationsClient)
                                                             .services(servicesClient)
                                                             .tasks(tasksClient)
                                                             .serviceBrokers(serviceBrokersClient)
                                                             .serviceKeys(serviceKeysClient)
                                                             .organizations(organizationsClient)
                                                             .spaces(spacesClient)
                                                             .logs(logsClient)
                                                             .builds(buildsClient)
                                                             .events(eventsClient)
                                                             .stacks(stacksClient)
                                                             .packages(packagesClient)
                                                             .droplets(dropletsClient)
                                                             .build();

        this.controllerUrl = controllerUrl;
        this.credentials = credentials;
        this.restTemplate = restTemplate;
        this.oAuthClient = oAuthClient;

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
        client.domains()
                   .addDomain(domainName);
    }

    @Override
    public void addRoute(String host, String domainName, String path) {
        client.routes()
                   .add(host, domainName, path);
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
        client.services()
                   .bindService(applicationName, serviceName, null);
    }

    @Override
    public void bindService(String applicationName, String serviceName, Map<String, Object> parameters) {
        client.services()
                   .bindService(applicationName, serviceName, parameters);
    }

    @Override
    public void bindStagingSecurityGroup(String securityGroupName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void createApplication(String name, Staging staging, Integer memory, List<String> uris, List<String> serviceNames) {
        client.applications()
                   .actions()
                   .create(name, staging, null, memory, uris, serviceNames, null);
    }

    @Override
    public void createApplication(String name, Staging staging, Integer diskQuota, Integer memory, List<String> uris,
                                  List<String> serviceNames, DockerInfo dockerInfo) {
        client.applications()
                   .actions()
                   .create(name, staging, diskQuota, memory, uris, serviceNames, dockerInfo);
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
        client.services()
                   .create(service);
    }

    @Override
    public void createServiceBroker(CloudServiceBroker serviceBroker) {
        client.serviceBrokers()
                   .create(serviceBroker);
    }

    @Override
    public CloudServiceKey createServiceKey(String serviceName, String name, Map<String, Object> parameters) {
        return client.serviceKeys()
                          .create(serviceName, name, parameters);
    }

    @Override
    public void createSpace(String spaceName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials) {
        client.services()
                   .createUserProvidedService(service, credentials);
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials, String syslogDrainUrl) {
        client.services()
                   .createUserProvidedService(service, credentials, syslogDrainUrl);
    }

    @Override
    public void deleteAllApplications() {
        client.applications()
                   .actions()
                   .deleteAll();
    }

    @Override
    public void deleteAllServices() {
        client.services()
                   .deleteAll();
    }

    @Override
    public void deleteApplication(String applicationName) {
        client.applications()
                   .actions()
                   .delete(applicationName);
    }

    @Override
    public void deleteDomain(String domainName) {
        client.domains()
                   .deleteDomain(domainName);
    }

    /**
     * Delete routes that do not have any application which is assigned to them.
     *
     * @return deleted routes or an empty list if no routes have been found
     */
    @Override
    public List<CloudRoute> deleteOrphanedRoutes() {
        return client.routes()
                          .deleteOrphaned();
    }

    @Override
    public void deleteQuota(String quotaName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void deleteRoute(String host, String domainName, String path) {
        client.routes()
                   .delete(host, domainName, path);
    }

    @Override
    public void deleteSecurityGroup(String securityGroupName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public void deleteService(String serviceName) {
        client.services()
                   .delete(serviceName);
    }

    @Override
    public void deleteServiceBroker(String name) {
        client.serviceBrokers()
                   .delete(name);
    }

    @Override
    public void deleteServiceKey(String serviceName, String serviceKeyName) {
        client.serviceKeys()
                   .delete(serviceName, serviceKeyName);
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
        return client.applications()
                          .getApplication(applicationName, required);
    }

    @Override
    public CloudApplication getApplication(UUID applicationGuid) {
        return client.applications()
                          .getApplication(applicationGuid);
    }

    @Override
    public Map<String, String> getApplicationEnvironment(String applicationName) {
        return client.applications()
                          .environment()
                          .get(applicationName);
    }

    @Override
    public Map<String, String> getApplicationEnvironment(UUID applicationGuid) {
        return client.applications()
                          .environment()
                          .get(applicationGuid);
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String applicationName) {
        return client.applications()
                          .events()
                          .get(applicationName);
    }

    @Override
    public List<CloudEvent> getApplicationEvents(UUID applicationGuid) {
        return client.applications()
                          .events()
                          .get(applicationGuid);
    }

    @Override
    public InstancesInfo getApplicationInstances(String applicationName) {
        return client.applications()
                          .instances()
                          .get(applicationName);
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication application) {
        return client.applications()
                          .instances()
                          .get(application);
    }

    @Override
    public List<CloudApplication> getApplications() {
        return client.applications()
                          .getApplications();
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
        return client.domains()
                          .getDefaultDomain();
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return client.domains()
                          .getSharedDomains();
    }

    @Override
    public List<CloudDomain> getDomains() {
        return client.domains()
                          .getDomains();
    }

    @Override
    public List<CloudDomain> getDomainsForOrganization() {
        return client.domains()
                          .getDomainsForOrganization();
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return client.domains()
                          .getPrivateDomains();
    }

    @Override
    public List<CloudEvent> getEvents() {
        return client.events()
                          .getEvents();
    }

    @Override
    public String getFile(String applicationName, int instanceIndex, String filePath, int startPosition, int endPosition) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public CloudInfo getInfo() {
        return fetch(this::getInfoResource, ImmutableRawCloudInfo::of);
    }

    private Mono<GetInfoResponse> getInfoResource() {
        return v3Client.info()
                       .get(GetInfoRequest.builder()
                                          .build());
    }

    @Override
    public Map<String, String> getLogs(String applicationName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public CloudOrganization getOrganization(String organizationName) {
        return client.organizations()
                          .getOrganization(organizationName, true);
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
        return client.organizations()
                          .getOrganization(organizationName, required);
    }

    @Override
    public Map<String, CloudUser> getOrganizationUsers(String organizationName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public List<CloudOrganization> getOrganizations() {
        return client.organizations()
                          .getOrganizations();
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
        return client.logs()
                          .getRecentLogs(applicationName);
    }

    @Override
    public List<ApplicationLog> getRecentLogs(UUID applicationGuid) {
        return client.logs()
                          .getRecentLogs(applicationGuid);
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        return client.routes()
                          .getAll(domainName);
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
        return client.services()
                          .getService(serviceName);
    }

    @Override
    public CloudService getService(String serviceName, boolean required) {
        return client.services()
                          .getService(serviceName, required);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name) {
        return client.serviceBrokers()
                          .get(name, true);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        return client.serviceBrokers()
                          .get(name, required);
    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        return client.serviceBrokers()
                          .getAll();
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceName) {
        return client.services()
                          .getServiceInstance(serviceName);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceName, boolean required) {
        return client.services()
                          .getServiceInstance(serviceName, required);
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(String serviceName) {
        return client.serviceKeys()
                          .get(serviceName);
    }

    @Override
    public Map<String, Object> getServiceParameters(UUID guid) {
        return client.services()
                          .getServiceParameters(guid);
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        return client.services()
                          .getServiceOfferings();
    }

    @Override
    public List<CloudService> getServices() {
        return client.services()
                          .getServices();
    }

    @Override
    public CloudSpace getSpace(UUID spaceGuid) {
        return client.spaces()
                          .getSpace(spaceGuid);
    }

    @Override
    public CloudSpace getSpace(String organizationName, String spaceName) {
        return client.spaces()
                          .getSpace(organizationName, spaceName, true);
    }

    @Override
    public CloudSpace getSpace(String organizationName, String spaceName, boolean required) {
        return client.spaces()
                          .getSpace(organizationName, spaceName, required);
    }

    @Override
    public CloudSpace getSpace(String spaceName) {
        return client.spaces()
                          .getSpace(spaceName, true);
    }

    @Override
    public CloudSpace getSpace(String spaceName, boolean required) {
        return client.spaces()
                          .getSpace(spaceName, required);
    }

    @Override
    public List<UUID> getSpaceAuditors(String spaceName) {
        return client.spaces()
                          .getSpaceAuditors(spaceName);
    }

    @Override
    public List<UUID> getSpaceAuditors(String organizationName, String spaceName) {
        return client.spaces()
                          .getSpaceAuditors(organizationName, spaceName);
    }

    @Override
    public List<UUID> getSpaceAuditors() {
        return client.spaces()
                          .getSpaceAuditors();
    }

    @Override
    public List<UUID> getSpaceAuditors(UUID spaceGuid) {
        return client.spaces()
                          .getSpaceAuditors(spaceGuid);
    }

    @Override
    public List<UUID> getSpaceDevelopers(String spaceName) {
        return client.spaces()
                          .getSpaceDevelopers(spaceName);
    }

    @Override
    public List<UUID> getSpaceDevelopers(String organizationName, String spaceName) {
        return client.spaces()
                          .getSpaceDevelopers(organizationName, spaceName);
    }

    @Override
    public List<UUID> getSpaceDevelopers() {
        return client.spaces()
                          .getSpaceDevelopers();
    }

    @Override
    public List<UUID> getSpaceDevelopers(UUID spaceGuid) {
        return client.spaces()
                          .getSpaceDevelopers(spaceGuid);
    }

    @Override
    public List<UUID> getSpaceManagers(String spaceName) {
        return client.spaces()
                          .getSpaceManagers(spaceName);
    }

    @Override
    public List<UUID> getSpaceManagers(String organizationName, String spaceName) {
        return client.spaces()
                          .getSpaceManagers(organizationName, spaceName);
    }

    @Override
    public List<UUID> getSpaceManagers() {
        return client.spaces()
                          .getSpaceManagers();
    }

    @Override
    public List<UUID> getSpaceManagers(UUID spaceGuid) {
        return client.spaces()
                          .getSpaceManagers(spaceGuid);
    }

    @Override
    public List<CloudSpace> getSpaces() {
        return client.spaces()
                          .getSpaces();
    }

    @Override
    public List<CloudSpace> getSpaces(String organizationName) {
        return client.spaces()
                          .getSpaces(organizationName);
    }

    @Override
    public List<CloudSpace> getSpacesBoundToSecurityGroup(String securityGroupName) {
        throw new UnsupportedOperationException(MESSAGE_FEATURE_IS_NOT_YET_IMPLEMENTED);
    }

    @Override
    public CloudStack getStack(String name) {
        return client.stacks()
                          .getStack(name, true);
    }

    @Override
    public CloudStack getStack(String name, boolean required) {
        return client.stacks()
                          .getStack(name, required);
    }

    @Override
    public List<CloudStack> getStacks() {
        return client.stacks()
                          .getStacks();
    }

    @Override
    public String getStagingLogs(StartingInfo info, int offset) {
        return client.logs()
                          .getStagingLogs(info, offset);
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
        client.applications()
                   .actions()
                   .rename(applicationName, newName);
    }

    @Override
    public StartingInfo restartApplication(String applicationName) {
        return client.applications()
                          .actions()
                          .restart(applicationName);
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
        return client.applications()
                          .actions()
                          .start(applicationName);
    }

    @Override
    public void stopApplication(String applicationName) {
        client.applications()
                   .actions()
                   .stop(applicationName);
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
        client.services()
                   .unbindService(applicationName, serviceName);
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
        client.applications()
                   .diskQuota()
                   .update(applicationName, diskQuota);
    }

    @Override
    public void updateApplicationEnv(String applicationName, Map<String, String> env) {
        client.applications()
                   .environment()
                   .update(applicationName, env);
    }

    @Override
    public void updateApplicationInstances(String applicationName, int instances) {
        client.applications()
                   .instances()
                   .update(applicationName, instances);
    }

    @Override
    public void updateApplicationMemory(String applicationName, int memory) {
        client.applications()
                   .memory()
                   .update(applicationName, memory);
    }

    @Override
    public List<String> updateApplicationServices(String applicationName,
                                                  Map<String, Map<String, Object>> serviceNamesWithBindingParameters) {
        return client.applications()
                          .services()
                          .update(applicationName, serviceNamesWithBindingParameters);
    }

    @Override
    public void updateApplicationStaging(String applicationName, Staging staging) {
        client.applications()
                   .staging()
                   .update(applicationName, staging);
    }

    @Override
    public void updateApplicationUris(String applicationName, List<String> uris) {
        client.applications()
                   .uris()
                   .update(applicationName, uris);
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
        client.serviceBrokers()
                   .update(serviceBroker);
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, boolean visibility) {
        client.serviceBrokers()
                   .updateServicePlanVisibility(name, visibility);
    }

    @Override
    public CloudTask getTask(UUID taskGuid) {
        return client.tasks()
                          .getTask(taskGuid);
    }

    @Override
    public List<CloudTask> getTasks(String applicationName) {
        return client.tasks()
                          .getTasks(applicationName);
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task) {
        return client.tasks()
                          .runTask(applicationName, task);
    }

    @Override
    public CloudTask cancelTask(UUID taskGuid) {
        return client.tasks()
                          .cancelTask(taskGuid);
    }

    @Override
    public void uploadApplication(String applicationName, File file, UploadStatusCallback callback) throws IOException {
        client.applications()
                   .upload()
                   .sync(applicationName, file, callback);
    }

    @Override
    public void uploadApplication(String applicationName, InputStream inputStream, UploadStatusCallback callback) throws IOException {
        client.applications()
                   .upload()
                   .sync(applicationName, inputStream, callback);
    }

    @Override
    public UploadToken asyncUploadApplication(String applicationName, File file, UploadStatusCallback callback) throws IOException {
        return client.applications()
                          .upload()
                          .async(applicationName, file, callback);
    }

    @Override
    public Upload getUploadStatus(UUID packageGuid) {
        return client.packages()
                          .getUploadStatus(packageGuid);
    }

    @Override
    public CloudBuild getBuild(UUID buildGuid) {
        return client.builds()
                          .getBuild(buildGuid);
    }

    @Override
    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid) {
        return client.builds()
                          .getBuildsForApplication(applicationGuid);
    }

    @Override
    public List<CloudBuild> getBuildsForPackage(UUID packageGuid) {
        return client.builds()
                          .getBuildsForPackage(packageGuid);
    }

    @Override
    public CloudBuild createBuild(UUID packageGuid) {
        return client.builds()
                          .createBuild(packageGuid);
    }

    @Override
    public void bindDropletToApp(UUID dropletGuid, UUID applicationGuid) {
        client.droplets()
                   .bindDropletToApp(dropletGuid, applicationGuid);
    }

}
