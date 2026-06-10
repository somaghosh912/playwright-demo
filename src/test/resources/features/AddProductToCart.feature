@Web @Regression
Feature: Add product to cart end-to-end

  Background:
    Given user opens the application

  @Smoke
  Scenario Outline: Logged-in user can add a product to the cart
    When user logs in with credentials from test data "<userKey>"
    And user searches for the "<productKey>" product
    And user adds the "<productKey>" product to cart
    Then user should see the "<productKey>" product in the cart

    Examples:
      | userKey | productKey |
      | default | backpack   |
      | default | bikeLight  |
