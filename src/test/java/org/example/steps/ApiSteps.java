package org.example.steps;

import io.cucumber.java.PendingException;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.example.api.ApiClient;
import org.example.config.ConfigManager;
import org.example.utils.DataReader;
import org.testng.Assert;

import java.util.HashMap;
import java.util.Map;

public class ApiSteps {

    private final Map<String, Object> queryParams = new HashMap<>();
    private Response response;

    @Given("user prepares a GET request with parameter {string} as {string}")
    public void user_prepares_a_get_request_with_parameter(String key, String value) {
        queryParams.put(key, value);
    }

    @When("user sends the GET request")
    public void user_sends_the_get_request() {
        response = ApiClient.get(
                ConfigManager.apiBaseUrl(),
                queryParams
        );
    }

    @Then("the response status code should be {int}")
    public void the_response_status_code_should_be(int expectedStatusCode) {
        Assert.assertEquals(
                response.getStatusCode(),
                expectedStatusCode,
                "Status code validation failed"
        );
    }

    @Then("the response contains {string} as {string}")
    public void the_response_contains_as(String key, String expectedValue) {
        System.out.println(response.getStatusCode());
        System.out.println(response.getBody().asString());
        String actualValue = response.jsonPath()
                .getString("args." + key);

        Assert.assertEquals(
                actualValue,
                expectedValue,
                "Response value validation failed"
        );
    }
}
