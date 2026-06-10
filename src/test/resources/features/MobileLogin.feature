@Mobile @Regression
Feature: Mobile login

  Scenario: Existing user can log in on mobile
    When user logs into the mobile app with "default"
    Then the home screen is displayed
