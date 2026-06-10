package org.example.listener;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.cucumber.plugin.event.*;
import io.cucumber.plugin.Plugin;
import org.example.utils.TestLogger;
import org.example.utils.ScreenshotManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * ExtentReports Listener for Cucumber
 * Captures test execution and embeds screenshots automatically
 * Generates beautiful HTML reports with embedded images
 */
public class ExtentReportsListener implements Plugin {

    private static ExtentReports extentReports;
    private static final ThreadLocal<ExtentTest> scenarioTest = ThreadLocal.withInitial(() -> null);
    private static final ThreadLocal<ExtentTest> stepTest = ThreadLocal.withInitial(() -> null);
    private static final Map<String, String> scenarioOutcomeMap = new HashMap<>();

    private static final String REPORT_PATH = "target/extent-reports";
    private static final String REPORT_FILE = REPORT_PATH + "/extent-report.html";

    public ExtentReportsListener() {
        initializeExtentReports();
    }

    /**
     * Initialize ExtentReports configuration
     */
    private static synchronized void initializeExtentReports() {
        if (extentReports == null) {
            try {
                // Create report directory
                Files.createDirectories(Paths.get(REPORT_PATH));
                
                // Configure ExtentReports
                ExtentSparkReporter sparkReporter = new ExtentSparkReporter(REPORT_FILE);
                
                // Configure theme and settings
                sparkReporter.config()
                    .setTheme(Theme.DARK)
                    .setDocumentTitle("Playwright Test Automation Report")
                    .setReportName("Test Execution Report")
                    .setTimelineEnabled(true)
                    .setTimeStampFormat("yyyy-MM-dd HH:mm:ss");
                
                extentReports = new ExtentReports();
                extentReports.attachReporter(sparkReporter);
                
                // Add system information
                extentReports.setSystemInfo("Test Framework", "Playwright + Cucumber + TestNG");
                extentReports.setSystemInfo("Environment", System.getProperty("env", "qa"));
                extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));
                extentReports.setSystemInfo("OS Name", System.getProperty("os.name"));
                extentReports.setSystemInfo("OS Version", System.getProperty("os.version"));
                
                TestLogger.logInfo("ExtentReports initialized: %s", REPORT_FILE);
            } catch (IOException e) {
                TestLogger.logError("Failed to initialize ExtentReports: %s", e.getMessage());
            }
        }
    }

    /**
     * Handle test case started event
     */
    @io.cucumber.plugin.EventListener
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseStarted.class, this::handleTestCaseStarted);
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
        publisher.registerHandlerFor(TestStepStarted.class, this::handleTestStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::handleTestStepFinished);
    }

    /**
     * Handle test case started event
     */
    private void handleTestCaseStarted(TestCaseStarted event) {
        String scenarioName = event.getTestCase().getName();
        String tags = event.getTestCase().getTags().toString();
        
        // Create extent test
        ExtentTest test = extentReports.createTest(scenarioName)
            .assignCategory(event.getTestCase().getKeyword())
            .assignDevice("Web");
        
        // Add tags as metadata
        for (String tag : event.getTestCase().getTags()) {
            test.assignCategory(tag.replace("@", ""));
        }
        
        scenarioTest.set(test);
        TestLogger.logInfo("ExtentReports: Test case started - %s", scenarioName);
    }

    /**
     * Handle test case finished event
     */
    private void handleTestCaseFinished(TestCaseFinished event) {
        ExtentTest test = scenarioTest.get();
        if (test != null) {
            String scenarioName = event.getTestCase().getName();
            
            if (event.getResult().getStatus() == Status.PASSED) {
                test.pass("Scenario passed");
                scenarioOutcomeMap.put(scenarioName, "PASSED");
            } else if (event.getResult().getStatus() == Status.FAILED) {
                test.fail("Scenario failed");
                
                // Embed failure details
                if (event.getResult().getError() != null) {
                    test.fail(event.getResult().getError());
                }
                
                // Embed latest screenshot on failure
                embedLatestScreenshot(test, scenarioName);
                scenarioOutcomeMap.put(scenarioName, "FAILED");
            } else if (event.getResult().getStatus() == Status.SKIPPED) {
                test.skip("Scenario skipped");
                scenarioOutcomeMap.put(scenarioName, "SKIPPED");
            }
            
            TestLogger.logInfo("ExtentReports: Test case finished - %s - Status: %s", 
                             scenarioName, event.getResult().getStatus());
        }
    }

    /**
     * Handle test step started event
     */
    private void handleTestStepStarted(TestStepStarted event) {
        if (event.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep step = (PickleStepTestStep) event.getTestStep();
            String stepText = step.getStep().getText();
            
            ExtentTest parentTest = scenarioTest.get();
            if (parentTest != null) {
                ExtentTest childTest = parentTest.createNode("Step", stepText);
                stepTest.set(childTest);
                TestLogger.logInfo("ExtentReports: Step started - %s", stepText);
            }
        }
    }

    /**
     * Handle test step finished event
     */
    private void handleTestStepFinished(TestStepFinished event) {
        ExtentTest stepNode = stepTest.get();
        if (stepNode != null && event.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep step = (PickleStepTestStep) event.getTestStep();
            String stepText = step.getStep().getText();
            
            if (event.getResult().getStatus() == Status.PASSED) {
                stepNode.pass(stepText);
                // Embed screenshot after each step
                embedStepScreenshot(stepNode, stepText);
            } else if (event.getResult().getStatus() == Status.FAILED) {
                stepNode.fail(event.getResult().getError());
                // Embed screenshot on failure
                embedStepScreenshot(stepNode, stepText);
            } else if (event.getResult().getStatus() == Status.SKIPPED) {
                stepNode.skip("Step skipped");
            }
            
            TestLogger.logInfo("ExtentReports: Step finished - %s - Status: %s", 
                             stepText, event.getResult().getStatus());
        }
    }

    /**
     * Embed screenshot for a step
     */
    private void embedStepScreenshot(ExtentTest stepNode, String stepName) {
        try {
            ScreenshotManager.ScreenshotRecord screenshot = ScreenshotManager.getLatestScreenshot();
            if (screenshot != null) {
                // Embed using Base64
                stepNode.addScreenCaptureFromBase64String(
                    screenshot.getBase64Image(),
                    "Screenshot - " + stepName
                );
                TestLogger.logDebug("Screenshot embedded for step: %s", stepName);
            }
        } catch (Exception e) {
            TestLogger.logWarn("Failed to embed screenshot for step %s: %s", stepName, e.getMessage());
        }
    }

    /**
     * Embed latest screenshot on failure
     */
    private void embedLatestScreenshot(ExtentTest test, String scenarioName) {
        try {
            ScreenshotManager.ScreenshotRecord screenshot = ScreenshotManager.getLatestScreenshot();
            if (screenshot != null) {
                // Embed using Base64
                test.addScreenCaptureFromBase64String(
                    screenshot.getBase64Image(),
                    "Failure Screenshot - " + scenarioName
                );
                TestLogger.logInfo("Failure screenshot embedded for scenario: %s", scenarioName);
            }
        } catch (Exception e) {
            TestLogger.logWarn("Failed to embed failure screenshot: %s", e.getMessage());
        }
    }

    /**
     * Finalize reports after all scenarios complete
     */
    public static void finalizeReports() {
        if (extentReports != null) {
            extentReports.flush();
            TestLogger.logInfo("ExtentReports finalized: %s", REPORT_FILE);
        }
    }

    /**
     * Get current scenario test
     */
    public static ExtentTest getCurrentScenarioTest() {
        return scenarioTest.get();
    }

    /**
     * Get current step test
     */
    public static ExtentTest getCurrentStepTest() {
        return stepTest.get();
    }

    /**
     * Get scenario outcome
     */
    public static String getScenarioOutcome(String scenarioName) {
        return scenarioOutcomeMap.getOrDefault(scenarioName, "UNKNOWN");
    }

    /**
     * Get report file path
     */
    public static String getReportPath() {
        return REPORT_FILE;
    }
}
