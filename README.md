# Description [![Build Status](https://github.com/SAP/cf-java-client-sap/actions/workflows/main.yml/badge.svg?branch=master)](https://travis-ci.org/SAP/cf-java-client-sap) [![REUSE status](https://api.reuse.software/badge/github.com/SAP/cf-java-client-sap)](https://api.reuse.software/info/github.com/SAP/cf-java-client-sap)

This is a facade that hides the official Cloud Foundry Java client (https://github.com/cloudfoundry/cf-java-client) under a **synchronous** API similar to the one it had back in version 1.1.4.RELEASE (see https://github.com/cloudfoundry/cf-java-client/tree/v1.1.4.RELEASE).

# Introduction

The `cf-java-client` project is a Java language binding for interacting with a Cloud Foundry instance. It provides similar functionality to the Cloud Foundry command line client (https://github.com/cloudfoundry/cli), such as creating services and applications, binding services to applications or even registering service brokers. It communicates with the [Cloud Foundry Controller](https://docs.cloudfoundry.org/concepts/architecture/cloud-controller.html) by making HTTP requests to the controller's REST API, which is documented at https://apidocs.cloudfoundry.org/ (V2) and https://v3-apidocs.cloudfoundry.org/ (V3).

# Requirements
* Installed Java 11 
* Installer and configured [Apache Maven](http://maven.apache.org/)
* Access to [SAP Business Technology Platform Cloud Foundry environment](https://sap.com/products/business-technology-platform.html) or other Cloud Foundry instance

# Download and Installation

In order to use this client in your application, you need to include the following dependency in your `pom.xml`:

```xml
<dependency>
    <groupId>com.sap.cloud.lm.sl</groupId>
    <artifactId>cloudfoundry-client-facade</artifactId>
    <version>...</version>
</dependency>
```
The latest version can always be found in Maven Central: https://mvnrepository.com/artifact/com.sap.cloud.lm.sl/cloudfoundry-client-facade

# Usage

The following is a very simple sample application that connects to a Cloud Foundry instance, logs in, and displays some information about the Cloud Foundry account. When running the program, provide the Cloud Foundry target API endpoint, along with a valid user name and password as command-line parameters.

```java
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import com.sap.cloudfoundry.client.facade.domain.CloudApplication;
import com.sap.cloudfoundry.client.facade.domain.CloudServiceInstance;
import com.sap.cloudfoundry.client.facade.domain.CloudSpace;

public final class JavaSample {

    public static void main(String[] args) {
        String target = args[0];
        String username = args[1];
        String password = args[2];

        CloudCredentials credentials = new CloudCredentials(username, password);
        CloudControllerClient client = new CloudControllerClientImpl(getTargetURL(target), credentials);
        client.login();

        System.out.printf("%nSpaces:%n");
        for (CloudSpace space : client.getSpaces()) {
            System.out.printf("  %s\t(%s)%n", space.getName(), space.getOrganization()
                                                                    .getName());
        }

        System.out.printf("%nApplications:%n");
        for (CloudApplication application : client.getApplications()) {
            System.out.printf("  %s%n", application.getName());
        }

        System.out.printf("%nServices%n");
        for (CloudServiceInstance service : client.getServiceInstances()) {
            System.out.printf("  %s\t(%s)%n", service.getName(), service.getLabel());
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
```

# Compiling and Packaging

The project is built using Java 11 and [Apache Maven](http://maven.apache.org/) and you can use the following command to do so:

```shell
mvn clean install
```

Additionally, the project uses [Immutables](https://immutables.github.io/) to generate value objects. As a result, it won't compile in IDEs like Eclipse or IntelliJ unless you also have an enabled annotation processor. See [this guide](https://immutables.github.io/apt.html) for instructions on how to configure your IDE.

# Support
Check how to obtain support by opening an [issue](CONTRIBUTING.md#report-an-issue).

# License
Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
This file is licensed under the Apache Software License, v. 2 except as noted otherwise in the [LICENSE](LICENSE) file.
