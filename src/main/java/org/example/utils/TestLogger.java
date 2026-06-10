package org.example.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Central Logging Mechanism for Test Automation Framework
 * Provides context-aware logging with thread safety for Web, Mobile, and API tests
 * Captures clear stack traces for failure analysis
 */
public class TestLogger {

    private static final Logger logger = LogManager.getLogger(TestLogger.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    // Log levels
    public enum LogLevel {
        INFO, WARN, ERROR, DEBUG, TRACE
    }

    // Test execution context
    private static final ThreadLocal<TestContext> testContext = ThreadLocal.withInitial(TestContext::new);

    /**
     * Initialize test context for a scenario
     * Call this in @Before hook
     */
    public static void initializeScenario(String scenarioName, String testType) {
        TestContext context = testContext.get();
        context.setScenarioName(scenarioName);
        context.setTestType(testType); // WEB, MOBILE, API
        context.setStartTime(LocalDateTime.now());
        
        // Set MDC (Mapped Diagnostic Context) for Log4j2
        ThreadContext.put("SCENARIO", scenarioName);
        ThreadContext.put("TEST_TYPE", testType);
        
        logInfo("═══════════════════════════════════════════════════════════════");
        logInfo(String.format("Scenario Started: %s | Test Type: %s", scenarioName, testType));
        logInfo(String.format("Time: %s", LocalDateTime.now().format(formatter)));
        logInfo("═══════════════════════════════════════════════════════════════");
    }

    /**
     * Clean up test context
     * Call this in @After hook
     */
    public static void cleanupScenario(boolean passed, String failureMessage) {
        TestContext context = testContext.get();
        LocalDateTime endTime = LocalDateTime.now();
        
        String status = passed ? "PASSED ✓" : "FAILED ✗";
        logInfo("───────────────────────────────────────────────────────────────");
        logInfo(String.format("Scenario Status: %s", status));
        logInfo(String.format("Duration: %d ms", java.time.temporal.ChronoUnit.MILLIS.between(context.getStartTime(), endTime)));
        
        if (!passed && failureMessage != null && !failureMessage.isEmpty()) {
            logError(String.format("Failure Reason: %s", failureMessage));
        }
        logInfo("═══════════════════════════════════════════════════════════════\n");
        
        // Clear context
        ThreadContext.clearAll();
        testContext.remove();
    }

    /**
     * Get current test context
     */
    public static TestContext getContext() {
        return testContext.get();
    }

    /**
     * Log info level message with timestamp and context
     */
    public static void logInfo(String message) {
        logWithContext(LogLevel.INFO, message, null);
    }

    /**
     * Log info with formatted string
     */
    public static void logInfo(String format, Object... args) {
        logInfo(String.format(format, args));
    }

    /**
     * Log warning level message
     */
    public static void logWarn(String message) {
        logWithContext(LogLevel.WARN, message, null);
    }

    /**
     * Log warning with formatted string
     */
    public static void logWarn(String format, Object... args) {
        logWarn(String.format(format, args));
    }

    /**
     * Log error level message with exception and stack trace
     */
    public static void logError(String message) {
        logWithContext(LogLevel.ERROR, message, null);
    }

    /**
     * Log error with exception and detailed stack trace
     */
    public static void logError(String message, Throwable exception) {
        StringBuilder sb = new StringBuilder(message);
        if (exception != null) {
            sb.append("\n").append(getDetailedStackTrace(exception));
        }
        logWithContext(LogLevel.ERROR, sb.toString(), exception);
    }

    /**
     * Log error with formatted string
     */
    public static void logError(String format, Object... args) {
        logError(String.format(format, args));
    }

    /**
     * Log debug level message
     */
    public static void logDebug(String message) {
        logWithContext(LogLevel.DEBUG, message, null);
    }

    /**
     * Log debug with formatted string
     */
    public static void logDebug(String format, Object... args) {
        logDebug(String.format(format, args));
    }

    /**
     * Log test step execution
     */
    public static void logStep(String stepDescription) {
        logInfo("→ Step: %s", stepDescription);
    }

    /**
     * Log test step with expected vs actual values
     */
    public static void logStepWithAssertion(String stepDescription, String expected, String actual) {
        logInfo("→ Step: %s", stepDescription);
        logInfo("  Expected: %s", expected);
        logInfo("  Actual: %s", actual);
    }

    /**
     * Log API request details
     */
    public static void logApiRequest(String method, String endpoint, String requestBody) {
        logInfo("→ API Request");
        logInfo("  Method: %s", method);
        logInfo("  Endpoint: %s", endpoint);
        if (requestBody != null && !requestBody.isEmpty()) {
            logInfo("  Body: %s", requestBody);
        }
    }

    /**
     * Log API response details
     */
    public static void logApiResponse(int statusCode, String responseBody) {
        logInfo("→ API Response");
        logInfo("  Status Code: %d", statusCode);
        if (responseBody != null && !responseBody.isEmpty()) {
            logInfo("  Body: %s", responseBody);
        }
    }

    /**
     * Log screenshot capture
     */
    public static void logScreenshotCapture(String filePath) {
        logInfo("📸 Screenshot captured: %s", filePath);
    }

    /**
     * Get detailed stack trace from exception
     */
    private static String getDetailedStackTrace(Throwable throwable) {
        if (throwable == null) return "";
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println("\n" + "═".repeat(70));
        pw.println("EXCEPTION STACK TRACE:");
        pw.println("═".repeat(70));
        pw.println(String.format("Exception Type: %s", throwable.getClass().getName()));
        pw.println(String.format("Exception Message: %s", throwable.getMessage()));
        pw.println("\nStack Trace:");
        throwable.printStackTrace(pw);
        
        // Print cause if present
        if (throwable.getCause() != null) {
            pw.println("\nCaused by:");
            throwable.getCause().printStackTrace(pw);
        }
        
        pw.println("═".repeat(70));
        pw.flush();
        return sw.toString();
    }

    /**
     * Internal method to log with context
     */
    private static void logWithContext(LogLevel level, String message, Throwable exception) {
        TestContext context = testContext.get();
        
        String contextStr = String.format("[%s | %s]", 
            context.getTestType() != null ? context.getTestType() : "UNKNOWN",
            context.getScenarioName() != null ? context.getScenarioName() : "NO_SCENARIO");
        
        String formattedMessage = String.format("%s %s", contextStr, message);
        
        switch (level) {
            case INFO:
                if (exception != null) {
                    logger.info(formattedMessage, exception);
                } else {
                    logger.info(formattedMessage);
                }
                break;
            case WARN:
                if (exception != null) {
                    logger.warn(formattedMessage, exception);
                } else {
                    logger.warn(formattedMessage);
                }
                break;
            case ERROR:
                if (exception != null) {
                    logger.error(formattedMessage, exception);
                } else {
                    logger.error(formattedMessage);
                }
                break;
            case DEBUG:
                if (exception != null) {
                    logger.debug(formattedMessage, exception);
                } else {
                    logger.debug(formattedMessage);
                }
                break;
            case TRACE:
                if (exception != null) {
                    logger.trace(formattedMessage, exception);
                } else {
                    logger.trace(formattedMessage);
                }
                break;
        }
    }

    /**
     * Inner class to hold test context information
     */
    public static class TestContext {
        private String scenarioName;
        private String testType;
        private LocalDateTime startTime;
        private String currentStep;
        private String browserName;
        private String platformName;

        public String getScenarioName() {
            return scenarioName;
        }

        public void setScenarioName(String scenarioName) {
            this.scenarioName = scenarioName;
        }

        public String getTestType() {
            return testType;
        }

        public void setTestType(String testType) {
            this.testType = testType;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public void setStartTime(LocalDateTime startTime) {
            this.startTime = startTime;
        }

        public String getCurrentStep() {
            return currentStep;
        }

        public void setCurrentStep(String currentStep) {
            this.currentStep = currentStep;
        }

        public String getBrowserName() {
            return browserName;
        }

        public void setBrowserName(String browserName) {
            this.browserName = browserName;
        }

        public String getPlatformName() {
            return platformName;
        }

        public void setPlatformName(String platformName) {
            this.platformName = platformName;
        }
    }
}
