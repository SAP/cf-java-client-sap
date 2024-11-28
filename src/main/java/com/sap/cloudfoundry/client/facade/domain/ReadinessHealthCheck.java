package com.sap.cloudfoundry.client.facade.domain;

import java.util.Map;

public record ReadinessHealthCheck(String type, Map<String, Object> data) {
}
