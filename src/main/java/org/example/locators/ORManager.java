package org.example.locators;

import com.fasterxml.jackson.databind.JsonNode;
import org.example.config.ConfigManager;
import org.example.utils.JsonUtil;

public final class ORManager {

    private static volatile JsonNode root;

    private ORManager() {}

    private static JsonNode load() {
        if (root == null) {
            synchronized (ORManager.class) {
                if (root == null) root = JsonUtil.read(ConfigManager.locatorsFile());
            }
        }
        return root;
    }

    /** Look up a dotted key (e.g. "login.username") from locators.json. */
    public static String get(String key) {
        JsonNode node = load();
        for (String part : key.split("\\.")) {
            node = node.path(part);
        }
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            throw new IllegalArgumentException("Locator key not found: " + key);
        }
        return node.asText();
    }
}
