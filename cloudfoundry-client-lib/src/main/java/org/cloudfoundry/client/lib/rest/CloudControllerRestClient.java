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
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.cloudfoundry.client.lib.oauth2.OAuthClient;
import org.cloudfoundry.client.v3.Metadata;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Interface defining operations available for the cloud controller REST client implementations
 *
 * @author Thomas Risberg
 */
public interface CloudControllerRestClient {

    void addDomain(String domainName);

    void addRoute(String host, String domainName, String path);

    void bindServiceInstance(String applicationName, String serviceInstanceName);

    void bindServiceInstance(String applicationName, String serviceInstanceName, Map<String, Object> parameters);

    void createApplication(String applicationName, Staging staging, Integer memory, List<String> uris);

    void createApplication(String applicationName, Staging staging, Integer disk, Integer memory, List<String> uris, DockerInfo dockerInfo);

    void createServiceInstance(CloudServiceInstance serviceInstance);

    void createServiceBroker(CloudServiceBroker serviceBroker);

    CloudServiceKey createServiceKey(String serviceInstanceName, String serviceKeyName, Map<String, Object> parameters);

    void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance, Map<String, Object> credentials);

    void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance, Map<String, Object> credentials, String syslogDrainUrl);

    void deleteAllApplications();

    void deleteAllServiceInstances();

    void deleteApplication(String applicationName);

    void deleteDomain(String domainName);

    List<CloudRoute> deleteOrphanedRoutes();

    void deleteRoute(String host, String domainName, String path);

    void deleteServiceInstance(String serviceInstanceName);

    void deleteServiceInstance(CloudServiceInstance serviceInstance);

    void deleteServiceBroker(String name);

    void deleteServiceKey(String serviceInstanceName, String serviceKeyName);

    void deleteServiceKey(CloudServiceKey serviceKey);

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

    CloudDomain getDefaultDomain();

    List<CloudDomain> getDomains();

    List<CloudDomain> getDomainsForOrganization();

    List<CloudEvent> getEvents();

    CloudInfo getInfo();

    CloudOrganization getOrganization(String organizationName);

    CloudOrganization getOrganization(String organizationName, boolean required);

    List<CloudOrganization> getOrganizations();

    List<CloudDomain> getPrivateDomains();

    List<ApplicationLog> getRecentLogs(String applicationName);

    List<ApplicationLog> getRecentLogs(UUID applicationGuid);

    List<CloudRoute> getRoutes(String domainName);

    CloudServiceInstance getServiceInstance(String serviceInstanceName);

    CloudServiceInstance getServiceInstance(String serviceInstanceName, boolean required);

    List<CloudServiceBinding> getServiceBindings(UUID serviceInstanceGuid);

    CloudServiceBroker getServiceBroker(String name);

    CloudServiceBroker getServiceBroker(String name, boolean required);

    List<CloudServiceBroker> getServiceBrokers();

    List<CloudServiceKey> getServiceKeys(String serviceInstanceName);

    List<CloudServiceKey> getServiceKeys(CloudServiceInstance serviceInstance);

    List<CloudServiceOffering> getServiceOfferings();

    List<CloudServiceInstance> getServiceInstances();

    List<CloudDomain> getSharedDomains();

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

    CloudStack getStack(String name);

    CloudStack getStack(String name, boolean required);

    List<CloudStack> getStacks();

    OAuth2AccessToken login();

    void logout();

    void registerRestLogListener(RestLogCallback callBack);

    void rename(String applicationName, String newName);

    StartingInfo restartApplication(String applicationName);

    void setResponseErrorHandler(ResponseErrorHandler errorHandler);

    StartingInfo startApplication(String applicationName);

    void stopApplication(String applicationName);

    void unRegisterRestLogListener(RestLogCallback callBack);

    void unbindServiceInstance(String applicationName, String serviceInstanceName);

    void unbindServiceInstance(CloudApplication application, CloudServiceInstance serviceInstance);

    void updateApplicationDiskQuota(String applicationName, int disk);

    void updateApplicationEnv(String applicationName, Map<String, String> env);

    void updateApplicationInstances(String applicationName, int instances);

    void updateApplicationMemory(String applicationName, int memory);

    void updateApplicationStaging(String applicationName, Staging staging);

    void updateApplicationUris(String applicationName, List<String> uris);

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

    Map<String, Object> getServiceInstanceParameters(UUID guid);

    Map<String, Object> getServiceBindingParameters(UUID guid);

    List<CloudBuild> getBuildsForPackage(UUID packageGuid);

    List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector);

    List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector);

    void updateApplicationMetadata(UUID guid, Metadata metadata);

    void updateServiceInstanceMetadata(UUID guid, Metadata metadata);

}
