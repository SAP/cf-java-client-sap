package com.sap.cloudfoundry.client.facade.domain;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.sap.cloudfoundry.client.facade.Nullable;
import org.cloudfoundry.AllowNulls;
import org.immutables.value.Value;

import java.util.Map;

@Value.Immutable
@JsonSerialize(as = ImmutableLifecycle.class)
@JsonDeserialize(as = ImmutableLifecycle.class)
public interface Lifecycle {

    LifecycleType getType();

    @Nullable
    @AllowNulls
    Map<String, Object> getData();

}
