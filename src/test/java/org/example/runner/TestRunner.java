package org.example.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.example.listener.CucumberTestListener;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;

@Listeners({CucumberTestListener.class})
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {"org.example.steps", "org.example.hooks"},
        plugin = {
                "pretty",
                "html:target/cucumber-reports/report.html",
                "json:target/cucumber-reports/report.json",
                "io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm"
        },
        monochrome = true,
        publish = false,
        tags = "@Web"
)
public class TestRunner extends AbstractTestNGCucumberTests {

    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
