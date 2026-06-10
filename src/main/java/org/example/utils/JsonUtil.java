package org.example.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class JsonUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonUtil() {}

    public static JsonNode read(String path) {
        try {
            return MAPPER.readTree(new File(path));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read JSON: " + path, e);
        }
    }

    public static Map<String, Object> readAsMap(String path) {
        try {
            return MAPPER.readValue(new File(path), new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read JSON map: " + path, e);
        }
    }
}
