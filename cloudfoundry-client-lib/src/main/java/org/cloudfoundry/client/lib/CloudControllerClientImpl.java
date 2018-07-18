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
import org.cloudfoundry.client.lib.domain.CloudUser;
import org.cloudfoundry.client.lib.domain.CrashesInfo;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Upload;
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
     * Construct client without a default org and space.
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

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, CloudSpace sessionSpace, boolean trustSelfSignedCerts) {
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
     * Construct a client with a default space name and org name.
     */

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, String orgName, String spaceName) {
        this(credentials, cloudControllerUrl, orgName, spaceName, null, false);
    }

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, String orgName, String spaceName,
        boolean trustSelfSignedCerts) {
        this(credentials, cloudControllerUrl, orgName, spaceName, null, trustSelfSignedCerts);
    }

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, String orgName, String spaceName,
        HttpProxyConfiguration httpProxyConfiguration) {
        this(credentials, cloudControllerUrl, orgName, spaceName, httpProxyConfiguration, false);
    }

    public CloudControllerClientImpl(CloudCredentials credentials, URL cloudControllerUrl, String orgName, String spaceName,
        HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
        Assert.notNull(cloudControllerUrl, "URL for cloud controller cannot be null");
        CloudControllerRestClientFactory cloudControllerClientFactory = new CloudControllerRestClientFactory(httpProxyConfiguration,
            trustSelfSignedCerts);
        this.cc = cloudControllerClientFactory.newCloudController(cloudControllerUrl, credentials, orgName, spaceName);
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
    public void associateAuditorWithSpace(String orgName, String spaceName) {
        cc.associateAuditorWithSpace(orgName, spaceName, null);
    }

    @Override
    public void associateAuditorWithSpace(String orgName, String spaceName, String userGuid) {
        cc.associateAuditorWithSpace(orgName, spaceName, userGuid);
    }

    @Override
    public void associateDeveloperWithSpace(String spaceName) {
        cc.associateDeveloperWithSpace(null, spaceName, null);
    }

    @Override
    public void associateDeveloperWithSpace(String orgName, String spaceName) {
        cc.associateDeveloperWithSpace(orgName, spaceName, null);
    }

    @Override
    public void associateDeveloperWithSpace(String orgName, String spaceName, String userGuid) {
        cc.associateDeveloperWithSpace(orgName, spaceName, userGuid);
    }

    @Override
    public void associateManagerWithSpace(String spaceName) {
        cc.associateManagerWithSpace(null, spaceName, null);
    }

    @Override
    public void associateManagerWithSpace(String orgName, String spaceName) {
        cc.associateManagerWithSpace(orgName, spaceName, null);
    }

    @Override
    public void associateManagerWithSpace(String orgName, String spaceName, String userGuid) {
        cc.associateManagerWithSpace(orgName, spaceName, userGuid);
    }

    @Override
    public void bindRunningSecurityGroup(String securityGroupName) {
        cc.bindRunningSecurityGroup(securityGroupName);
    }

    @Override
    public void bindSecurityGroup(String orgName, String spaceName, String securityGroupName) {
        cc.bindSecurityGroup(orgName, spaceName, securityGroupName);
    }

    @Override
    public void bindService(String appName, String serviceName) {
        cc.bindService(appName, serviceName);
    }

    @Override
    public void bindStagingSecurityGroup(String securityGroupName) {
        cc.bindStagingSecurityGroup(securityGroupName);
    }

    @Override
    public void createApplication(String appName, Staging staging, Integer memory, List<String> uris, List<String> serviceNames) {
        cc.createApplication(appName, staging, memory, uris, serviceNames);
    }

    @Override
    public void createApplication(String appName, Staging staging, Integer disk, Integer memory, List<String> uris,
        List<String> serviceNames) {
        cc.createApplication(appName, staging, disk, memory, uris, serviceNames);
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
    public void debugApplication(String appName, DebugMode mode) {
        cc.debugApplication(appName, mode);
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
    public void deleteApplication(String appName) {
        cc.deleteApplication(appName);
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
    public void deleteSpace(String spaceName) {
        cc.deleteSpace(spaceName);
    }

    @Override
    public CloudApplication getApplication(String appName) {
        return cc.getApplication(appName);
    }

    @Override
    public CloudApplication getApplication(String appName, boolean required) {
        return cc.getApplication(appName, required);
    }

    @Override
    public CloudApplication getApplication(UUID appGuid) {
        return cc.getApplication(appGuid);
    }

    @Override
    public CloudApplication getApplication(UUID appGuid, boolean required) {
        return cc.getApplication(appGuid, required);
    }

    @Override
    public Map<String, Object> getApplicationEnvironment(UUID appGuid) {
        return cc.getApplicationEnvironment(appGuid);
    }

    @Override
    public Map<String, Object> getApplicationEnvironment(String appName) {
        return cc.getApplicationEnvironment(appName);
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String appName) {
        return cc.getApplicationEvents(appName);
    }

    @Override
    public InstancesInfo getApplicationInstances(String appName) {
        return cc.getApplicationInstances(appName);
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication app) {
        return cc.getApplicationInstances(app);
    }

    @Override
    public ApplicationStats getApplicationStats(String appName) {
        return cc.getApplicationStats(appName);
    }

    @Override
    public List<CloudApplication> getApplications() {
        return cc.getApplications();
    }

    @Override
    public List<CloudApplication> getApplications(String inlineDepth) {
        return cc.getApplications(inlineDepth);
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
    public Map<String, String> getCrashLogs(String appName) {
        return cc.getCrashLogs(appName);
    }

    @Override
    public CrashesInfo getCrashes(String appName) {
        return cc.getCrashes(appName);
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
    public List<CloudDomain> getDomainsForOrg() {
        return cc.getDomainsForOrg();
    }

    @Override
    public List<CloudEvent> getEvents() {
        return cc.getEvents();
    }

    @Override
    public String getFile(String appName, int instanceIndex, String filePath) {
        return cc.getFile(appName, instanceIndex, filePath, 0, -1);
    }

    @Override
    public String getFile(String appName, int instanceIndex, String filePath, int startPosition) {
        Assert.isTrue(startPosition >= 0, startPosition + " is not a valid value for start position, it should be 0 or greater.");
        return cc.getFile(appName, instanceIndex, filePath, startPosition, -1);
    }

    @Override
    public String getFile(String appName, int instanceIndex, String filePath, int startPosition, int endPosition) {
        Assert.isTrue(startPosition >= 0, startPosition + " is not a valid value for start position, it should be 0 or greater.");
        Assert.isTrue(endPosition > startPosition, endPosition
            + " is not a valid value for end position, it should be greater than startPosition " + "which is " + startPosition + ".");
        return cc.getFile(appName, instanceIndex, filePath, startPosition, endPosition - 1);
    }

    // list services, un/provision services, modify instance

    @Override
    public String getFileTail(String appName, int instanceIndex, String filePath, int length) {
        Assert.isTrue(length > 0, length + " is not a valid value for length, it should be 1 or greater.");
        return cc.getFile(appName, instanceIndex, filePath, -1, length);
    }

    /**
     * @deprecated use {@link #streamLogs(String, ApplicationLogListener)} or {@link #getRecentLogs(String)}
     */
    @Override
    public Map<String, String> getLogs(String appName) {
        return cc.getLogs(appName);
    }

    @Override
    public CloudOrganization getOrganization(String orgName) {
        return cc.getOrganization(orgName);
    }

    @Override
    public CloudOrganization getOrganization(String orgName, boolean required) {
        return cc.getOrganization(orgName, required);
    }

    @Override
    public Map<String, CloudUser> getOrganizationUsers(String orgName) {
        return cc.getOrganizationUsers(orgName);
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
    public List<ApplicationLog> getRecentLogs(String appName) {
        return cc.getRecentLogs(appName);
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
    public List<UUID> getSpaceAuditors(UUID spaceGuid){
        return cc.getSpaceAuditors(spaceGuid);
    }

    @Override
    public List<UUID> getSpaceAuditors(String orgName, String spaceName) {
        return cc.getSpaceAuditors(orgName, spaceName);
    }

    @Override
    public List<UUID> getSpaceDevelopers(String spaceName) {
        return cc.getSpaceDevelopers(null, spaceName);
    }
    
    @Override
    public List<UUID> getSpaceDevelopers(UUID spaceGuid){
        return cc.getSpaceDevelopers(spaceGuid);
    }

    @Override
    public List<UUID> getSpaceDevelopers(String orgName, String spaceName) {
        return cc.getSpaceDevelopers(orgName, spaceName);
    }

    @Override
    public List<UUID> getSpaceManagers(String spaceName) {
        return cc.getSpaceManagers(null, spaceName);
    }
    
    @Override
    public List<UUID> getSpaceManagers(UUID spaceGuid){
        return cc.getSpaceManagers(spaceGuid);
    }

    @Override
    public List<UUID> getSpaceManagers(String orgName, String spaceName) {
        return cc.getSpaceManagers(orgName, spaceName);
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
    public void openFile(String appName, int instanceIndex, String filePath, ClientHttpResponseCallback callback) {
        cc.openFile(appName, instanceIndex, filePath, callback);
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
    public void rename(String appName, String newName) {
        cc.rename(appName, newName);
    }

    @Override
    public StartingInfo restartApplication(String appName) {
        return cc.restartApplication(appName);
    }

    @Override
    public void setQuotaToOrg(String orgName, String quotaName) {
        cc.setQuotaToOrg(orgName, quotaName);
    }

    @Override
    public void setResponseErrorHandler(ResponseErrorHandler errorHandler) {
        cc.setResponseErrorHandler(errorHandler);
    }

    @Override
    public StartingInfo startApplication(String appName) {
        return cc.startApplication(appName);
    }

    @Override
    public void stopApplication(String appName) {
        cc.stopApplication(appName);
    }

    @Override
    public StreamingLogToken streamLogs(String appName, ApplicationLogListener listener) {
        return cc.streamLogs(appName, listener);
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
    public void unbindSecurityGroup(String orgName, String spaceName, String securityGroupName) {
        cc.unbindSecurityGroup(orgName, spaceName, securityGroupName);
    }

    @Override
    public void unbindService(String appName, String serviceName) {
        cc.unbindService(appName, serviceName);
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
    public void updateApplicationDiskQuota(String appName, int disk) {
        cc.updateApplicationDiskQuota(appName, disk);
    }

    @Override
    public void updateApplicationEnv(String appName, Map<String, String> env) {
        cc.updateApplicationEnv(appName, env);
    }

    @Override
    public void updateApplicationEnv(String appName, List<String> env) {
        cc.updateApplicationEnv(appName, env);
    }

    @Override
    public void updateApplicationInstances(String appName, int instances) {
        cc.updateApplicationInstances(appName, instances);
    }

    @Override
    public void updateApplicationMemory(String appName, int memory) {
        cc.updateApplicationMemory(appName, memory);
    }

    @Override
    public void updateApplicationServices(String appName, List<String> services) {
        cc.updateApplicationServices(appName, services);
    }

    @Override
    public void updateApplicationStaging(String appName, Staging staging) {
        cc.updateApplicationStaging(appName, staging);
    }

    @Override
    public void updateApplicationUris(String appName, List<String> uris) {
        cc.updateApplicationUris(appName, uris);
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
    public void uploadApplication(String appName, String file) throws IOException {
        cc.uploadApplication(appName, new File(file), null);
    }

    @Override
    public void uploadApplication(String appName, File file) throws IOException {
        cc.uploadApplication(appName, file, null);
    }

    @Override
    public void uploadApplication(String appName, File file, UploadStatusCallback callback) throws IOException {
        cc.uploadApplication(appName, file, callback);
    }

    @Override
    public void uploadApplication(String appName, InputStream inputStream) throws IOException {
        cc.uploadApplication(appName, inputStream, null);
    }

    @Override
    public void uploadApplication(String appName, InputStream inputStream, UploadStatusCallback callback) throws IOException {
        cc.uploadApplication(appName, inputStream, callback);
    }

    @Override
    public void uploadApplication(String appName, ApplicationArchive archive) throws IOException {
        cc.uploadApplication(appName, archive, null);
    }

    @Override
    public void uploadApplication(String appName, ApplicationArchive archive, UploadStatusCallback callback) throws IOException {
        cc.uploadApplication(appName, archive, callback);
    }

    @Override
    public String asyncUploadApplication(String appName, File file) throws IOException {
        return cc.asyncUploadApplication(appName, file, null);
    }

    @Override
    public String asyncUploadApplication(String appName, File file, UploadStatusCallback callback) throws IOException {
        return cc.asyncUploadApplication(appName, file, callback);
    }

    @Override
    public String asyncUploadApplication(String appName, ApplicationArchive archive) throws IOException {
        return cc.asyncUploadApplication(appName, archive, null);
    }

    @Override
    public String asyncUploadApplication(String appName, ApplicationArchive archive, UploadStatusCallback callback) throws IOException {
        return cc.asyncUploadApplication(appName, archive, callback);
    }

    @Override
    public Upload getUploadStatus(String uploadToken) {
        return cc.getUploadStatus(uploadToken);
    }

}
