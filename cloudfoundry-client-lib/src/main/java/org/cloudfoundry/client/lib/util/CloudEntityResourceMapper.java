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

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudDomain;
import org.cloudfoundry.client.lib.domain.CloudEvent;
import org.cloudfoundry.client.lib.domain.CloudEvent.Participant;
import org.cloudfoundry.client.lib.domain.CloudJob;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.CloudOrganization;
import org.cloudfoundry.client.lib.domain.CloudPackage;
import org.cloudfoundry.client.lib.domain.CloudQuota;
import org.cloudfoundry.client.lib.domain.CloudRoute;
import org.cloudfoundry.client.lib.domain.CloudSecurityGroup;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudServiceBinding;
import org.cloudfoundry.client.lib.domain.CloudServiceBroker;
import org.cloudfoundry.client.lib.domain.CloudServiceInstance;
import org.cloudfoundry.client.lib.domain.CloudServiceKey;
import org.cloudfoundry.client.lib.domain.CloudServiceOffering;
import org.cloudfoundry.client.lib.domain.CloudServicePlan;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.domain.CloudStack;
import org.cloudfoundry.client.lib.domain.CloudTask;
import org.cloudfoundry.client.lib.domain.CloudUser;
import org.cloudfoundry.client.lib.domain.DockerCredentials;
import org.cloudfoundry.client.lib.domain.DockerInfo;
import org.cloudfoundry.client.lib.domain.ErrorDetails;
import org.cloudfoundry.client.lib.domain.ImmutableCloudApplication;
import org.cloudfoundry.client.lib.domain.ImmutableCloudBuild;
import org.cloudfoundry.client.lib.domain.ImmutableCloudDomain;
import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent;
import org.cloudfoundry.client.lib.domain.ImmutableCloudEvent.ImmutableParticipant;
import org.cloudfoundry.client.lib.domain.ImmutableCloudJob;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.domain.ImmutableCloudOrganization;
import org.cloudfoundry.client.lib.domain.ImmutableCloudPackage;
import org.cloudfoundry.client.lib.domain.ImmutableCloudQuota;
import org.cloudfoundry.client.lib.domain.ImmutableCloudRoute;
import org.cloudfoundry.client.lib.domain.ImmutableCloudSecurityGroup;
import org.cloudfoundry.client.lib.domain.ImmutableCloudService;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBinding;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceBroker;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceInstance;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceKey;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServiceOffering;
import org.cloudfoundry.client.lib.domain.ImmutableCloudServicePlan;
import org.cloudfoundry.client.lib.domain.ImmutableCloudSpace;
import org.cloudfoundry.client.lib.domain.ImmutableCloudStack;
import org.cloudfoundry.client.lib.domain.ImmutableCloudTask;
import org.cloudfoundry.client.lib.domain.ImmutableCloudUser;
import org.cloudfoundry.client.lib.domain.ImmutableDockerCredentials;
import org.cloudfoundry.client.lib.domain.ImmutableDockerInfo;
import org.cloudfoundry.client.lib.domain.ImmutableErrorDetails;
import org.cloudfoundry.client.lib.domain.ImmutableSecurityGroupRule;
import org.cloudfoundry.client.lib.domain.ImmutableStaging;
import org.cloudfoundry.client.lib.domain.PackageState;
import org.cloudfoundry.client.lib.domain.SecurityGroupRule;
import org.cloudfoundry.client.lib.domain.Staging;
import org.cloudfoundry.client.lib.domain.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class handling the mapping of the cloud domain objects
 *
 * @author Thomas Risberg
 */
// TODO: use some more advanced JSON mapping framework?
public class CloudEntityResourceMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudEntityResourceMapper.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

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
    public static <T> T getV2ResourceAttribute(Map<String, Object> resource, String attributeName, Class<T> targetClass) {
        if (resource == null) {
            return null;
        }
        Map<String, Object> entity = (Map<String, Object>) resource.get("entity");
        return getValue(entity, attributeName, targetClass);
    }

    public static <T> T getV3ResourceAttribute(Map<String, Object> resource, String attributeName, Class<T> targetClass) {
        // In V3, the entities are embedded in the resources.
        return getValue(resource, attributeName, targetClass);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getValue(Map<String, Object> map, String key, Class<T> targetClass) {
        if (map == null) {
            return null;
        }
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (targetClass == String.class) {
            return (T) String.valueOf(value);
        }
        if (targetClass == Long.class) {
            return (T) Long.valueOf(String.valueOf(value));
        }
        if (targetClass == Integer.class || targetClass == Boolean.class || targetClass == Map.class || targetClass == List.class) {
            return (T) value;
        }
        if (targetClass == UUID.class && value instanceof String) {
            return (T) parseGuid((String) value);
        }
        if (targetClass == Date.class && value instanceof String) {
            return (T) parseDate((String) value);
        }
        throw new IllegalArgumentException("Error during mapping - unsupported class for attribute mapping " + targetClass.getName());
    }

    @SuppressWarnings("unchecked")
    public static CloudMetadata getV2Metadata(Map<String, Object> resource) {
        Map<String, Object> metadata = (Map<String, Object>) resource.getOrDefault("metadata", Collections.emptyMap());
        return toMetadata(metadata);
    }

    public static CloudMetadata getV3Metadata(Map<String, Object> resource) {
        // In V3, the metadata is embedded in the resources.
        return toMetadata(resource);
    }

    private static CloudMetadata toMetadata(Map<String, Object> metadata) {
        UUID guid = getValue(metadata, "guid", UUID.class);
        if (guid == null) {
            return null;
        }
        return ImmutableCloudMetadata.builder()
                                     .guid(guid)
                                     .createdAt(getValue(metadata, "created_at", Date.class))
                                     .updatedAt(getValue(metadata, "updated_at", Date.class))
                                     .url(getValue(metadata, "url", String.class))
                                     .build();
    }

    private static UUID parseGuid(String guid) {
        try {
            return UUID.fromString(guid);
        } catch (IllegalArgumentException e) {
            LOGGER.warn(MessageFormat.format("Could not parse GUID string: \"{0}\"", guid), e);
            return null;
        }
    }

    private static Date parseDate(String dateString) {
        if (dateString != null) {
            try {
                Instant instant = parseInstant(dateString);
                return Date.from(instant);
            } catch (DateTimeParseException e) {
                LOGGER.warn(MessageFormat.format("Could not parse date string: \"{0}\"", dateString), e);
            }
        }
        return null;
    }

    private static Instant parseInstant(String date) {
        String isoDate = toIsoDate(date);
        return ZonedDateTime.parse(isoDate, DATE_TIME_FORMATTER)
                            .toInstant();
    }

    private static String toIsoDate(String date) {
        // If the time zone part of the date contains a colon (e.g. 2013-09-19T21:56:36+00:00)
        // then remove it before parsing.
        return date.replaceFirst(":(?=[0-9]{2}$)", "")
                   .replaceFirst("Z$", "+0000");
    }

    public UUID getGuidOfV2Resource(Map<String, Object> resource) {
        return getV2Metadata(resource).getGuid();
    }

    public UUID getGuidOfV3Resource(Map<String, Object> resource) {
        return getV3Metadata(resource).getGuid();
    }

    public String getV2ResourceName(Map<String, Object> resource) {
        return getV2ResourceAttribute(resource, "name", String.class);
    }

    public String getV3ResourceName(Map<String, Object> resource) {
        return getV3ResourceAttribute(resource, "name", String.class);
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
        if (targetClass == CloudTask.class) {
            return (T) mapTaskResource(resource);
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
        if (targetClass == CloudServiceKey.class) {
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
        if (targetClass == CloudPackage.class) {
            return (T) mapPackageResource(resource);
        }
        if (targetClass == CloudBuild.class) {
            return (T) mapBuildResource(resource);
        }

        throw new IllegalArgumentException("Error during mapping - unsupported class for entity mapping " + targetClass.getName());
    }

    private CloudPackage mapPackageResource(Map<String, Object> resource) {
        return ImmutableCloudPackage.builder()
                                    .metadata(getV3Metadata(resource))
                                    .status(getPackageStatus(resource))
                                    .data(getPackageData(resource))
                                    .type(getPackageType(resource))
                                    .build();
    }

    private Status getPackageStatus(Map<String, Object> resource) {
        String status = getV3ResourceAttribute(resource, "state", String.class).toUpperCase();
        return Status.valueOf(status);
    }

    @SuppressWarnings("unchecked")
    private CloudPackage.Data getPackageData(Map<String, Object> resource) {
        Map<String, Object> data = getV3ResourceAttribute(resource, "data", Map.class);
        return ImmutableCloudPackage.ImmutableData.builder()
                                                  .checksum(parseChecksumMap(getValue(data, "checksum", Map.class)))
                                                  .error(getValue(data, "error", String.class))
                                                  .build();
    }

    private CloudPackage.Checksum parseChecksumMap(Map<String, Object> checksumMap) {
        return ImmutableCloudPackage.ImmutableChecksum.builder()
                                                      .algorithm(getValue(checksumMap, "type", String.class))
                                                      .value(getValue(checksumMap, "value", String.class))
                                                      .build();
    }

    private CloudPackage.Type getPackageType(Map<String, Object> resource) {
        String type = getV3ResourceAttribute(resource, "type", String.class).toUpperCase();
        return CloudPackage.Type.valueOf(type);
    }

    private CloudBuild mapBuildResource(Map<String, Object> resource) {
        return ImmutableCloudBuild.builder()
                                  .metadata(getV3Metadata(resource))
                                  .state(getBuildState(resource))
                                  .createdBy(getCreatedBy(resource))
                                  .dropletInfo(getDropletInfo(resource))
                                  .packageInfo(getPackageInfo(resource))
                                  .error(getError(resource))
                                  .build();
    }

    private CloudBuild.State getBuildState(Map<String, Object> resource) {
        String state = getV3ResourceAttribute(resource, "state", String.class);
        return CloudBuild.State.fromString(state);
    }

    @SuppressWarnings("unchecked")
    private CloudBuild.CreatedBy getCreatedBy(Map<String, Object> resource) {
        Map<String, Object> createdByMap = getV3ResourceAttribute(resource, "created_by", Map.class);
        return ImmutableCloudBuild.ImmutableCreatedBy.builder()
                                                     .guid(getValue(createdByMap, "guid", UUID.class))
                                                     .name(getValue(createdByMap, "name", String.class))
                                                     .build();
    }

    private String getError(Map<String, Object> resource) {
        return getV3ResourceAttribute(resource, "error", String.class);
    }

    @SuppressWarnings("unchecked")
    private CloudBuild.DropletInfo getDropletInfo(Map<String, Object> resource) {
        Map<String, Object> dropletMap = getV3ResourceAttribute(resource, "droplet", Map.class);
        if (dropletMap == null) {
            return null;
        }
        return ImmutableCloudBuild.ImmutableDropletInfo.builder()
                                                       .guid(getValue(dropletMap, "guid", UUID.class))
                                                       .build();
    }

    @SuppressWarnings("unchecked")
    private CloudBuild.PackageInfo getPackageInfo(Map<String, Object> resource) {
        Map<String, Object> packageMap = getV3ResourceAttribute(resource, "package", Map.class);
        if (packageMap == null) {
            return null;
        }
        return ImmutableCloudBuild.ImmutablePackageInfo.builder()
                                                       .guid(getValue(packageMap, "guid", UUID.class))
                                                       .build();
    }

    @SuppressWarnings("unchecked")
    private List<SecurityGroupRule> getSecurityGroupRules(Map<String, Object> resource) {
        List<SecurityGroupRule> rules = new ArrayList<>();
        List<Map<String, Object>> jsonRules = getV2ResourceAttribute(resource, "rules", List.class);
        for (Map<String, Object> jsonRule : jsonRules) {
            SecurityGroupRule rule = ImmutableSecurityGroupRule.builder()
                                                               .protocol((String) jsonRule.get("protocol"))
                                                               .ports((String) jsonRule.get("ports"))
                                                               .destination((String) jsonRule.get("destination"))
                                                               .log((Boolean) jsonRule.get("log"))
                                                               .type((Integer) jsonRule.get("type"))
                                                               .code((Integer) jsonRule.get("code"))
                                                               .build();
            rules.add(rule);
        }
        return rules;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private CloudApplication mapApplicationResource(Map<String, Object> resource) {
        ImmutableCloudApplication.Builder builder = ImmutableCloudApplication.builder()
                                                                             .metadata(getV2Metadata(resource))
                                                                             .name(getV2ResourceName(resource));

        String command = getV2ResourceAttribute(resource, "command", String.class);
        String buildpack = getV2ResourceAttribute(resource, "buildpack", String.class);
        String detectedBuildpack = getV2ResourceAttribute(resource, "detected_buildpack", String.class);
        String stackName = getStackName(resource);
        Integer healthCheckTimeout = getV2ResourceAttribute(resource, "health_check_timeout", Integer.class);
        String healthCheckType = getV2ResourceAttribute(resource, "health_check_type", String.class);
        String healthCheckHttpEndpoint = getV2ResourceAttribute(resource, "health_check_http_endpoint", String.class);
        Boolean sshEnabled = getV2ResourceAttribute(resource, "enable_ssh", Boolean.class);
        String dockerImage = getV2ResourceAttribute(resource, "docker_image", String.class);
        Map<String, String> dockerCredentials = getV2ResourceAttribute(resource, "docker_credentials", Map.class);
        DockerInfo dockerInfo = createDockerInfo(dockerImage, dockerCredentials);

        Staging staging = ImmutableStaging.builder()
                                          .command(command)
                                          .addBuildpack(buildpack)
                                          .stack(stackName)
                                          .healthCheckTimeout(healthCheckTimeout)
                                          .detectedBuildpack(detectedBuildpack)
                                          .healthCheckType(healthCheckType)
                                          .healthCheckHttpEndpoint(healthCheckHttpEndpoint)
                                          .isSshEnabled(sshEnabled)
                                          .dockerInfo(dockerInfo)
                                          .build();

        builder.instances(getV2ResourceAttribute(resource, "instances", Integer.class))
               .state(CloudApplication.State.valueOf(getV2ResourceAttribute(resource, "state", String.class)));

        Integer runningInstancesAttribute = getV2ResourceAttribute(resource, "running_instances", Integer.class);
        if (runningInstancesAttribute != null) {
            builder.runningInstances(runningInstancesAttribute);
        }
        builder.staging(staging);

        String stateAsString = getV2ResourceAttribute(resource, "package_state", String.class);
        if (stateAsString != null) {
            PackageState packageState = PackageState.valueOf(stateAsString);
            builder.packageState(packageState);
        }

        String stagingFailedDescription = getV2ResourceAttribute(resource, "staging_failed_description", String.class);
        builder.stagingError(stagingFailedDescription);

        Map<String, Object> spaceResource = getEmbeddedResource(resource, "space");
        if (spaceResource != null) {
            CloudSpace space = mapSpaceResource(spaceResource);
            builder.space(space);
        }

        Map envMap = getV2ResourceAttribute(resource, "environment_json", Map.class);
        Map<String, String> resultMap = convertEnvironmentValuesToJson(envMap);
        if (!resultMap.isEmpty()) {
            builder.env(resultMap);
        }

        return builder.memory(getV2ResourceAttribute(resource, "memory", Integer.class))
                      .diskQuota(getV2ResourceAttribute(resource, "disk_quota", Integer.class))
                      .services(getApplicationServices(resource))
                      .build();
    }

    private Map<String, String> convertEnvironmentValuesToJson(Map<String, Object> envMap) {
        if (envMap == null) {
            return Collections.emptyMap();
        }
        return envMap.entrySet()
                     .stream()
                     .collect(Collectors.toMap(Entry::getKey, this::convertValueToString));
    }

    private String convertValueToString(Entry<String, Object> entry) {
        if (entry.getValue() instanceof String) {
            return (String) entry.getValue();
        }
        return JsonUtil.convertToJson(entry.getValue(), true);
    }

    private String getStackName(Map<String, Object> applicationResource) {
        Map<String, Object> stackResource = getEmbeddedResource(applicationResource, "stack");
        if (stackResource != null) {
            return mapStackResource(stackResource).getName();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<String> getApplicationServices(Map<String, Object> applicationResource) {
        List<Map<String, Object>> serviceBindings = getV2ResourceAttribute(applicationResource, "service_bindings", List.class);
        if (serviceBindings == null) {
            return Collections.emptyList();
        }
        return serviceBindings.stream()
                              .map(binding -> (Map<String, Object>) getV2ResourceAttribute(binding, "service_instance", Map.class))
                              .map(this::getV2ResourceName)
                              .filter(Objects::nonNull)
                              .collect(Collectors.toList());
    }

    private DockerInfo createDockerInfo(String dockerImage, Map<String, String> dockerCredentials) {
        if (dockerImage == null) {
            return null;
        }
        return ImmutableDockerInfo.builder()
                                  .image(dockerImage)
                                  .credentials(createDockerCredentials(dockerCredentials))
                                  .build();
    }

    private DockerCredentials createDockerCredentials(Map<String, String> dockerCredentials) {
        String username = dockerCredentials.get("username");
        String password = dockerCredentials.get("password");
        if (username == null || password == null) {
            return null;
        }
        return ImmutableDockerCredentials.builder()
                                         .username(username)
                                         .password(password)
                                         .build();
    }

    private CloudSecurityGroup mapApplicationSecurityGroupResource(Map<String, Object> resource) {
        return ImmutableCloudSecurityGroup.builder()
                                          .metadata(getV2Metadata(resource))
                                          .name(getV2ResourceName(resource))
                                          .isRunningDefault(getV2ResourceAttribute(resource, "running_default", Boolean.class))
                                          .isStagingDefault(getV2ResourceAttribute(resource, "staging_default", Boolean.class))
                                          .rules(getSecurityGroupRules(resource))
                                          .build();
    }

    private CloudDomain mapDomainResource(Map<String, Object> resource) {
        return ImmutableCloudDomain.builder()
                                   .metadata(getV2Metadata(resource))
                                   .name(getV2ResourceName(resource))
                                   .build();
    }

    private CloudEvent mapEventResource(Map<String, Object> resource) {
        return ImmutableCloudEvent.builder()
                                  .metadata(getV2Metadata(resource))
                                  .name(getV2ResourceName(resource))
                                  .actor(getActor(resource))
                                  .actee(getActee(resource))
                                  .timestamp(parseDate(getV2ResourceAttribute(resource, "timestamp", String.class)))
                                  .type(getV2ResourceAttribute(resource, "type", String.class))
                                  .build();
    }

    private Participant getActor(Map<String, Object> resource) {
        UUID actorGuid = getV2ResourceAttribute(resource, "actor", UUID.class);
        String actorType = getV2ResourceAttribute(resource, "actor_type", String.class);
        String actorName = getV2ResourceAttribute(resource, "actor_name", String.class);
        return ImmutableParticipant.builder()
                                   .guid(actorGuid)
                                   .name(actorName)
                                   .type(actorType)
                                   .build();
    }

    private Participant getActee(Map<String, Object> resource) {
        UUID acteeGuid = getV2ResourceAttribute(resource, "actee", UUID.class);
        String acteeType = getV2ResourceAttribute(resource, "actee_type", String.class);
        String acteeName = getV2ResourceAttribute(resource, "actee_name", String.class);
        return ImmutableParticipant.builder()
                                   .guid(acteeGuid)
                                   .name(acteeName)
                                   .type(acteeType)
                                   .build();
    }

    private CloudTask mapTaskResource(Map<String, Object> resource) {
        return ImmutableCloudTask.builder()
                                 .metadata(getV3Metadata(resource))
                                 .name(getV3ResourceName(resource))
                                 .command(getV3ResourceAttribute(resource, "command", String.class))
                                 .limits(getTaskLimits(resource))
                                 .result(getTaskResult(resource))
                                 .state(getTaskState(resource))
                                 .build();
    }

    private CloudTask.State getTaskState(Map<String, Object> resource) {
        String stateAsString = getV3ResourceAttribute(resource, "state", String.class);
        return stateAsString == null ? null : CloudTask.State.valueOf(stateAsString);
    }

    private CloudTask.Limits getTaskLimits(Map<String, Object> resource) {
        return ImmutableCloudTask.ImmutableLimits.builder()
                                                 .disk(getV3ResourceAttribute(resource, "disk_in_mb", Integer.class))
                                                 .memory(getV3ResourceAttribute(resource, "memory_in_mb", Integer.class))
                                                 .build();
    }

    private CloudTask.Result getTaskResult(Map<String, Object> resource) {
        @SuppressWarnings("unchecked")
        Map<String, Object> result = getV3ResourceAttribute(resource, "result", Map.class);
        String failureReason = getValue(result, "failure_reason", String.class);
        return ImmutableCloudTask.ImmutableResult.builder()
                                                 .failureReason(failureReason)
                                                 .build();
    }

    private CloudJob mapJobResource(Map<String, Object> resource) {
        return ImmutableCloudJob.builder()
                                .metadata(getV2Metadata(resource))
                                .status(getJobStatus(resource))
                                .errorDetails(getJobErrorDetails(resource))
                                .build();
    }

    private CloudJob.Status getJobStatus(Map<String, Object> resource) {
        String status = getV2ResourceAttribute(resource, "status", String.class);
        return CloudJob.Status.fromString(status);
    }

    @SuppressWarnings("unchecked")
    private ErrorDetails getJobErrorDetails(Map<String, Object> resource) {
        Map<String, Object> errorDetailsResource = getV2ResourceAttribute(resource, "error_details", Map.class);
        if (errorDetailsResource == null) {
            return null;
        }
        return ImmutableErrorDetails.builder()
                                    .code(getValue(errorDetailsResource, "code", Long.class))
                                    .description(getValue(errorDetailsResource, "description", String.class))
                                    .errorCode(getValue(errorDetailsResource, "error_code", String.class))
                                    .build();
    }

    private CloudOrganization mapOrganizationResource(Map<String, Object> resource) {
        return ImmutableCloudOrganization.builder()
                                         .metadata(getV2Metadata(resource))
                                         .name(getV2ResourceName(resource))
                                         .build();
    }

    private CloudQuota mapQuotaResource(Map<String, Object> resource) {
        return ImmutableCloudQuota.builder()
                                  .metadata(getV2Metadata(resource))
                                  .name(getV2ResourceName(resource))
                                  .build();
    }

    private CloudRoute mapRouteResource(Map<String, Object> resource) {
        @SuppressWarnings("unchecked")
        List<Object> apps = getV2ResourceAttribute(resource, "apps", List.class);
        String host = getV2ResourceAttribute(resource, "host", String.class);
        String path = getV2ResourceAttribute(resource, "path", String.class);
        boolean hasBoundService = getV2ResourceAttribute(resource, "service_instance_guid", String.class) != null;
        CloudDomain domain = mapDomainResource(getEmbeddedResource(resource, "domain"));
        return ImmutableCloudRoute.builder()
                                  .metadata(getV2Metadata(resource))
                                  .host(host)
                                  .domain(domain)
                                  .path(path)
                                  .appsUsingRoute(apps.size())
                                  .hasServiceUsingRoute(hasBoundService)
                                  .build();
    }

    @SuppressWarnings("unchecked")
    private CloudServiceBinding mapServiceBinding(Map<String, Object> resource) {
        Map<String, Object> bindingParameters = getV2ResourceAttribute(resource, "service_binding_parameters", Map.class);
        bindingParameters = bindingParameters == null ? Collections.emptyMap() : bindingParameters;
        return ImmutableCloudServiceBinding.builder()
                                           .metadata(getV2Metadata(resource))
                                           .name(getV2ResourceName(resource))
                                           .applicationGuid(getV2ResourceAttribute(resource, "app_guid", UUID.class))
                                           .syslogDrainUrl(getV2ResourceAttribute(resource, "syslog_drain_url", String.class))
                                           .credentials(getV2ResourceAttribute(resource, "credentials", Map.class))
                                           .bindingOptions(getV2ResourceAttribute(resource, "binding_options", Map.class))
                                           .bindingParameters(bindingParameters)
                                           .build();
    }

    private CloudServiceBroker mapServiceBrokerResource(Map<String, Object> resource) {
        return ImmutableCloudServiceBroker.builder()
                                          .metadata(getV2Metadata(resource))
                                          .name(getV2ResourceAttribute(resource, "name", String.class))
                                          .url(getV2ResourceAttribute(resource, "broker_url", String.class))
                                          .username(getV2ResourceAttribute(resource, "auth_username", String.class))
                                          .spaceGuid(getV2ResourceAttribute(resource, "space_guid", String.class))
                                          .build();
    }

    @SuppressWarnings("unchecked")
    private CloudServiceInstance mapServiceInstanceResource(Map<String, Object> resource) {
        ImmutableCloudServiceInstance.Builder builder = ImmutableCloudServiceInstance.builder()
                                                                                     .metadata(getV2Metadata(resource))
                                                                                     .name(getV2ResourceName(resource));

        builder.type(getV2ResourceAttribute(resource, "type", String.class));
        builder.dashboardUrl(getV2ResourceAttribute(resource, "dashboard_url", String.class));
        builder.credentials(getV2ResourceAttribute(resource, "credentials", Map.class));

        CloudService service = mapServiceResource(resource);
        builder.service(service);

        List<Map<String, Object>> bindingsResource = getEmbeddedResourceList(getEntity(resource), "service_bindings");
        List<CloudServiceBinding> bindings = bindingsResource.stream()
                                                             .map(this::mapServiceBinding)
                                                             .collect(Collectors.toList());
        builder.bindings(bindings);

        return builder.build();
    }

    private CloudServiceOffering mapServiceOfferingResource(Map<String, Object> resource) {
        ImmutableCloudServiceOffering.Builder builder = ImmutableCloudServiceOffering.builder()
                                                                                     .metadata(getV2Metadata(resource))
                                                                                     .name(getV2ResourceAttribute(resource, "label",
                                                                                                                  String.class))
                                                                                     .provider(getV2ResourceAttribute(resource, "provider",
                                                                                                                      String.class))
                                                                                     .version(getV2ResourceAttribute(resource, "version",
                                                                                                                     String.class))
                                                                                     .description(getV2ResourceAttribute(resource,
                                                                                                                         "description",
                                                                                                                         String.class))
                                                                                     .isActive(getV2ResourceAttribute(resource, "active",
                                                                                                                      Boolean.class))
                                                                                     .isBindable(getV2ResourceAttribute(resource,
                                                                                                                        "bindable",
                                                                                                                        Boolean.class))
                                                                                     .url(getV2ResourceAttribute(resource, "url",
                                                                                                                 String.class))
                                                                                     .infoUrl(getV2ResourceAttribute(resource, "info_url",
                                                                                                                     String.class))
                                                                                     .uniqueId(getV2ResourceAttribute(resource, "unique_id",
                                                                                                                      String.class))
                                                                                     .extra(getV2ResourceAttribute(resource, "extra",
                                                                                                                   String.class))
                                                                                     .docUrl(getV2ResourceAttribute(resource,
                                                                                                                    "documentation_url",
                                                                                                                    String.class));
        List<Map<String, Object>> servicePlanList = getEmbeddedResourceList(getEntity(resource), "service_plans");
        if (servicePlanList != null) {
            for (Map<String, Object> servicePlanResource : servicePlanList) {
                CloudServicePlan servicePlan = mapServicePlanResource(servicePlanResource);
                builder.addServicePlan(servicePlan);
            }
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private CloudServiceKey mapServiceKeyResource(Map<String, Object> resource) {
        return ImmutableCloudServiceKey.builder()
                                       .metadata(getV2Metadata(resource))
                                       .name(getV2ResourceAttribute(resource, "name", String.class))
                                       .credentials(getV2ResourceAttribute(resource, "credentials", Map.class))
                                       .build();
    }

    private CloudServicePlan mapServicePlanResource(Map<String, Object> servicePlanResource) {
        Boolean publicPlan = getV2ResourceAttribute(servicePlanResource, "public", Boolean.class);
        publicPlan = publicPlan == null ? true : publicPlan;
        return ImmutableCloudServicePlan.builder()
                                        .metadata(getV2Metadata(servicePlanResource))
                                        .name(getV2ResourceAttribute(servicePlanResource, "name", String.class))
                                        .description(getV2ResourceAttribute(servicePlanResource, "description", String.class))
                                        .isFree(getV2ResourceAttribute(servicePlanResource, "free", Boolean.class))
                                        .extra(getV2ResourceAttribute(servicePlanResource, "extra", String.class))
                                        .uniqueId(getV2ResourceAttribute(servicePlanResource, "unique_id", String.class))
                                        .isPublic(publicPlan)
                                        .build();
    }

    private CloudService mapServiceResource(Map<String, Object> resource) {
        ImmutableCloudService.Builder builder = ImmutableCloudService.builder()
                                                                     .metadata(getV2Metadata(resource))
                                                                     .name(getV2ResourceName(resource));
        Map<String, Object> servicePlanResource = getEmbeddedResource(resource, "service_plan");
        if (servicePlanResource != null) {
            builder.plan(getV2ResourceAttribute(servicePlanResource, "name", String.class));

            Map<String, Object> serviceResource = getEmbeddedResource(servicePlanResource, "service");
            if (serviceResource != null) {
                builder.label(getV2ResourceAttribute(serviceResource, "label", String.class));
                builder.provider(getV2ResourceAttribute(serviceResource, "provider", String.class));
                builder.version(getV2ResourceAttribute(serviceResource, "version", String.class));
            }
        }
        return builder.build();
    }

    private CloudSpace mapSpaceResource(Map<String, Object> resource) {
        Map<String, Object> organizationMap = getEmbeddedResource(resource, "organization");
        CloudOrganization organization = null;
        if (organizationMap != null) {
            organization = mapOrganizationResource(organizationMap);
        }
        return ImmutableCloudSpace.builder()
                                  .metadata(getV2Metadata(resource))
                                  .name(getV2ResourceName(resource))
                                  .organization(organization)
                                  .build();
    }

    private CloudStack mapStackResource(Map<String, Object> resource) {
        return ImmutableCloudStack.builder()
                                  .metadata(getV2Metadata(resource))
                                  .name(getV2ResourceName(resource))
                                  .description(getV2ResourceAttribute(resource, "description", String.class))
                                  .build();
    }

    private CloudUser mapUserResource(Map<String, Object> resource) {
        boolean isActiveUser = getV2ResourceAttribute(resource, "active", Boolean.class);
        boolean isAdminUser = getV2ResourceAttribute(resource, "admin", Boolean.class);
        String defaultSpaceGuid = getV2ResourceAttribute(resource, "default_space_guid", String.class);
        String username = getV2ResourceAttribute(resource, "username", String.class);
        return ImmutableCloudUser.builder()
                                 .metadata(getV2Metadata(resource))
                                 .name(username)
                                 .isAdmin(isAdminUser)
                                 .isActive(isActiveUser)
                                 .defaultSpaceGuid(defaultSpaceGuid)
                                 .build();
    }
}
