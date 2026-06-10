package org.example.pages;

import com.microsoft.playwright.Page;

public class CartPage extends BasePage {

    public CartPage(Page page) {
        super(page);
    }

    public boolean containsProduct(String productName) {
        return $("cart.itemName").filter(
                new com.microsoft.playwright.Locator.FilterOptions().setHasText(productName)
        ).count() > 0;
    }
}
