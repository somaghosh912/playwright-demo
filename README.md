# playwright-demo — BDD Test Automation Framework

A unified Java + Cucumber + TestNG framework that automates **Web (Playwright)**,
**Mobile (Appium)** and **API (REST Assured)** layers through the same Gherkin
feature pipeline.

---

## 1. Prerequisites

| Tool     | Version             | Notes                                                  |
|----------|---------------------|--------------------------------------------------------|
| Java     | 17+                 | Tested with Temurin 17                                 |
| Maven    | 3.9+                | `mvn -v`                                               |
| Node.js  | 18+                 | Required by Playwright browsers                        |
| Appium   | 2.x                 | `npm i -g appium` + `appium driver install uiautomator2` |
| Allure   | 2.29+ (optional)    | For the Allure dashboard                               |

First-time browser install for Playwright:

```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install"
```

---

## 2. Architecture

```
                +--------------------+
                |  Cucumber Features |   <-- business-readable Gherkin
                +---------+----------+
                          |
                +---------v----------+
                |   Step Definitions |   (org.example.steps)
                +---+-----+-----+----+
                    |     |     |
       Web (POM)    |     |     |  API (REST Assured)
   +----------------+     |     +-------------------+
   |                      |                         |
+--v-----------+   +------v-------+      +----------v-----------+
| Playwright   |   | Appium       |      | ApiClient            |
| Factory      |   | DriverFactory|      | (RestAssured spec)   |
+--+-----------+   +------+-------+      +----------+-----------+
   |                      |                         |
   +----------+-----------+-------------------------+
              |
       +------v------+        +-----------------+
       | ConfigMgr   |        | Object          |
       | (JSON-only, |        | Repository      |
       | Jackson-    |        | (locators.json) |
       | backed)     |        +-----------------+
       +-------------+        +-----------------+
       | DataReader  |        | Reporting       |
       | (JSON/YAML/ |        | (Cucumber HTML/ |
       | Excel)      |        | JSON + Allure + |
       +-------------+        | ExtentReports)  |
                              +-----------------+
                              | TestLogger      |
                              | (Central Log)   |
                              +-----------------+
                              | ScreenCapture   |
                              | (Multi-platform)|
                              +-----------------+
```

Key patterns: **POM**, **Object Repository**, **Driver Factory (thread-local)**,
**Singleton Playwright per thread**, **Externalised Test Data**, **Tagged execution**,
**Central Logging**, **Cross-platform Screenshots**, **Report Integration**.

---

## 3. Folder Structure

```
playwright-demo/
├── pom.xml
├── testng.xml
├── README.md
└── src/
    ├── main/
    │   ├── java/org/example/
    │   │   ├── api/              ApiClient (REST Assured)
    │   │   ├── config/           JSON-only ConfigManager (Jackson)
    │   │   ├── driver/           PlaywrightFactory, AppiumDriverFactory
    │   │   ├── locators/         ORManager (object repository)
    │   │   ├── pages/            BasePage + Page Objects
    │   │   └── utils/            
    │   │       ├── TestLogger           (Central logging mechanism)
    │   │       ├── ScreenshotManager    (Organized screenshot capture)
    │   │       ├── ScreenshotCapture    (Cross-platform screenshot API)
    │   │       ├── JsonUtil, YamlUtil, ExcelUtil, DataReader
    │   └── resources/
    │       └── log4j2.xml         (Enhanced logging configuration)
    └── test/
        ├── java/org/example/
        │   ├── hooks/
        │   │   └── TestHooks      (Scenario lifecycle with logging/screenshots)
        │   ├── listener/
        │   │   ├── ExtentReportsListener    (ExtentReports integration)
        │   │   └── AllureReportListener     (Allure integration)
        │   ├── runner/           TestRunner (Cucumber + TestNG)
        │   └── steps/            WebSteps, MobileSteps, ApiSteps
        └── resources/
            ├── features/         *.feature (Gherkin)
            ├── config/           config.json, qa.json, uat.json,
            │                     caps-android.json, caps-ios.json
            ├── locators/         locators.json
            └── testdata/         testdata.json, testdata.yaml, testdata.xlsx
```

---

## 4. Configuration

`ConfigManager` is a small Jackson-backed loader. All configuration is in JSON.
Merge order (later wins):

1. `src/test/resources/config/config.json` (defaults)
2. `src/test/resources/config/${env}.json` (per-environment overrides)
3. `-D` JVM system properties using dotted keys (e.g. `-Dweb.browser=firefox`)

Switch environment / browser from CLI:

```bash
mvn test -Denv=uat
mvn test -Dweb.browser=firefox -Dweb.headless=false
```

---

## 5. Managing Test Data

All credentials, product names and API payloads live outside feature/step code in:

| Source        | File                                      | Reader              |
|---------------|-------------------------------------------|---------------------|
| JSON          | `src/test/resources/testdata/testdata.json` | `DataReader.jsonString(...)` |
| YAML          | `src/test/resources/testdata/testdata.yaml` | `DataReader.yamlValue(...)`  |
| Excel (.xlsx) | `src/test/resources/testdata/testdata.xlsx` | `ExcelUtil.readSheet(...)`   |

Feature files reference data **by key**, never by literal:

```gherkin
When user logs in with credentials from test data "default"
And user adds the "backpack" product to cart
```

`default` and `backpack` resolve at runtime against `testdata.json`.

---

## 6. Object Repository (OR)

Locators are maintained outside step/page logic in a single JSON tree:

- `src/test/resources/locators/locators.json`

`ORManager.get("login.username")` walks the dotted key and returns the locator
string. Pages never hard-code selectors:

```java
$("login.username").fill(username);
```

To update a selector, change `locators.json` only — no Java recompile of tests
needed beyond the standard build.

---

## 7. Central Logging Mechanism

### Overview
The framework includes a **centralized logging system** (`TestLogger`) that provides:
- **Context-aware logging** with test type and scenario name
- **Clear stack traces** for failure analysis
- **Thread-safe** logging for parallel execution
- **Multiple log levels** (INFO, WARN, ERROR, DEBUG)
- **Separate log files** for Web, Mobile, and API tests
- **Test execution context** tracking

### Log Files Location
```
target/logs/
├── automation.log       # All logs
├── web_tests.log       # Web test logs only
├── mobile_tests.log    # Mobile test logs only
├── api_tests.log       # API test logs only
├── errors.log          # Errors only (for quick failure analysis)
└── debug.log           # Debug level logs
```

### Usage Examples

```java
// Basic logging
TestLogger.logInfo("Login successful");
TestLogger.logWarn("Element not found, retrying...");
TestLogger.logError("Test failed", exception);

// Formatted logging
TestLogger.logInfo("User: %s logged in successfully", username);

// Step logging
TestLogger.logStep("Click login button");

// Step with assertions
TestLogger.logStepWithAssertion("Verify page title", 
    "Login Page", actualTitle);

// API logging
TestLogger.logApiRequest("POST", "/api/login", requestBody);
TestLogger.logApiResponse(200, responseBody);

// Screenshot logging
TestLogger.logScreenshotCapture(filePath);
```

### In Cucumber Hooks

```java
@Before
public void beforeScenario(Scenario scenario) {
    String testType = determineTestType(scenario);
    TestLogger.initializeScenario(scenario.getName(), testType);
    // testType can be "WEB", "MOBILE", or "API"
}

@After
public void afterScenario(Scenario scenario) {
    boolean passed = !scenario.isFailed();
    String failureMessage = scenario.isFailed() ? "See logs for details" : null;
    TestLogger.cleanupScenario(passed, failureMessage);
}
```

### Log Pattern
Each log entry includes:
- **Timestamp** (ISO 8601 format)
- **Log Level** (INFO, WARN, ERROR, etc.)
- **Thread ID** (for parallel execution tracking)
- **Test Type** (WEB, MOBILE, API)
- **Scenario Name**
- **Message**

Example log output:
```
2026-06-10 17:45:32.123 INFO  [main] [WEB | Login Scenario] → Step: User enters credentials
2026-06-10 17:45:35.456 ERROR [main] [WEB | Login Scenario] Login failed: Element not found
```

---

## 8. Screenshot Management

### Overview
The framework provides **unified screenshot capture** across all platforms:
- **Playwright** (Web browser automation)
- **Appium** (iOS/Android mobile app automation)
- **Selenium** (Legacy web automation)

Screenshots are automatically:
- **Organized by test type** (web/, mobile/, api/ folders)
- **Organized by scenario** (scenario-specific folders with timestamps)
- **Embedded in Allure Report**
- **Embedded in ExtentReports**
- **Included in HTML index** with Base64 encoding

### Folder Structure
```
target/screenshots/
├── Login_Scenario_2026-06-10_17-45-30/
│   ├── web/
│   │   ├── 2026-06-10_17-45-32-123_User_enters_username.png
│   │   ├── 2026-06-10_17-45-33-456_User_enters_password.png
│   │   └── 2026-06-10_17-45-35-789_Click_login_button.png
│   ├── mobile/  (if mobile steps in scenario)
│   └── screenshots_index.html  (embeds all screenshots)
└── API_Test_Scenario_2026-06-10_17-50-00/
    └── api/
        └── 2026-06-10_17-50-01-234_API_Response_Screenshot.png
```

### Usage - Web Tests (Playwright)

```java
@When("user takes screenshot")
public void takeWebScreenshot() {
    Page page = PlaywrightFactory.getPage();
    byte[] screenshot = ScreenshotCapture.capturePlaywrightScreenshot(page);
    ScreenshotManager.captureWebScreenshot(screenshot, "Step description");
}

// Or using the universal method
@Then("I verify login page")
public void verifyLoginPage() {
    Page page = PlaywrightFactory.getPage();
    ScreenshotCapture.captureScreenshot(page, "Verify login page");
}

// Full-page screenshot
byte[] fullPageScreenshot = ScreenshotCapture.capturePlaywrightFullPageScreenshot(page);

// Element screenshot
byte[] elementScreenshot = ScreenshotCapture.capturePlaywrightElementScreenshot(
    page, "#login-button");
```

### Usage - Mobile Tests (Appium)

```java
@When("user takes mobile screenshot")
public void takeMobileScreenshot() {
    AppiumDriver driver = AppiumDriverFactory.getDriver();
    byte[] screenshot = ScreenshotCapture.captureAppiumScreenshot(driver);
    ScreenshotManager.captureMobileScreenshot(screenshot, "Step description");
}

// With mobile context
@Then("verify app screen")
public void verifyAppScreen() {
    AppiumDriver driver = AppiumDriverFactory.getDriver();
    byte[] screenshot = ScreenshotCapture.captureAppiumScreenshotWithContext(
        driver, "Verify app screen");
}

// Universal method (auto-detects Appium driver)
ScreenshotCapture.captureScreenshot(driver, "Mobile step");
```

### Usage - API Tests

```java
@Then("capture API response screenshot")
public void captureApiResponse() {
    // Can capture UI response screenshots if applicable
    byte[] screenshot = ScreenshotCapture.captureScreenshot(someDriver, 
        "API Response verification");
}
```

### Screenshot Methods

```java
// Web (Playwright)
byte[] screenshot = ScreenshotCapture.capturePlaywrightScreenshot(page);

// Mobile (Appium)
byte[] screenshot = ScreenshotCapture.captureAppiumScreenshot(driver);

// Selenium
byte[] screenshot = ScreenshotCapture.captureSeleniumScreenshot(webDriver);

// Universal (auto-detect)
ScreenshotCapture.captureScreenshot(driver, "Step name");

// Full-page (Playwright only)
byte[] fullPage = ScreenshotCapture.capturePlaywrightFullPageScreenshot(page);

// Element (Playwright)
byte[] element = ScreenshotCapture.capturePlaywrightElementScreenshot(page, selector);

// With wait
byte[] screenshot = ScreenshotCapture.captureScreenshotWithWait(driver, "Step", 1000);
```

### Automatic Screenshot Capture in Hooks

The `TestHooks` class automatically initializes screenshot management:

```java
@Before
public void beforeScenario(Scenario scenario) {
    TestLogger.initializeScenario(scenario.getName(), testType);
    ScreenshotManager.initializeScenario(scenario.getName());
}

@After
public void afterScenario(Scenario scenario) {
    String indexPath = ScreenshotManager.generateScreenshotIndex();
    if (indexPath != null) {
        TestLogger.logInfo("Screenshot index: %s", indexPath);
    }
    ScreenshotManager.cleanupScenario();
}
```

---

## 9. Report Integration

### Allure Reports

Allure reports are automatically generated with:
- **Embedded screenshots** for each step
- **Failure screenshots** on test failure
- **Stack traces** for errors
- **Test duration** and timeline
- **Category labels** (tags)

Access Allure report:
```bash
mvn allure:serve
```

Allure reports location: `target/allure-results/`

**Features:**
- Screenshots embedded as attachments
- Full stack traces for debugging
- Timeline view of execution
- Test categorization by tags
- Trend analysis across runs

### ExtentReports

ExtentReports generates beautiful HTML reports with:
- **Embedded screenshots** in each step
- **Failure details** with stack traces
- **Device information** for mobile tests
- **System information** (Java version, OS, etc.)
- **Dark theme** for easy reading

Report location: `target/extent-reports/extent-report.html`

**Features:**
- Real-time report generation
- Embedded Base64 images (self-contained HTML)
- Step-level screenshot attachment
- Test categorization
- Comprehensive execution summary

### Cucumber HTML Reports

Standard Cucumber HTML reports with:
- **Feature overview**
- **Scenario execution status**
- **Step details**

Report location: `target/cucumber-reports/report.html`

### Log Files

All test logs with full context:

```bash
# View all logs
cat target/logs/automation.log

# View only errors
cat target/logs/errors.log

# View web test logs
cat target/logs/web_tests.log

# View mobile test logs
cat target/logs/mobile_tests.log

# View API test logs
cat target/logs/api_tests.log
```

---

## 10. Running Tests

### Web (Playwright)

```bash
mvn clean test
mvn clean test -Dweb.browser=firefox
mvn clean test -Dweb.browser=webkit -Dweb.headless=false
mvn clean test -Dcucumber.filter.tags="@Web and @Smoke"
```

Cross-browser is controlled by `web.browser=chromium|firefox|webkit` in
`config.json` (or any `${env}.json`) or `-D`.

### Mobile (Appium)

1. Start Appium server: `appium`
2. Boot an emulator or connect a device
3. Run mobile-tagged scenarios:

```bash
mvn test -Dcucumber.filter.tags="@Mobile" -Dmobile.platform=android
mvn test -Dcucumber.filter.tags="@Mobile" -Dmobile.platform=ios -Dmobile.capsFile=src/test/resources/config/caps-ios.json
```

### API (REST Assured)

```bash
mvn test -Dcucumber.filter.tags="@API"
```

### With Logging Control

```bash
# All logs including debug
mvn test -Dlog.level=DEBUG

# Only errors and warnings
mvn test -Dlog.level=WARN
```

### Parallel Execution

Configured in `testng.xml` (`parallel="tests" thread-count="3"`) and the
`@DataProvider(parallel = true)` on `TestRunner`. All drivers and loggers use
`ThreadLocal` storage to remain isolated.

```bash
mvn test -Dthreads=5  # Override thread count
```

---

## 11. Reports

| Report               | Location                                   | Features                          |
|----------------------|--------------------------------------------|-----------------------------------|
| Allure (raw)         | `target/allure-results/`                   | Screenshots, stack traces         |
| Allure (rendered)    | `mvn allure:serve`                         | Interactive dashboard             |
| ExtentReports        | `target/extent-reports/extent-report.html` | Embedded images, dark theme       |
| Cucumber HTML        | `target/cucumber-reports/report.html`      | Feature overview                  |
| Cucumber JSON        | `target/cucumber-reports/report.json`      | Programmatic analysis             |
| TestNG default       | `target/surefire-reports/`                 | TestNG summary                    |
| Logs (All)           | `target/logs/automation.log`                | Complete execution logs           |
| Logs (Web)           | `target/logs/web_tests.log`                 | Web test logs only                |
| Logs (Mobile)        | `target/logs/mobile_tests.log`              | Mobile test logs only             |
| Logs (API)           | `target/logs/api_tests.log`                 | API test logs only                |
| Logs (Errors)        | `target/logs/errors.log`                    | Errors only (quick debugging)     |
| Logs (Debug)         | `target/logs/debug.log`                     | Debug level logs                  |
| Screenshots          | `target/screenshots/`                      | Organized by scenario & type      |
| Screenshot Index     | `target/screenshots/*/screenshots_index.html` | HTML index with embedded images |

---

## 12. Sharing Results

- **Email** the rendered Allure HTML or ExtentReports.
- **Jenkins / GitHub Actions**: publish `target/*` directories as build artifacts:
  - `target/cucumber-reports/**`
  - `target/allure-results/**`
  - `target/extent-reports/**`
  - `target/logs/**`
  - `target/screenshots/**`
  
- Install the *Cucumber Reports*, *Allure*, and *ExtentReports* Jenkins plugins for live dashboards.
- **Allure dashboard**: `mvn allure:serve` or host `allure generate target/allure-results`.

---

## 13. CI/CD

### GitHub Actions Workflow

```yaml
name: e2e-tests
on: [push, pull_request]
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '17' }
      - uses: actions/setup-node@v4
        with: { node-version: '20' }
      - name: Install Playwright browsers
        run: mvn -B exec:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args=install
      - name: Run tests
        run: mvn -B test -Dcucumber.filter.tags="@Web and @Regression"
      - uses: actions/upload-artifact@v4
        if: always()
        with:
          name: test-reports
          path: |
            target/cucumber-reports
            target/allure-results
            target/extent-reports
            target/surefire-reports
            target/logs
            target/screenshots
```

---

## 14. Best Practices Followed

- ✅ BDD via Cucumber with clean, business-readable Gherkin
- ✅ Page Object Model + Object Repository (single `locators.json`)
- ✅ Externalised test data (JSON / YAML / Excel)
- ✅ Thread-local Driver / Playwright Factory — parallel safe
- ✅ JSON-only config with environment overrides (`qa.json`, `uat.json`, …)
- ✅ Playwright auto-wait (no `Thread.sleep`)
- ✅ **Central logging mechanism** with context-aware logging
- ✅ **Cross-platform screenshot capture** (Playwright, Appium, Selenium)
- ✅ **Automatic screenshot embedding** in Allure and ExtentReports
- ✅ **Organized screenshot folders** by test type and scenario
- ✅ SLF4J + Log4j2 logging with multiple log files
- ✅ Cross-browser (Chromium / Firefox / WebKit) via config
- ✅ Tag-based execution (`@Web`, `@Mobile`, `@API`, `@Smoke`, `@Regression`)
- ✅ Modular separation: Web / Mobile / API
- ✅ CI/CD friendly artifacts (HTML, JSON, Allure, ExtentReports, logs, screenshots)

---

## 15. Troubleshooting

### Screenshots Not Captured
- Ensure `ScreenshotManager.initializeScenario()` is called in `@Before` hook
- Verify driver is not null before capturing
- Check `target/logs/` for error messages

### Logs Not Showing Test Context
- Verify `TestLogger.initializeScenario()` is called before logging
- Check that scenario tags include `@Web`, `@Mobile`, or `@API`
- Review `target/logs/debug.log` for context initialization

### Reports Not Generated
- **Allure**: Ensure `target/allure-results/` directory exists
- **ExtentReports**: Check `target/extent-reports/` for HTML file
- **Cucumber**: Verify `target/cucumber-reports/` is not empty
- Run `mvn clean` to remove cached reports

### Appium Screenshots Not Embedding
- Verify Appium driver is properly initialized
- Check mobile platform capabilities
- Review `target/logs/mobile_tests.log` for errors

---

## 16. Documentation & Examples

For detailed implementation examples and best practices, refer to:
- Step definitions in `src/test/java/org/example/steps/`
- Test hooks in `src/test/java/org/example/hooks/TestHooks.java`
- Listeners in `src/test/java/org/example/listener/`
- Utilities in `src/main/java/org/example/utils/`

