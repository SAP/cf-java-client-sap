package com.sap.cloudfoundry.client.facade;

import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudBuild;
import com.sap.cloudfoundry.client.facade.domain.CloudPackage;
import com.sap.cloudfoundry.client.facade.domain.InstanceInfo;
import com.sap.cloudfoundry.client.facade.domain.InstanceState;
import com.sap.cloudfoundry.client.facade.domain.Status;

public class ApplicationUtil {

    public static CloudPackage uploadApplication(CloudControllerClient client, String applicationName, Path pathToFile)
        throws InterruptedException {
        CloudPackage cloudPackage = client.asyncUploadApplication(applicationName, pathToFile, Duration.ofMinutes(4));
        while (cloudPackage.getStatus() != Status.READY && !hasUploadFailed(cloudPackage.getStatus())) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            cloudPackage = client.getPackage(cloudPackage.getGuid());
        }
        if (hasUploadFailed(cloudPackage.getStatus())) {
            fail(MessageFormat.format("Cloud package is in invalid state: \"{0}\"", cloudPackage.getStatus()));
        }
        return cloudPackage;
    }

    private static boolean hasUploadFailed(Status status) {
        return status == Status.EXPIRED || status == Status.FAILED;
    }

    public static void stageApplication(CloudControllerClient client, String applicationName, CloudPackage cloudPackage)
        throws InterruptedException {
        UUID applicationGuid = client.getApplicationGuid(applicationName);
        CloudBuild build = createBuildForPackage(client, cloudPackage);
        client.bindDropletToApp(build.getDropletInfo()
                                     .getGuid(),
                                applicationGuid);
    }

    public static CloudBuild createBuildForPackage(CloudControllerClient client, CloudPackage cloudPackage) throws InterruptedException {
        CloudBuild build = client.createBuild(cloudPackage.getGuid());
        while (build.getState() == CloudBuild.State.STAGING) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            build = client.getBuild(build.getGuid());
        }
        if (build.getState() == CloudBuild.State.FAILED) {
            fail(MessageFormat.format("Cloud build is in invalid state: \"{0}\"", build.getState()));
        }
        return build;
    }

    public static void startApplication(CloudControllerClient client, String applicationName) throws InterruptedException {
        client.startApplication(applicationName);

        CloudApplication app = client.getApplication(applicationName);
        List<InstanceInfo> appInstances = client.getApplicationInstances(app)
                                                .getInstances();
        while (!hasAppInstancesStarted(appInstances) && !hasAppInstancesFailed(appInstances)) {
            Thread.sleep(TimeUnit.SECONDS.toMillis(1));
            appInstances = client.getApplicationInstances(app)
                                 .getInstances();
        }

        if (hasAppInstancesFailed(appInstances)) {
            fail(MessageFormat.format("Some or all application instances of application \"{0}\" failed to start", applicationName));
        }
    }

    private static boolean hasAppInstancesStarted(List<InstanceInfo> appInstances) {
        return appInstances.stream()
                           .allMatch(appInstance -> appInstance.getState()
                                                               .equals(InstanceState.RUNNING));
    }

    private static boolean hasAppInstancesFailed(List<InstanceInfo> appInstances) {
        return appInstances.stream()
                           .anyMatch(appInstance -> hasAppInstanceStartFailed(appInstance.getState()));
    }

    private static boolean hasAppInstanceStartFailed(InstanceState instanceState) {
        return instanceState == InstanceState.DOWN || instanceState == InstanceState.CRASHED;
    }

}
