package org.example.hooks;

import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.Scenario;
import org.example.utils.TestLogger;
import org.example.utils.ScreenshotManager;

/**
 * Cucumber Hooks for Test Lifecycle Management
 * Integrates central logging and screenshot management
 * Handles setup and teardown for all test types (Web, Mobile, API)
 */
public class TestHooks {

    /**
     * Before Hook - Executes before each scenario
     * Initializes logging and screenshot management
     */
    @Before
    public void beforeScenario(Scenario scenario) {
        // Extract test type from scenario tags
        String testType = determineTestType(scenario);
        
        // Initialize logging with scenario context
        TestLogger.initializeScenario(scenario.getName(), testType);
        TestLogger.logInfo("Scenario Tags: %s", scenario.getSourceTagNames());
        
        // Initialize screenshot directory for scenario
        ScreenshotManager.initializeScenario(scenario.getName());
        TestLogger.logInfo("Screenshot management initialized for: %s", scenario.getName());
    }

    /**
     * After Hook - Executes after each scenario
     * Captures failure information, generates reports, and cleanup
     */
    @After
    public void afterScenario(Scenario scenario) {
        boolean scenarioPassed = scenario.isFailed();
        String failureMessage = null;
        
        if (scenarioPassed) {
            // Scenario failed - capture detailed failure info
            failureMessage = captureFailureDetails(scenario);
        }
        
        // Log scenario summary
        TestLogger.cleanupScenario(!scenarioPassed, failureMessage);
        
        // Generate screenshot index if screenshots were captured
        String screenshotIndexPath = ScreenshotManager.generateScreenshotIndex();
        if (screenshotIndexPath != null) {
            TestLogger.logInfo("Screenshot index generated at: %s", screenshotIndexPath);
        }
        
        // Cleanup screenshot resources
        ScreenshotManager.cleanupScenario();
        
        // Log file paths for easy access
        TestLogger.logInfo("📋 Test artifacts:");
        TestLogger.logInfo("  Logs: target/logs/automation.log");
        if (screenshotIndexPath != null) {
            TestLogger.logInfo("  Screenshots: %s", ScreenshotManager.getBaseScreenshotDirectory());
        }
    }

    /**
     * Determine test type from scenario tags
     * Looks for @Web, @Mobile, or @API tags
     */
    private String determineTestType(Scenario scenario) {
        if (scenario.getSourceTagNames().contains("@Web")) {
            return "WEB";
        } else if (scenario.getSourceTagNames().contains("@Mobile")) {
            return "MOBILE";
        } else if (scenario.getSourceTagNames().contains("@API")) {
            return "API";
        }
        return "UNKNOWN";
    }

    /**
     * Capture detailed failure information
     * Extracts error messages from scenario execution
     */
    private String captureFailureDetails(Scenario scenario) {
        StringBuilder failureDetails = new StringBuilder();
        
        TestLogger.logError("═══════════════════════════════════════════════════════════════");
        TestLogger.logError("SCENARIO FAILED - Failure Details:");
        TestLogger.logError("═══════════════════════════════════════════════════════════════");
        
        // Get scenario steps
        var steps = scenario.getSteps();
        if (!steps.isEmpty()) {
            for (var step : steps) {
                // Check if step result indicates failure
                TestLogger.logError("Step: %s", step.getText());
            }
        }
        
        TestLogger.logError("Scenario Name: %s", scenario.getName());
        TestLogger.logError("Scenario ID: %s", scenario.getId());
        
        failureDetails.append("Scenario: ").append(scenario.getName());
        failureDetails.append(" | ID: ").append(scenario.getId());
        
        return failureDetails.toString();
    }
}
