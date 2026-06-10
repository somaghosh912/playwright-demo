package org.example.steps;

import io.appium.java_client.AppiumBy;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.example.driver.AppiumDriverFactory;
import org.example.utils.DataReader;
import org.testng.Assert;

public class MobileSteps {

    @When("user logs into the mobile app with {string}")
    public void loginMobile(String userKey) {
        String username = DataReader.jsonString("users", userKey, "username");
        String password = DataReader.jsonString("users", userKey, "password");
        AppiumDriverFactory.driver().findElement(AppiumBy.accessibilityId("username")).sendKeys(username);
        AppiumDriverFactory.driver().findElement(AppiumBy.accessibilityId("password")).sendKeys(password);
        AppiumDriverFactory.driver().findElement(AppiumBy.accessibilityId("loginBtn")).click();
    }

    @Then("the home screen is displayed")
    public void verifyHome() {
        boolean displayed = AppiumDriverFactory.driver()
                .findElement(AppiumBy.accessibilityId("homeTitle")).isDisplayed();
        Assert.assertTrue(displayed, "Home screen not displayed");
    }
}
