package org.example.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Screenshot Management Utility
 * Handles screenshot capture for Web, Mobile, and API tests with organized folder structure
 * Maintains a registry of captured screenshots per scenario
 */
public class ScreenshotManager {

    private static final String BASE_SCREENSHOT_DIR = "target/screenshots";
    private static final String WEB_DIR = "web";
    private static final String MOBILE_DIR = "mobile";
    private static final String API_DIR = "api";
    
    private static final DateTimeFormatter fileNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss-SSS");
    private static final DateTimeFormatter folderNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    // Thread-local to store screenshots per scenario execution
    private static final ThreadLocal<List<ScreenshotRecord>> scenarioScreenshots = ThreadLocal.withInitial(ArrayList::new);
    private static final ThreadLocal<String> scenarioFolderPath = ThreadLocal.withInitial(() -> null);

    /**
     * Screenshot record to track captured images
     */
    public static class ScreenshotRecord {
        private final String fileName;
        private final String filePath;
        private final String testType;
        private final String stepName;
        private final LocalDateTime captureTime;

        public ScreenshotRecord(String fileName, String filePath, String testType, String stepName, LocalDateTime captureTime) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.testType = testType;
            this.stepName = stepName;
            this.captureTime = captureTime;
        }

        public String getFileName() {
            return fileName;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getTestType() {
            return testType;
        }

        public String getStepName() {
            return stepName;
        }

        public LocalDateTime getCaptureTime() {
            return captureTime;
        }
    }

    /**
     * Initialize screenshot structure for a scenario
     * Creates scenario-specific folder with timestamp
     */
    public static void initializeScenario(String scenarioName) {
        String timestamp = LocalDateTime.now().format(folderNameFormatter);
        String sanitizedScenarioName = sanitizeFileName(scenarioName);
        String scenarioFolder = String.format("%s/%s_%s", BASE_SCREENSHOT_DIR, sanitizedScenarioName, timestamp);
        
        try {
            Files.createDirectories(Paths.get(scenarioFolder));
            scenarioFolderPath.set(scenarioFolder);
            scenarioScreenshots.set(new ArrayList<>());
            TestLogger.logInfo("Screenshot directory initialized: %s", scenarioFolder);
        } catch (IOException e) {
            TestLogger.logError("Failed to create screenshot directory: %s", e.getMessage());
            throw new RuntimeException("Failed to initialize screenshot directory", e);
        }
    }

    /**
     * Capture screenshot for Web tests
     */
    public static String captureWebScreenshot(byte[] screenshotBytes, String stepName) throws IOException {
        return captureScreenshot(screenshotBytes, "WEB", stepName);
    }

    /**
     * Capture screenshot for Mobile tests
     */
    public static String captureMobileScreenshot(byte[] screenshotBytes, String stepName) throws IOException {
        return captureScreenshot(screenshotBytes, "MOBILE", stepName);
    }

    /**
     * Capture screenshot for API tests (if applicable - e.g., UI response)
     */
    public static String captureApiScreenshot(byte[] screenshotBytes, String stepName) throws IOException {
        return captureScreenshot(screenshotBytes, "API", stepName);
    }

    /**
     * Generic screenshot capture method
     */
    private static String captureScreenshot(byte[] screenshotBytes, String testType, String stepName) throws IOException {
        if (screenshotBytes == null || screenshotBytes.length == 0) {
            TestLogger.logWarn("Empty screenshot data received for test type: %s", testType);
            return null;
        }

        String scenarioFolder = scenarioFolderPath.get();
        if (scenarioFolder == null) {
            TestLogger.logWarn("Scenario not initialized. Initialize with initializeScenario() first");
            return null;
        }

        // Create type-specific subdirectory
        String typeFolder = getTypeFolder(testType);
        String fullPath = String.format("%s/%s", scenarioFolder, typeFolder);
        
        try {
            Files.createDirectories(Paths.get(fullPath));
        } catch (IOException e) {
            TestLogger.logError("Failed to create type-specific screenshot directory: %s", e.getMessage());
            throw e;
        }

        // Generate screenshot filename
        String timestamp = LocalDateTime.now().format(fileNameFormatter);
        String sanitizedStepName = sanitizeFileName(stepName);
        String fileName = String.format("%s_%s.png", timestamp, sanitizedStepName);
        String filePath = String.format("%s/%s", fullPath, fileName);

        try {
            Files.write(Paths.get(filePath), screenshotBytes);
            
            ScreenshotRecord record = new ScreenshotRecord(fileName, filePath, testType, stepName, LocalDateTime.now());
            scenarioScreenshots.get().add(record);
            
            TestLogger.logScreenshotCapture(filePath);
            return filePath;
        } catch (IOException e) {
            TestLogger.logError("Failed to write screenshot file: %s", e.getMessage());
            throw e;
        }
    }

    /**
     * Get all screenshots captured in current scenario
     */
    public static List<ScreenshotRecord> getScenarioScreenshots() {
        return new ArrayList<>(scenarioScreenshots.get());
    }

    /**
     * Get screenshots for a specific test type
     */
    public static List<ScreenshotRecord> getScreenshotsByType(String testType) {
        return scenarioScreenshots.get().stream()
            .filter(record -> record.getTestType().equalsIgnoreCase(testType))
            .toList();
    }

    /**
     * Generate screenshot index HTML for scenario
     */
    public static String generateScreenshotIndex() {
        String scenarioFolder = scenarioFolderPath.get();
        if (scenarioFolder == null) {
            return null;
        }

        List<ScreenshotRecord> screenshots = getScenarioScreenshots();
        if (screenshots.isEmpty()) {
            return null;
        }

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("  <title>Screenshot Report</title>\n");
        html.append("  <style>\n");
        html.append("    body { font-family: Arial, sans-serif; margin: 20px; background: #f5f5f5; }\n");
        html.append("    .container { max-width: 1200px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }\n");
        html.append("    h1 { color: #333; border-bottom: 2px solid #007bff; padding-bottom: 10px; }\n");
        html.append("    .type-section { margin: 20px 0; }\n");
        html.append("    .type-title { font-size: 18px; font-weight: bold; color: #007bff; margin: 15px 0 10px 0; }\n");
        html.append("    .screenshot-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 15px; }\n");
        html.append("    .screenshot-card { border: 1px solid #ddd; border-radius: 5px; overflow: hidden; box-shadow: 0 2px 5px rgba(0,0,0,0.1); }\n");
        html.append("    .screenshot-card img { width: 100%; height: auto; display: block; }\n");
        html.append("    .screenshot-info { padding: 10px; background: #f9f9f9; font-size: 12px; }\n");
        html.append("    .step-name { font-weight: bold; color: #333; margin-bottom: 5px; }\n");
        html.append("    .timestamp { color: #666; font-size: 11px; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <div class=\"container\">\n");
        html.append("    <h1>Scenario Screenshots Report</h1>\n");

        // Group screenshots by type
        String currentType = null;
        for (ScreenshotRecord record : screenshots) {
            if (!record.getTestType().equals(currentType)) {
                if (currentType != null) {
                    html.append("    </div>\n"); // Close previous screenshot-grid
                }
                currentType = record.getTestType();
                html.append("    <div class=\"type-section\">\n");
                html.append("      <div class=\"type-title\">").append(currentType).append(" Test Screenshots</div>\n");
                html.append("      <div class=\"screenshot-grid\">\n");
            }

            html.append("        <div class=\"screenshot-card\">\n");
            html.append("          <img src=\"").append(record.getFileName()).append("\" alt=\"").append(record.getStepName()).append("\">\n");
            html.append("          <div class=\"screenshot-info\">\n");
            html.append("            <div class=\"step-name\">").append(record.getStepName()).append("</div>\n");
            html.append("            <div class=\"timestamp\">").append(record.getCaptureTime()).append("</div>\n");
            html.append("          </div>\n");
            html.append("        </div>\n");
        }

        if (!screenshots.isEmpty()) {
            html.append("      </div>\n"); // Close last screenshot-grid
            html.append("    </div>\n"); // Close last type-section
        }

        html.append("  </div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        // Write HTML file
        try {
            String indexPath = String.format("%s/screenshots_index.html", scenarioFolderPath.get());
            Files.write(Paths.get(indexPath), html.toString().getBytes());
            TestLogger.logInfo("Screenshot index generated: %s", indexPath);
            return indexPath;
        } catch (IOException e) {
            TestLogger.logError("Failed to generate screenshot index: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Clean up scenario resources
     */
    public static void cleanupScenario() {
        scenarioFolderPath.remove();
        scenarioScreenshots.remove();
    }

    /**
     * Get type-specific folder name
     */
    private static String getTypeFolder(String testType) {
        return switch (testType.toUpperCase()) {
            case "WEB" -> WEB_DIR;
            case "MOBILE" -> MOBILE_DIR;
            case "API" -> API_DIR;
            default -> testType.toLowerCase();
        };
    }

    /**
     * Sanitize filename to remove invalid characters
     */
    private static String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Get screenshot base directory
     */
    public static String getBaseScreenshotDirectory() {
        return BASE_SCREENSHOT_DIR;
    }

    /**
     * Get current scenario folder path
     */
    public static String getCurrentScenarioFolder() {
        return scenarioFolderPath.get();
    }
}
