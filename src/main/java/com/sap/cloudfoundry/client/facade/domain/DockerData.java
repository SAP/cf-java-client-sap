package com.sap.cloudfoundry.client.facade.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableDockerData.class)
@JsonDeserialize(as = ImmutableDockerData.class)
public interface DockerData extends CloudPackage.PackageData {

    String getImage();

    @Nullable
    String getUsername();

    @Nullable
    String getPassword();

}
