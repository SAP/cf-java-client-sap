package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.AllowNulls;
import org.cloudfoundry.client.v3.servicebindings.ServiceBinding;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.Nullable;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceKey;
import com.sap.cloudfoundry.client.facade.domain.Derivable;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudServiceKey;

import java.util.Map;

@Value.Immutable
public abstract class RawCloudServiceKey extends RawCloudEntity<CloudServiceKey> {

    public abstract ServiceBinding getServiceBinding();

    @Nullable
    @AllowNulls
    public abstract Map<String, Object> getCredentials();

    public abstract Derivable<CloudServiceInstance> getServiceInstance();

    @Override
    public CloudServiceKey derive() {
        ServiceBinding serviceBinding = getServiceBinding();
        return ImmutableCloudServiceKey.builder()
                                       .metadata(parseResourceMetadata(serviceBinding))
                                       .name(serviceBinding.getName())
                                       .credentials(getCredentials())
                                       .serviceInstance(getServiceInstance().derive())
                                       .build();
    }

}
