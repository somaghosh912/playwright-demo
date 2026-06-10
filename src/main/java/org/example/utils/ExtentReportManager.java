package org.example.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExtentReportManager {

    private static final Logger LOG = LoggerFactory.getLogger(ExtentReportManager.class);
    private static ExtentReports extentReports;
    private static ThreadLocal<ExtentTest> extentTestThread = new ThreadLocal<>();

    static {
        initializeExtentReports();
    }

    private static void initializeExtentReports() {
        String reportPath = generateReportPath();
        ExtentSparkReporter sparkReporter = new ExtentSparkReporter(reportPath);
        sparkReporter.config().setTheme(Theme.DARK);
        sparkReporter.config().setReportName("Playwright Test Report");
        sparkReporter.config().setDocumentTitle("Test Execution Report");
        sparkReporter.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");

        extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);
        extentReports.setSystemInfo("Environment", System.getProperty("test.environment", "Local"));
        extentReports.setSystemInfo("Browser", System.getProperty("browser", "Chromium"));
        extentReports.setSystemInfo("OS", System.getProperty("os.name"));
        extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));

        LOG.info("Extent Reports initialized at: {}", reportPath);
    }

    private static String generateReportPath() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String reportDir = "target/extent-reports";
        new File(reportDir).mkdirs();
        return reportDir + "/extent-report-" + timestamp + ".html";
    }

    public static void createTest(String testName, String description) {
        ExtentTest test = extentReports.createTest(testName, description);
        extentTestThread.set(test);
        LOG.info("Test created in Extent Report: {}", testName);
    }

    public static ExtentTest getTest() {
        return extentTestThread.get();
    }

    public static void pass(String message) {
        ExtentTest test = getTest();
        if (test != null) {
            test.pass(message);
        }
    }

    public static void fail(String message) {
        ExtentTest test = getTest();
        if (test != null) {
            test.fail(message);
        }
    }

    public static void skip(String message) {
        ExtentTest test = getTest();
        if (test != null) {
            test.skip(message);
        }
    }

    public static void info(String message) {
        ExtentTest test = getTest();
        if (test != null) {
            test.info(message);
        }
    }

    public static void addScreenshot(String screenshotPath) {
        ExtentTest test = getTest();
        if (test != null) {
            test.addScreenCaptureFromPath(screenshotPath);
        }
    }

    public static void addScreenshotAsBase64(byte[] screenshotBytes, String title) {
        ExtentTest test = getTest();
        if (test != null) {
            String base64 = java.util.Base64.getEncoder().encodeToString(screenshotBytes);
            test.addScreenCaptureFromBase64String(base64, title);
        }
    }

    public static void flush() {
        if (extentReports != null) {
            extentReports.flush();
            LOG.info("Extent Reports flushed and report generated");
        }
        extentTestThread.remove();
    }

    public static void clearThread() {
        extentTestThread.remove();
    }
}
