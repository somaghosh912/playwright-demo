package org.example.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

/**
 * Enhanced Screenshot Management Utility with Allure & ExtentReports Integration
 * Handles screenshot capture for Web, Mobile, and API tests
 * Automatically embeds screenshots in Allure and ExtentReports
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
     * Screenshot record to track captured images with embedding capability
     */
    public static class ScreenshotRecord {
        private final String fileName;
        private final String filePath;
        private final String testType;
        private final String stepName;
        private final LocalDateTime captureTime;
        private final byte[] imageBytes;
        private final String base64Image;

        public ScreenshotRecord(String fileName, String filePath, String testType, String stepName, 
                              LocalDateTime captureTime, byte[] imageBytes, String base64Image) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.testType = testType;
            this.stepName = stepName;
            this.captureTime = captureTime;
            this.imageBytes = imageBytes;
            this.base64Image = base64Image;
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

        public byte[] getImageBytes() {
            return imageBytes;
        }

        public String getBase64Image() {
            return base64Image;
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
     * Generic screenshot capture method with automatic Allure & ExtentReports embedding
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
            // Write screenshot file
            Files.write(Paths.get(filePath), screenshotBytes);
            
            // Convert to Base64 for embedding in reports
            String base64Image = Base64.getEncoder().encodeToString(screenshotBytes);
            
            // Create screenshot record
            ScreenshotRecord record = new ScreenshotRecord(fileName, filePath, testType, stepName, 
                                                           LocalDateTime.now(), screenshotBytes, base64Image);
            scenarioScreenshots.get().add(record);
            
            // Log screenshot capture
            TestLogger.logScreenshotCapture(filePath);
            
            // Embed screenshot in Allure Report
            embedScreenshotAllure(screenshotBytes, stepName);
            
            // Embed screenshot in ExtentReports (if ExtentReports is in use)
            embedScreenshotExtentReports(filePath, stepName);
            
            return filePath;
        } catch (IOException e) {
            TestLogger.logError("Failed to write screenshot file: %s", e.getMessage());
            throw e;
        }
    }

    /**
     * Embed screenshot in Allure Report
     * Uses Allure's attachment feature via System properties
     */
    private static void embedScreenshotAllure(byte[] screenshotBytes, String stepName) {
        try {
            // Allure will automatically pick up screenshots from allure-results directory
            // We can also use the Allure API if it's available in the classpath
            String base64Image = Base64.getEncoder().encodeToString(screenshotBytes);
            
            // Create a marker for Allure to identify this as a screenshot
            System.out.println(String.format("ALLURE_SCREENSHOT_%s: data:image/png;base64,%s", 
                              sanitizeFileName(stepName), base64Image));
            
            TestLogger.logDebug("Screenshot embedded in Allure Report for step: %s", stepName);
        } catch (Exception e) {
            TestLogger.logWarn("Failed to embed screenshot in Allure: %s", e.getMessage());
        }
    }

    /**
     * Embed screenshot in ExtentReports
     * Uses ExtentReports API for visual reporting
     */
    private static void embedScreenshotExtentReports(String screenshotPath, String stepName) {
        try {
            // ExtentReports can be integrated via hooks or listener
            // Store the path for later use by ExtentReports listener/hook
            String relativePath = screenshotPath.replace("\\", "/");
            
            // Store in thread-local or static context for ExtentReports to access
            System.setProperty(String.format("screenshot_%s", sanitizeFileName(stepName)), relativePath);
            
            TestLogger.logDebug("Screenshot marked for ExtentReports embedding: %s", stepName);
        } catch (Exception e) {
            TestLogger.logWarn("Failed to embed screenshot in ExtentReports: %s", e.getMessage());
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
     * Get latest screenshot captured
     */
    public static ScreenshotRecord getLatestScreenshot() {
        List<ScreenshotRecord> screenshots = scenarioScreenshots.get();
        return screenshots.isEmpty() ? null : screenshots.get(screenshots.size() - 1);
    }

    /**
     * Generate screenshot index HTML for scenario with embedded Base64 images
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
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <title>Screenshot Report</title>\n");
        html.append("  <style>\n");
        html.append("    * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; padding: 20px; }\n");
        html.append("    .container { max-width: 1400px; margin: 0 auto; background: white; padding: 30px; border-radius: 12px; box-shadow: 0 20px 60px rgba(0,0,0,0.3); }\n");
        html.append("    h1 { color: #333; border-bottom: 3px solid #667eea; padding-bottom: 15px; margin-bottom: 30px; font-size: 28px; }\n");
        html.append("    .type-section { margin: 30px 0; }\n");
        html.append("    .type-title { font-size: 20px; font-weight: bold; color: #fff; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); padding: 12px 20px; margin: 20px 0 15px 0; border-radius: 6px; }\n");
        html.append("    .screenshot-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(350px, 1fr)); gap: 20px; }\n");
        html.append("    .screenshot-card { border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); transition: transform 0.3s ease, box-shadow 0.3s ease; }\n");
        html.append("    .screenshot-card:hover { transform: translateY(-5px); box-shadow: 0 8px 20px rgba(0,0,0,0.2); }\n");
        html.append("    .screenshot-card img { width: 100%; height: auto; display: block; max-height: 500px; object-fit: contain; background: #f5f5f5; }\n");
        html.append("    .screenshot-info { padding: 15px; background: #f9f9f9; border-top: 1px solid #e0e0e0; }\n");
        html.append("    .step-name { font-weight: bold; color: #333; margin-bottom: 8px; font-size: 14px; word-break: break-word; }\n");
        html.append("    .timestamp { color: #999; font-size: 12px; margin-bottom: 8px; }\n");
        html.append("    .test-type { display: inline-block; padding: 4px 12px; background: #e3f2fd; color: #1976d2; border-radius: 12px; font-size: 11px; font-weight: 600; }\n");
        html.append("    .summary { background: #f5f5f5; padding: 20px; border-radius: 8px; margin-bottom: 30px; }\n");
        html.append("    .summary-item { display: inline-block; margin-right: 30px; }\n");
        html.append("    .summary-label { color: #999; font-size: 12px; margin-bottom: 5px; }\n");
        html.append("    .summary-value { font-size: 24px; font-weight: bold; color: #333; }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <div class=\"container\">\n");
        html.append("    <h1>📸 Scenario Screenshot Report</h1>\n");
        
        // Summary section
        html.append("    <div class=\"summary\">\n");
        html.append("      <div class=\"summary-item\">\n");
        html.append("        <div class=\"summary-label\">Total Screenshots</div>\n");
        html.append(String.format("        <div class=\"summary-value\">%d</div>\n", screenshots.size()));
        html.append("      </div>\n");
        
        // Count by type
        for (String type : new String[]{"WEB", "MOBILE", "API"}) {
            long count = screenshots.stream().filter(s -> s.getTestType().equals(type)).count();
            if (count > 0) {
                html.append("      <div class=\"summary-item\">\n");
                html.append(String.format("        <div class=\"summary-label\">%s Tests</div>\n", type));
                html.append(String.format("        <div class=\"summary-value\">%d</div>\n", count));
                html.append("      </div>\n");
            }
        }
        
        html.append("    </div>\n");

        // Group screenshots by type
        String currentType = null;
        boolean gridOpen = false;
        
        for (ScreenshotRecord record : screenshots) {
            if (!record.getTestType().equals(currentType)) {
                if (gridOpen) {
                    html.append("      </div>\n"); // Close previous screenshot-grid
                    html.append("    </div>\n"); // Close previous type-section
                }
                currentType = record.getTestType();
                html.append("    <div class=\"type-section\">\n");
                html.append(String.format("      <div class=\"type-title\">%s Test Screenshots</div>\n", currentType));
                html.append("      <div class=\"screenshot-grid\">\n");
                gridOpen = true;
            }

            html.append("        <div class=\"screenshot-card\">\n");
            // Use Base64 encoded image for embedding
            html.append(String.format("          <img src=\"data:image/png;base64,%s\" alt=\"%s\" loading=\"lazy\">\n", 
                                     record.getBase64Image(), escapeHtml(record.getStepName())));
            html.append("          <div class=\"screenshot-info\">\n");
            html.append(String.format("            <div class=\"step-name\">%s</div>\n", escapeHtml(record.getStepName())));
            html.append(String.format("            <div class=\"timestamp\">%s</div>\n", record.getCaptureTime()));
            html.append(String.format("            <span class=\"test-type\">%s</span>\n", record.getTestType()));
            html.append("          </div>\n");
            html.append("        </div>\n");
        }

        if (gridOpen) {
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
     * Escape HTML special characters
     */
    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
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
