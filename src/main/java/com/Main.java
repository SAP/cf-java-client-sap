package com;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.UUID;

import com.sap.cloudfoundry.client.facade.CloudControllerClient;
import com.sap.cloudfoundry.client.facade.CloudControllerClientImpl;
import com.sap.cloudfoundry.client.facade.CloudCredentials;
import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudMetadata;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudOrganization;
import com.sap.cloudfoundry.client.facade.domain.ImmutableCloudSpace;

public class Main {

    public static void main(String[] args) {
        String target = "<API>";
        String username = "<USER>";
        String password = "<PASSWORD>";
        CloudOrganization cloudOrganization = ImmutableCloudOrganization.builder()
                                                                        .name("<ORG_NAME>")
                                                                        .build();
        CloudSpace cloudSpace = ImmutableCloudSpace.builder()
                                                   .metadata(ImmutableCloudMetadata.builder()
                                                                                   .guid(UUID.fromString("<SPACE_GUID>"))
                                                                                   .build())
                                                   .organization(cloudOrganization)
                                                   .name("<SPACE_NAME>")
                                                   .build();
        CloudCredentials credentials = new CloudCredentials(username, password);
        CloudControllerClient client = new CloudControllerClientImpl(getTargetURL(target), credentials, cloudSpace, false);

        System.out.printf("%nApplications:%n");
        for (CloudApplication application : client.getApplications()) {
            System.out.printf("  %s%n", application.getName());
        }

    }

    private static URL getTargetURL(String target) {
        try {
            return URI.create(target)
                      .toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("The target URL is not valid: " + e.getMessage());
        }
    }
}
