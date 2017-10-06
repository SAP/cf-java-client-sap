# cf-java-client

[![Build Status](https://travis-ci.org/SAP/cf-java-client-sap.svg?branch=master)](https://travis-ci.org/SAP/cf-java-client-sap)

This is a fork of the 1.1.4.RELEASE version of the official Cloud Foundry Java client (https://github.com/cloudfoundry/cf-java-client/tree/v1.1.4.RELEASE). It has been modified to include several bugfixes and improvements.

# Introduction

The `cf-java-client` project is a Java language binding for interacting with a Cloud Foundry instance. It provides similar functionality to the Cloud Foundry command line client (https://github.com/cloudfoundry/cli), such as creating services and applications, binding services to applications or even registering service brokers. It communicates with the [Cloud Foundry Controller](https://docs.cloudfoundry.org/concepts/architecture/cloud-controller.html) by making HTTP requests to the controller's REST API, which is documented at https://apidocs.cloudfoundry.org/.

# Components

In addition to the actual client, the official `cf-java-client` repository contains a [Maven](http://maven.apache.org/) and a [Gradle](http://www.gradle.org/) plugin for deploying and managing applications through Maven goals and Gradle tasks respectively. However, since we've made no changes to these plugins, they've been removed from this fork entirely. The client implementation is the only remaining component and it can be found under the `cloudfoundry-client-lib` subdirectory.

# Usage

In order to use this client in your application, you need to include the following dependency in your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>org.cloudfoundry</groupId>
        <artifactId>cloudfoundry-client-lib</artifactId>
        <version>1.1.4.RELEASE-sap-02</version>
    </dependency>
    ...
</dependencies>
```

The following is a very simple sample application that connects to a Cloud Foundry instance, logs in, and displays some information about the Cloud Foundry account. When running the program, provide the Cloud Foundry target API endpoint, along with a valid user name and password as command-line parameters.

```java
import org.cloudfoundry.client.lib.CloudCredentials;
import org.cloudfoundry.client.lib.CloudFoundryClient;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.cloudfoundry.client.lib.domain.CloudService;
import org.cloudfoundry.client.lib.domain.CloudSpace;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public final class JavaSample {

    public static void main(String[] args) {
        String target = args[0];
        String user = args[1];
        String password = args[2];

        CloudCredentials credentials = new CloudCredentials(user, password);
        CloudFoundryClient client = new CloudFoundryClient(credentials, getTargetURL(target));
        client.login();

        System.out.printf("%nSpaces:%n");
        for (CloudSpace space : client.getSpaces()) {
            System.out.printf("  %s\t(%s)%n", space.getName(), space.getOrganization().getName());
        }

        System.out.printf("%nApplications:%n");
        for (CloudApplication application : client.getApplications()) {
            System.out.printf("  %s%n", application.getName());
        }

        System.out.printf("%nServices%n");
        for (CloudService service : client.getServices()) {
            System.out.printf("  %s\t(%s)%n", service.getName(), service.getLabel());
        }
    }

    private static URL getTargetURL(String target) {
        try {
            return URI.create(target).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("The target URL is not valid: " + e.getMessage());
        }
    }

}
```

# Compiling and Packaging

The project is built using [Apache Maven](http://maven.apache.org/) and you can use the following command to do so:

```shell
mvn clean install
```

# License
Copyright (c) 2017 SAP SE or an SAP affiliate company. All rights reserved.
This file is licensed under the Apache Software License, v. 2 except as noted otherwise in the [LICENSE](https://github.com/SAP/cf-java-client-sap/blob/master/LICENSE) file.
