package org.example.steps;

import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.example.driver.PlaywrightFactory;
import org.example.pages.CartPage;
import org.example.pages.InventoryPage;
import org.example.pages.LoginPage;
import org.example.utils.DataReader;
import org.testng.Assert;

public class WebSteps {

    private LoginPage loginPage;
    private InventoryPage inventoryPage;
    private CartPage cartPage;

    @Given("user opens the application")
    public void openApp() {
        loginPage = new LoginPage(PlaywrightFactory.page()).open();
    }

    @When("user logs in with credentials from test data {string}")
    public void login(String userKey) {
        String username = DataReader.jsonString("users", userKey, "username");
        String password = DataReader.jsonString("users", userKey, "password");
        inventoryPage = loginPage.login(username, password);
    }

    @And("user searches for the {string} product")
    public void searchProduct(String productKey) {
        String productName = DataReader.jsonString("products", productKey, "name");
        inventoryPage.searchProduct(productName);
    }

    @And("user adds the {string} product to cart")
    public void addProductToCart(String productKey) {
        String productName = DataReader.jsonString("products", productKey, "name");
        inventoryPage.addToCart(productName);
    }

    @Then("user should see the {string} product in the cart")
    public void verifyProductInCart(String productKey) {
        String productName = DataReader.jsonString("products", productKey, "name");
        cartPage = inventoryPage.openCart();
        Assert.assertTrue(cartPage.containsProduct(productName),
                "Expected product '" + productName + "' to be in the cart");
    }
}
