package org.example.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;

/**
 * JSON-backed configuration. Merge order (later wins):
 *   1. src/test/resources/config/config.json   (defaults)
 *   2. src/test/resources/config/${env}.json   (per-environment overrides)
 *   3. -D system properties using dotted keys, e.g. -Dweb.browser=firefox
 */
public final class ConfigManager {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BASE = "src/test/resources/config/config.json";
    private static volatile JsonNode root;

    private ConfigManager() {}

    public static JsonNode root() {
        if (root == null) {
            synchronized (ConfigManager.class) {
                if (root == null) root = load();
            }
        }
        return root;
    }

    private static JsonNode load() {
        try {
            ObjectNode merged = (ObjectNode) MAPPER.readTree(new File(BASE));
            String env = System.getProperty("env", merged.path("env").asText("qa"));
            File envFile = new File("src/test/resources/config/" + env + ".json");
            if (envFile.exists()) {
                merge(merged, (ObjectNode) MAPPER.readTree(envFile));
            }
            applySystemProperties(merged);
            merged.put("env", env);
            return merged;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load config JSON", e);
        }
    }

    private static void merge(ObjectNode target, ObjectNode source) {
        source.fieldNames().forEachRemaining(field -> {
            JsonNode src = source.get(field);
            JsonNode tgt = target.get(field);
            if (src.isObject() && tgt != null && tgt.isObject()) {
                merge((ObjectNode) tgt, (ObjectNode) src);
            } else {
                target.set(field, src);
            }
        });
    }

    private static void applySystemProperties(ObjectNode target) {
        System.getProperties().forEach((k, v) -> {
            String key = k.toString();
            if (!key.contains(".") || key.startsWith("java.") || key.startsWith("sun.") || key.startsWith("os.") || key.startsWith("user.")) {
                return;
            }
            setByPath(target, key.split("\\."), v.toString());
        });
    }

    private static void setByPath(ObjectNode node, String[] path, String value) {
        ObjectNode cursor = node;
        for (int i = 0; i < path.length - 1; i++) {
            JsonNode next = cursor.get(path[i]);
            if (next == null || !next.isObject()) {
                next = MAPPER.createObjectNode();
                cursor.set(path[i], next);
            }
            cursor = (ObjectNode) next;
        }
        cursor.put(path[path.length - 1], value);
    }

    private static JsonNode at(String dottedKey) {
        JsonNode node = root();
        for (String part : dottedKey.split("\\.")) {
            node = node.path(part);
        }
        if (node.isMissingNode() || node.isNull()) {
            throw new IllegalArgumentException("Config key not found: " + dottedKey);
        }
        return node;
    }

    public static String getString(String key) { return at(key).asText(); }
    public static int    getInt(String key)    { return at(key).asInt(); }
    public static boolean getBool(String key)  { return at(key).asBoolean(); }

    public static String env()              { return getString("env"); }
    public static String webBaseUrl()       { return getString("web.baseUrl"); }
    public static String browser()          { return getString("web.browser"); }
    public static boolean headless()        { return getBool("web.headless"); }
    public static int playwrightTimeout()   { return getInt("web.timeoutMs"); }
    public static String apiBaseUrl()       { return getString("api.baseUrl"); }
    public static String mobilePlatform()   { return getString("mobile.platform"); }
    public static String appiumUrl()        { return getString("mobile.appiumUrl"); }
    public static String mobileCapsFile()   { return getString("mobile.capsFile"); }
    public static String dataJson()         { return getString("data.json"); }
    public static String dataYaml()         { return getString("data.yaml"); }
    public static String dataExcel()        { return getString("data.excel"); }
    public static String locatorsFile()     { return getString("locators.file"); }
}
