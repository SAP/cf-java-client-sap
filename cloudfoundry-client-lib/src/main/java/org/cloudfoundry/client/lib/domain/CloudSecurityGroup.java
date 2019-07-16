package org.cloudfoundry.client.lib.domain;

import java.util.List;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudSecurityGroup.class)
@JsonDeserialize(as = ImmutableCloudSecurityGroup.class)
public interface CloudSecurityGroup extends CloudEntity, Derivable<CloudSecurityGroup> {

    List<SecurityGroupRule> getRules();

    @Nullable
    Boolean isRunningDefault();

    @Nullable
    Boolean isStagingDefault();

    @Override
    default CloudSecurityGroup derive() {
        return this;
    }

}
