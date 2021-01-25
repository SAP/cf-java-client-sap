package com.sap.cloudfoundry.client.facade.util;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EnvironmentUtil {

    private EnvironmentUtil() {

    }

    public static Map<String, String> parse(Map<String, Object> env) {
        Map<String, String> result = new LinkedHashMap<>();
        if (env == null) {
            return result;
        }
        for (Map.Entry<String, Object> envEntry : env.entrySet()) {
            result.put(envEntry.getKey(), convertValueToString(envEntry.getValue()));
        }
        return result;
    }

    private static String convertValueToString(Object value) {
        if (value == null) {
            return null;
        }
        return value instanceof String ? (String) value : JsonUtil.convertToJson(value);
    }
}
