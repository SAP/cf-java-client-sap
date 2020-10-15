package org.cloudfoundry.client.lib.domain;

import java.util.Map;
import java.util.UUID;

import org.cloudfoundry.client.lib.domain.annotation.Nullable;
import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Value.Immutable
@JsonSerialize(as = ImmutableCloudServiceBinding.class)
@JsonDeserialize(as = ImmutableCloudServiceBinding.class)
public abstract class CloudServiceBinding extends CloudEntity implements Derivable<CloudServiceBinding> {

    public abstract UUID getApplicationGuid();

    @Nullable
    public abstract Map<String, Object> getBindingOptions();

    @Nullable
    public abstract Map<String, Object> getCredentials();

    @Nullable
    public abstract String getSyslogDrainUrl();

    @Override
    public CloudServiceBinding derive() {
        return this;
    }

}
