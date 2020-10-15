package com.sap.cloudfoundry.client.facade;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class TestUtil {

    private static ObjectMapper objectMapper = createObjectMapper();

    private TestUtil() {

    }

    private static ObjectMapper createObjectMapper() {
        return new ObjectMapper();
    }

    public static String readFileContent(String fileName, Class<?> clazz) throws IOException {
        InputStream resource = clazz.getResourceAsStream(fileName);
        return IOUtils.toString(resource);
    }

    public static <T> T fromJson(String content, Class<T> clazz) throws IOException {
        return objectMapper.readValue(content, clazz);
    }
}
