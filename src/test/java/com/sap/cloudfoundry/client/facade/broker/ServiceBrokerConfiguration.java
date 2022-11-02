package com.sap.cloudfoundry.client.facade.broker;

import java.util.List;

import org.immutables.value.Value;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;

@Value.Immutable
@JsonSerialize(as = ImmutableServiceBrokerConfiguration.class)
@JsonDeserialize(as = ImmutableServiceBrokerConfiguration.class)
public interface ServiceBrokerConfiguration {

    @Nullable
    Integer getAsyncDurationInMillis();

    @Nullable
    Integer getSyncDurationInMillis();

    List<FailConfiguration> getFailConfigurations();

}
