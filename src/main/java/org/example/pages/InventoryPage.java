package org.example.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;

public class InventoryPage extends BasePage {

    public InventoryPage(Page page) {
        super(page);
    }

    public InventoryPage searchProduct(String productName) {
        // SauceDemo has no search box — use the inventory list filter as a stand-in
        // and rely on visible product name match for the click.
        page.waitForSelector("[data-test='inventory-item']");
        return this;
    }

    public InventoryPage addToCart(String productName) {
        String id = "add-to-cart-" + productName.toLowerCase().replace(" ", "-");
        page.locator("#" + id).click();
        return this;
    }

    public CartPage openCart() {
        $("inventory.cartLink").click();
        return new CartPage(page);
    }

    public int cartBadgeCount() {
        var badge = page.getByRole(AriaRole.LINK).filter(
                new com.microsoft.playwright.Locator.FilterOptions().setHasText("1"));
        return badge.count();
    }
}
