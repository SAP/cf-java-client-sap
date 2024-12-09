package com.sap.cloudfoundry.client.facade.adapters;

import java.util.HashMap;
import java.util.Map;

import org.cloudfoundry.client.v3.BuildpackData;
import org.cloudfoundry.client.v3.CnbData;
import org.cloudfoundry.client.v3.LifecycleData;
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

@Value.Immutable
public abstract class RawCloudApplication extends RawCloudEntity<CloudApplication> {

    public static final String BUILDPACKS = "buildpacks";
    public static final String STACK = "stack";

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
        org.cloudfoundry.client.v3.LifecycleType lifecycleType = lifecycle.getType();

        if (isBuildpackOrCnb(lifecycleType)) {
            addLifecycleData(data, lifecycle.getData());
        }

        return ImmutableLifecycle.builder()
                                 .type(LifecycleType.valueOf(lifecycle.getType()
                                                                      .toString()
                                                                      .toUpperCase()))
                                 .data(data)
                                 .build();
    }

    private static boolean isBuildpackOrCnb(org.cloudfoundry.client.v3.LifecycleType lifecycleType) {
        return lifecycleType == org.cloudfoundry.client.v3.LifecycleType.BUILDPACK
            || lifecycleType == org.cloudfoundry.client.v3.LifecycleType.CNB;
    }

    private static void addLifecycleData(Map<String, Object> data, LifecycleData lifecycleData) {
        if (lifecycleData instanceof BuildpackData buildpackData) {
            data.put(BUILDPACKS, buildpackData.getBuildpacks());
            data.put(STACK, buildpackData.getStack());
        } else if (lifecycleData instanceof CnbData cnbData) {
            data.put(BUILDPACKS, cnbData.getBuildpacks());
            data.put(STACK, cnbData.getStack());
        }
    }

}
