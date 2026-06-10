package org.example.listener;

import org.example.utils.ExtentReportManager;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CucumberTestListener implements ITestListener {

    private static final Logger LOG = LoggerFactory.getLogger(CucumberTestListener.class);

    @Override
    public void onStart(ITestContext context) {
        LOG.info("Test execution started");
    }

    @Override
    public void onFinish(ITestContext context) {
        LOG.info("Test execution finished - Flushing Extent Reports");
        ExtentReportManager.flush();
    }

    @Override
    public void onTestStart(ITestResult result) {
        // Handled in Hooks
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        // Handled in Hooks
    }

    @Override
    public void onTestFailure(ITestResult result) {
        // Handled in Hooks
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        LOG.info("Test skipped: {}", result.getName());
        ExtentReportManager.skip("Test skipped: " + result.getName());
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        // Not typically used
    }
}
