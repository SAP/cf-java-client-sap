package org.cloudfoundry.client.lib.adapters;

import org.cloudfoundry.client.lib.domain.CloudPackage;
import org.cloudfoundry.client.lib.domain.ImmutableCloudPackage;
import org.cloudfoundry.client.lib.domain.Status;
import org.cloudfoundry.client.v3.Checksum;
import org.cloudfoundry.client.v3.ChecksumType;
import org.cloudfoundry.client.v3.packages.BitsData;
import org.cloudfoundry.client.v3.packages.DockerData;
import org.cloudfoundry.client.v3.packages.Package;
import org.cloudfoundry.client.v3.packages.PackageData;
import org.cloudfoundry.client.v3.packages.PackageResource;
import org.cloudfoundry.client.v3.packages.PackageState;
import org.cloudfoundry.client.v3.packages.PackageType;
import org.junit.jupiter.api.Test;

public class RawCloudPackageTest {

    private static final PackageState STATE = PackageState.EXPIRED;
    private static final String ERROR = "blabla";
    private static final ChecksumType CHECKSUM_TYPE = ChecksumType.SHA256;
    private static final String CHECKSUM_VALUE = "7251a608605c0f45710f8415bba0d117c90598824697a2a5d00850ea9b179112";

    private static final Status EXPECTED_STATUS = Status.EXPIRED;
    private static final String EXPECTED_CHECKSUM_ALGORITHM = CHECKSUM_TYPE.toString();

    @Test
    public void testDeriveWithBitsData() {
        RawCloudEntityTest.testDerive(buildExpectedPackage(CloudPackage.Type.BITS), buildRawPackage(PackageType.BITS));
    }

    @Test
    public void testDeriveWithDockerData() {
        RawCloudEntityTest.testDerive(buildExpectedPackage(CloudPackage.Type.DOCKER), buildRawPackage(PackageType.DOCKER));
    }

    private static CloudPackage buildExpectedPackage(CloudPackage.Type type) {
        return ImmutableCloudPackage.builder()
            .metadata(RawCloudEntityTest.EXPECTED_METADATA_PARSED_FROM_V3_RESOURCE)
            .data(buildExpectedData(type))
            .type(type)
            .status(EXPECTED_STATUS)
            .build();
    }

    private static CloudPackage.Data buildExpectedData(CloudPackage.Type type) {
        if (type == CloudPackage.Type.DOCKER) {
            return null;
        }
        return ImmutableCloudPackage.ImmutableData.builder()
            .checksum(buildExpectedChecksum())
            .error(ERROR)
            .build();
    }

    private static CloudPackage.Checksum buildExpectedChecksum() {
        return ImmutableCloudPackage.ImmutableChecksum.builder()
            .algorithm(EXPECTED_CHECKSUM_ALGORITHM)
            .value(CHECKSUM_VALUE)
            .build();
    }

    private static RawCloudPackage buildRawPackage(PackageType type) {
        return ImmutableRawCloudPackage.builder()
            .resource(buildTestResource(type))
            .build();
    }

    private static Package buildTestResource(PackageType type) {
        return PackageResource.builder()
            .id(RawCloudEntityTest.GUID_STRING)
            .createdAt(RawCloudEntityTest.CREATED_AT_STRING)
            .updatedAt(RawCloudEntityTest.UPDATED_AT_STRING)
            .type(type)
            .data(buildTestData(type))
            .state(STATE)
            .build();
    }

    private static PackageData buildTestData(PackageType type) {
        if (type == PackageType.DOCKER) {
            return buildDockerData();
        }
        return buildBitsData();
    }

    private static DockerData buildDockerData() {
        return DockerData.builder()
            .image("")
            .username("")
            .password("")
            .build();
    }

    private static BitsData buildBitsData() {
        return BitsData.builder()
            .checksum(buildTestChecksum())
            .error(ERROR)
            .build();
    }

    private static Checksum buildTestChecksum() {
        return Checksum.builder()
            .type(CHECKSUM_TYPE)
            .value(CHECKSUM_VALUE)
            .build();
    }

}
