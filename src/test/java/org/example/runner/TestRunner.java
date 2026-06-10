package org.example.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.example.listener.ExtentReportsListener;
import org.example.listener.AllureReportListener;
import org.example.utils.TestLogger;

/**
 * Cucumber TestNG Test Runner
 * Integrates with:
 * - Allure Reports (via allure-cucumber7-jvm plugin)
 * - ExtentReports (via custom listener)
 * - Central Logging (TestLogger)
 * - Screenshot Management (ScreenshotManager & ScreenshotCapture)
 *
 * Executes Gherkin feature files and generates comprehensive reports
 * with embedded screenshots, logs, and detailed execution traces.
 */
@CucumberOptions(
    // Feature files location
    features = {"src/test/resources/features"},
    
    // Step definitions and hooks location
    glue = {
        "org.example.steps",
        "org.example.hooks"
    },
    
    // Report plugins
    plugin = {
        // Pretty console output
        "pretty",
        
        // Cucumber HTML Reports
        "html:target/cucumber-reports/report.html",
        "json:target/cucumber-reports/report.json",
        
        // Allure Reports - automatically embeds screenshots via plugin
        "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm",
        
        // Custom ExtentReports Listener - embeds screenshots
        "org.example.listener.ExtentReportsListener",
        
        // Custom Allure Report Listener - additional screenshot handling
        "org.example.listener.AllureReportListener",
        
        // Standard output
        "progress",
        "summary"
    },
    
    // Execution tags - can be overridden via -Dcucumber.filter.tags
    // Examples:
    // @Web - Web tests only
    // @Mobile - Mobile tests only
    // @API - API tests only
    // @Smoke - Smoke tests
    // @Regression - Regression tests
    tags = "not @Ignore",
    
    // Step definition match
    stepNotifications = true,
    
    // Test execution settings
    dryRun = false,
    monochrome = false,
    
    // Publish results
    publish = false,
    publishQuietly = false
)
public class TestRunner extends AbstractTestNGCucumberTests {

    /**
     * Setup before test execution
     * Initializes reporting framework
     */
    @BeforeClass(alwaysRun = true)
    public void beforeTestExecution() {
        TestLogger.logInfo("═══════════════════════════════════════════════════════════════");
        TestLogger.logInfo("Test Execution Started");
        TestLogger.logInfo("═══════════════════════════════════════════════════════════════");
        TestLogger.logInfo("Framework: Playwright + Cucumber + TestNG");
        TestLogger.logInfo("Allure Reports: target/allure-results/");
        TestLogger.logInfo("  View: mvn allure:serve");
        TestLogger.logInfo("ExtentReports: target/extent-reports/extent-report.html");
        TestLogger.logInfo("Cucumber Reports: target/cucumber-reports/report.html");
        TestLogger.logInfo("Screenshots: target/screenshots/");
        TestLogger.logInfo("Logs: target/logs/automation.log");
        TestLogger.logInfo("═══════════════════════════════════════════════════════════════");
    }

    /**
     * Cleanup after test execution
     * Finalizes reporting and logs
     */
    @AfterClass(alwaysRun = true)
    public void afterTestExecution() {
        TestLogger.logInfo("═══════════════════════════════════════════════════════════════");
        TestLogger.logInfo("Test Execution Completed");
        TestLogger.logInfo("═══════════════════════════════════════════════════════════════");
        
        // Finalize ExtentReports
        try {
            ExtentReportsListener.finalizeReports();
            TestLogger.logInfo("✓ ExtentReports finalized successfully");
        } catch (Exception e) {
            TestLogger.logWarn("Failed to finalize ExtentReports: %s", e.getMessage());
        }
        
        TestLogger.logInfo("═══════════════════════════════════════════════════════════════");
        TestLogger.logInfo("📊 Report Locations:");
        TestLogger.logInfo("═══════════════════════════════════════════════════════════════");
        TestLogger.logInfo("Allure Report:");
        TestLogger.logInfo("  Location: target/allure-results/");
        TestLogger.logInfo("  View: mvn allure:serve");
        TestLogger.logInfo("");
        TestLogger.logInfo("ExtentReports:");
        TestLogger.logInfo("  File: target/extent-reports/extent-report.html");
        TestLogger.logInfo("  Open in browser directly");
        TestLogger.logInfo("");
        TestLogger.logInfo("Cucumber HTML Report:");
        TestLogger.logInfo("  File: target/cucumber-reports/report.html");
        TestLogger.logInfo("  Open in browser directly");
        TestLogger.logInfo("");
        TestLogger.logInfo("Screenshots:");
        TestLogger.logInfo("  Directory: target/screenshots/");
        TestLogger.logInfo("  Index: target/screenshots/*/screenshots_index.html");
        TestLogger.logInfo("");
        TestLogger.logInfo("Test Logs:");
        TestLogger.logInfo("  All Logs: target/logs/automation.log");
        TestLogger.logInfo("  Web Tests: target/logs/web_tests.log");
        TestLogger.logInfo("  Mobile Tests: target/logs/mobile_tests.log");
        TestLogger.logInfo("  API Tests: target/logs/api_tests.log");
        TestLogger.logInfo("  Errors Only: target/logs/errors.log");
        TestLogger.logInfo("═══════════════════════════════════════════════════════════════");
    }

    /**
     * Parallel execution configuration
     * Uncomment @DataProvider to enable parallel test execution
     * Set thread-count in pom.xml surefire configuration
     */
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
