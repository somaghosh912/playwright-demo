package org.example.listener;

import io.cucumber.plugin.event.*;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureLifecycle;
import org.example.utils.TestLogger;
import org.example.utils.ScreenshotManager;
import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.*;
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
public class AllureReportListener implements ConcurrentEventListener {

    /**
     * Set event publisher for Cucumber events
     */
    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseStarted.class, this::handleTestCaseStarted);
        publisher.registerHandlerFor(TestCaseFinished.class, this::handleTestCaseFinished);
        publisher.registerHandlerFor(TestStepStarted.class, this::handleTestStepStarted);
        publisher.registerHandlerFor(TestStepFinished.class, this::handleTestStepFinished);
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

        TestLogger.logInfo(
                "Allure: Scenario Started - %s",
                scenarioName);

        addTextAttachment(
                "Scenario Information",
                "Scenario: " + scenarioName);
    }

    /**
     * Handle test case finished
     */
    private void handleTestCaseFinished(TestCaseFinished event) {

        String scenarioName = event.getTestCase().getName();

        if (event.getResult().getStatus() == Status.FAILED) {

            if (event.getResult().getError() != null) {

                addTextAttachment(
                        "Failure Details",
                        getStackTrace(event.getResult().getError()));
            }

            embedFailureScreenshot();
        }

        TestLogger.logInfo(
                "Allure: Scenario Finished - %s [%s]",
                scenarioName,
                event.getResult().getStatus());
    }

    /**
     * Handle test step started
     */
    private void handleTestStepStarted(TestStepStarted event) {

        if (event.getTestStep() instanceof PickleStepTestStep step) {

            TestLogger.logDebug(
                    "Step Started : %s",
                    step.getStep().getText());
        }
    }

    /**
     * Handle test step finished
     */
    private void handleTestStepFinished(TestStepFinished event) {

        if (event.getTestStep() instanceof PickleStepTestStep step) {

            String stepText = step.getStep().getText();

            if (event.getResult().getStatus() == Status.FAILED) {

                embedStepScreenshot(stepText);

                if (event.getResult().getError() != null) {

                    addTextAttachment(
                            "Failed Step",
                            stepText + "\n\n"
                                    + getStackTrace(event.getResult().getError()));
                }
            }

            TestLogger.logDebug(
                    "Step Finished : %s [%s]",
                    stepText,
                    event.getResult().getStatus());
        }
    }

    /**
     * Handle test run finished
     */
    private void handleTestRunFinished(TestRunFinished event) {
        TestLogger.logInfo("Allure: Test run finished");
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

            ScreenshotManager.ScreenshotRecord screenshot =
                    ScreenshotManager.getLatestScreenshot();

            if (screenshot != null &&
                    screenshot.getImageBytes() != null) {

                Allure.addAttachment(
                        "Screenshot - " + stepName,
                        "image/png",
                        new ByteArrayInputStream(
                                screenshot.getImageBytes()),
                        ".png");

                TestLogger.logDebug(
                        "Screenshot attached for step: %s",
                        stepName);
            }

        } catch (Exception e) {

            TestLogger.logWarn(
                    "Failed to attach screenshot for step %s: %s",
                    stepName,
                    e.getMessage());
        }
    }

    /**
     * Embed failure screenshot
     */
    private void embedFailureScreenshot() {

        try {

            ScreenshotManager.ScreenshotRecord screenshot =
                    ScreenshotManager.getLatestScreenshot();

            if (screenshot != null
                    && screenshot.getImageBytes() != null) {

                Allure.addAttachment(
                        "Failure Screenshot",
                        "image/png",
                        new ByteArrayInputStream(
                                screenshot.getImageBytes()),
                        ".png");
            }

        } catch (Exception e) {

            TestLogger.logWarn(
                    "Failed to attach screenshot : %s",
                    e.getMessage());
        }
    }

    /**
     * Add failure details to test case
     */
    private void addFailureDetails(Throwable error) {

        try {

            StringBuilder details = new StringBuilder();

            details.append("Exception: ")
                    .append(error.getClass().getName())
                    .append("\n\n");

            details.append("Message: ")
                    .append(error.getMessage())
                    .append("\n\n");

            details.append("Stack Trace:\n")
                    .append(getStackTrace(error));

            Allure.addAttachment(
                    "Failure Details",
                    "text/plain",
                    new ByteArrayInputStream(
                            details.toString().getBytes(StandardCharsets.UTF_8)),
                    ".txt");

        } catch (Exception e) {

            TestLogger.logWarn(
                    "Failed to add failure details to Allure: %s",
                    e.getMessage());
        }
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


}
