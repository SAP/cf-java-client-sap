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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.cloudfoundry.client.lib.domain.CloudPackage;
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
import org.cloudfoundry.client.lib.domain.DropletInfo;
import org.cloudfoundry.client.lib.domain.InstancesInfo;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Upload;
import org.cloudfoundry.client.lib.rest.CloudControllerRestClient;
import org.cloudfoundry.client.lib.rest.CloudControllerRestClientFactory;
import org.cloudfoundry.client.lib.rest.ImmutableCloudControllerRestClientFactory;
import org.cloudfoundry.client.v2.serviceinstances.LastOperation;
import org.cloudfoundry.client.v3.Metadata;
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
    public void addRoute(String host, String domainName, String path) {
        handleExceptions(() -> delegate.addRoute(host, domainName, path));
    }

    @Override
    public void bindServiceInstance(String applicationName, String serviceInstanceName) {
        handleExceptions(() -> delegate.bindServiceInstance(applicationName, serviceInstanceName));
    }

    @Override
    public void bindServiceInstance(String applicationName, String serviceInstanceName, Map<String, Object> parameters,
                                    ApplicationServicesUpdateCallback updateServicesCallback) {
        try {
            handleExceptions(() -> delegate.bindServiceInstance(applicationName, serviceInstanceName, parameters));
        } catch (CloudOperationException e) {
            updateServicesCallback.onError(e, applicationName, serviceInstanceName);
        }
    }

    @Override
    public void createApplication(String applicationName, Staging staging, Integer memory, List<String> uris) {
        handleExceptions(() -> delegate.createApplication(applicationName, staging, memory, uris));
    }

    @Override
    public void createApplication(String applicationName, Staging staging, Integer disk, Integer memory, List<String> uris,
                                  DockerInfo dockerInfo) {
        handleExceptions(() -> delegate.createApplication(applicationName, staging, disk, memory, uris, dockerInfo));
    }

    @Override
    public void createServiceInstance(CloudServiceInstance serviceInstance) {
        handleExceptions(() -> delegate.createServiceInstance(serviceInstance));
    }

    @Override
    public void createServiceBroker(CloudServiceBroker serviceBroker) {
        handleExceptions(() -> delegate.createServiceBroker(serviceBroker));
    }

    @Override
    public CloudServiceKey createServiceKey(String serviceInstanceName, String serviceKeyName, Map<String, Object> parameters) {
        return handleExceptions(() -> delegate.createServiceKey(serviceInstanceName, serviceKeyName, parameters));
    }

    @Override
    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance, Map<String, Object> credentials) {
        handleExceptions(() -> delegate.createUserProvidedServiceInstance(serviceInstance, credentials));
    }

    @Override
    public void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance, Map<String, Object> credentials,
                                                  String syslogDrainUrl) {
        handleExceptions(() -> delegate.createUserProvidedServiceInstance(serviceInstance, credentials, syslogDrainUrl));
    }

    @Override
    public void deleteAllApplications() {
        handleExceptions(() -> delegate.deleteAllApplications());
    }

    @Override
    public void deleteAllServiceInstances() {
        handleExceptions(() -> delegate.deleteAllServiceInstances());
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
    public void deleteRoute(String host, String domainName, String path) {
        handleExceptions(() -> delegate.deleteRoute(host, domainName, path));
    }

    @Override
    public void deleteServiceInstance(String serviceInstanceName) {
        handleExceptions(() -> delegate.deleteServiceInstance(serviceInstanceName));
    }

    @Override
    public void deleteServiceInstance(CloudServiceInstance serviceInstance) {
        handleExceptions(() -> delegate.deleteServiceInstance(serviceInstance));
    }

    @Override
    public void deleteServiceBroker(String name) {
        handleExceptions(() -> delegate.deleteServiceBroker(name));
    }

    @Override
    public void deleteServiceKey(String serviceInstanceName, String serviceKeyName) {
        handleExceptions(() -> delegate.deleteServiceKey(serviceInstanceName, serviceKeyName));
    }

    @Override
    public void deleteServiceKey(CloudServiceKey serviceKey) {
        handleExceptions(() -> delegate.deleteServiceKey(serviceKey));
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
    public List<CloudEvent> getEventsByActee(UUID uuid) {
        return handleExceptions(() -> delegate.getEventsByActee(uuid));
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
    public List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector) {
        return handleExceptions(() -> delegate.getApplicationsByMetadataLabelSelector(labelSelector));
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
    public CloudOrganization getOrganization(String organizationName) {
        return handleExceptions(() -> delegate.getOrganization(organizationName));
    }

    @Override
    public CloudOrganization getOrganization(String organizationName, boolean required) {
        return handleExceptions(() -> delegate.getOrganization(organizationName, required));
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
    public CloudServiceInstance getServiceInstance(String serviceInstanceName) {
        return handleExceptions(() -> delegate.getServiceInstance(serviceInstanceName));
    }

    @Override
    public CloudServiceInstance getServiceInstance(String serviceInstanceName, boolean required) {
        return handleExceptions(() -> delegate.getServiceInstance(serviceInstanceName, required));
    }

    @Override
    public List<CloudServiceBinding> getServiceBindings(UUID serviceInstanceGuid) {
        return handleExceptions(() -> delegate.getServiceBindings(serviceInstanceGuid));
    }

    @Override
    public Map<String, Object> getServiceInstanceParameters(UUID guid) {
        return handleExceptions(() -> delegate.getServiceInstanceParameters(guid));
    }

    @Override
    public Map<String, Object> getServiceBindingParameters(UUID guid) {
        return handleExceptions(() -> delegate.getServiceBindingParameters(guid));
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(String serviceInstanceName) {
        return handleExceptions(() -> delegate.getServiceKeys(serviceInstanceName));
    }

    @Override
    public List<CloudServiceKey> getServiceKeys(CloudServiceInstance serviceInstance) {
        return handleExceptions(() -> delegate.getServiceKeys(serviceInstance));
    }

    @Override
    public List<CloudServiceOffering> getServiceOfferings() {
        return handleExceptions(() -> delegate.getServiceOfferings());
    }

    @Override
    public List<CloudServiceInstance> getServiceInstances() {
        return handleExceptions(() -> delegate.getServiceInstances());
    }

    @Override
    public List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector) {
        return handleExceptions(() -> delegate.getServiceInstancesByMetadataLabelSelector(labelSelector));
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
    public OAuth2AccessToken login() {
        return handleExceptions(() -> delegate.login());
    }

    @Override
    public void logout() {
        handleExceptions(() -> delegate.logout());
    }

    @Override
    public void registerRestLogListener(RestLogCallback callBack) {
        delegate.registerRestLogListener(callBack);
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
    public void unRegisterRestLogListener(RestLogCallback callBack) {
        delegate.unRegisterRestLogListener(callBack);
    }

    @Override
    public void unbindServiceInstance(String applicationName, String serviceInstanceName,
                                      ApplicationServicesUpdateCallback applicationServicesUpdateCallback) {
        try {
            handleExceptions(() -> delegate.unbindServiceInstance(applicationName, serviceInstanceName));
        } catch (CloudOperationException e) {
            applicationServicesUpdateCallback.onError(e, applicationName, serviceInstanceName);
        }
    }

    @Override
    public void unbindServiceInstance(String applicationName, String serviceInstanceName) {
        handleExceptions(() -> delegate.unbindServiceInstance(applicationName, serviceInstanceName));
    }

    @Override
    public void unbindServiceInstance(CloudApplication application, CloudServiceInstance serviceInstance) {
        handleExceptions(() -> delegate.unbindServiceInstance(application, serviceInstance));
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
    public void updateApplicationMetadata(UUID guid, Metadata metadata) {
        handleExceptions(() -> delegate.updateApplicationMetadata(guid, metadata));
    }

    @Override
    public void updateApplicationMemory(String applicationName, int memory) {
        handleExceptions(() -> delegate.updateApplicationMemory(applicationName, memory));
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
    public void updateServiceInstanceMetadata(UUID guid, Metadata metadata) {
        handleExceptions(() -> delegate.updateServiceInstanceMetadata(guid, metadata));
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
    public LastOperation getServiceLastOperation(String serviceName) {
        return handleExceptions(() -> delegate.getServiceLastOperation(serviceName));
    }

    @Override
    public void updateServicePlan(CloudServiceInstance service) {
        handleExceptions(() -> delegate.updateServicePlan(service));
    }

    @Override
    public void updateServiceParameters(CloudServiceInstance service) {
        handleExceptions(() -> delegate.updateServiceParameters(service));
    }

    @Override
    public void updateServiceTags(CloudServiceInstance service) {
        handleExceptions(() -> delegate.updateServiceTags(service));
    }

    @Override
    public void uploadApplication(String applicationName, String file) {
        handleExceptions(() -> delegate.uploadApplication(applicationName, Paths.get(file), null));
    }

    @Override
    public void uploadApplication(String applicationName, Path file) {
        handleExceptions(() -> delegate.uploadApplication(applicationName, file, null));
    }

    @Override
    public void uploadApplication(String applicationName, Path file, UploadStatusCallback callback) {
        handleExceptions(() -> delegate.uploadApplication(applicationName, file, callback));
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
    public CloudPackage asyncUploadApplication(String applicationName, Path file) {
        return handleExceptions(() -> delegate.asyncUploadApplication(applicationName, file, null));
    }

    @Override
    public CloudPackage asyncUploadApplication(String applicationName, Path file, UploadStatusCallback callback) {
        return handleExceptions(() -> delegate.asyncUploadApplication(applicationName, file, callback));
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

    @Override
    public DropletInfo getCurrentDropletForApplication(UUID applicationGuid) {
        return handleExceptions(() -> delegate.getCurrentDropletForApplication(applicationGuid));
    }

    @Override
    public CloudPackage getPackage(UUID packageGuid) {
        return handleExceptions(() -> delegate.getPackage(packageGuid));
    }

    @Override
    public List<CloudPackage> getPackagesForApplication(UUID applicationGuid) {
        return handleExceptions(() -> delegate.getPackagesForApplication(applicationGuid));
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
        try {
            runnable.run();
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

}
