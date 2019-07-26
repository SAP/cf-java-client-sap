package org.cloudfoundry.client.lib.adapters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudBuild;
import org.cloudfoundry.client.lib.domain.CloudMetadata;
import org.cloudfoundry.client.lib.domain.Derivable;
import org.cloudfoundry.client.lib.domain.ImmutableCloudMetadata;
import org.cloudfoundry.client.v2.Metadata;
import org.cloudfoundry.client.v3.applications.ApplicationState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RawCloudEntityTest {

    static final String GUID_STRING = "3725650a-8725-4401-a949-c68f83d54a86";
    static final String CREATED_AT_STRING = "2017-06-22T13:38:41Z";
    static final String UPDATED_AT_STRING = "2019-03-21T12:29:24Z";
    static final String URL_STRING = "/v2/apps/3725650a-8725-4401-a949-c68f83d54a86";

    static final String NAME = "foo";
    static final UUID GUID = UUID.fromString(GUID_STRING);
    static final Date CREATED_AT = fromZonedDateTime(ZonedDateTime.of(2017, 6, 22, 13, 38, 41, 0, ZoneId.of("Z")));
    static final Date UPDATED_AT = fromZonedDateTime(ZonedDateTime.of(2019, 3, 21, 12, 29, 24, 0, ZoneId.of("Z")));

    static final Metadata METADATA = Metadata.builder()
        .id(GUID_STRING)
        .createdAt(CREATED_AT_STRING)
        .updatedAt(UPDATED_AT_STRING)
        .url(URL_STRING)
        .build();

    static final CloudMetadata EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE = ImmutableCloudMetadata.builder()
        .guid(GUID)
        .createdAt(CREATED_AT)
        .updatedAt(UPDATED_AT)
        .build();
    static final CloudMetadata EXPECTED_METADATA = ImmutableCloudMetadata.builder()
        .guid(GUID)
        .createdAt(CREATED_AT)
        .updatedAt(UPDATED_AT)
        .url(URL_STRING)
        .build();

    static Date fromZonedDateTime(ZonedDateTime dateTime) {
        return Date.from(dateTime.toInstant());
    }

    @Test
    public void testParseV2ResourceMetadata() {
        org.cloudfoundry.client.v2.applications.ApplicationResource resource = org.cloudfoundry.client.v2.applications.ApplicationResource
            .builder()
            .metadata(Metadata.builder()
                .id(GUID_STRING)
                .createdAt(CREATED_AT_STRING)
                .updatedAt(UPDATED_AT_STRING)
                .url(URL_STRING)
                .build())
            .build();

        CloudMetadata metadata = RawCloudEntity.parseResourceMetadata(resource);
        assertEquals(EXPECTED_METADATA, metadata);
    }

    @Test
    public void testParseV3ResourceMetadata() {
        org.cloudfoundry.client.v3.organizations.Organization resource = org.cloudfoundry.client.v3.organizations.OrganizationResource
            .builder()
            .id(GUID_STRING)
            .createdAt(CREATED_AT_STRING)
            .updatedAt(UPDATED_AT_STRING)
            .name(NAME)
            .build();

        CloudMetadata metadata = RawCloudEntity.parseResourceMetadata(resource);
        assertEquals(EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE, metadata);
    }

    @Test
    public void testParseNullableGuid() {
        assertNull(RawCloudEntity.parseNullableGuid(null));
    }

    @Test
    public void testParseNullableDate() {
        assertNull(RawCloudEntity.parseNullableDate(null));
    }

    @Test
    public void testParseGuid() {
        assertEquals(GUID, RawCloudEntity.parseGuid(GUID_STRING));
    }

    @Test
    public void testParseGuidWithInvalidGuid() {
        assertNull(RawCloudEntity.parseGuid("foo"));
    }

    @Test
    public void testParseDateWithInvalidDate() {
        assertNull(RawCloudEntity.parseDate("foo"));
    }

    @Test
    public void testParseDateWithInvalidFormat() {
        assertNull(RawCloudEntity.parseDate("16.07.2019 15:30:25"));
    }

    @Test
    public void testParseDate() {
        assertEquals(CREATED_AT, RawCloudEntity.parseDate(CREATED_AT_STRING));
    }

    @Test
    public void testParseEnum() {
        CloudApplication.State state = RawCloudEntity.parseEnum(ApplicationState.STARTED, CloudApplication.State.class);
        assertEquals(CloudApplication.State.STARTED, state);
    }

    @Test
    public void testParseEnumWithIncompatibleEnumTypes() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> RawCloudEntity.parseEnum(ApplicationState.STARTED, CloudBuild.State.class));
    }

    @Test
    public void testDeriveFromNullable() {
        assertEquals(NAME, RawCloudEntity.deriveFromNullable(() -> NAME));
    }

    @Test
    public void testDeriveFromNullableWithNull() {
        assertNull(RawCloudEntity.deriveFromNullable(null));
    }

    @Test
    public void testDerive() {
        List<Derivable<String>> derivables = Arrays.asList(() -> NAME, () -> GUID_STRING);
        assertEquals(Arrays.asList(NAME, GUID_STRING), RawCloudEntity.derive(derivables));
    }

    static <T> void testDerive(T expected, Derivable<T> derivable) {
        assertEquals(expected, derivable.derive());
    }

}
