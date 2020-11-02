package com.sap.cloudfoundry.client.facade.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.v3.Metadata;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.web.reactive.function.client.WebClient;

import com.sap.cloudfoundry.client.facade.StartingInfo;
import com.sap.cloudfoundry.client.facade.UploadStatusCallback;
import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudBuild;
import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.CloudEvent;
import com.sap.cloudfoundry.client.facade.domain.CloudInfo;
import com.sap.cloudfoundry.client.facade.domain.CloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBinding;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceBroker;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceOffering;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.CloudStack;
import com.sap.cloudfoundry.client.facade.domain.CloudTask;
import com.sap.cloudfoundry.client.facade.domain.DockerInfo;
import com.sap.cloudfoundry.client.facade.domain.DropletInfo;
import com.sap.cloudfoundry.client.facade.domain.InstancesInfo;
import com.sap.cloudfoundry.client.facade.domain.Staging;
import com.sap.cloudfoundry.client.facade.domain.Upload;
import com.sap.cloudfoundry.client.facade.domain.UserRole;
import com.sap.cloudfoundry.client.facade.oauth2.OAuthClient;

/**
 * Interface defining operations available for the cloud controller REST client implementations
 *
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

    List<CloudEvent> getEventsByActee(UUID uuid);

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

    List<CloudSpace> getSpaces();

    List<CloudSpace> getSpaces(String organizationName);

    CloudStack getStack(String name);

    CloudStack getStack(String name, boolean required);

    List<CloudStack> getStacks();

    OAuth2AccessToken login();

    void logout();

    void rename(String applicationName, String newName);

    StartingInfo restartApplication(String applicationName);

    StartingInfo startApplication(String applicationName);

    void stopApplication(String applicationName);

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

    void updateServicePlan(String serviceName, String planName);

    void updateServiceParameters(String serviceName, Map<String, Object> parameters);

    void updateServiceTags(String serviceName, List<String> tags);

    void uploadApplication(String applicationName, Path file, UploadStatusCallback callback);

    void uploadApplication(String applicationName, InputStream inputStream, UploadStatusCallback callback) throws IOException;

    CloudPackage asyncUploadApplication(String applicationName, Path file, UploadStatusCallback callback);

    Upload getUploadStatus(UUID packageGuid);

    CloudTask getTask(UUID taskGuid);

    List<CloudTask> getTasks(String applicationName);

    CloudTask runTask(String applicationName, CloudTask task);

    CloudTask cancelTask(UUID taskGuid);

    CloudBuild createBuild(UUID packageGuid);

    CloudBuild getBuild(UUID packageGuid);

    void bindDropletToApp(UUID dropletGuid, UUID applicationGuid);

    List<CloudBuild> getBuildsForApplication(UUID applicationGuid);

    WebClient getWebClient();

    OAuthClient getOAuthClient();

    Map<String, Object> getServiceInstanceParameters(UUID guid);

    Map<String, Object> getServiceBindingParameters(UUID guid);

    List<CloudBuild> getBuildsForPackage(UUID packageGuid);

    List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector);

    List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector);

    void updateApplicationMetadata(UUID guid, Metadata metadata);

    void updateServiceInstanceMetadata(UUID guid, Metadata metadata);

    DropletInfo getCurrentDropletForApplication(UUID applicationGuid);

    CloudPackage getPackage(UUID packageGuid);

    List<CloudPackage> getPackagesForApplication(UUID applicationGuid);

    UserRole getUserRoleBySpaceGuidAndUserGuid(UUID spaceGuid, UUID userGuid);
}
