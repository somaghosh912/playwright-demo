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
       +-------------+        | TestNG default) |
                              +-----------------+
```

Key patterns: **POM**, **Object Repository**, **Driver Factory (thread-local)**,
**Singleton Playwright per thread**, **Externalised Test Data**, **Tagged execution**.

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
    │   │   └── utils/            JsonUtil, YamlUtil, ExcelUtil, DataReader
    │   └── resources/
    │       └── log4j2.xml
    └── test/
        ├── java/org/example/
        │   ├── hooks/            Cucumber @Before/@After
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

## 7. Running Tests

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

### Parallel execution

Configured in `testng.xml` (`parallel="tests" thread-count="3"`) and the
`@DataProvider(parallel = true)` on `TestRunner`. All drivers use
`ThreadLocal` storage to remain isolated.

---

## 8. Reports

| Report           | Location                                       |
|------------------|------------------------------------------------|
| Cucumber HTML    | `target/cucumber-reports/report.html`          |
| Cucumber JSON    | `target/cucumber-reports/report.json`          |
| Allure (raw)     | `target/allure-results/`                       |
| Allure (rendered)| `mvn allure:serve`                             |
| TestNG default   | `target/surefire-reports/`                     |
| Logs             | `target/logs/automation.log`                   |

---

## 9. Sharing Results

- **Email** the rendered Cucumber HTML.
- **Jenkins / GitHub Actions**: publish `target/cucumber-reports/**` and
  `target/allure-results/**` as build artifacts; install the *Cucumber Reports*
  and *Allure* Jenkins plugins for live dashboards.
- **Allure dashboard**: `mvn allure:serve` or host `allure generate target/allure-results`.

---

## 10. CI/CD

Minimal GitHub Actions workflow:

```yaml
name: e2e
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
          name: reports
          path: |
            target/cucumber-reports
            target/allure-results
            target/surefire-reports
```

---

## 11. Best Practices Followed

- BDD via Cucumber with clean, business-readable Gherkin
- Page Object Model + Object Repository (single `locators.json`)
- Externalised test data (JSON / YAML / Excel)
- Thread-local Driver / Playwright Factory — parallel safe
- JSON-only config with environment overrides (`qa.json`, `uat.json`, …)
- Playwright auto-wait (no `Thread.sleep`)
- SLF4J + Log4j2 logging
- Cross-browser (Chromium / Firefox / WebKit) via config
- Tag-based execution (`@Web`, `@Mobile`, `@API`, `@Smoke`, `@Regression`)
- Modular separation: Web / Mobile / API
- CI/CD friendly artifacts (HTML, JSON, Allure, surefire)
