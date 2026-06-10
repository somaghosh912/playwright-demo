@API @Regression
Feature: GET API Testing

  Scenario: Verify GET request returns query parameter
    Given user prepares a GET request with parameter "name" as "Soma"
    When user sends the GET request
    Then the response status code should be 200
    And the response contains "name" as "Soma"


