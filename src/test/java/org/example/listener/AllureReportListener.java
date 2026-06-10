package org.example.listener;

import io.cucumber.plugin.event.*;
import io.cucumber.plugin.Plugin;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import org.example.utils.TestLogger;
import org.example.utils.ScreenshotManager;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Allure Reporter Listener for Cucumber
 * Integrates Cucumber scenarios with Allure reporting
 * Automatically embeds screenshots and attachments
 */
public class AllureReportListener implements Plugin {

    private final AllureLifecycle lifecycle = Allure.getLifecycle();
    private static final Map<String, String> scenarioUuids = new HashMap<>();
    private final ThreadLocal<String> currentScenarioUuid = ThreadLocal.withInitial(() -> null);

    /**
     * Set event publisher for Cucumber events
     */
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestRunStarted.class, this::handleTestRunStarted);
        publisher.registerHandlerFor(TestCaseStarted.class, this::handleTestCaseStarted);
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
        publisher.registerHandlerFor(TestStepStarted.class, this::handleTestStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::handleTestStepFinished);
        publisher.registerHandlerFor(TestRunFinished.class, this::handleTestRunFinished);
    }

    /**
     * Handle test run started
     */
    private void handleTestRunStarted(TestRunStarted event) {
        TestLogger.logInfo("Allure: Test run started");
    }

    /**
     * Handle test case started
     */
    private void handleTestCaseStarted(TestCaseStarted event) {
        String scenarioName = event.getTestCase().getName();
        String scenarioId = UUID.randomUUID().toString();
        
        currentScenarioUuid.set(scenarioId);
        scenarioUuids.put(scenarioName, scenarioId);
        
        // Start Allure test
        lifecycle.startTestCase(scenarioId);
        
        // Set test name and description
        io.qameta.allure.model.TestCase testCase = lifecycle.getCurrentTestCase().get();
        if (testCase != null) {
            testCase.setName(scenarioName);
            testCase.setDescription("Scenario: " + scenarioName);
        }
        
        // Add labels
        addLabels(event);
        
        TestLogger.logInfo("Allure: Test case started - %s [%s]", scenarioName, scenarioId);
    }

    /**
     * Handle test case finished
     */
    private void handleTestCaseFinished(TestCaseFinished event) {
        String scenarioName = event.getTestCase().getName();
        
        // Update test result status
        if (event.getResult().getStatus() == Status.PASSED) {
            lifecycle.updateTestCase(tc -> tc.setStatus(io.qameta.allure.model.Status.PASSED));
            TestLogger.logInfo("Allure: Test case passed - %s", scenarioName);
        } else if (event.getResult().getStatus() == Status.FAILED) {
            lifecycle.updateTestCase(tc -> tc.setStatus(io.qameta.allure.model.Status.FAILED));
            
            // Add failure details
            if (event.getResult().getError() != null) {
                addFailureDetails(event.getResult().getError());
            }
            
            // Embed failure screenshot
            embedFailureScreenshot();
            
            TestLogger.logInfo("Allure: Test case failed - %s", scenarioName);
        } else if (event.getResult().getStatus() == Status.SKIPPED) {
            lifecycle.updateTestCase(tc -> tc.setStatus(io.qameta.allure.model.Status.SKIPPED));
            TestLogger.logInfo("Allure: Test case skipped - %s", scenarioName);
        }
        
        // Stop test case
        lifecycle.stopTestCase();
        currentScenarioUuid.remove();
    }

    /**
     * Handle test step started
     */
    private void handleTestStepStarted(TestStepStarted event) {
        if (event.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep step = (PickleStepTestStep) event.getTestStep();
            String stepText = step.getStep().getText();
            
            // Start step in Allure
            String stepId = UUID.randomUUID().toString();
            lifecycle.startStep(stepId, new io.qameta.allure.model.StepResult()
                .setName(stepText)
                .setStart(System.currentTimeMillis()));
            
            TestLogger.logDebug("Allure: Step started - %s", stepText);
        }
    }

    /**
     * Handle test step finished
     */
    private void handleTestStepFinished(TestStepFinished event) {
        if (event.getTestStep() instanceof PickleStepTestStep) {
            PickleStepTestStep step = (PickleStepTestStep) event.getTestStep();
            String stepText = step.getStep().getText();
            
            // Update step status
            if (event.getResult().getStatus() == Status.PASSED) {
                lifecycle.updateStep(sr -> sr.setStatus(io.qameta.allure.model.Status.PASSED));
                
                // Embed screenshot after step
                embedStepScreenshot(stepText);
            } else if (event.getResult().getStatus() == Status.FAILED) {
                lifecycle.updateStep(sr -> sr.setStatus(io.qameta.allure.model.Status.FAILED));
                
                if (event.getResult().getError() != null) {
                    lifecycle.updateStep(sr -> sr.setStatusDetails(
                        new io.qameta.allure.model.StatusDetails()
                            .setMessage(event.getResult().getError().getMessage())
                            .setTrace(getStackTrace(event.getResult().getError()))
                    ));
                }
                
                // Embed screenshot on failure
                embedStepScreenshot(stepText);
            } else if (event.getResult().getStatus() == Status.SKIPPED) {
                lifecycle.updateStep(sr -> sr.setStatus(io.qameta.allure.model.Status.SKIPPED));
            }
            
            // Stop step
            lifecycle.stopStep();
            
            TestLogger.logDebug("Allure: Step finished - %s - Status: %s", 
                              stepText, event.getResult().getStatus());
        }
    }

    /**
     * Handle test run finished
     */
    private void handleTestRunFinished(TestRunFinished event) {
        TestLogger.logInfo("Allure: Test run finished");
    }

    /**
     * Add labels to test case
     */
    private void addLabels(TestCaseStarted event) {
        // Add tags as labels
        for (String tag : event.getTestCase().getTags()) {
            String cleanTag = tag.replace("@", "");
            lifecycle.updateTestCase(tc -> {
                tc.addLabel(cleanTag, cleanTag);
                return tc;
            });
        }
        
        // Determine and add test type label
        String testType = determineTestType(event.getTestCase().getTags());
        lifecycle.updateTestCase(tc -> {
            tc.addLabel("testType", testType);
            return tc;
        });
    }

    /**
     * Determine test type from tags
     */
    private String determineTestType(java.util.List<String> tags) {
        for (String tag : tags) {
            if (tag.equalsIgnoreCase("@Web")) {
                return "WEB";
            } else if (tag.equalsIgnoreCase("@Mobile")) {
                return "MOBILE";
            } else if (tag.equalsIgnoreCase("@API")) {
                return "API";
            }
        }
        return "UNKNOWN";
    }

    /**
     * Embed screenshot after step execution
     */
    private void embedStepScreenshot(String stepName) {
        try {
            ScreenshotManager.ScreenshotRecord screenshot = ScreenshotManager.getLatestScreenshot();
            if (screenshot != null && screenshot.getImageBytes() != null) {
                lifecycle.addAttachment(
                    "Screenshot - " + stepName,
                    "image/png",
                    ".png",
                    new ByteArrayInputStream(screenshot.getImageBytes())
                );
                TestLogger.logDebug("Screenshot embedded in Allure for step: %s", stepName);
            }
        } catch (Exception e) {
            TestLogger.logWarn("Failed to embed screenshot in Allure for step %s: %s", stepName, e.getMessage());
        }
    }

    /**
     * Embed failure screenshot
     */
    private void embedFailureScreenshot() {
        try {
            ScreenshotManager.ScreenshotRecord screenshot = ScreenshotManager.getLatestScreenshot();
            if (screenshot != null && screenshot.getImageBytes() != null) {
                lifecycle.addAttachment(
                    "Failure Screenshot",
                    "image/png",
                    ".png",
                    new ByteArrayInputStream(screenshot.getImageBytes())
                );
                TestLogger.logInfo("Failure screenshot embedded in Allure");
            }
        } catch (Exception e) {
            TestLogger.logWarn("Failed to embed failure screenshot in Allure: %s", e.getMessage());
        }
    }

    /**
     * Add failure details to test case
     */
    private void addFailureDetails(Throwable error) {
        lifecycle.updateTestCase(tc -> {
            tc.setStatusDetails(new io.qameta.allure.model.StatusDetails()
                .setMessage(error.getMessage())
                .setTrace(getStackTrace(error))
            );
            return tc;
        });
    }

    /**
     * Get stack trace from exception
     */
    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName()).append(": ").append(throwable.getMessage()).append("\n");
        
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        
        if (throwable.getCause() != null) {
            sb.append("Caused by: ").append(getStackTrace(throwable.getCause()));
        }
        
        return sb.toString();
    }

    /**
     * Add custom attachment to Allure report
     */
    public static void addAttachment(String name, String mimeType, byte[] content) {
        try {
            Allure.addAttachment(name, mimeType, new ByteArrayInputStream(content), "");
            TestLogger.logDebug("Attachment added to Allure: %s", name);
        } catch (Exception e) {
            TestLogger.logWarn("Failed to add attachment to Allure: %s", e.getMessage());
        }
    }

    /**
     * Add text attachment to Allure report
     */
    public static void addTextAttachment(String name, String content) {
        try {
            Allure.addAttachment(name, "text/plain", new ByteArrayInputStream(
                content.getBytes(StandardCharsets.UTF_8)), ".txt");
            TestLogger.logDebug("Text attachment added to Allure: %s", name);
        } catch (Exception e) {
            TestLogger.logWarn("Failed to add text attachment to Allure: %s", e.getMessage());
        }
    }

    /**
     * Get scenario UUID
     */
    public static String getScenarioUuid(String scenarioName) {
        return scenarioUuids.getOrDefault(scenarioName, "UNKNOWN");
    }
}
