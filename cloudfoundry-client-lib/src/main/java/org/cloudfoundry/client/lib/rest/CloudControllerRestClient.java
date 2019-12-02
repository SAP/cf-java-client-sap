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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.RestLogCallback;
import org.cloudfoundry.client.lib.StartingInfo;
import org.cloudfoundry.client.lib.UploadStatusCallback;
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
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Interface defining operations available for the cloud controller REST client implementations
 *
 * @author Thomas Risberg
 */
public interface CloudControllerRestClient {

    // User and Info methods

    void addDomain(String domainName);

    void addRoute(String host, String domainName, String path);

    void associateAuditorWithSpace(String organizationName, String spaceName, String userGuid);

    void associateDeveloperWithSpace(String organizationName, String spaceName, String userGuid);

    void associateManagerWithSpace(String organizationName, String spaceName, String userGuid);

    void bindRunningSecurityGroup(String securityGroupName);

    void bindSecurityGroup(String organizationName, String spaceName, String securityGroupName);

    void bindService(String applicationName, String serviceName);

    void bindService(String applicationName, String serviceName, Map<String, Object> parameters);

    void bindStagingSecurityGroup(String securityGroupName);

    void createApplication(String applicationName, Staging staging, Integer memory, List<String> uris, List<String> serviceNames);

    void createApplication(String applicationName, Staging staging, Integer disk, Integer memory, List<String> uris,
                           List<String> serviceNames, DockerInfo dockerInfo);

    // Service methods

    void createQuota(CloudQuota quota);

    void createSecurityGroup(CloudSecurityGroup securityGroup);

    void createSecurityGroup(String name, InputStream jsonRulesFile);

    void createService(CloudService service);

    void createServiceBroker(CloudServiceBroker serviceBroker);

    CloudServiceKey createServiceKey(String service, String serviceKey, Map<String, Object> parameters);

    void createSpace(String spaceName);

    void createUserProvidedService(CloudService service, Map<String, Object> credentials);

    void createUserProvidedService(CloudService service, Map<String, Object> credentials, String syslogDrainUrl);

    void deleteAllApplications();

    void deleteAllServices();

    void deleteApplication(String applicationName);

    void deleteDomain(String domainName);

    List<CloudRoute> deleteOrphanedRoutes();

    void deleteQuota(String quotaName);

    // App methods

    void deleteRoute(String host, String domainName, String path);

    void deleteSecurityGroup(String securityGroupName);

    void deleteService(String service);

    void deleteServiceBroker(String name);

    void deleteServiceKey(String service, String serviceKey);

    void deleteSpace(String spaceName);

    CloudApplication getApplication(String applicationName);

    CloudApplication getApplication(String applicationName, boolean required);

    CloudApplication getApplication(UUID applicationGuid);

    Map<String, String> getApplicationEnvironment(UUID applicationGuid);

    Map<String, String> getApplicationEnvironment(String applicationName);

    List<CloudEvent> getApplicationEvents(String applicationName);

    List<CloudEvent> getApplicationEvents(UUID applicationGuid);

    InstancesInfo getApplicationInstances(String applicationName);

    InstancesInfo getApplicationInstances(CloudApplication app);

    List<CloudApplication> getApplications();

    URL getControllerUrl();

    Map<String, String> getCrashLogs(String applicationName);

    CloudDomain getDefaultDomain();

    List<CloudDomain> getDomains();

    List<CloudDomain> getDomainsForOrganization();

    List<CloudEvent> getEvents();

    String getFile(String applicationName, int instanceIndex, String filePath, int startPosition, int endPosition);

    CloudInfo getInfo();

    Map<String, String> getLogs(String applicationName);

    CloudOrganization getOrganization(String organizationName);

    CloudOrganization getOrganization(String organizationName, boolean required);

    Map<String, CloudUser> getOrganizationUsers(String organizationName);

    List<CloudOrganization> getOrganizations();

    List<CloudDomain> getPrivateDomains();

    CloudQuota getQuota(String quotaName);

    CloudQuota getQuota(String quotaName, boolean required);

    List<CloudQuota> getQuotas();

    List<ApplicationLog> getRecentLogs(String applicationName);

    List<ApplicationLog> getRecentLogs(UUID applicationGuid);

    List<CloudRoute> getRoutes(String domainName);

    List<CloudSecurityGroup> getRunningSecurityGroups();

    CloudSecurityGroup getSecurityGroup(String securityGroupName);

    CloudSecurityGroup getSecurityGroup(String securityGroupName, boolean required);

    List<CloudSecurityGroup> getSecurityGroups();

    CloudService getService(String service);

    CloudService getService(String service, boolean required);

    CloudServiceBroker getServiceBroker(String name);

    CloudServiceBroker getServiceBroker(String name, boolean required);

    List<CloudServiceBroker> getServiceBrokers();

    CloudServiceInstance getServiceInstance(String serviceName);

    CloudServiceInstance getServiceInstance(String serviceName, boolean required);

    List<CloudServiceKey> getServiceKeys(String serviceName);

    List<CloudServiceOffering> getServiceOfferings();

    List<CloudService> getServices();

    List<CloudDomain> getSharedDomains();

    // Space management

    CloudSpace getSpace(UUID spaceGuid);

    CloudSpace getSpace(String organizationName, String spaceName);

    CloudSpace getSpace(String organizationName, String spaceName, boolean required);

    CloudSpace getSpace(String spaceName);

    CloudSpace getSpace(String spaceName, boolean required);

    List<UUID> getSpaceAuditors();

    List<UUID> getSpaceAuditors(String spaceName);

    List<UUID> getSpaceAuditors(String organizationName, String spaceName);

    List<UUID> getSpaceAuditors(UUID spaceGuid);

    List<UUID> getSpaceDevelopers();

    List<UUID> getSpaceDevelopers(String spaceName);

    List<UUID> getSpaceDevelopers(String organizationName, String spaceName);

    List<UUID> getSpaceDevelopers(UUID spaceGuid);

    List<UUID> getSpaceManagers();

    List<UUID> getSpaceManagers(String spaceName);

    List<UUID> getSpaceManagers(String organizationName, String spaceName);

    List<UUID> getSpaceManagers(UUID spaceGuid);

    List<CloudSpace> getSpaces();

    List<CloudSpace> getSpaces(String organizationName);

    List<CloudSpace> getSpacesBoundToSecurityGroup(String securityGroupName);

    CloudStack getStack(String name);

    CloudStack getStack(String name, boolean required);

    List<CloudStack> getStacks();

    List<CloudSecurityGroup> getStagingSecurityGroups();

    OAuth2AccessToken login();

    void logout();

    void register(String email, String password);

    void registerRestLogListener(RestLogCallback callBack);

    // Misc. utility methods

    void removeDomain(String domainName);

    void rename(String applicationName, String newName);

    StartingInfo restartApplication(String applicationName);

    void setQuotaToOrganization(String organizationName, String quotaName);

    void setResponseErrorHandler(ResponseErrorHandler errorHandler);

    StartingInfo startApplication(String applicationName);

    void stopApplication(String applicationName);

    void unRegisterRestLogListener(RestLogCallback callBack);

    void unbindRunningSecurityGroup(String securityGroupName);

    void unbindSecurityGroup(String organizationName, String spaceName, String securityGroupName);

    void unbindService(String applicationName, String serviceName);

    void unbindStagingSecurityGroup(String securityGroupName);

    void unregister();

    void updateApplicationDiskQuota(String applicationName, int disk);

    // Security Group Operations

    void updateApplicationEnv(String applicationName, Map<String, String> env);

    void updateApplicationInstances(String applicationName, int instances);

    void updateApplicationMemory(String applicationName, int memory);

    List<String> updateApplicationServices(String applicationName, Map<String, Map<String, Object>> serviceIdsWithBindingParameters);

    void updateApplicationStaging(String applicationName, Staging staging);

    void updateApplicationUris(String applicationName, List<String> uris);

    void updatePassword(String newPassword);

    void updatePassword(CloudCredentials credentials, String newPassword);

    void updateQuota(CloudQuota quota, String name);

    void updateSecurityGroup(CloudSecurityGroup securityGroup);

    void updateSecurityGroup(String name, InputStream jsonRulesFile);

    void updateServiceBroker(CloudServiceBroker serviceBroker);

    void updateServicePlanVisibilityForBroker(String name, boolean visibility);

    void uploadApplication(String applicationName, File file, UploadStatusCallback callback) throws IOException;

    void uploadApplication(String applicationName, InputStream inputStream, UploadStatusCallback callback) throws IOException;

    UploadToken asyncUploadApplication(String applicationName, File file, UploadStatusCallback callback) throws IOException;

    Upload getUploadStatus(UUID packageGuid);

    CloudTask getTask(UUID taskGuid);

    List<CloudTask> getTasks(String applicationName);

    CloudTask runTask(String applicationName, CloudTask task);

    CloudTask cancelTask(UUID taskGuid);

    CloudBuild createBuild(UUID packageGuid);

    CloudBuild getBuild(UUID packageGuid);

    void bindDropletToApp(UUID dropletGuid, UUID applicationGuid);

    List<CloudBuild> getBuildsForApplication(UUID applicationGuid);

    RestTemplate getRestTemplate();

    OAuthClient getOAuthClient();

    Map<String, Object> getServiceParameters(UUID guid);

    List<CloudBuild> getBuildsForPackage(UUID packageGuid);

}
