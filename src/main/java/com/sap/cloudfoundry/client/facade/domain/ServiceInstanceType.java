package com.sap.cloudfoundry.client.facade.domain;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * This enum is replaced by {@link org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType}
 */
@Deprecated
public enum ServiceInstanceType {

    USER_PROVIDED("user_provided_service_instance"), MANAGED("managed_service_instance");

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
        return Arrays.stream(ServiceInstanceType.values())
                     .filter(serviceInstanceType -> serviceInstanceType.toString()
                                                                       .equalsIgnoreCase(type))
                     .findFirst()
                     .orElse(MANAGED);
    }
}
