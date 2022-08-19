package com.sap.cloudfoundry.client.facade.adapters;

import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.applications.Application;
import org.cloudfoundry.client.v3.applications.ApplicationState;
import org.immutables.value.Value;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.Derivable;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudApplication;
import com.sap.cloudfoundry.client.facade.domain.ImmutableLifecycle;
import com.sap.cloudfoundry.client.facade.domain.Lifecycle;
import com.sap.cloudfoundry.client.facade.domain.LifecycleType;

import java.util.HashMap;
import java.util.Map;

@Value.Immutable
public abstract class RawCloudApplication extends RawCloudEntity<CloudApplication> {

    public abstract Application getApplication();

    public abstract Derivable<CloudSpace> getSpace();

    @Override
    public CloudApplication derive() {
        Application app = getApplication();
        return ImmutableCloudApplication.builder()
                                        .metadata(parseResourceMetadata(app))
                                        .v3Metadata(app.getMetadata())
                                        .name(app.getName())
                                        .state(parseState(app.getState()))
                                        .lifecycle(parseLifecycle(app.getLifecycle()))
                                        .space(getSpace().derive())
                                        .build();
    }

    private static CloudApplication.State parseState(ApplicationState state) {
        return CloudApplication.State.valueOf(state.getValue());
    }

    private static Lifecycle parseLifecycle(org.cloudfoundry.client.v3.Lifecycle lifecycle) {
        Map<String, Object> data = new HashMap<>();
        if (lifecycle.getType() == org.cloudfoundry.client.v3.LifecycleType.BUILDPACK) {
            var buildpackData = (BuildpackData) lifecycle.getData();
            data.put("buildpacks", buildpackData.getBuildpacks());
            data.put("stack", buildpackData.getStack());
        }
        return ImmutableLifecycle.builder()
                                 .type(LifecycleType.valueOf(lifecycle.getType()
                                                                      .toString()
                                                                      .toUpperCase()))
                                 .data(data)
                                 .build();
    }

}
