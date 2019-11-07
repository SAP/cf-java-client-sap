package org.cloudfoundry.client.lib.adapters;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.lib.util.CloudEntityResourceMapper;
import org.cloudfoundry.client.v2.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class RawCloudEntity<T> implements Derivable<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudEntityResourceMapper.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

    protected RawCloudEntity() {
        // Recommended by Sonar.
    }

    protected static CloudMetadata parseResourceMetadata(org.cloudfoundry.client.v2.Resource<?> resource) {
        Metadata metadata = resource.getMetadata();
        return ImmutableCloudMetadata.builder()
                                     .guid(parseNullableGuid(metadata.getId()))
                                     .createdAt(parseNullableDate(metadata.getCreatedAt()))
                                     .updatedAt(parseNullableDate(metadata.getUpdatedAt()))
                                     .url(metadata.getUrl())
                                     .build();
    }

    protected static CloudMetadata parseResourceMetadata(org.cloudfoundry.client.v3.Resource resource) {
        return ImmutableCloudMetadata.builder()
                                     .guid(parseNullableGuid(resource.getId()))
                                     .createdAt(parseNullableDate(resource.getCreatedAt()))
                                     .updatedAt(parseNullableDate(resource.getUpdatedAt()))
                                     .build();
    }

    protected static UUID parseNullableGuid(String guid) {
        return guid == null ? null : parseGuid(guid);
    }

    protected static UUID parseGuid(String guid) {
        try {
            return UUID.fromString(guid);
        } catch (IllegalArgumentException e) {
            LOGGER.warn(MessageFormat.format("Could not parse GUID string: \"{0}\"", guid), e);
            return null;
        }
    }

    protected static Date parseNullableDate(String date) {
        return date == null ? null : parseDate(date);
    }

    protected static Date parseDate(String dateString) {
        try {
            Instant instant = parseInstant(dateString);
            return Date.from(instant);
        } catch (DateTimeParseException e) {
            LOGGER.warn(MessageFormat.format("Could not parse date string: \"{0}\"", dateString), e);
            return null;
        }
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

    protected static <E extends Enum<E>> E parseEnum(Enum<?> value, Class<E> targetEnum) {
        String name = value.name()
                           .toUpperCase();
        return Enum.valueOf(targetEnum, name);
    }

    protected static <D> D deriveFromNullable(Derivable<D> derivable) {
        return derivable == null ? null : derivable.derive();
    }

    protected static <D> Collection<D> derive(Collection<Derivable<D>> derivables) {
        return derivables.stream()
                         .map(Derivable::derive)
                         .collect(Collectors.toList());
    }

}
