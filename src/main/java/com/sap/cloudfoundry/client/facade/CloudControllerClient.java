package com.sap.cloudfoundry.client.facade;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.cloudfoundry.client.v3.Metadata;
import org.springframework.security.oauth2.common.OAuth2AccessToken;

import com.sap.cloudfoundry.client.facade.domain.ApplicationLog;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudBuild;
import com.sap.cloudfoundry.client.facade.domain.CloudDomain;
import com.sap.cloudfoundry.client.facade.domain.CloudEvent;
import com.sap.cloudfoundry.client.facade.domain.CloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.CloudRoute;
import com.sap.cloudfoundry.client.facade.domain.CloudRouteSummary;
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

/**
 * The interface defining operations making up the Cloud Foundry Java client's API.
 *
 */
public interface CloudControllerClient {

    /**
     * Add a private domain in the current organization.
     *
     * @param domainName the domain to add
     */
    void addDomain(String domainName);

    /**
     * Register a new route to the a domain.
     *
     * @param host the host of the route to register
     * @param domainName the domain of the route to register
     */
    void addRoute(String host, String domainName, String path);

    /**
     * Associate (provision) a service with an application.
     *
     * @param applicationName the application name
     * @param serviceInstanceName the service instance name
     */
    void bindServiceInstance(String applicationName, String serviceInstanceName);

    /**
     * Associate (provision) a service with an application.
     *
     * @param applicationName the application name
     * @param serviceInstanceName the service instance name
     * @param parameters the binding parameters
     * @param updateServicesCallback callback used for error handling
     */
    void bindServiceInstance(String applicationName, String serviceInstanceName, Map<String, Object> parameters,
                             ApplicationServicesUpdateCallback updateServicesCallback);

    /**
     * Create application.
     *
     * @param applicationName application name
     * @param staging staging info
     * @param disk disk quota to use in MB
     * @param memory memory to use in MB
     * @param routes list of route summary info for the app
     */
    void createApplication(String applicationName, Staging staging, Integer disk, Integer memory, Set<CloudRouteSummary> routes);

    /**
     * Create a service instance.
     *
     * @param serviceInstance cloud service instance info
     */
    void createServiceInstance(CloudServiceInstance serviceInstance);

    /**
     * Create a service broker.
     *
     * @param serviceBroker cloud service broker info
     */
    void createServiceBroker(CloudServiceBroker serviceBroker);

    /**
     * Create a service key.
     * 
     * @param serviceInstanceName name of service instance
     * @param serviceKeyName name of service-key
     * @param parameters parameters of service-key
     * @return cloudServiceKey
     */
    CloudServiceKey createServiceKey(String serviceInstanceName, String serviceKeyName, Map<String, Object> parameters);

    /**
     * Create a user-provided service instance.
     *
     * @param serviceInstance cloud service instance info
     * @param credentials the user-provided service instance credentials
     */
    void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance, Map<String, Object> credentials);

    /**
     * Create a user-provided service instance for logging.
     *
     * @param serviceInstance cloud service instance info
     * @param credentials the user-provided service instance credentials
     * @param syslogDrainUrl for a logging service instance
     */
    void createUserProvidedServiceInstance(CloudServiceInstance serviceInstance, Map<String, Object> credentials, String syslogDrainUrl);

    /**
     * Delete all applications.
     */
    void deleteAllApplications();

    /**
     * Delete all service instances.
     */
    void deleteAllServiceInstances();

    /**
     * Delete application.
     *
     * @param applicationName name of application
     */
    void deleteApplication(String applicationName);

    /**
     * Delete a private domain in the current organization.
     *
     * @param domainName the domain to delete
     */
    void deleteDomain(String domainName);

    /**
     * Delete routes that do not have any application which is assigned to them.
     */
    List<CloudRoute> deleteOrphanedRoutes();

    /**
     * Delete a registered route from the space of the current session.
     *
     * @param host the host of the route to delete
     * @param domainName the domain of the route to delete
     */
    void deleteRoute(String host, String domainName, String path);

    /**
     * Delete cloud service instance.
     *
     * @param serviceInstance name of service instance
     */
    void deleteServiceInstance(String serviceInstance);

    /**
     * 
     * @param serviceInstance {@link CloudServiceInstance}
     */
    void deleteServiceInstance(CloudServiceInstance serviceInstance);

    /**
     * Delete a service broker.
     *
     * @param name the service broker name
     */
    void deleteServiceBroker(String name);

    /**
     * Delete a service key.
     * 
     * @param serviceInstanceName name of service instance
     * @param serviceKeyName name of service key
     */
    void deleteServiceKey(String serviceInstanceName, String serviceKeyName);

    /**
     * Delete a service key.
     * 
     * @param serviceKey {@link CloudServiceKey} object
     */
    void deleteServiceKey(CloudServiceKey serviceKey);

    /**
     * Get cloud application with the specified name.
     *
     * @param applicationName name of the app
     * @return the cloud application
     */
    CloudApplication getApplication(String applicationName);

    /**
     * Get cloud application with the specified name.
     *
     * @param applicationName name of the app
     * @param required if true, and organization is not found, throw an exception
     * @return the cloud application
     */
    CloudApplication getApplication(String applicationName, boolean required);

    /**
     * Get cloud application with the specified GUID.
     *
     * @param guid GUID of the app
     * @return the cloud application
     */
    CloudApplication getApplication(UUID guid);

    /**
     * Get the GUID of the cloud application with the specified name.
     *
     * @param applicationName name of the app
     * @return the cloud application's guid
     */
    UUID getApplicationGuid(String applicationName);

    String getApplicationName(UUID applicationGuid);

    /**
     * Get application environment variables for the app with the specified name.
     *
     * @param applicationName name of the app
     * @return the cloud application environment variables
     */
    Map<String, String> getApplicationEnvironment(String applicationName);

    /**
     * Get application environment variables for the app with the specified GUID.
     *
     * @param applicationGuid GUID of the app
     * @return the cloud application environment variables
     */
    Map<String, String> getApplicationEnvironment(UUID applicationGuid);

    /**
     * Get application events.
     *
     * @param applicationName name of application
     * @return application events
     */
    List<CloudEvent> getApplicationEvents(String applicationName);

    List<CloudEvent> getEventsByActee(UUID uuid);

    /**
     * Get application instances info for application.
     *
     * @param app the application.
     * @return instances info
     */
    InstancesInfo getApplicationInstances(CloudApplication app);

    /**
     * Get all applications in the currently targeted space. This method has EXTREMELY poor performance for spaces with a lot of
     * applications.
     *
     * @return list of applications
     */
    List<CloudApplication> getApplications();

    /**
     * Get the URL used for the cloud controller.
     *
     * @return the cloud controller URL
     */
    URL getCloudControllerUrl();

    /**
     * Gets the default domain for the current org, which is the first shared domain.
     *
     * @return the default domain
     */
    CloudDomain getDefaultDomain();

    /**
     * Get list of all domain shared and private domains.
     *
     * @return list of domains
     */
    List<CloudDomain> getDomains();

    /**
     * Get list of all domain registered for the current organization.
     *
     * @return list of domains
     */
    List<CloudDomain> getDomainsForOrganization();

    /**
     * Get system events.
     *
     * @return all system events
     */
    List<CloudEvent> getEvents();

    /**
     * Get the organization with the specified name.
     *
     * @param organizationName name of organization
     * @return
     */
    CloudOrganization getOrganization(String organizationName);

    /**
     * Get the organization with the specified name.
     *
     * @param organizationName name of organization
     * @param required if true, and organization is not found, throw an exception
     * @return
     */
    CloudOrganization getOrganization(String organizationName, boolean required);

    /**
     * Get all organizations for the current cloud. This method has poor performance when there are a lot of organizations.
     *
     * @return list of organizations
     */
    List<CloudOrganization> getOrganizations();

    /**
     * Get list of all private domains.
     *
     * @return list of private domains
     */
    List<CloudDomain> getPrivateDomains();

    /**
     * Stream recent log entries.
     *
     * Stream logs that were recently produced for an app.
     *
     * @param applicationName the name of the application
     * @return the list of recent log entries
     */
    List<ApplicationLog> getRecentLogs(String applicationName);

    /**
     * Get recent log entries.
     *
     * Get logs that were recently produced for an app.
     *
     * @param applicationGuid the guid of the application
     * @return the list of recent log entries
     */
    List<ApplicationLog> getRecentLogs(UUID applicationGuid);

    /**
     * Get the info for all routes for a domain.
     *
     * @param domainName the domain the routes belong to
     * @return list of routes
     */
    List<CloudRoute> getRoutes(String domainName);

    /**
     * Get a service broker.
     *
     * @param name the service broker name
     * @return the service broker
     */
    CloudServiceBroker getServiceBroker(String name);

    /**
     * Get a service broker.
     *
     * @param name the service broker name
     * @param required if true, and organization is not found, throw an exception
     * @return the service broker
     */
    CloudServiceBroker getServiceBroker(String name, boolean required);

    /**
     * Get all service brokers.
     *
     * @return
     */
    List<CloudServiceBroker> getServiceBrokers();

    UUID getRequiredServiceInstanceGuid(String name);

    /**
     * Get a service instance.
     *
     * @param serviceInstanceName name of the service instance
     * @return the service instance info
     */
    CloudServiceInstance getServiceInstance(String serviceInstanceName);

    /**
     * Get a service instance.
     *
     * @param serviceInstanceName name of the service instance
     * @param required if true, and organization is not found, throw an exception
     * @return the service instance info
     */
    CloudServiceInstance getServiceInstance(String serviceInstanceName, boolean required);

    /**
     * Get the bindings for a particular service instance.
     *
     *
     * @param serviceInstanceGuid the GUID of the service instance
     * @return the bindings
     */
    List<CloudServiceBinding> getServiceBindings(UUID serviceInstanceGuid);

    CloudServiceBinding getServiceBindingForApplication(UUID applicationId, UUID serviceInstanceGuid);

    /**
     * Get all service instance parameters.
     *
     * @param guid The service instance guid
     * @return service instance parameters in key-value pairs
     */
    Map<String, Object> getServiceInstanceParameters(UUID guid);

    /**
     * Get all service binding parameters.
     *
     * @param guid The service binding guid
     * @return service binding parameters in key-value pairs
     */
    Map<String, Object> getServiceBindingParameters(UUID guid);

    /**
     * Get service keys for a service instance.
     *
     * @param serviceInstanceName name containing service keys
     * @return the service keys info
     */
    List<CloudServiceKey> getServiceKeys(String serviceInstanceName);

    /**
     * Get service keys for a service instance.
     *
     * @param serviceInstance instance containing service keys
     * @return the service keys info
     */
    List<CloudServiceKey> getServiceKeys(CloudServiceInstance serviceInstance);

    /**
     * Get all service offerings.
     *
     * @return list of service offerings
     */
    List<CloudServiceOffering> getServiceOfferings();

    /**
     * Get all service instances in the currently targeted space. This method has EXTREMELY poor performance for spaces with a lot of
     * service instances.
     *
     * @return list of service instances
     */
    List<CloudServiceInstance> getServiceInstances();

    /**
     * Get list of all shared domains.
     *
     * @return list of shared domains
     */
    List<CloudDomain> getSharedDomains();

    /**
     * Get space name with the specified GUID.
     * 
     */
    CloudSpace getSpace(UUID spaceGuid);

    /**
     * Get space name with the specified name.
     * 
     */
    CloudSpace getSpace(String organizationName, String spaceName);

    /**
     * Get space name with the specified name.
     * 
     */
    CloudSpace getSpace(String organizationName, String spaceName, boolean required);

    /**
     * Get space name with the specified name.
     * 
     */
    CloudSpace getSpace(String spaceName);

    /**
     * Get space name with the specified name.
     * 
     */
    CloudSpace getSpace(String spaceName, boolean required);

    /**
     * Get all spaces for the current cloud. This method has EXTREMELY poor performance when there are a lot of spaces.
     *
     * @return list of spaces
     */
    List<CloudSpace> getSpaces();

    /**
     * Get list of CloudSpaces for organization.
     *
     * @return List of CloudSpace objects containing the space info
     */
    List<CloudSpace> getSpaces(String organizationName);

    /**
     * Get a stack by name.
     *
     * @param name the name of the stack to get
     * @return the stack
     */
    CloudStack getStack(String name);

    /**
     * Get a stack by name.
     *
     * @param name the name of the stack to get
     * @param required if true, and organization is not found, throw an exception
     * @return the stack, or null if not found
     */
    CloudStack getStack(String name, boolean required);

    /**
     * Get the list of stacks available for staging applications.
     *
     * @return the list of available stacks
     */
    List<CloudStack> getStacks();

    /**
     * Login using the credentials already set for the client.
     *
     * @return authentication token
     */
    OAuth2AccessToken login();

    /**
     * Logout closing the current session.
     */
    void logout();

    /**
     * Rename an application.
     *
     * @param applicationName the current name
     * @param newName the new name
     */
    void rename(String applicationName, String newName);

    /**
     * Restart application.
     *
     * @param applicationName name of application
     */
    void restartApplication(String applicationName);

    /**
     * Start application. May return starting info if the response obtained after the start request contains headers . If the response does
     * not contain headers, null is returned instead.
     *
     * @param applicationName name of application
     */
    void startApplication(String applicationName);

    /**
     * Stop application.
     *
     * @param applicationName name of application
     */
    void stopApplication(String applicationName);

    /**
     * Un-associate (unprovision) a service from an application.
     *
     * @param applicationName the application name
     * @param serviceInstanceName the service instance name
     * @param applicationServicesUpdateCallback callback used for error handling
     */
    void unbindServiceInstance(String applicationName, String serviceInstanceName,
                               ApplicationServicesUpdateCallback applicationServicesUpdateCallback);

    /**
     * Un-associate (unprovision) a service from an application.
     *
     * @param applicationName the application name
     * @param serviceInstanceName the service instance name
     */
    void unbindServiceInstance(String applicationName, String serviceInstanceName);

    /**
     * Un-associate (unprovision) a service from an application.
     *
     * @param application the application instance
     * @param serviceInstance the service instance
     */
    void unbindServiceInstance(CloudApplication application, CloudServiceInstance serviceInstance);

    void unbindServiceInstance(UUID applicationGuid, UUID serviceInstanceGuid);

    /**
     * Update application disk quota.
     *
     * @param applicationName name of application
     * @param disk new disk setting in MB
     */
    void updateApplicationDiskQuota(String applicationName, int disk);

    /**
     * Update application env using a map where the key specifies the name of the environment variable and the value the value of the
     * environment variable..
     *
     * @param applicationName name of application
     * @param env map of environment settings
     */
    void updateApplicationEnv(String applicationName, Map<String, String> env);

    /**
     * Update application instances.
     *
     * @param applicationName name of application
     * @param instances number of instances to use
     */
    void updateApplicationInstances(String applicationName, int instances);

    /**
     * Update application memory.
     *
     * @param applicationName name of application
     * @param memory new memory setting in MB
     */
    void updateApplicationMemory(String applicationName, int memory);

    /**
     * Update application staging information.
     *
     * @param applicationName name of appplication
     * @param staging staging information for the app
     */
    void updateApplicationStaging(String applicationName, Staging staging);

    /**
     * Update application Routes.
     *
     * @param applicationName name of application
     * @param routes list of route summary info for the routes the app should use
     */
    void updateApplicationRoutes(String applicationName, Set<CloudRouteSummary> routes);

    /**
     * Update a service broker (unchanged forces catalog refresh).
     *
     * @param serviceBroker cloud service broker info
     */
    void updateServiceBroker(CloudServiceBroker serviceBroker);

    /**
     * Service plans are private by default when a service broker's catalog is fetched/updated. This method will update the visibility of
     * all plans for a broker to either public or private.
     *
     * @param name the service broker name
     * @param visibility true for public, false for private
     */
    void updateServicePlanVisibilityForBroker(String name, boolean visibility);

    void updateServicePlan(String serviceName, String planName);

    void updateServiceParameters(String serviceName, Map<String, Object> parameters);

    void updateServiceTags(String serviceName, List<String> tags);

    /**
     * Upload an application to Cloud Foundry.
     *
     * @param applicationName application name
     * @param file path to the application archive or folder
     */
    void uploadApplication(String applicationName, String file);

    /**
     * Upload an application to Cloud Foundry.
     *
     * @param applicationName the application name
     * @param file the application archive or folder
     */
    void uploadApplication(String applicationName, Path file);

    /**
     * Upload an application to Cloud Foundry.
     *
     * @param applicationName the application name
     * @param file the application archive
     * @param callback a callback interface used to provide progress information or {@code null}
     */
    void uploadApplication(String applicationName, Path file, UploadStatusCallback callback);

    /**
     * Upload an application to Cloud Foundry.
     *
     * This form of {@code uploadApplication} will read the passed {@code InputStream} and copy the contents to a temporary file for upload.
     *
     * @param applicationName the application name
     * @param inputStream the InputStream to read from
     * @throws java.io.IOException
     */
    void uploadApplication(String applicationName, InputStream inputStream) throws IOException;

    /**
     * Upload an application to Cloud Foundry.
     *
     * This form of {@code uploadApplication} will read the passed {@code InputStream} and copy the contents to a temporary file for upload.
     *
     * @param applicationName the application name
     * @param inputStream the InputStream to read from
     * @param callback a callback interface used to provide progress information or {@code null}
     * @throws java.io.IOException
     */
    void uploadApplication(String applicationName, InputStream inputStream, UploadStatusCallback callback) throws IOException;

    CloudPackage asyncUploadApplication(String applicationName, Path file);

    CloudPackage asyncUploadApplication(String applicationName, Path file, UploadStatusCallback callback);

    Upload getUploadStatus(UUID packageGuid);

    CloudTask getTask(UUID taskGuid);

    /**
     * Get the list of one-off tasks currently known for the given application.
     * 
     * @param applicationName the application to look for tasks
     * @return the list of known tasks
     * @throws UnsupportedOperationException if the targeted controller does not support tasks
     */
    List<CloudTask> getTasks(String applicationName);

    /**
     * Run a one-off task on an application.
     * 
     * @param applicationName the application to run the task on
     * @param task the task to run
     * @return the ran task
     * @throws UnsupportedOperationException if the targeted controller does not support tasks
     */
    CloudTask runTask(String applicationName, CloudTask task);

    /**
     * Cancel the given task.
     * 
     * @param taskGuid the GUID of the task to cancel
     * @return the cancelled task
     */
    CloudTask cancelTask(UUID taskGuid);

    CloudBuild createBuild(UUID packageGuid);

    CloudBuild getBuild(UUID buildGuid);

    void bindDropletToApp(UUID dropletGuid, UUID applicationGuid);

    List<CloudBuild> getBuildsForApplication(UUID applicationGuid);

    List<CloudBuild> getBuildsForPackage(UUID packageGuid);

    List<CloudApplication> getApplicationsByMetadataLabelSelector(String labelSelector);

    void updateApplicationMetadata(UUID guid, Metadata metadata);

    List<CloudServiceInstance> getServiceInstancesByMetadataLabelSelector(String labelSelector);

    List<CloudServiceInstance> getServiceInstancesWithoutAuxiliaryContentByMetadataLabelSelector(String labelSelector);

    void updateServiceInstanceMetadata(UUID guid, Metadata metadata);

    DropletInfo getCurrentDropletForApplication(UUID applicationGuid);

    CloudPackage getPackage(UUID packageGuid);

    List<CloudPackage> getPackagesForApplication(UUID applicationGuid);

    List<UserRole> getUserRolesBySpaceAndUser(UUID spaceGuid, UUID userGuid);

    CloudPackage createDockerPackage(UUID applicationGuid, DockerInfo dockerInfo);

}
