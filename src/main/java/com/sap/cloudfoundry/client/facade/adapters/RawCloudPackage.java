package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v3.Checksum;
import org.cloudfoundry.client.v3.packages.BitsData;
import org.cloudfoundry.client.v3.packages.Package;
import org.cloudfoundry.client.v3.packages.PackageType;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudPackage;
import com.sap.cloudfoundry.client.facade.domain.Status;

@Value.Immutable
public abstract class RawCloudPackage extends RawCloudEntity<CloudPackage> {

    @Value.Parameter
    public abstract Package getResource();

    @Override
    public CloudPackage derive() {
        Package resource = getResource();
        return ImmutableCloudPackage.builder()
                                    .metadata(parseResourceMetadata(resource))
                                    .status(parseStatus(resource))
                                    .data(parseData(resource))
                                    .type(parseType(resource))
                                    .build();
    }

    private static Status parseStatus(Package resource) {
        return parseEnum(resource.getState(), Status.class);
    }

    private static CloudPackage.Data parseData(Package resource) {
        if (resource.getType() == PackageType.BITS) {
            return parseBitsData((BitsData) resource.getData());
        }
        return null;
    }

    private static CloudPackage.Data parseBitsData(BitsData data) {
        return ImmutableCloudPackage.ImmutableData.builder()
                                                  .checksum(parseChecksum(data.getChecksum()))
                                                  .error(data.getError())
                                                  .build();
    }

    private static CloudPackage.Checksum parseChecksum(Checksum checksum) {
        return ImmutableCloudPackage.ImmutableChecksum.builder()
                                                      .algorithm(checksum.getType()
                                                                         .toString())
                                                      .value(checksum.getValue())
                                                      .build();
    }

    private static CloudPackage.Type parseType(Package resource) {
        return parseEnum(resource.getType(), CloudPackage.Type.class);
    }

}
