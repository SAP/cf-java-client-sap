/*
 * Copyright 2009-2012 the original author or authors.
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

package org.cloudfoundry.client.lib.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudEntity;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudJob;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudQuota;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudSecurityGroup;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.domain.CloudUser;
import org.cloudfoundry.client.lib.domain.DockerCredentials;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.PackageState;
import org.cloudfoundry.client.lib.domain.SecurityGroupRule;
import org.cloudfoundry.client.lib.domain.ServiceKey;
import org.cloudfoundry.client.lib.domain.Staging;

/**
 * Class handling the mapping of the cloud domain objects
 *
 * @author Thomas Risberg
 */
// TODO: use some more advanced JSON mapping framework?
public class CloudEntityResourceMapper {

    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getEmbeddedResource(Map<String, Object> resource, String embeddedResourceName) {
        Map<String, Object> entity = (Map<String, Object>) resource.get("entity");
        return (Map<String, Object>) entity.get(embeddedResourceName);
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getEmbeddedResourceList(Map<String, Object> resource, String embeddedResourceName) {
        return (List<Map<String, Object>>) resource.get(embeddedResourceName);
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getEntity(Map<String, Object> resource) {
        return (Map<String, Object>) resource.get("entity");
    }

    @SuppressWarnings("unchecked")
    public static <T> T getAttributeOfV2Resource(Map<String, Object> resource, String attributeName, Class<T> targetClass) {
        if (resource == null) {
            return null;
        }
        Map<String, Object> entity = (Map<String, Object>) resource.get("entity");
        return getResourceAttribute(entity, attributeName, targetClass);
    }

    public static <T> T getAttributeOfV3Resource(Map<String, Object> resource, String attributeName, Class<T> targetClass) {
        // In V3, the entities are embedded in the resources.
        return getResourceAttribute(resource, attributeName, targetClass);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getResourceAttribute(Map<String, Object> resource, String attributeName, Class<T> targetClass) {
        if (resource == null) {
            return null;
        }
        Object attributeValue = resource.get(attributeName);
        if (attributeValue == null) {
            return null;
        }
        if (targetClass == String.class) {
            return (T) String.valueOf(attributeValue);
        }
        if (targetClass == Long.class) {
            return (T) Long.valueOf(String.valueOf(attributeValue));
        }
        if (targetClass == Integer.class || targetClass == Boolean.class || targetClass == Map.class || targetClass == List.class) {
            return (T) attributeValue;
        }
        if (targetClass == UUID.class && attributeValue instanceof String) {
            return (T) UUID.fromString((String) attributeValue);
        }
        throw new IllegalArgumentException("Error during mapping - unsupported class for attribute mapping " + targetClass.getName());
    }

    @SuppressWarnings("unchecked")
    public static CloudEntity.Meta getV2Meta(Map<String, Object> resource) {
        Map<String, Object> metadata = (Map<String, Object>) resource.getOrDefault("metadata", Collections.emptyMap());
        return toMeta(metadata);
    }

    public static CloudEntity.Meta getV3Meta(Map<String, Object> resource) {
        // In V3, the metadata is embedded in the resources.
        return toMeta(resource);
    }

    private static CloudEntity.Meta toMeta(Map<String, Object> metadata) {
        UUID guid = parseGuid(String.valueOf(metadata.get("guid")));
        if (guid == null) {
            return null;
        }
        Date createdDate = parseDate(String.valueOf(metadata.get("created_at")));
        Date updatedDate = parseDate(String.valueOf(metadata.get("updated_at")));
        String url = String.valueOf(metadata.get("url"));
        return new CloudEntity.Meta(guid, createdDate, updatedDate, url);
    }

    private static UUID parseGuid(String guid) {
        try {
            return UUID.fromString(guid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Date parseDate(String dateString) {
        if (dateString != null) {
            try {
                // if the time zone part of the dateString contains a colon (e.g. 2013-09-19T21:56:36+00:00)
                // then remove it before parsing
                String isoDateString = dateString.replaceFirst(":(?=[0-9]{2}$)", "")
                    .replaceFirst("Z$", "+0000");
                return dateFormatter.parse(isoDateString);
            } catch (Exception ignore) {
            }
        }
        return null;
    }

    public UUID getGuidOfV2Resource(Map<String, Object> resource) {
        return getV2Meta(resource).getGuid();
    }

    public UUID getGuidOfV3Resource(Map<String, Object> resource) {
        return getV3Meta(resource).getGuid();
    }

    public String getNameOfV2Resource(Map<String, Object> resource) {
        return getAttributeOfV2Resource(resource, "name", String.class);
    }

    public String getNameOfV3Resource(Map<String, Object> resource) {
        return getAttributeOfV3Resource(resource, "name", String.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T mapResource(Map<String, Object> resource, Class<T> targetClass) {
        if (targetClass == CloudSpace.class) {
            return (T) mapSpaceResource(resource);
        }
        if (targetClass == CloudOrganization.class) {
            return (T) mapOrganizationResource(resource);
        }
        if (targetClass == CloudDomain.class) {
            return (T) mapDomainResource(resource);
        }
        if (targetClass == CloudRoute.class) {
            return (T) mapRouteResource(resource);
        }
        if (targetClass == CloudApplication.class) {
            return (T) mapApplicationResource(resource);
        }
        if (targetClass == CloudEvent.class) {
            return (T) mapEventResource(resource);
        }
        if (targetClass == CloudService.class) {
            return (T) mapServiceResource(resource);
        }
        if (targetClass == CloudServiceInstance.class) {
            return (T) mapServiceInstanceResource(resource);
        }
        if (targetClass == CloudServiceOffering.class) {
            return (T) mapServiceOfferingResource(resource);
        }
        if (targetClass == ServiceKey.class) {
            return (T) mapServiceKeyResource(resource);
        }
        if (targetClass == CloudServiceBroker.class) {
            return (T) mapServiceBrokerResource(resource);
        }
        if (targetClass == CloudStack.class) {
            return (T) mapStackResource(resource);
        }
        if (targetClass == CloudQuota.class) {
            return (T) mapQuotaResource(resource);
        }
        if (targetClass == CloudSecurityGroup.class) {
            return (T) mapApplicationSecurityGroupResource(resource);
        }
        if (targetClass == CloudJob.class) {
            return (T) mapJobResource(resource);
        }
        if (targetClass == CloudUser.class) {
            return (T) mapUserResource(resource);
        }
        throw new IllegalArgumentException("Error during mapping - unsupported class for entity mapping " + targetClass.getName());
    }

    @SuppressWarnings("unchecked")
    private List<SecurityGroupRule> getSecurityGroupRules(Map<String, Object> resource) {
        List<SecurityGroupRule> rules = new ArrayList<SecurityGroupRule>();
        List<Map<String, Object>> jsonRules = getAttributeOfV2Resource(resource, "rules", List.class);
        for (Map<String, Object> jsonRule : jsonRules) {
            rules.add(new SecurityGroupRule((String) jsonRule.get("protocol"), (String) jsonRule.get("ports"),
                (String) jsonRule.get("destination"), (Boolean) jsonRule.get("log"), (Integer) jsonRule.get("type"),
                (Integer) jsonRule.get("code")));
        }
        return rules;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private CloudApplication mapApplicationResource(Map<String, Object> resource) {
        CloudApplication app = new CloudApplication(getV2Meta(resource), getNameOfV2Resource(resource));
        app.setInstances(getAttributeOfV2Resource(resource, "instances", Integer.class));
        app.setServices(new ArrayList<String>());
        app.setState(CloudApplication.AppState.valueOf(getAttributeOfV2Resource(resource, "state", String.class)));
        // TODO: debug
        app.setDebug(null);

        Integer runningInstancesAttribute = getAttributeOfV2Resource(resource, "running_instances", Integer.class);
        if (runningInstancesAttribute != null) {
            app.setRunningInstances(runningInstancesAttribute);
        }
        String command = getAttributeOfV2Resource(resource, "command", String.class);
        String buildpack = getAttributeOfV2Resource(resource, "buildpack", String.class);
        String detectedBuildpack = getAttributeOfV2Resource(resource, "detected_buildpack", String.class);
        Map<String, Object> stackResource = getEmbeddedResource(resource, "stack");
        CloudStack stack = mapStackResource(stackResource);
        Integer healthCheckTimeout = getAttributeOfV2Resource(resource, "health_check_timeout", Integer.class);
        String healthCheckType = getAttributeOfV2Resource(resource, "health_check_type", String.class);
        String healthCheckHttpEndpoint = getAttributeOfV2Resource(resource, "health_check_http_endpoint", String.class);
        Boolean sshEnabled = getAttributeOfV2Resource(resource, "enable_ssh", Boolean.class);
        String dockerImage = getAttributeOfV2Resource(resource, "docker_image", String.class);
        Map<String, String> dockerCredentials = getAttributeOfV2Resource(resource, "docker_credentials", Map.class);
        DockerInfo dockerInfo = createDockerInfo(dockerImage, dockerCredentials);

        Staging staging = new Staging.StagingBuilder().command(command)
            .buildpackUrl(buildpack)
            .stack(stack.getName())
            .healthCheckTimeout(healthCheckTimeout)
            .detectedBuildpack(detectedBuildpack)
            .healthCheckType(healthCheckType)
            .healthCheckHttpEndpoint(healthCheckHttpEndpoint)
            .sshEnabled(sshEnabled)
            .dockerInfo(dockerInfo)
            .build();

        app.setStaging(staging);

        String stateAsString = getAttributeOfV2Resource(resource, "package_state", String.class);
        if (stateAsString != null) {
            PackageState packageState = PackageState.valueOf(stateAsString);
            app.setPackageState(packageState);
        }

        String stagingFailedDescription = getAttributeOfV2Resource(resource, "staging_failed_description", String.class);
        app.setStagingError(stagingFailedDescription);

        Map<String, Object> spaceResource = getEmbeddedResource(resource, "space");
        if (spaceResource != null) {
            CloudSpace space = mapSpaceResource(spaceResource);
            app.setSpace(space);
        }

        Map envMap = getAttributeOfV2Resource(resource, "environment_json", Map.class);
        if (envMap.size() > 0) {
            app.setEnv(envMap);
        }
        app.setMemory(getAttributeOfV2Resource(resource, "memory", Integer.class));
        app.setDiskQuota(getAttributeOfV2Resource(resource, "disk_quota", Integer.class));
        List<Map<String, Object>> serviceBindings = getAttributeOfV2Resource(resource, "service_bindings", List.class);
        List<String> serviceList = new ArrayList<String>();
        for (Map<String, Object> binding : serviceBindings) {
            Map<String, Object> service = getAttributeOfV2Resource(binding, "service_instance", Map.class);
            String serviceName = getNameOfV2Resource(service);
            if (serviceName != null) {
                serviceList.add(serviceName);
            }
        }
        app.setServices(serviceList);
        return app;
    }

    private DockerInfo createDockerInfo(String dockerImage, Map<String, String> dockerCredentials) {
        if (dockerImage == null) {
            return null;
        }
        DockerInfo dockerInfo = new DockerInfo(dockerImage);
        String username = dockerCredentials.get("username");
        String password = dockerCredentials.get("password");
        if (username == null || password == null) {
            return dockerInfo;
        }
        DockerCredentials credentials = new DockerCredentials(username, password);
        dockerInfo.setDockerCredentials(credentials);

        return dockerInfo;
    }

    private CloudSecurityGroup mapApplicationSecurityGroupResource(Map<String, Object> resource) {
        return new CloudSecurityGroup(getV2Meta(resource), getNameOfV2Resource(resource), getSecurityGroupRules(resource),
            getAttributeOfV2Resource(resource, "running_default", Boolean.class),
            getAttributeOfV2Resource(resource, "staging_default", Boolean.class));
    }

    private CloudDomain mapDomainResource(Map<String, Object> resource) {
        @SuppressWarnings("unchecked")
        Map<String, Object> ownerResource = getAttributeOfV2Resource(resource, "owning_organization", Map.class);
        CloudOrganization owner;
        if (ownerResource == null) {
            owner = new CloudOrganization(CloudEntity.Meta.defaultMeta(), "none");
        } else {
            owner = mapOrganizationResource(ownerResource);
        }
        return new CloudDomain(getV2Meta(resource), getNameOfV2Resource(resource), owner);
    }

    private CloudEvent mapEventResource(Map<String, Object> resource) {
        CloudEvent event = new CloudEvent(getV2Meta(resource), getNameOfV2Resource(resource));
        event.setType(getAttributeOfV2Resource(resource, "type", String.class));
        event.setActor(getAttributeOfV2Resource(resource, "actor", String.class));
        event.setActorType(getAttributeOfV2Resource(resource, "actor_type", String.class));
        event.setActorName(getAttributeOfV2Resource(resource, "actor_name", String.class));
        event.setActee(getAttributeOfV2Resource(resource, "actee", String.class));
        event.setActeeType(getAttributeOfV2Resource(resource, "actee_type", String.class));
        event.setActeeName(getAttributeOfV2Resource(resource, "actee_name", String.class));
        Date timestamp = parseDate(getAttributeOfV2Resource(resource, "timestamp", String.class));
        event.setTimestamp(timestamp);

        return event;
    }

    private CloudJob mapJobResource(Map<String, Object> resource) {
        String status = getAttributeOfV2Resource(resource, "status", String.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> errorDetailsResource = getAttributeOfV2Resource(resource, "error_details", Map.class);
        CloudJob.ErrorDetails errorDetails = null;
        if (errorDetailsResource != null) {
            Long code = Long.valueOf(String.valueOf(errorDetailsResource.get("code")));
            String description = (String) errorDetailsResource.get("description");
            String errorCode = (String) errorDetailsResource.get("error_code");
            errorDetails = new CloudJob.ErrorDetails(code, description, errorCode);
        }

        return new CloudJob(getV2Meta(resource), CloudJob.Status.getEnum(status), errorDetails);
    }

    private CloudOrganization mapOrganizationResource(Map<String, Object> resource) {
        Boolean billingEnabled = getAttributeOfV2Resource(resource, "billing_enabled", Boolean.class);
        Map<String, Object> quotaDefinition = getEmbeddedResource(resource, "quota_definition");
        CloudQuota quota = null;
        if (quotaDefinition != null) {
            quota = mapQuotaResource(quotaDefinition);
        }
        return new CloudOrganization(getV2Meta(resource), getNameOfV2Resource(resource), quota, billingEnabled);
    }

    private CloudQuota mapQuotaResource(Map<String, Object> resource) {
        Boolean nonBasicServicesAllowed = getAttributeOfV2Resource(resource, "non_basic_services_allowed", Boolean.class);
        int totalServices = getAttributeOfV2Resource(resource, "total_services", Integer.class);
        int totalRoutes = getAttributeOfV2Resource(resource, "total_routes", Integer.class);
        long memoryLimit = getAttributeOfV2Resource(resource, "memory_limit", Long.class);

        return new CloudQuota(getV2Meta(resource), getNameOfV2Resource(resource), nonBasicServicesAllowed, totalServices, totalRoutes,
            memoryLimit);
    }

    private CloudRoute mapRouteResource(Map<String, Object> resource) {
        @SuppressWarnings("unchecked")
        List<Object> apps = getAttributeOfV2Resource(resource, "apps", List.class);
        String host = getAttributeOfV2Resource(resource, "host", String.class);
        CloudDomain domain = mapDomainResource(getEmbeddedResource(resource, "domain"));
        return new CloudRoute(getV2Meta(resource), host, domain, apps.size());
    }

    @SuppressWarnings("unchecked")
    private CloudServiceBinding mapServiceBinding(Map<String, Object> resource) {
        CloudServiceBinding binding = new CloudServiceBinding(getV2Meta(resource), getNameOfV2Resource(resource));

        binding.setAppGuid(UUID.fromString(getAttributeOfV2Resource(resource, "app_guid", String.class)));
        binding.setSyslogDrainUrl(getAttributeOfV2Resource(resource, "syslog_drain_url", String.class));
        binding.setCredentials(getAttributeOfV2Resource(resource, "credentials", Map.class));
        binding.setBindingOptions(getAttributeOfV2Resource(resource, "binding_options", Map.class));

        return binding;
    }

    private CloudServiceBroker mapServiceBrokerResource(Map<String, Object> resource) {
        return new CloudServiceBroker(getV2Meta(resource), getAttributeOfV2Resource(resource, "name", String.class),
            getAttributeOfV2Resource(resource, "broker_url", String.class),
            getAttributeOfV2Resource(resource, "auth_username", String.class));
    }

    @SuppressWarnings("unchecked")
    private CloudServiceInstance mapServiceInstanceResource(Map<String, Object> resource) {
        CloudServiceInstance serviceInstance = new CloudServiceInstance(getV2Meta(resource), getNameOfV2Resource(resource));

        serviceInstance.setType(getAttributeOfV2Resource(resource, "type", String.class));
        serviceInstance.setDashboardUrl(getAttributeOfV2Resource(resource, "dashboard_url", String.class));
        serviceInstance.setCredentials(getAttributeOfV2Resource(resource, "credentials", Map.class));

        Map<String, Object> servicePlanResource = getEmbeddedResource(resource, "service_plan");
        if (servicePlanResource != null) {
            serviceInstance.setServicePlan(mapServicePlanResource(servicePlanResource));
        }

        CloudService service = mapServiceResource(resource);
        serviceInstance.setService(service);

        List<Map<String, Object>> bindingsResource = getEmbeddedResourceList(getEntity(resource), "service_bindings");
        List<CloudServiceBinding> bindings = new ArrayList<>(bindingsResource.size());
        for (Map<String, Object> bindingResource : bindingsResource) {
            bindings.add(mapServiceBinding(bindingResource));
        }
        serviceInstance.setBindings(bindings);

        return serviceInstance;
    }

    private CloudServiceOffering mapServiceOfferingResource(Map<String, Object> resource) {
        CloudServiceOffering cloudServiceOffering = new CloudServiceOffering(getV2Meta(resource),
            getAttributeOfV2Resource(resource, "label", String.class), getAttributeOfV2Resource(resource, "provider", String.class),
            getAttributeOfV2Resource(resource, "version", String.class), getAttributeOfV2Resource(resource, "description", String.class),
            getAttributeOfV2Resource(resource, "active", Boolean.class), getAttributeOfV2Resource(resource, "bindable", Boolean.class),
            getAttributeOfV2Resource(resource, "url", String.class), getAttributeOfV2Resource(resource, "info_url", String.class),
            getAttributeOfV2Resource(resource, "unique_id", String.class), getAttributeOfV2Resource(resource, "extra", String.class),
            getAttributeOfV2Resource(resource, "documentation_url", String.class));
        List<Map<String, Object>> servicePlanList = getEmbeddedResourceList(getEntity(resource), "service_plans");
        if (servicePlanList != null) {
            for (Map<String, Object> servicePlanResource : servicePlanList) {
                CloudServicePlan servicePlan = mapServicePlanResource(servicePlanResource);
                servicePlan.setServiceOffering(cloudServiceOffering);
                cloudServiceOffering.addCloudServicePlan(servicePlan);
            }
        }
        return cloudServiceOffering;
    }

    @SuppressWarnings("unchecked")
    private ServiceKey mapServiceKeyResource(Map<String, Object> resource) {
        ServiceKey serviceKey = new ServiceKey(getV2Meta(resource), getAttributeOfV2Resource(resource, "name", String.class), null,
            getAttributeOfV2Resource(resource, "credentials", Map.class), null);

        return serviceKey;
    }

    private CloudServicePlan mapServicePlanResource(Map<String, Object> servicePlanResource) {
        Boolean publicPlan = getAttributeOfV2Resource(servicePlanResource, "public", Boolean.class);

        return new CloudServicePlan(getV2Meta(servicePlanResource), getAttributeOfV2Resource(servicePlanResource, "name", String.class),
            getAttributeOfV2Resource(servicePlanResource, "description", String.class),
            getAttributeOfV2Resource(servicePlanResource, "free", Boolean.class), publicPlan == null ? true : publicPlan,
            getAttributeOfV2Resource(servicePlanResource, "extra", String.class),
            getAttributeOfV2Resource(servicePlanResource, "unique_id", String.class));
    }

    private CloudService mapServiceResource(Map<String, Object> resource) {
        CloudService cloudService = new CloudService(getV2Meta(resource), getNameOfV2Resource(resource));
        Map<String, Object> servicePlanResource = getEmbeddedResource(resource, "service_plan");
        if (servicePlanResource != null) {
            cloudService.setPlan(getAttributeOfV2Resource(servicePlanResource, "name", String.class));

            Map<String, Object> serviceResource = getEmbeddedResource(servicePlanResource, "service");
            if (serviceResource != null) {
                cloudService.setLabel(getAttributeOfV2Resource(serviceResource, "label", String.class));
                cloudService.setProvider(getAttributeOfV2Resource(serviceResource, "provider", String.class));
                cloudService.setVersion(getAttributeOfV2Resource(serviceResource, "version", String.class));
            }
        }
        return cloudService;
    }

    private CloudSpace mapSpaceResource(Map<String, Object> resource) {
        Map<String, Object> organizationMap = getEmbeddedResource(resource, "organization");
        CloudOrganization organization = null;
        if (organizationMap != null) {
            organization = mapOrganizationResource(organizationMap);
        }
        return new CloudSpace(getV2Meta(resource), getNameOfV2Resource(resource), organization);
    }

    private CloudStack mapStackResource(Map<String, Object> resource) {
        return new CloudStack(getV2Meta(resource), getNameOfV2Resource(resource),
            getAttributeOfV2Resource(resource, "description", String.class));
    }

    private CloudUser mapUserResource(Map<String, Object> resource) {
        boolean isActiveUser = getAttributeOfV2Resource(resource, "active", Boolean.class);
        boolean isAdminUser = getAttributeOfV2Resource(resource, "admin", Boolean.class);
        String defaultSpaceGuid = getAttributeOfV2Resource(resource, "default_space_guid", String.class);
        String username = getAttributeOfV2Resource(resource, "username", String.class);

        return new CloudUser(getV2Meta(resource), username, isAdminUser, isActiveUser, defaultSpaceGuid);
    }

}
