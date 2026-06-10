package org.example.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.example.driver.AppiumDriverFactory;
import org.example.driver.PlaywrightFactory;
import org.example.utils.ExtentReportManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Hooks {

    private static final Logger LOG = LoggerFactory.getLogger(Hooks.class);

    @Before(value = "@Web", order = 10)
    public void startWeb(Scenario scenario) {
        LOG.info("Starting Web scenario: {}", scenario.getName());
        ExtentReportManager.createTest(scenario.getName(), "Web Test");
        ExtentReportManager.info("Started scenario: " + scenario.getName());
        PlaywrightFactory.startBrowser();
    }

    @Before(value = "@Mobile", order = 10)
    public void startMobile(Scenario scenario) {
        LOG.info("Starting Mobile scenario: {}", scenario.getName());
        ExtentReportManager.createTest(scenario.getName(), "Mobile Test");
        ExtentReportManager.info("Started scenario: " + scenario.getName());
        AppiumDriverFactory.start();
    }

    @After(value = "@Web", order = 10)
    public void stopWeb(Scenario scenario) {
        if (scenario.isFailed() && PlaywrightFactory.page() != null) {
            byte[] png = PlaywrightFactory.page().screenshot();
            scenario.attach(png, "image/png", "failure-screenshot");
            ExtentReportManager.addScreenshotAsBase64(png, "Failure Screenshot");
            ExtentReportManager.fail("Test failed - " + scenario.getName());
        } else if (scenario.isFailed()) {
            ExtentReportManager.fail("Test failed - " + scenario.getName());
        } else {
            ExtentReportManager.pass("Test passed - " + scenario.getName());
        }
        attachVideo(scenario);
        PlaywrightFactory.quit();
        ExtentReportManager.clearThread();
    }

    @After(value = "@Mobile", order = 10)
    public void stopMobile(Scenario scenario) {
        if (scenario.isFailed()) {
            ExtentReportManager.fail("Mobile test failed - " + scenario.getName());
        } else {
            ExtentReportManager.pass("Mobile test passed - " + scenario.getName());
        }
        AppiumDriverFactory.quit();
        ExtentReportManager.clearThread();
    }

    private void attachVideo(Scenario scenario) {
        try {
            String videoPath = PlaywrightFactory.getVideoPath();
            if (videoPath != null) {
                Path path = Paths.get(videoPath);
                if (Files.exists(path)) {
                    byte[] videoBytes = Files.readAllBytes(path);
                    scenario.attach(videoBytes, "video/webm", "test-video");
                    LOG.info("Video attached to scenario: {}", scenario.getName());
                }
            }
        } catch (Exception e) {
            LOG.warn("Could not attach video to scenario: {}", e.getMessage());
        }
    }
}
