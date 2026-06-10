package org.example.utils;

import io.appium.java_client.AppiumDriver;
import com.microsoft.playwright.Page;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.IOException;

/**
 * Cross-Platform Screenshot Utility
 * Handles screenshot capture for:
 * - Playwright (Web Browser Automation)
 * - Appium (Mobile App Automation - iOS/Android)
 * - Selenium (Legacy Web Automation)
 *
 * Provides unified interface for all automation tools
 */
public class ScreenshotCapture {

    /**
     * Capture screenshot from Playwright Page
     * Used for Web browser automation tests
     */
    public static byte[] capturePlaywrightScreenshot(Page page) {
        try {
            if (page == null) {
                TestLogger.logWarn("Playwright Page is null. Cannot capture screenshot.");
                return null;
            }
            
            byte[] screenshot = page.screenshot();
            TestLogger.logDebug("Playwright screenshot captured successfully");
            return screenshot;
        } catch (Exception e) {
            TestLogger.logError("Failed to capture Playwright screenshot: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Capture screenshot from Appium Driver
     * Used for Mobile app automation (iOS/Android)
     * Works with UIAutomator2, XCUITest, etc.
     */
    public static byte[] captureAppiumScreenshot(AppiumDriver driver) {
        try {
            if (driver == null) {
                TestLogger.logWarn("Appium Driver is null. Cannot capture screenshot.");
                return null;
            }
            
            byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
            TestLogger.logDebug("Appium screenshot captured successfully");
            return screenshot;
        } catch (Exception e) {
            TestLogger.logError("Failed to capture Appium screenshot: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Capture screenshot from Selenium WebDriver
     * Used for legacy or hybrid Selenium-based tests
     */
    public static byte[] captureSeleniumScreenshot(WebDriver driver) {
        try {
            if (driver == null) {
                TestLogger.logWarn("Selenium WebDriver is null. Cannot capture screenshot.");
                return null;
            }
            
            if (driver instanceof TakesScreenshot) {
                byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                TestLogger.logDebug("Selenium screenshot captured successfully");
                return screenshot;
            } else {
                TestLogger.logWarn("WebDriver does not support screenshot capability");
                return null;
            }
        } catch (Exception e) {
            TestLogger.logError("Failed to capture Selenium screenshot: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Universal screenshot capture - auto-detects driver type
     * Returns true if screenshot captured successfully
     */
    public static boolean captureScreenshot(Object driver, String stepName) {
        if (driver == null) {
            TestLogger.logWarn("Driver is null. Cannot capture screenshot for step: %s", stepName);
            return false;
        }

        byte[] screenshotBytes = null;

        // Detect driver type and capture accordingly
        if (driver instanceof Page) {
            screenshotBytes = capturePlaywrightScreenshot((Page) driver);
        } else if (driver instanceof AppiumDriver) {
            screenshotBytes = captureAppiumScreenshot((AppiumDriver) driver);
        } else if (driver instanceof WebDriver) {
            screenshotBytes = captureSeleniumScreenshot((WebDriver) driver);
        } else {
            TestLogger.logWarn("Unknown driver type: %s. Cannot capture screenshot.", driver.getClass().getName());
            return false;
        }

        // If screenshot captured, save it
        if (screenshotBytes != null && screenshotBytes.length > 0) {
            try {
                // Determine test type from driver
                String testType = determineTestType(driver);
                
                // Capture using ScreenshotManager
                String filePath = captureScreenshotByType(screenshotBytes, testType, stepName);
                
                if (filePath != null) {
                    TestLogger.logInfo("Screenshot captured and saved: %s", filePath);
                    return true;
                }
            } catch (IOException e) {
                TestLogger.logError("Failed to save screenshot: %s", e.getMessage());
                return false;
            }
        }

        TestLogger.logWarn("Failed to capture screenshot for step: %s", stepName);
        return false;
    }

    /**
     * Determine test type based on driver instance
     */
    private static String determineTestType(Object driver) {
        if (driver instanceof Page) {
            return "WEB";
        } else if (driver instanceof AppiumDriver) {
            try {
                // Check if it's iOS or Android
                AppiumDriver appiumDriver = (AppiumDriver) driver;
                String platformName = (String) appiumDriver.getCapabilities().getCapability("platformName");
                
                if (platformName != null) {
                    if (platformName.equalsIgnoreCase("iOS")) {
                        TestLogger.logDebug("Mobile platform detected: iOS");
                    } else if (platformName.equalsIgnoreCase("Android")) {
                        TestLogger.logDebug("Mobile platform detected: Android");
                    }
                }
                return "MOBILE";
            } catch (Exception e) {
                TestLogger.logDebug("Could not determine mobile platform: %s", e.getMessage());
                return "MOBILE";
            }
        } else if (driver instanceof WebDriver) {
            return "WEB";
        }
        return "UNKNOWN";
    }

    /**
     * Capture screenshot by test type
     */
    private static String captureScreenshotByType(byte[] screenshotBytes, String testType, String stepName) throws IOException {
        switch (testType.toUpperCase()) {
            case "WEB":
                return ScreenshotManager.captureWebScreenshot(screenshotBytes, stepName);
            case "MOBILE":
                return ScreenshotManager.captureMobileScreenshot(screenshotBytes, stepName);
            case "API":
                return ScreenshotManager.captureApiScreenshot(screenshotBytes, stepName);
            default:
                return ScreenshotManager.captureWebScreenshot(screenshotBytes, stepName);
        }
    }

    /**
     * Capture Appium screenshot with additional mobile context
     * Includes device information in logs
     */
    public static byte[] captureAppiumScreenshotWithContext(AppiumDriver driver, String stepName) {
        try {
            if (driver == null) {
                TestLogger.logWarn("Appium Driver is null. Cannot capture screenshot.");
                return null;
            }

            // Get device information
            String deviceName = (String) driver.getCapabilities().getCapability("deviceName");
            String platformVersion = (String) driver.getCapabilities().getCapability("platformVersion");
            String platformName = (String) driver.getCapabilities().getCapability("platformName");

            // Log context
            TestLogger.logInfo("Mobile Screenshot Context - Device: %s, Platform: %s %s", 
                             deviceName, platformName, platformVersion);

            // Capture screenshot
            byte[] screenshot = driver.getScreenshotAs(OutputType.BYTES);
            
            TestLogger.logDebug("Appium screenshot captured successfully for step: %s", stepName);
            return screenshot;
        } catch (Exception e) {
            TestLogger.logError("Failed to capture Appium screenshot: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Capture full-page screenshot (for Playwright only)
     * Captures entire page including areas outside viewport
     */
    public static byte[] capturePlaywrightFullPageScreenshot(Page page) {
        try {
            if (page == null) {
                TestLogger.logWarn("Playwright Page is null. Cannot capture full-page screenshot.");
                return null;
            }

            byte[] screenshot = page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
            TestLogger.logDebug("Playwright full-page screenshot captured successfully");
            return screenshot;
        } catch (Exception e) {
            TestLogger.logError("Failed to capture Playwright full-page screenshot: %s", e.getMessage());
            return null;
        }
    }

    /**
     * Capture screenshot of specific element (Playwright)
     */
    public static byte[] capturePlaywrightElementScreenshot(Page page, String selector) {
        try {
            if (page == null) {
                TestLogger.logWarn("Playwright Page is null. Cannot capture element screenshot.");
                return null;
            }

            byte[] screenshot = page.locator(selector).screenshot();
            TestLogger.logDebug("Playwright element screenshot captured for selector: %s", selector);
            return screenshot;
        } catch (Exception e) {
            TestLogger.logError("Failed to capture element screenshot for selector %s: %s", selector, e.getMessage());
            return null;
        }
    }

    /**
     * Capture screenshot with optional wait
     * Useful for waiting for dynamic elements to appear
     */
    public static byte[] captureScreenshotWithWait(Object driver, String stepName, int waitTimeMs) {
        try {
            if (waitTimeMs > 0) {
                TestLogger.logDebug("Waiting %d ms before capturing screenshot", waitTimeMs);
                Thread.sleep(waitTimeMs);
            }

            byte[] screenshot = null;
            if (driver instanceof Page) {
                screenshot = capturePlaywrightScreenshot((Page) driver);
            } else if (driver instanceof AppiumDriver) {
                screenshot = captureAppiumScreenshot((AppiumDriver) driver);
            } else if (driver instanceof WebDriver) {
                screenshot = captureSeleniumScreenshot((WebDriver) driver);
            }

            if (screenshot != null) {
                TestLogger.logDebug("Screenshot captured with wait for step: %s", stepName);
            }

            return screenshot;
        } catch (InterruptedException e) {
            TestLogger.logError("Screenshot capture with wait interrupted: %s", e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }
}
