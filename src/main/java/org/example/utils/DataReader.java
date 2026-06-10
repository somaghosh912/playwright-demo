package org.example.utils;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.config.ConfigManager;

import java.util.Map;

public final class DataReader {

    private static volatile JsonNode jsonRoot;
    private static volatile Map<String, Object> yamlRoot;

    private DataReader() {}

    private static JsonNode json() {
        if (jsonRoot == null) {
            synchronized (DataReader.class) {
                if (jsonRoot == null) jsonRoot = JsonUtil.read(ConfigManager.dataJson());
            }
        }
        return jsonRoot;
    }

    private static Map<String, Object> yaml() {
        if (yamlRoot == null) {
            synchronized (DataReader.class) {
                if (yamlRoot == null) yamlRoot = YamlUtil.readAsMap(ConfigManager.dataYaml());
            }
        }
        return yamlRoot;
    }

    public static String jsonString(String... path) {
        JsonNode node = json();
        for (String key : path) node = node.path(key);
        if (node.isMissingNode() || node.isNull()) {
            throw new IllegalArgumentException("JSON test data not found at path: " + String.join(".", path));
        }
        return node.asText();
    }

    @SuppressWarnings("unchecked")
    public static Object yamlValue(String... path) {
        Object current = yaml();
        for (String key : path) {
            if (!(current instanceof Map)) {
                throw new IllegalArgumentException("YAML path not traversable at: " + key);
            }
            current = ((Map<String, Object>) current).get(key);
            if (current == null) {
                throw new IllegalArgumentException("YAML test data not found at path: " + String.join(".", path));
            }
        }
        return current;
    }
}
