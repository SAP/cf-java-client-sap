package org.cloudfoundry.client.lib.rest.clients.droplets;

import java.util.UUID;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudSpace;
import org.cloudfoundry.client.lib.rest.clients.CloudControllerBaseClient;
import org.cloudfoundry.client.v3.Relationship;
import org.cloudfoundry.client.v3.applications.SetApplicationCurrentDropletRequest;

public class CloudControllerDropletsClientImpl extends CloudControllerBaseClient implements CloudControllerDropletsClient {

    public CloudControllerDropletsClientImpl(CloudSpace target, CloudFoundryClient v3Client) {
        super(target, v3Client);
    }

    @Override
    public void bindDropletToApp(UUID dropletGuid, UUID applicationGuid) {
        v3Client.applicationsV3()
                .setCurrentDroplet(SetApplicationCurrentDropletRequest.builder()
                                                                      .applicationId(applicationGuid.toString())
                                                                      .data(Relationship.builder()
                                                                                        .id(dropletGuid.toString())
                                                                                        .build())
                                                                      .build())
                .block();
    }

}
