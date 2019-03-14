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

package org.cloudfoundry.client.lib;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.archive.ApplicationArchive;
import org.cloudfoundry.client.lib.domain.ApplicationLog;
import org.cloudfoundry.client.lib.domain.ApplicationStats;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudApplication.DebugMode;
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
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.CloudUser;
import org.cloudfoundry.client.lib.domain.CrashesInfo;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.domain.UploadToken;
import org.cloudfoundry.client.lib.rest.CloudControllerRestClient;
import org.cloudfoundry.client.lib.rest.CloudControllerRestClientFactory;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;

/**
 * A Java client to exercise the Cloud Foundry API.
 *
 * @author Ramnivas Laddad
 * @author A.B.Srinivasan
 * @author Jennifer Hickey
 * @author Dave Syer
 * @author Thomas Risberg
 * @author Alexander Orlov
 */
public class CloudControllerClientImpl implements CloudControllerClient {

    private CloudControllerRestClient cc;

    private CloudInfo info;

    /**
     * Construct client for anonymous user. Useful only to get to the '/info' endpoint.
     */

    public CloudControllerClientImpl(URL cloudControllerUrl) {
        this(null, cloudControllerUrl, null, (HttpProxyConfiguration) null, false);
    }

    public CloudControllerClientImpl(URL cloudControllerUrl, boolean trustSelfSignedCerts) {
        this(null, cloudControllerUrl, null, (HttpProxyConfiguration) null, trustSelfSignedCerts);
    }

    public CloudControllerClientImpl(URL cloudControllerUrl, HttpProxyConfiguration httpProxyConfiguration) {
        this(null, cloudControllerUrl, null, httpProxyConfiguration, false);
    }

    public CloudControllerClientImpl(URL cloudControllerUrl, HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
        this(null, cloudControllerUrl, null, httpProxyConfiguration, trustSelfSignedCerts);
    }

    /**
     * Construct client without a default organization and space.
     */

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl) {
        this(credentials, cloudControllerUrl, null, (HttpProxyConfiguration) null, false);
    }

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, boolean trustSelfSignedCerts) {
        this(credentials, cloudControllerUrl, null, (HttpProxyConfiguration) null, trustSelfSignedCerts);
    }

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, HttpProxyConfiguration httpProxyConfiguration) {
        this(credentials, cloudControllerUrl, null, httpProxyConfiguration, false);
    }

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, HttpProxyConfiguration httpProxyConfiguration,
        boolean trustSelfSignedCerts) {
        this(credentials, cloudControllerUrl, null, httpProxyConfiguration, trustSelfSignedCerts);
    }

    /**
     * Construct a client with a default CloudSpace.
     */

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, CloudSpace sessionSpace) {
        this(credentials, cloudControllerUrl, sessionSpace, null, false);
    }

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, CloudSpace sessionSpace,
        boolean trustSelfSignedCerts) {
        this(credentials, cloudControllerUrl, sessionSpace, null, trustSelfSignedCerts);
    }

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, CloudSpace sessionSpace,
        HttpProxyConfiguration httpProxyConfiguration) {
        this(credentials, cloudControllerUrl, sessionSpace, httpProxyConfiguration, false);
    }

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, CloudSpace sessionSpace,
        HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
        Assert.notNull(cloudControllerUrl, "URL for cloud controller cannot be null");
        CloudControllerRestClientFactory cloudControllerClientFactory = new CloudControllerRestClientFactory(httpProxyConfiguration,
            trustSelfSignedCerts);
        this.cc = cloudControllerClientFactory.newCloudController(cloudControllerUrl, credentials, sessionSpace);
    }

    /**
     * Construct a client with a default space name and organization name.
     */

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, String organizationName, String spaceName) {
        this(credentials, cloudControllerUrl, organizationName, spaceName, null, false);
    }

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, String organizationName, String spaceName,
        boolean trustSelfSignedCerts) {
        this(credentials, cloudControllerUrl, organizationName, spaceName, null, trustSelfSignedCerts);
    }

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, String organizationName, String spaceName,
        HttpProxyConfiguration httpProxyConfiguration) {
        this(credentials, cloudControllerUrl, organizationName, spaceName, httpProxyConfiguration, false);
    }

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, String organizationName, String spaceName,
        HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
        Assert.notNull(cloudControllerUrl, "URL for cloud controller cannot be null");
        CloudControllerRestClientFactory cloudControllerClientFactory = new CloudControllerRestClientFactory(httpProxyConfiguration,
            trustSelfSignedCerts);
        this.cc = cloudControllerClientFactory.newCloudController(cloudControllerUrl, credentials, organizationName, spaceName);
    }

    /**
     * Construct a client with a pre-configured CloudControllerClient
     */
    public CloudControllerClientImpl(CloudControllerRestClient cc) {
        this.cc = cc;
    }

    @Override
    public void addDomain(String domainName) {
        cc.addDomain(domainName);
    }

    @Override
    public void addRoute(String host, String domainName) {
        cc.addRoute(host, domainName);
    }

    @Override
    public void associateAuditorWithSpace(String spaceName) {
        cc.associateAuditorWithSpace(null, spaceName, null);
    }

    @Override
    public void associateAuditorWithSpace(String organizationName, String spaceName) {
        cc.associateAuditorWithSpace(organizationName, spaceName, null);
    }

    @Override
    public void associateAuditorWithSpace(String organizationName, String spaceName, String userGuid) {
        cc.associateAuditorWithSpace(organizationName, spaceName, userGuid);
    }

    @Override
    public void associateDeveloperWithSpace(String spaceName) {
        cc.associateDeveloperWithSpace(null, spaceName, null);
    }

    @Override
    public void associateDeveloperWithSpace(String organizationName, String spaceName) {
        cc.associateDeveloperWithSpace(organizationName, spaceName, null);
    }

    @Override
    public void associateDeveloperWithSpace(String organizationName, String spaceName, String userGuid) {
        cc.associateDeveloperWithSpace(organizationName, spaceName, userGuid);
    }

    @Override
    public void associateManagerWithSpace(String spaceName) {
        cc.associateManagerWithSpace(null, spaceName, null);
    }

    @Override
    public void associateManagerWithSpace(String organizationName, String spaceName) {
        cc.associateManagerWithSpace(organizationName, spaceName, null);
    }

    @Override
    public void associateManagerWithSpace(String organizationName, String spaceName, String userGuid) {
        cc.associateManagerWithSpace(organizationName, spaceName, userGuid);
    }

    @Override
    public void bindRunningSecurityGroup(String securityGroupName) {
        cc.bindRunningSecurityGroup(securityGroupName);
    }

    @Override
    public void bindSecurityGroup(String organizationName, String spaceName, String securityGroupName) {
        cc.bindSecurityGroup(organizationName, spaceName, securityGroupName);
    }

    @Override
    public void bindService(String applicationName, String serviceName) {
        cc.bindService(applicationName, serviceName);
    }

    @Override
    public void bindStagingSecurityGroup(String securityGroupName) {
        cc.bindStagingSecurityGroup(securityGroupName);
    }

    @Override
    public void createApplication(String applicationName, Staging staging, Integer memory, List<String> uris, List<String> serviceNames) {
        cc.createApplication(applicationName, staging, memory, uris, serviceNames);
    }

    @Override
    public void createApplication(String applicationName, Staging staging, Integer disk, Integer memory, List<String> uris,
        List<String> serviceNames, DockerInfo dockerInfo) {
        cc.createApplication(applicationName, staging, disk, memory, uris, serviceNames, dockerInfo);
    }

    @Override
    public void createQuota(CloudQuota quota) {
        cc.createQuota(quota);
    }

    @Override
    public void createSecurityGroup(CloudSecurityGroup securityGroup) {
        cc.createSecurityGroup(securityGroup);
    }

    @Override
    public void createSecurityGroup(String name, InputStream jsonRulesFile) {
        cc.createSecurityGroup(name, jsonRulesFile);
    }

    @Override
    public void createService(CloudService service) {
        cc.createService(service);
    }

    @Override
    public void createServiceBroker(CloudServiceBroker serviceBroker) {
        cc.createServiceBroker(serviceBroker);
    }

    @Override
    public void createServiceKey(String serviceName, String serviceKeyName, Map<String, Object> parameters) {
        cc.createServiceKey(serviceName, serviceKeyName, parameters);
    }

    @Override
    public void createSpace(String spaceName) {
        cc.createSpace(spaceName);
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials) {
        cc.createUserProvidedService(service, credentials);
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials, String syslogDrainUrl) {
        cc.createUserProvidedService(service, credentials, syslogDrainUrl);
    }

    @Override
    public void debugApplication(String applicationName, DebugMode mode) {
        cc.debugApplication(applicationName, mode);
    }

    @Override
    public void deleteAllApplications() {
        cc.deleteAllApplications();
    }

    @Override
    public void deleteAllServices() {
        cc.deleteAllServices();
    }

    @Override
    public void deleteApplication(String applicationName) {
        cc.deleteApplication(applicationName);
    }

    @Override
    public void deleteDomain(String domainName) {
        cc.deleteDomain(domainName);
    }

    @Override
    public List<CloudRoute> deleteOrphanedRoutes() {
        return cc.deleteOrphanedRoutes();
    }

    @Override
    public void deleteQuota(String quotaName) {
        cc.deleteQuota(quotaName);
    }

    @Override
    public void deleteRoute(String host, String domainName) {
        cc.deleteRoute(host, domainName);
    }

    @Override
    public void deleteSecurityGroup(String securityGroupName) {
        cc.deleteSecurityGroup(securityGroupName);
    }

    @Override
    public void deleteService(String service) {
        cc.deleteService(service);
    }

    @Override
    public void deleteServiceBroker(String name) {
        cc.deleteServiceBroker(name);
    }

    @Override
    public void deleteServiceKey(String service, String serviceKey) {
        cc.deleteServiceKey(service, serviceKey);
    }

    @Override
    public void deleteSpace(String spaceName) {
        cc.deleteSpace(spaceName);
    }

    @Override
    public CloudApplication getApplication(String applicationName) {
        return cc.getApplication(applicationName);
    }

    @Override
    public CloudApplication getApplication(String applicationName, boolean required) {
        return cc.getApplication(applicationName, required);
    }

    @Override
    public CloudApplication getApplication(UUID applicationGuid) {
        return cc.getApplication(applicationGuid);
    }

    @Override
    public CloudApplication getApplication(UUID applicationGuid, boolean required) {
        return cc.getApplication(applicationGuid, required);
    }

    @Override
    public Map<String, Object> getApplicationEnvironment(UUID applicationGuid) {
        return cc.getApplicationEnvironment(applicationGuid);
    }

    @Override
    public Map<String, Object> getApplicationEnvironment(String applicationName) {
        return cc.getApplicationEnvironment(applicationName);
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String applicationName) {
        return cc.getApplicationEvents(applicationName);
    }

    @Override
    public InstancesInfo getApplicationInstances(String applicationName) {
        return cc.getApplicationInstances(applicationName);
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication app) {
        return cc.getApplicationInstances(app);
    }

    @Override
    public ApplicationStats getApplicationStats(String applicationName) {
        return cc.getApplicationStats(applicationName);
    }

    @Override
    public List<CloudApplication> getApplications() {
        return cc.getApplications();
    }

    @Override
    public List<CloudApplication> getApplications(boolean fetchAdditionalInfo) {
        return cc.getApplications(fetchAdditionalInfo);
    }

    @Override
    public URL getCloudControllerUrl() {
        return cc.getCloudControllerUrl();
    }

    @Override
    public CloudInfo getCloudInfo() {
        if (info == null) {
            info = cc.getInfo();
        }
        return info;
    }

    /**
     * @deprecated use {@link #streamLogs(String, ApplicationLogListener)} or {@link #getRecentLogs(String)}
     */
    @Override
    public Map<String, String> getCrashLogs(String applicationName) {
        return cc.getCrashLogs(applicationName);
    }

    @Override
    public CrashesInfo getCrashes(String applicationName) {
        return cc.getCrashes(applicationName);
    }

    @Override
    public CloudDomain getDefaultDomain() {
        return cc.getDefaultDomain();
    }

    @Override
    public List<CloudDomain> getDomains() {
        return cc.getDomains();
    }

    @Override
    public List<CloudDomain> getDomainsForOrganization() {
        return cc.getDomainsForOrganization();
    }

    @Override
    public List<CloudEvent> getEvents() {
        return cc.getEvents();
    }

    @Override
    public String getFile(String applicationName, int instanceIndex, String filePath) {
        return cc.getFile(applicationName, instanceIndex, filePath, 0, -1);
    }

    @Override
    public String getFile(String applicationName, int instanceIndex, String filePath, int startPosition) {
        Assert.isTrue(startPosition >= 0, startPosition + " is not a valid value for start position, it should be 0 or greater.");
        return cc.getFile(applicationName, instanceIndex, filePath, startPosition, -1);
    }

    @Override
    public String getFile(String applicationName, int instanceIndex, String filePath, int startPosition, int endPosition) {
        Assert.isTrue(startPosition >= 0, startPosition + " is not a valid value for start position, it should be 0 or greater.");
        Assert.isTrue(endPosition > startPosition, endPosition
            + " is not a valid value for end position, it should be greater than startPosition " + "which is " + startPosition + ".");
        return cc.getFile(applicationName, instanceIndex, filePath, startPosition, endPosition - 1);
    }

    // list services, un/provision services, modify instance

    @Override
    public String getFileTail(String applicationName, int instanceIndex, String filePath, int length) {
        Assert.isTrue(length > 0, length + " is not a valid value for length, it should be 1 or greater.");
        return cc.getFile(applicationName, instanceIndex, filePath, -1, length);
    }

    /**
     * @deprecated use {@link #streamLogs(String, ApplicationLogListener)} or {@link #getRecentLogs(String)}
     */
    @Override
    public Map<String, String> getLogs(String applicationName) {
        return cc.getLogs(applicationName);
    }

    @Override
    public CloudOrganization getOrganization(String organizationName) {
        return cc.getOrganization(organizationName);
    }

    @Override
    public CloudOrganization getOrganization(String organizationName, boolean required) {
        return cc.getOrganization(organizationName, required);
    }

    @Override
    public Map<String, CloudUser> getOrganizationUsers(String organizationName) {
        return cc.getOrganizationUsers(organizationName);
    }

    @Override
    public List<CloudOrganization> getOrganizations() {
        return cc.getOrganizations();
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return cc.getPrivateDomains();
    }

    @Override
    public CloudQuota getQuota(String quotaName) {
        return cc.getQuota(quotaName);
    }

    @Override
    public CloudQuota getQuota(String quotaName, boolean required) {
        return cc.getQuota(quotaName, required);
    }

    @Override
    public List<CloudQuota> getQuotas() {
        return cc.getQuotas();
    }

    @Override
    public List<ApplicationLog> getRecentLogs(String applicationName) {
        return cc.getRecentLogs(applicationName);
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        return cc.getRoutes(domainName);
    }

    @Override
    public List<CloudSecurityGroup> getRunningSecurityGroups() {
        return cc.getRunningSecurityGroups();
    }

    @Override
    public CloudSecurityGroup getSecurityGroup(String securityGroupName) {
        return cc.getSecurityGroup(securityGroupName);
    }

    @Override
    public CloudSecurityGroup getSecurityGroup(String securityGroupName, boolean required) {
        return cc.getSecurityGroup(securityGroupName, required);
    }

    @Override
    public List<CloudSecurityGroup> getSecurityGroups() {
        return cc.getSecurityGroups();
    }

    @Override
    public CloudService getService(String service) {
        return cc.getService(service);
    }

    @Override
    public CloudService getService(String service, boolean required) {
        return cc.getService(service, required);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name) {
        return cc.getServiceBroker(name);
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        return cc.getServiceBroker(name, required);
    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        return cc.getServiceBrokers();
    }

    @Override
    public CloudServiceInstance getServiceInstance(String service) {
        return cc.getServiceInstance(service);
    }

    @Override
    public CloudServiceInstance getServiceInstance(String service, boolean required) {
        return cc.getServiceInstance(service, required);
    }

    @Override
    public List<ServiceKey> getServiceKeys(String serviceName) {
        return cc.getServiceKeys(serviceName);
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        return cc.getServiceOfferings();
    }

    @Override
    public List<CloudService> getServices() {
        return cc.getServices();
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return cc.getSharedDomains();
    }

    @Override
    public CloudSpace getSpace(String spaceName) {
        return cc.getSpace(spaceName);
    }

    @Override
    public CloudSpace getSpace(String spaceName, boolean required) {
        return cc.getSpace(spaceName, required);
    }

    @Override
    public List<UUID> getSpaceAuditors(String spaceName) {
        return cc.getSpaceAuditors(null, spaceName);
    }

    @Override
    public List<UUID> getSpaceAuditors(UUID spaceGuid) {
        return cc.getSpaceAuditors(spaceGuid);
    }

    @Override
    public List<UUID> getSpaceAuditors(String organizationName, String spaceName) {
        return cc.getSpaceAuditors(organizationName, spaceName);
    }

    @Override
    public List<UUID> getSpaceDevelopers(String spaceName) {
        return cc.getSpaceDevelopers(null, spaceName);
    }

    @Override
    public List<UUID> getSpaceDevelopers(UUID spaceGuid) {
        return cc.getSpaceDevelopers(spaceGuid);
    }

    @Override
    public List<UUID> getSpaceDevelopers(String organizationName, String spaceName) {
        return cc.getSpaceDevelopers(organizationName, spaceName);
    }

    @Override
    public List<UUID> getSpaceManagers(String spaceName) {
        return cc.getSpaceManagers(null, spaceName);
    }

    @Override
    public List<UUID> getSpaceManagers(UUID spaceGuid) {
        return cc.getSpaceManagers(spaceGuid);
    }

    @Override
    public List<UUID> getSpaceManagers(String organizationName, String spaceName) {
        return cc.getSpaceManagers(organizationName, spaceName);
    }

    @Override
    public List<CloudSpace> getSpaces() {
        return cc.getSpaces();
    }

    @Override
    public List<CloudSpace> getSpacesBoundToSecurityGroup(String securityGroupName) {
        return cc.getSpacesBoundToSecurityGroup(securityGroupName);
    }

    @Override
    public CloudStack getStack(String name) {
        return cc.getStack(name);
    }

    @Override
    public CloudStack getStack(String name, boolean required) {
        return cc.getStack(name, required);
    }

    @Override
    public List<CloudStack> getStacks() {
        return cc.getStacks();
    }

    @Override
    public String getStagingLogs(StartingInfo info, int offset) {
        return cc.getStagingLogs(info, offset);
    }

    @Override
    public List<CloudSecurityGroup> getStagingSecurityGroups() {
        return cc.getStagingSecurityGroups();
    }

    @Override
    public OAuth2AccessToken login() {
        return cc.login();
    }

    @Override
    public void logout() {
        cc.logout();
    }

    @Override
    public void openFile(String applicationName, int instanceIndex, String filePath, ClientHttpResponseCallback callback) {
        cc.openFile(applicationName, instanceIndex, filePath, callback);
    }

    @Override
    public void register(String email, String password) {
        cc.register(email, password);
    }

    @Override
    public void registerRestLogListener(RestLogCallback callBack) {
        cc.registerRestLogListener(callBack);
    }

    @Override
    public void removeDomain(String domainName) {
        cc.removeDomain(domainName);
    }

    @Override
    public void rename(String applicationName, String newName) {
        cc.rename(applicationName, newName);
    }

    @Override
    public StartingInfo restartApplication(String applicationName) {
        return cc.restartApplication(applicationName);
    }

    @Override
    public void setQuotaToOrganization(String organizationName, String quotaName) {
        cc.setQuotaToOrganization(organizationName, quotaName);
    }

    @Override
    public void setResponseErrorHandler(ResponseErrorHandler errorHandler) {
        cc.setResponseErrorHandler(errorHandler);
    }

    @Override
    public StartingInfo startApplication(String applicationName) {
        return cc.startApplication(applicationName);
    }

    @Override
    public void stopApplication(String applicationName) {
        cc.stopApplication(applicationName);
    }

    @Override
    public StreamingLogToken streamLogs(String applicationName, ApplicationLogListener listener) {
        return cc.streamLogs(applicationName, listener);
    }

    @Override
    public void unRegisterRestLogListener(RestLogCallback callBack) {
        cc.unRegisterRestLogListener(callBack);
    }

    @Override
    public void unbindRunningSecurityGroup(String securityGroupName) {
        cc.unbindRunningSecurityGroup(securityGroupName);
    }

    @Override
    public void unbindSecurityGroup(String organizationName, String spaceName, String securityGroupName) {
        cc.unbindSecurityGroup(organizationName, spaceName, securityGroupName);
    }

    @Override
    public void unbindService(String applicationName, String serviceName) {
        cc.unbindService(applicationName, serviceName);
    }

    @Override
    public void unbindStagingSecurityGroup(String securityGroupName) {
        cc.unbindStagingSecurityGroup(securityGroupName);
    }

    @Override
    public void unregister() {
        cc.unregister();
    }

    @Override
    public void updateApplicationDiskQuota(String applicationName, int disk) {
        cc.updateApplicationDiskQuota(applicationName, disk);
    }

    @Override
    public void updateApplicationEnv(String applicationName, Map<String, String> env) {
        cc.updateApplicationEnv(applicationName, env);
    }

    @Override
    public void updateApplicationEnv(String applicationName, List<String> env) {
        cc.updateApplicationEnv(applicationName, env);
    }

    @Override
    public void updateApplicationInstances(String applicationName, int instances) {
        cc.updateApplicationInstances(applicationName, instances);
    }

    @Override
    public void updateApplicationMemory(String applicationName, int memory) {
        cc.updateApplicationMemory(applicationName, memory);
    }

    @Override
    public void updateApplicationServices(String applicationName, List<String> services) {
        cc.updateApplicationServices(applicationName, services);
    }

    @Override
    public void updateApplicationStaging(String applicationName, Staging staging) {
        cc.updateApplicationStaging(applicationName, staging);
    }

    @Override
    public void updateApplicationUris(String applicationName, List<String> uris) {
        cc.updateApplicationUris(applicationName, uris);
    }

    @Override
    public void updatePassword(String newPassword) {
        cc.updatePassword(newPassword);
    }

    @Override
    public void updatePassword(CloudCredentials credentials, String newPassword) {
        cc.updatePassword(credentials, newPassword);
    }

    @Override
    public void updateQuota(CloudQuota quota, String name) {
        cc.updateQuota(quota, name);
    }

    @Override
    public void updateSecurityGroup(CloudSecurityGroup securityGroup) {
        cc.updateSecurityGroup(securityGroup);
    }

    @Override
    public void updateSecurityGroup(String name, InputStream jsonRulesFile) {
        cc.updateSecurityGroup(name, jsonRulesFile);
    }

    @Override
    public void updateServiceBroker(CloudServiceBroker serviceBroker) {
        cc.updateServiceBroker(serviceBroker);
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, boolean visibility) {
        cc.updateServicePlanVisibilityForBroker(name, visibility);
    }

    @Override
    public void uploadApplication(String applicationName, String file) throws IOException {
        cc.uploadApplication(applicationName, new File(file), null);
    }

    @Override
    public void uploadApplication(String applicationName, File file) throws IOException {
        cc.uploadApplication(applicationName, file, null);
    }

    @Override
    public void uploadApplication(String applicationName, File file, UploadStatusCallback callback) throws IOException {
        cc.uploadApplication(applicationName, file, callback);
    }

    @Override
    public void uploadApplication(String applicationName, InputStream inputStream) throws IOException {
        cc.uploadApplication(applicationName, inputStream, null);
    }

    @Override
    public void uploadApplication(String applicationName, InputStream inputStream, UploadStatusCallback callback) throws IOException {
        cc.uploadApplication(applicationName, inputStream, callback);
    }

    @Override
    public void uploadApplication(String applicationName, ApplicationArchive archive) throws IOException {
        cc.uploadApplication(applicationName, archive, null);
    }

    @Override
    public void uploadApplication(String applicationName, ApplicationArchive archive, UploadStatusCallback callback) throws IOException {
        cc.uploadApplication(applicationName, archive, callback);
    }

    @Override
    public UploadToken asyncUploadApplication(String applicationName, File file) throws IOException {
        return cc.asyncUploadApplication(applicationName, file, null);
    }

    @Override
    public UploadToken asyncUploadApplication(String applicationName, File file, UploadStatusCallback callback) throws IOException {
        return cc.asyncUploadApplication(applicationName, file, callback);
    }

    @Override
    public UploadToken asyncUploadApplication(String applicationName, ApplicationArchive archive) throws IOException {
        return cc.asyncUploadApplication(applicationName, archive, null);
    }

    @Override
    public UploadToken asyncUploadApplication(String applicationName, ApplicationArchive archive, UploadStatusCallback callback)
        throws IOException {
        return cc.asyncUploadApplication(applicationName, archive, callback);
    }

    @Override
    public Upload getUploadStatus(String uploadToken) {
        return cc.getUploadStatus(uploadToken);
    }

    @Override
    public CloudBuild createBuild(UUID packageGuid) {
        return cc.createBuild(packageGuid);
    }

    @Override
    public CloudBuild getBuild(UUID buildGuid) {
        return cc.getBuild(buildGuid);
    }

    @Override
    public boolean areTasksSupported() {
        return cc.areTasksSupported();
    }

    @Override
    public CloudTask getTask(UUID taskGuid) {
        return cc.getTask(taskGuid);
    }

    @Override
    public List<CloudTask> getTasks(String applicationName) {
        return cc.getTasks(applicationName);
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task) {
        return cc.runTask(applicationName, task);
    }

    @Override
    public CloudTask cancelTask(UUID taskGuid) {
        return cc.cancelTask(taskGuid);
    }

    @Override
    public void bindDropletToApp(UUID dropletGuid, UUID applicationGuid) {
        cc.bindDropletToApp(dropletGuid, applicationGuid);
    }

}
