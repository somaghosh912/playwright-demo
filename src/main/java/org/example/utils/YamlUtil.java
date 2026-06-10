package org.example.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public final class YamlUtil {

    private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

    private YamlUtil() {}

    public static Map<String, Object> readAsMap(String path) {
        try {
            return MAPPER.readValue(new File(path), new TypeReference<>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read YAML: " + path, e);
        }
    }
}
