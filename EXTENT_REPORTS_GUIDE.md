# Extent Reports Integration Guide

## Overview
Extent Reports have been integrated into your Playwright automation framework to provide rich HTML test reports with detailed logging, screenshots, and test status tracking.

## Generated Report Location
After test execution, the Extent Report is generated at:
```
target/extent-reports/extent-report-{timestamp}.html
```

## Features

### 1. Automatic Test Lifecycle Management
- Tests are automatically created when scenarios start
- Test status (pass/fail/skip) is automatically captured
- Screenshots are attached for failed tests

### 2. Using ExtentReportManager in Your Tests

#### Available Methods

```java
// Create a test (automatically done in Hooks)
ExtentReportManager.createTest("Test Name", "Test Description");

// Log different severity levels
ExtentReportManager.pass("Test step passed");
ExtentReportManager.fail("Test step failed");
ExtentReportManager.skip("Test step skipped");
ExtentReportManager.info("Additional test information");

// Attach screenshots
ExtentReportManager.addScreenshot("/path/to/screenshot.png");
ExtentReportManager.addScreenshotAsBase64(screenshotBytes, "Screenshot Title");

// Flush the report (automatically done in TestListener)
ExtentReportManager.flush();

// Clear thread-local data
ExtentReportManager.clearThread();
```

### 3. Integration Points

#### In Step Definitions
```java
import org.example.utils.ExtentReportManager;

@When("user performs some action")
public void userPerformsAction() {
    // Your test logic
    ExtentReportManager.info("User clicked on login button");
    // More logic
    ExtentReportManager.pass("Action completed successfully");
}
```

#### In Hooks (Already Integrated)
- `@Before`: Creates test in Extent Report
- `@After`: Attaches screenshots and marks test status
- `CucumberTestListener`: Flushes report after all tests complete

### 4. Customization

#### Modify Report Theme
Edit `ExtentReportManager.java` - Change theme in `initializeExtentReports()`:
```java
sparkReporter.config().setTheme(Theme.STANDARD); // or Theme.DARK
```

#### Add System Information
Modify the system info setup in `ExtentReportManager.java`:
```java
extentReports.setSystemInfo("Custom Key", "Custom Value");
```

#### Change Report Name/Title
Edit these lines in `initializeExtentReports()`:
```java
sparkReporter.config().setReportName("Your Report Name");
sparkReporter.config().setDocumentTitle("Your Document Title");
```

## Best Practices

1. **Log Meaningful Messages**: Use `ExtentReportManager.info()` for test flow documentation
2. **Attach Screenshots**: Automatically done for failures; add manually for important steps
3. **Use Consistent Status**: Only pass/fail/skip/info to maintain report clarity
4. **Thread Safety**: All methods are thread-safe using ThreadLocal

## Maven Build

To build and run tests with Extent Reports:
```bash
mvn clean test
```

The report will be generated in `target/extent-reports/` directory.

## Report Contents

Each test entry includes:
- Test name and description
- Test status (PASS/FAIL/SKIP)
- Execution time
- System information (OS, Java version, Browser, Environment)
- Detailed logs of all logged events
- Attached screenshots
- Test timeline

## Troubleshooting

- **Report not generated**: Ensure tests complete and listener is properly configured
- **Screenshots not showing**: Verify screenshot bytes are not null before attaching
- **Multiple reports**: Each test run creates a new timestamped report file
