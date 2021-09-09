package com.sap.cloudfoundry.client.facade.adapters;

import java.io.IOException;

import org.cloudfoundry.client.v3.serviceinstances.ServiceInstanceType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/*
 * TODO: This custom deserializer is needed during rollout of the upcoming cf-java-client version 2.13.0.
         It can be deleted with version 2.14.0
 */
public class ServiceInstanceTypeDeserializer extends JsonDeserializer<ServiceInstanceType> {

    @Override
    public ServiceInstanceType deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        String serviceInstanceTypeAsString = jsonParser.getValueAsString();

        ServiceInstanceType serviceInstanceType = null;

        try {
            serviceInstanceType = ServiceInstanceType.from(serviceInstanceTypeAsString);
        } catch (IllegalArgumentException e) {
            serviceInstanceType = convertFromOldServiceInstanceType(serviceInstanceTypeAsString);
        }

        return serviceInstanceType;
    }

    private ServiceInstanceType convertFromOldServiceInstanceType(String value) {
        com.sap.cloudfoundry.client.facade.domain.ServiceInstanceType type = com.sap.cloudfoundry.client.facade.domain.ServiceInstanceType.valueOfWithDefault(value);
        return type == com.sap.cloudfoundry.client.facade.domain.ServiceInstanceType.MANAGED ? ServiceInstanceType.MANAGED
            : ServiceInstanceType.USER_PROVIDED;
    }

}
