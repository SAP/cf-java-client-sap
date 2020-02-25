package org.cloudfoundry.client.lib.domain;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum ServiceInstanceType {

    USER_PROVIDED("user_provided_service_instance"),
    MANAGED("managed_service_instance");

    @JsonValue
    private final String type;

    ServiceInstanceType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }

    public static ServiceInstanceType valueOfWithDefault(String type) {
        return Arrays.asList(ServiceInstanceType.values())
                     .stream()
                     .filter(serviceInstanceType -> serviceInstanceType.toString()
                                                                       .equalsIgnoreCase(type))
                     .findFirst()
                     .orElse(MANAGED);
    }
}
