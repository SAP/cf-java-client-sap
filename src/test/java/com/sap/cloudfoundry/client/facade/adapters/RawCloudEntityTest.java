package com.sap.cloudfoundry.client.facade.adapters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.cloudfoundry.client.v2.Metadata;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.ToOneRelationship;
import org.cloudfoundry.client.v3.applications.ApplicationState;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudBuild;
import com.sap.cloudfoundry.client.facade.domain.CloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.Derivable;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;

class RawCloudEntityTest {

    static final String GUID_STRING = "3725650a-8725-4401-a949-c68f83d54a86";
    static final String CREATED_AT_STRING = "2017-06-22T13:38:41Z";
    static final String UPDATED_AT_STRING = "2019-03-21T12:29:24Z";
    static final String URL_STRING = "/v2/apps/3725650a-8725-4401-a949-c68f83d54a86";

    static final String NAME = "foo";
    static final UUID GUID = UUID.fromString(GUID_STRING);
    static final LocalDateTime CREATED_AT = fromZonedDateTime(ZonedDateTime.of(2017, 6, 22, 13, 38, 41, 0, ZoneId.of("Z")));
    static final LocalDateTime UPDATED_AT = fromZonedDateTime(ZonedDateTime.of(2019, 3, 21, 12, 29, 24, 0, ZoneId.of("Z")));

    static final org.cloudfoundry.client.v3.Metadata V3_METADATA = org.cloudfoundry.client.v3.Metadata.builder()
                                                                                                      .build();
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

    static final CloudMetadata EXPECTED_METADATA_V3 = ImmutableCloudMetadata.builder()
                                                                            .guid(GUID)
                                                                            .createdAt(CREATED_AT)
                                                                            .updatedAt(UPDATED_AT)
                                                                            .build();

    static LocalDateTime fromZonedDateTime(ZonedDateTime dateTime) {
        return dateTime.toLocalDateTime();
    }

    @Test
    void testParseV2ResourceMetadata() {
        org.cloudfoundry.client.v2.applications.ApplicationResource resource = org.cloudfoundry.client.v2.applications.ApplicationResource.builder()
                                                                                                                                          .metadata(METADATA)
                                                                                                                                          .build();

        CloudMetadata metadata = RawCloudEntity.parseResourceMetadata(resource);
        assertEquals(EXPECTED_METADATA, metadata);
    }

    @Test
    void testParseV3ResourceMetadata() {
        org.cloudfoundry.client.v3.organizations.Organization resource = org.cloudfoundry.client.v3.organizations.OrganizationResource.builder()
                                                                                                                                      .id(GUID_STRING)
                                                                                                                                      .createdAt(CREATED_AT_STRING)
                                                                                                                                      .updatedAt(UPDATED_AT_STRING)
                                                                                                                                      .name(NAME)
                                                                                                                                      .metadata(V3_METADATA)
                                                                                                                                      .build();

        CloudMetadata metadata = RawCloudEntity.parseResourceMetadata(resource);
        assertEquals(EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE, metadata);
    }

    @Test
    void testParseNullableGuid() {
        assertNull(RawCloudEntity.parseNullableGuid(null));
    }

    @Test
    void testParseNullableDate() {
        assertNull(RawCloudEntity.parseNullableDate(null));
    }

    @Test
    void testParseGuid() {
        assertEquals(GUID, RawCloudEntity.parseGuid(GUID_STRING));
    }

    @Test
    void testParseGuidWithInvalidGuid() {
        assertNull(RawCloudEntity.parseGuid("foo"));
    }

    @Test
    void testParseDateWithInvalidDate() {
        assertNull(RawCloudEntity.parseDate("foo"));
    }

    @Test
    void testParseDateWithInvalidFormat() {
        assertNull(RawCloudEntity.parseDate("16.07.2019 15:30:25"));
    }

    @Test
    void testParseDate() {
        assertEquals(CREATED_AT, RawCloudEntity.parseDate(CREATED_AT_STRING));
    }

    @Test
    void testParseEnum() {
        CloudApplication.State state = RawCloudEntity.parseEnum(ApplicationState.STARTED, CloudApplication.State.class);
        assertEquals(CloudApplication.State.STARTED, state);
    }

    @Test
    void testParseEnumWithIncompatibleEnumTypes() {
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> RawCloudEntity.parseEnum(ApplicationState.STARTED, CloudBuild.State.class));
    }

    @Test
    void testDeriveFromNullable() {
        assertEquals(NAME, RawCloudEntity.deriveFromNullable(() -> NAME));
    }

    @Test
    void testDeriveFromNullableWithNull() {
        assertNull(RawCloudEntity.deriveFromNullable(null));
    }

    @Test
    void testDerive() {
        List<Derivable<String>> derivables = Arrays.asList(() -> NAME, () -> GUID_STRING);
        assertEquals(Arrays.asList(NAME, GUID_STRING), RawCloudEntity.derive(derivables));
    }

    static <T> void testDerive(T expected, Derivable<T> derivable) {
        assertEquals(expected, derivable.derive());
    }

    static ToOneRelationship buildToOneRelationship(String id) {
        return ToOneRelationship.builder()
                                .data(Relationship.builder()
                                                  .id(id)
                                                  .build())
                                .build();
    }

}
