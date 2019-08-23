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
import java.util.function.Supplier;

import org.cloudfoundry.AbstractCloudFoundryException;
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
import org.cloudfoundry.client.lib.rest.ApplicationServicesUpdater;
import org.cloudfoundry.client.lib.rest.CloudControllerRestClient;
import org.cloudfoundry.client.lib.rest.CloudControllerRestClientFactory;
import org.cloudfoundry.client.lib.rest.ImmutableCloudControllerRestClientFactory;
import org.springframework.http.HttpStatus;
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

    private CloudControllerRestClient delegate;
    private CloudInfo info;

    /**
     * Construct client for anonymous user. Useful only to get to the '/info' endpoint.
     */

    public CloudControllerClientImpl(URL controllerUrl) {
        this(controllerUrl, null, null, (HttpProxyConfiguration) null, false);
    }

    public CloudControllerClientImpl(URL controllerUrl, boolean trustSelfSignedCerts) {
        this(controllerUrl, null, null, (HttpProxyConfiguration) null, trustSelfSignedCerts);
    }

    public CloudControllerClientImpl(URL controllerUrl, HttpProxyConfiguration httpProxyConfiguration) {
        this(controllerUrl, null, null, httpProxyConfiguration, false);
    }

    public CloudControllerClientImpl(URL controllerUrl, HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
        this(controllerUrl, null, null, httpProxyConfiguration, trustSelfSignedCerts);
    }

    /**
     * Construct client without a default organization and space.
     */

    public CloudControllerClientImpl(URL controllerUrl, CloudCredentials credentials) {
        this(controllerUrl, credentials, null, (HttpProxyConfiguration) null, false);
    }

    public CloudControllerClientImpl(URL controllerUrl, CloudCredentials credentials, boolean trustSelfSignedCerts) {
        this(controllerUrl, credentials, null, (HttpProxyConfiguration) null, trustSelfSignedCerts);
    }

    public CloudControllerClientImpl(URL controllerUrl, CloudCredentials credentials, HttpProxyConfiguration httpProxyConfiguration) {
        this(controllerUrl, credentials, null, httpProxyConfiguration, false);
    }

    public CloudControllerClientImpl(URL controllerUrl, CloudCredentials credentials, HttpProxyConfiguration httpProxyConfiguration,
                                     boolean trustSelfSignedCerts) {
        this(controllerUrl, credentials, null, httpProxyConfiguration, trustSelfSignedCerts);
    }

    /**
     * Construct a client with a default CloudSpace.
     */

    public CloudControllerClientImpl(URL controllerUrl, CloudCredentials credentials, CloudSpace target) {
        this(controllerUrl, credentials, target, null, false);
    }

    public CloudControllerClientImpl(URL controllerUrl, CloudCredentials credentials, CloudSpace target, boolean trustSelfSignedCerts) {
        this(controllerUrl, credentials, target, null, trustSelfSignedCerts);
    }

    public CloudControllerClientImpl(URL controllerUrl, CloudCredentials credentials, CloudSpace target,
                                     HttpProxyConfiguration httpProxyConfiguration) {
        this(controllerUrl, credentials, target, httpProxyConfiguration, false);
    }

    public CloudControllerClientImpl(URL controllerUrl, CloudCredentials credentials, CloudSpace target,
                                     HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
        Assert.notNull(controllerUrl, "URL for cloud controller cannot be null");
        CloudControllerRestClientFactory restClientFactory = ImmutableCloudControllerRestClientFactory.builder()
                                                                                                      .httpProxyConfiguration(httpProxyConfiguration)
                                                                                                      .shouldTrustSelfSignedCertificates(trustSelfSignedCerts)
                                                                                                      .build();
        this.delegate = restClientFactory.createClient(controllerUrl, credentials, target);
    }

    /**
     * Construct a client with a default space name and organization name.
     */

    public CloudControllerClientImpl(URL controllerUrl, CloudCredentials credentials, String organizationName, String spaceName) {
        this(controllerUrl, credentials, organizationName, spaceName, null, false);
    }

    public CloudControllerClientImpl(URL controllerUrl, CloudCredentials credentials, String organizationName, String spaceName,
                                     boolean trustSelfSignedCerts) {
        this(controllerUrl, credentials, organizationName, spaceName, null, trustSelfSignedCerts);
    }

    public CloudControllerClientImpl(URL controllerUrl, CloudCredentials credentials, String organizationName, String spaceName,
                                     HttpProxyConfiguration httpProxyConfiguration) {
        this(controllerUrl, credentials, organizationName, spaceName, httpProxyConfiguration, false);
    }

    public CloudControllerClientImpl(URL controllerUrl, CloudCredentials credentials, String organizationName, String spaceName,
                                     HttpProxyConfiguration httpProxyConfiguration, boolean trustSelfSignedCerts) {
        Assert.notNull(controllerUrl, "URL for cloud controller cannot be null");
        CloudControllerRestClientFactory restClientFactory = ImmutableCloudControllerRestClientFactory.builder()
                                                                                                      .httpProxyConfiguration(httpProxyConfiguration)
                                                                                                      .shouldTrustSelfSignedCertificates(trustSelfSignedCerts)
                                                                                                      .build();
        this.delegate = restClientFactory.createClient(controllerUrl, credentials, organizationName, spaceName);
    }

    /**
     * Construct a client with a pre-configured CloudControllerClient
     */
    public CloudControllerClientImpl(CloudControllerRestClient delegate) {
        this.delegate = delegate;
    }

    @Override
    public void addDomain(String domainName) {
        handleExceptions(() -> delegate.addDomain(domainName));
    }

    @Override
    public void addRoute(String host, String domainName) {
        handleExceptions(() -> delegate.addRoute(host, domainName));
    }

    @Override
    public void associateAuditorWithSpace(String spaceName) {
        handleExceptions(() -> delegate.associateAuditorWithSpace(null, spaceName, null));
    }

    @Override
    public void associateAuditorWithSpace(String organizationName, String spaceName) {
        handleExceptions(() -> delegate.associateAuditorWithSpace(organizationName, spaceName, null));
    }

    @Override
    public void associateAuditorWithSpace(String organizationName, String spaceName, String userGuid) {
        handleExceptions(() -> delegate.associateAuditorWithSpace(organizationName, spaceName, userGuid));
    }

    @Override
    public void associateDeveloperWithSpace(String spaceName) {
        handleExceptions(() -> delegate.associateDeveloperWithSpace(null, spaceName, null));
    }

    @Override
    public void associateDeveloperWithSpace(String organizationName, String spaceName) {
        handleExceptions(() -> delegate.associateDeveloperWithSpace(organizationName, spaceName, null));
    }

    @Override
    public void associateDeveloperWithSpace(String organizationName, String spaceName, String userGuid) {
        handleExceptions(() -> delegate.associateDeveloperWithSpace(organizationName, spaceName, userGuid));
    }

    @Override
    public void associateManagerWithSpace(String spaceName) {
        handleExceptions(() -> delegate.associateManagerWithSpace(null, spaceName, null));
    }

    @Override
    public void associateManagerWithSpace(String organizationName, String spaceName) {
        handleExceptions(() -> delegate.associateManagerWithSpace(organizationName, spaceName, null));
    }

    @Override
    public void associateManagerWithSpace(String organizationName, String spaceName, String userGuid) {
        handleExceptions(() -> delegate.associateManagerWithSpace(organizationName, spaceName, userGuid));
    }

    @Override
    public void bindRunningSecurityGroup(String securityGroupName) {
        handleExceptions(() -> delegate.bindRunningSecurityGroup(securityGroupName));
    }

    @Override
    public void bindSecurityGroup(String organizationName, String spaceName, String securityGroupName) {
        handleExceptions(() -> delegate.bindSecurityGroup(organizationName, spaceName, securityGroupName));
    }

    @Override
    public void bindService(String applicationName, String serviceName) {
        handleExceptions(() -> delegate.bindService(applicationName, serviceName));
    }

    @Override
    public void bindService(String applicationName, String serviceName, Map<String, Object> parameters,
                            ApplicationServicesUpdateCallback updateServicesCallback) {
        try {
            handleExceptions(() -> delegate.bindService(applicationName, serviceName, parameters));
        } catch (CloudOperationException e) {
            updateServicesCallback.onError(e, applicationName, serviceName);
        }
    }

    @Override
    public void bindStagingSecurityGroup(String securityGroupName) {
        handleExceptions(() -> delegate.bindStagingSecurityGroup(securityGroupName));
    }

    @Override
    public void createApplication(String applicationName, Staging staging, Integer memory, List<String> uris, List<String> serviceNames) {
        handleExceptions(() -> delegate.createApplication(applicationName, staging, memory, uris, serviceNames));
    }

    @Override
    public void createApplication(String applicationName, Staging staging, Integer disk, Integer memory, List<String> uris,
                                  List<String> serviceNames, DockerInfo dockerInfo) {
        handleExceptions(() -> delegate.createApplication(applicationName, staging, disk, memory, uris, serviceNames, dockerInfo));
    }

    @Override
    public void createQuota(CloudQuota quota) {
        handleExceptions(() -> delegate.createQuota(quota));
    }

    @Override
    public void createSecurityGroup(CloudSecurityGroup securityGroup) {
        handleExceptions(() -> delegate.createSecurityGroup(securityGroup));
    }

    @Override
    public void createSecurityGroup(String name, InputStream jsonRulesFile) {
        handleExceptions(() -> delegate.createSecurityGroup(name, jsonRulesFile));
    }

    @Override
    public void createService(CloudService service) {
        handleExceptions(() -> delegate.createService(service));
    }

    @Override
    public void createServiceBroker(CloudServiceBroker serviceBroker) {
        handleExceptions(() -> delegate.createServiceBroker(serviceBroker));
    }

    @Override
    public void createServiceKey(String serviceName, String serviceKeyName, Map<String, Object> parameters) {
        handleExceptions(() -> delegate.createServiceKey(serviceName, serviceKeyName, parameters));
    }

    @Override
    public void createSpace(String spaceName) {
        handleExceptions(() -> delegate.createSpace(spaceName));
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials) {
        handleExceptions(() -> delegate.createUserProvidedService(service, credentials));
    }

    @Override
    public void createUserProvidedService(CloudService service, Map<String, Object> credentials, String syslogDrainUrl) {
        handleExceptions(() -> delegate.createUserProvidedService(service, credentials, syslogDrainUrl));
    }

    @Override
    public void deleteAllApplications() {
        handleExceptions(() -> delegate.deleteAllApplications());
    }

    @Override
    public void deleteAllServices() {
        handleExceptions(() -> delegate.deleteAllServices());
    }

    @Override
    public void deleteApplication(String applicationName) {
        handleExceptions(() -> delegate.deleteApplication(applicationName));
    }

    @Override
    public void deleteDomain(String domainName) {
        handleExceptions(() -> delegate.deleteDomain(domainName));
    }

    @Override
    public List<CloudRoute> deleteOrphanedRoutes() {
        return handleExceptions(() -> delegate.deleteOrphanedRoutes());
    }

    @Override
    public void deleteQuota(String quotaName) {
        handleExceptions(() -> delegate.deleteQuota(quotaName));
    }

    @Override
    public void deleteRoute(String host, String domainName) {
        handleExceptions(() -> delegate.deleteRoute(host, domainName));
    }

    @Override
    public void deleteSecurityGroup(String securityGroupName) {
        handleExceptions(() -> delegate.deleteSecurityGroup(securityGroupName));
    }

    @Override
    public void deleteService(String service) {
        handleExceptions(() -> delegate.deleteService(service));
    }

    @Override
    public void deleteServiceBroker(String name) {
        handleExceptions(() -> delegate.deleteServiceBroker(name));
    }

    @Override
    public void deleteServiceKey(String service, String serviceKey) {
        handleExceptions(() -> delegate.deleteServiceKey(service, serviceKey));
    }

    @Override
    public void deleteSpace(String spaceName) {
        handleExceptions(() -> delegate.deleteSpace(spaceName));
    }

    @Override
    public CloudApplication getApplication(String applicationName) {
        return handleExceptions(() -> delegate.getApplication(applicationName));
    }

    @Override
    public CloudApplication getApplication(String applicationName, boolean required) {
        return handleExceptions(() -> delegate.getApplication(applicationName, required));
    }

    @Override
    public CloudApplication getApplication(UUID applicationGuid) {
        return handleExceptions(() -> delegate.getApplication(applicationGuid));
    }

    @Override
    public Map<String, String> getApplicationEnvironment(UUID applicationGuid) {
        return handleExceptions(() -> delegate.getApplicationEnvironment(applicationGuid));
    }

    @Override
    public Map<String, String> getApplicationEnvironment(String applicationName) {
        return handleExceptions(() -> delegate.getApplicationEnvironment(applicationName));
    }

    @Override
    public List<CloudEvent> getApplicationEvents(String applicationName) {
        return handleExceptions(() -> delegate.getApplicationEvents(applicationName));
    }

    @Override
    public InstancesInfo getApplicationInstances(String applicationName) {
        return handleExceptions(() -> delegate.getApplicationInstances(applicationName));
    }

    @Override
    public InstancesInfo getApplicationInstances(CloudApplication app) {
        return handleExceptions(() -> delegate.getApplicationInstances(app));
    }

    @Override
    public List<CloudApplication> getApplications() {
        return handleExceptions(() -> delegate.getApplications());
    }

    @Override
    public URL getCloudControllerUrl() {
        return delegate.getControllerUrl();
    }

    @Override
    public CloudInfo getCloudInfo() {
        if (info == null) {
            info = handleExceptions(() -> delegate.getInfo());
        }
        return info;
    }

    /**
     * @deprecated use {@link #streamLogs(String, ApplicationLogListener)} or {@link #getRecentLogs(String)}
     */
    @Deprecated
    @Override
    public Map<String, String> getCrashLogs(String applicationName) {
        return handleExceptions(() -> delegate.getCrashLogs(applicationName));
    }

    @Override
    public CrashesInfo getCrashes(String applicationName) {
        return handleExceptions(() -> delegate.getCrashes(applicationName));
    }

    @Override
    public CloudDomain getDefaultDomain() {
        return handleExceptions(() -> delegate.getDefaultDomain());
    }

    @Override
    public List<CloudDomain> getDomains() {
        return handleExceptions(() -> delegate.getDomains());
    }

    @Override
    public List<CloudDomain> getDomainsForOrganization() {
        return handleExceptions(() -> delegate.getDomainsForOrganization());
    }

    @Override
    public List<CloudEvent> getEvents() {
        return handleExceptions(() -> delegate.getEvents());
    }

    @Override
    public String getFile(String applicationName, int instanceIndex, String filePath) {
        return handleExceptions(() -> delegate.getFile(applicationName, instanceIndex, filePath, 0, -1));
    }

    @Override
    public String getFile(String applicationName, int instanceIndex, String filePath, int startPosition) {
        Assert.isTrue(startPosition >= 0, startPosition + " is not a valid value for start position, it should be 0 or greater.");
        return handleExceptions(() -> delegate.getFile(applicationName, instanceIndex, filePath, startPosition, -1));
    }

    @Override
    public String getFile(String applicationName, int instanceIndex, String filePath, int startPosition, int endPosition) {
        Assert.isTrue(startPosition >= 0, startPosition + " is not a valid value for start position, it should be 0 or greater.");
        Assert.isTrue(endPosition > startPosition, endPosition
            + " is not a valid value for end position, it should be greater than startPosition " + "which is " + startPosition + ".");
        return handleExceptions(() -> delegate.getFile(applicationName, instanceIndex, filePath, startPosition, endPosition - 1));
    }

    // list services, un/provision services, modify instance

    @Override
    public String getFileTail(String applicationName, int instanceIndex, String filePath, int length) {
        Assert.isTrue(length > 0, length + " is not a valid value for length, it should be 1 or greater.");
        return handleExceptions(() -> delegate.getFile(applicationName, instanceIndex, filePath, -1, length));
    }

    /**
     * @deprecated use {@link #streamLogs(String, ApplicationLogListener)} or {@link #getRecentLogs(String)}
     */
    @Deprecated
    @Override
    public Map<String, String> getLogs(String applicationName) {
        return handleExceptions(() -> delegate.getLogs(applicationName));
    }

    @Override
    public CloudOrganization getOrganization(String organizationName) {
        return handleExceptions(() -> delegate.getOrganization(organizationName));
    }

    @Override
    public CloudOrganization getOrganization(String organizationName, boolean required) {
        return handleExceptions(() -> delegate.getOrganization(organizationName, required));
    }

    @Override
    public Map<String, CloudUser> getOrganizationUsers(String organizationName) {
        return handleExceptions(() -> delegate.getOrganizationUsers(organizationName));
    }

    @Override
    public List<CloudOrganization> getOrganizations() {
        return handleExceptions(() -> delegate.getOrganizations());
    }

    @Override
    public List<CloudDomain> getPrivateDomains() {
        return handleExceptions(() -> delegate.getPrivateDomains());
    }

    @Override
    public CloudQuota getQuota(String quotaName) {
        return handleExceptions(() -> delegate.getQuota(quotaName));
    }

    @Override
    public CloudQuota getQuota(String quotaName, boolean required) {
        return handleExceptions(() -> delegate.getQuota(quotaName, required));
    }

    @Override
    public List<CloudQuota> getQuotas() {
        return handleExceptions(() -> delegate.getQuotas());
    }

    @Override
    public List<ApplicationLog> getRecentLogs(String applicationName) {
        return handleExceptions(() -> delegate.getRecentLogs(applicationName));
    }

    @Override
    public List<ApplicationLog> getRecentLogs(UUID applicationGuid) {
        return handleExceptions(() -> delegate.getRecentLogs(applicationGuid));
    }

    @Override
    public List<CloudRoute> getRoutes(String domainName) {
        return handleExceptions(() -> delegate.getRoutes(domainName));
    }

    @Override
    public List<CloudSecurityGroup> getRunningSecurityGroups() {
        return handleExceptions(() -> delegate.getRunningSecurityGroups());
    }

    @Override
    public CloudSecurityGroup getSecurityGroup(String securityGroupName) {
        return handleExceptions(() -> delegate.getSecurityGroup(securityGroupName));
    }

    @Override
    public CloudSecurityGroup getSecurityGroup(String securityGroupName, boolean required) {
        return handleExceptions(() -> delegate.getSecurityGroup(securityGroupName, required));
    }

    @Override
    public List<CloudSecurityGroup> getSecurityGroups() {
        return handleExceptions(() -> delegate.getSecurityGroups());
    }

    @Override
    public CloudService getService(String service) {
        return handleExceptions(() -> delegate.getService(service));
    }

    @Override
    public CloudService getService(String service, boolean required) {
        return handleExceptions(() -> delegate.getService(service, required));
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name) {
        return handleExceptions(() -> delegate.getServiceBroker(name));
    }

    @Override
    public CloudServiceBroker getServiceBroker(String name, boolean required) {
        return handleExceptions(() -> delegate.getServiceBroker(name, required));
    }

    @Override
    public List<CloudServiceBroker> getServiceBrokers() {
        return handleExceptions(() -> delegate.getServiceBrokers());
    }

    @Override
    public CloudServiceInstance getServiceInstance(String service) {
        return handleExceptions(() -> delegate.getServiceInstance(service));
    }

    @Override
    public Map<String, Object> getServiceParameters(UUID guid) {
        return handleExceptions(() -> delegate.getServiceParameters(guid));
    }

    @Override
    public CloudServiceInstance getServiceInstance(String service, boolean required) {
        return handleExceptions(() -> delegate.getServiceInstance(service, required));
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(String serviceName) {
        return handleExceptions(() -> delegate.getServiceKeys(serviceName));
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        return handleExceptions(() -> delegate.getServiceOfferings());
    }

    @Override
    public List<CloudService> getServices() {
        return handleExceptions(() -> delegate.getServices());
    }

    @Override
    public List<CloudDomain> getSharedDomains() {
        return handleExceptions(() -> delegate.getSharedDomains());
    }

    @Override
    public CloudSpace getSpace(UUID spaceGuid) {
        return handleExceptions(() -> delegate.getSpace(spaceGuid));
    }

    @Override
    public CloudSpace getSpace(String organizationName, String spaceName) {
        return handleExceptions(() -> delegate.getSpace(organizationName, spaceName));
    }

    @Override
    public CloudSpace getSpace(String organizationName, String spaceName, boolean required) {
        return handleExceptions(() -> delegate.getSpace(organizationName, spaceName, required));
    }

    @Override
    public CloudSpace getSpace(String spaceName) {
        return handleExceptions(() -> delegate.getSpace(spaceName));
    }

    @Override
    public CloudSpace getSpace(String spaceName, boolean required) {
        return handleExceptions(() -> delegate.getSpace(spaceName, required));
    }

    @Override
    public List<UUID> getSpaceAuditors() {
        return handleExceptions(() -> delegate.getSpaceAuditors());
    }

    @Override
    public List<UUID> getSpaceAuditors(String spaceName) {
        return handleExceptions(() -> delegate.getSpaceAuditors(spaceName));
    }

    @Override
    public List<UUID> getSpaceAuditors(String organizationName, String spaceName) {
        return handleExceptions(() -> delegate.getSpaceAuditors(organizationName, spaceName));
    }

    @Override
    public List<UUID> getSpaceAuditors(UUID spaceGuid) {
        return handleExceptions(() -> delegate.getSpaceAuditors(spaceGuid));
    }

    @Override
    public List<UUID> getSpaceDevelopers() {
        return handleExceptions(() -> delegate.getSpaceDevelopers());
    }

    @Override
    public List<UUID> getSpaceDevelopers(String spaceName) {
        return handleExceptions(() -> delegate.getSpaceDevelopers(spaceName));
    }

    @Override
    public List<UUID> getSpaceDevelopers(String organizationName, String spaceName) {
        return handleExceptions(() -> delegate.getSpaceDevelopers(organizationName, spaceName));
    }

    @Override
    public List<UUID> getSpaceDevelopers(UUID spaceGuid) {
        return handleExceptions(() -> delegate.getSpaceDevelopers(spaceGuid));
    }

    @Override
    public List<UUID> getSpaceManagers() {
        return handleExceptions(() -> delegate.getSpaceManagers());
    }

    @Override
    public List<UUID> getSpaceManagers(String spaceName) {
        return handleExceptions(() -> delegate.getSpaceManagers(spaceName));
    }

    @Override
    public List<UUID> getSpaceManagers(String organizationName, String spaceName) {
        return handleExceptions(() -> delegate.getSpaceManagers(organizationName, spaceName));
    }

    @Override
    public List<UUID> getSpaceManagers(UUID spaceGuid) {
        return handleExceptions(() -> delegate.getSpaceManagers(spaceGuid));
    }

    @Override
    public List<CloudSpace> getSpaces() {
        return handleExceptions(() -> delegate.getSpaces());
    }

    @Override
    public List<CloudSpace> getSpaces(String organizationName) {
        return handleExceptions(() -> delegate.getSpaces(organizationName));
    }

    @Override
    public List<CloudSpace> getSpacesBoundToSecurityGroup(String securityGroupName) {
        return handleExceptions(() -> delegate.getSpacesBoundToSecurityGroup(securityGroupName));
    }

    @Override
    public CloudStack getStack(String name) {
        return handleExceptions(() -> delegate.getStack(name));
    }

    @Override
    public CloudStack getStack(String name, boolean required) {
        return handleExceptions(() -> delegate.getStack(name, required));
    }

    @Override
    public List<CloudStack> getStacks() {
        return handleExceptions(() -> delegate.getStacks());
    }

    @Override
    public String getStagingLogs(StartingInfo info, int offset) {
        return handleExceptions(() -> delegate.getStagingLogs(info, offset));
    }

    @Override
    public List<CloudSecurityGroup> getStagingSecurityGroups() {
        return handleExceptions(() -> delegate.getStagingSecurityGroups());
    }

    @Override
    public OAuth2AccessToken login() {
        return handleExceptions(() -> delegate.login());
    }

    @Override
    public void logout() {
        handleExceptions(() -> delegate.logout());
    }

    @Override
    public void openFile(String applicationName, int instanceIndex, String filePath, ClientHttpResponseCallback callback) {
        handleExceptions(() -> delegate.openFile(applicationName, instanceIndex, filePath, callback));
    }

    @Override
    public void register(String email, String password) {
        handleExceptions(() -> delegate.register(email, password));
    }

    @Override
    public void registerRestLogListener(RestLogCallback callBack) {
        delegate.registerRestLogListener(callBack);
    }

    @Override
    public void removeDomain(String domainName) {
        handleExceptions(() -> delegate.removeDomain(domainName));
    }

    @Override
    public void rename(String applicationName, String newName) {
        handleExceptions(() -> delegate.rename(applicationName, newName));
    }

    @Override
    public StartingInfo restartApplication(String applicationName) {
        return handleExceptions(() -> delegate.restartApplication(applicationName));
    }

    @Override
    public void setQuotaToOrganization(String organizationName, String quotaName) {
        handleExceptions(() -> delegate.setQuotaToOrganization(organizationName, quotaName));
    }

    @Override
    public void setResponseErrorHandler(ResponseErrorHandler errorHandler) {
        delegate.setResponseErrorHandler(errorHandler);
    }

    @Override
    public StartingInfo startApplication(String applicationName) {
        return handleExceptions(() -> delegate.startApplication(applicationName));
    }

    @Override
    public void stopApplication(String applicationName) {
        handleExceptions(() -> delegate.stopApplication(applicationName));
    }

    @Override
    public StreamingLogToken streamLogs(String applicationName, ApplicationLogListener listener) {
        return handleExceptions(() -> delegate.streamLogs(applicationName, listener));
    }

    @Override
    public void unRegisterRestLogListener(RestLogCallback callBack) {
        delegate.unRegisterRestLogListener(callBack);
    }

    @Override
    public void unbindRunningSecurityGroup(String securityGroupName) {
        handleExceptions(() -> delegate.unbindRunningSecurityGroup(securityGroupName));
    }

    @Override
    public void unbindSecurityGroup(String organizationName, String spaceName, String securityGroupName) {
        handleExceptions(() -> delegate.unbindSecurityGroup(organizationName, spaceName, securityGroupName));
    }

    @Override
    public void unbindService(String applicationName, String serviceName,
                              ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        try {
            handleExceptions(() -> delegate.unbindService(applicationName, serviceName));
        } catch (CloudOperationException e) {
            applicationServicesUpdateCallback.onError(e, applicationName, serviceName);
        }
    }

    @Override
    public void unbindService(String applicationName, String serviceName) {
        handleExceptions(() -> delegate.unbindService(applicationName, serviceName));
    }

    @Override
    public void unbindStagingSecurityGroup(String securityGroupName) {
        handleExceptions(() -> delegate.unbindStagingSecurityGroup(securityGroupName));
    }

    @Override
    public void unregister() {
        handleExceptions(() -> delegate.unregister());
    }

    @Override
    public void updateApplicationDiskQuota(String applicationName, int disk) {
        handleExceptions(() -> delegate.updateApplicationDiskQuota(applicationName, disk));
    }

    @Override
    public void updateApplicationEnv(String applicationName, Map<String, String> env) {
        handleExceptions(() -> delegate.updateApplicationEnv(applicationName, env));
    }

    @Override
    public void updateApplicationInstances(String applicationName, int instances) {
        handleExceptions(() -> delegate.updateApplicationInstances(applicationName, instances));
    }

    @Override
    public void updateApplicationMemory(String applicationName, int memory) {
        handleExceptions(() -> delegate.updateApplicationMemory(applicationName, memory));
    }

    @Override
    public List<String> updateApplicationServices(String applicationName,
                                                  Map<String, Map<String, Object>> serviceNamesWithBindingParameters,
                                                  ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        ApplicationServicesUpdater applicationServicesUpdater = new ApplicationServicesUpdater(this);
        return handleExceptions(() -> applicationServicesUpdater.updateApplicationServices(applicationName,
                                                                                           serviceNamesWithBindingParameters,
                                                                                           applicationServicesUpdateCallback));
    }

    @Override
    public void updateApplicationStaging(String applicationName, Staging staging) {
        handleExceptions(() -> delegate.updateApplicationStaging(applicationName, staging));
    }

    @Override
    public void updateApplicationUris(String applicationName, List<String> uris) {
        handleExceptions(() -> delegate.updateApplicationUris(applicationName, uris));
    }

    @Override
    public void updatePassword(String newPassword) {
        handleExceptions(() -> delegate.updatePassword(newPassword));
    }

    @Override
    public void updatePassword(CloudCredentials credentials, String newPassword) {
        handleExceptions(() -> delegate.updatePassword(credentials, newPassword));
    }

    @Override
    public void updateQuota(CloudQuota quota, String name) {
        handleExceptions(() -> delegate.updateQuota(quota, name));
    }

    @Override
    public void updateSecurityGroup(CloudSecurityGroup securityGroup) {
        handleExceptions(() -> delegate.updateSecurityGroup(securityGroup));
    }

    @Override
    public void updateSecurityGroup(String name, InputStream jsonRulesFile) {
        handleExceptions(() -> delegate.updateSecurityGroup(name, jsonRulesFile));
    }

    @Override
    public void updateServiceBroker(CloudServiceBroker serviceBroker) {
        handleExceptions(() -> delegate.updateServiceBroker(serviceBroker));
    }

    @Override
    public void updateServicePlanVisibilityForBroker(String name, boolean visibility) {
        handleExceptions(() -> delegate.updateServicePlanVisibilityForBroker(name, visibility));
    }

    @Override
    public void uploadApplication(String applicationName, String file) throws IOException {
        handleUploadExceptions(() -> delegate.uploadApplication(applicationName, new File(file), null));
    }

    @Override
    public void uploadApplication(String applicationName, File file) throws IOException {
        handleUploadExceptions(() -> delegate.uploadApplication(applicationName, file, null));
    }

    @Override
    public void uploadApplication(String applicationName, File file, UploadStatusCallback callback) throws IOException {
        handleUploadExceptions(() -> delegate.uploadApplication(applicationName, file, callback));
    }

    @Override
    public void uploadApplication(String applicationName, InputStream inputStream) throws IOException {
        handleUploadExceptions(() -> delegate.uploadApplication(applicationName, inputStream, null));
    }

    @Override
    public void uploadApplication(String applicationName, InputStream inputStream, UploadStatusCallback callback) throws IOException {
        handleUploadExceptions(() -> delegate.uploadApplication(applicationName, inputStream, callback));
    }

    @Override
    public UploadToken asyncUploadApplication(String applicationName, File file) throws IOException {
        return handleUploadExceptions(() -> delegate.asyncUploadApplication(applicationName, file, null));
    }

    @Override
    public UploadToken asyncUploadApplication(String applicationName, File file, UploadStatusCallback callback) throws IOException {
        return handleUploadExceptions(() -> delegate.asyncUploadApplication(applicationName, file, callback));
    }

    @Override
    public Upload getUploadStatus(UUID packageGuid) {
        return handleExceptions(() -> delegate.getUploadStatus(packageGuid));
    }

    @Override
    public CloudBuild createBuild(UUID packageGuid) {
        return handleExceptions(() -> delegate.createBuild(packageGuid));
    }

    @Override
    public CloudBuild getBuild(UUID buildGuid) {
        return handleExceptions(() -> delegate.getBuild(buildGuid));
    }

    @Override
    public CloudTask getTask(UUID taskGuid) {
        return handleExceptions(() -> delegate.getTask(taskGuid));
    }

    @Override
    public List<CloudTask> getTasks(String applicationName) {
        return handleExceptions(() -> delegate.getTasks(applicationName));
    }

    @Override
    public CloudTask runTask(String applicationName, CloudTask task) {
        return handleExceptions(() -> delegate.runTask(applicationName, task));
    }

    @Override
    public CloudTask cancelTask(UUID taskGuid) {
        return handleExceptions(() -> delegate.cancelTask(taskGuid));
    }

    @Override
    public void bindDropletToApp(UUID dropletGuid, UUID applicationGuid) {
        handleExceptions(() -> delegate.bindDropletToApp(dropletGuid, applicationGuid));
    }

    @Override
    public List<CloudBuild> getBuildsForApplication(UUID applicationGuid) {
        return handleExceptions(() -> delegate.getBuildsForApplication(applicationGuid));
    }

    @Override
    public List<CloudBuild> getBuildsForPackage(UUID packageGuid) {
        return handleExceptions(() -> delegate.getBuildsForPackage(packageGuid));
    }

    private void handleExceptions(Runnable runnable) {
        handleExceptions(() -> {
            runnable.run();
            return null;
        });
    }

    private <T> T handleExceptions(Supplier<T> runnable) {
        try {
            return runnable.get();
        } catch (AbstractCloudFoundryException e) {
            throw convertV3ClientException(e);
        }
    }

    private void handleUploadExceptions(UploadRunnable runnable) throws IOException {
        handleUploadExceptions(() -> {
            runnable.run();
            return null;
        });
    }

    private <T> T handleUploadExceptions(UploadSupplier<T> runnable) throws IOException {
        try {
            return runnable.get();
        } catch (AbstractCloudFoundryException e) {
            throw convertV3ClientException(e);
        }
    }

    private CloudOperationException convertV3ClientException(AbstractCloudFoundryException e) {
        HttpStatus httpStatus = HttpStatus.valueOf(e.getStatusCode());
        return new CloudOperationException(httpStatus, httpStatus.getReasonPhrase(), e.getMessage(), e);
    }

    /**
     * Necessary, because upload methods can throw IOExceptions and the standard Runnable interface cannot.
     */
    @FunctionalInterface
    private interface UploadRunnable {

        void run() throws IOException;

    }

    /**
     * Necessary, because upload methods can throw IOExceptions and the standard Supplier interface cannot.
     */
    @FunctionalInterface
    private interface UploadSupplier<T> {

        T get() throws IOException;

    }

}
