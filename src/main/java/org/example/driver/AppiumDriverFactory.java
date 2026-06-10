package org.example.driver;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.example.config.ConfigManager;
import org.example.utils.JsonUtil;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Map;

public final class AppiumDriverFactory {

    private static final Logger LOG = LoggerFactory.getLogger(AppiumDriverFactory.class);
    private static final ThreadLocal<AppiumDriver> DRIVER = new ThreadLocal<>();

    private AppiumDriverFactory() {}

    public static AppiumDriver start() {
        Map<String, Object> caps = JsonUtil.readAsMap(ConfigManager.mobileCapsFile());
        DesiredCapabilities desired = new DesiredCapabilities();
        caps.forEach(desired::setCapability);

        try {
            URL url = new URL(ConfigManager.appiumUrl());
            AppiumDriver driver = "ios".equalsIgnoreCase(ConfigManager.mobilePlatform())
                    ? new IOSDriver(url, desired)
                    : new AndroidDriver(url, desired);
            DRIVER.set(driver);
            LOG.info("Started Appium driver platform={}", ConfigManager.mobilePlatform());
            return driver;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start Appium driver: " + e.getMessage(), e);
        }
    }

    public static AppiumDriver driver() {
        return DRIVER.get();
    }

    public static void quit() {
        AppiumDriver driver = DRIVER.get();
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
            DRIVER.remove();
        }
    }
}
