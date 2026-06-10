package org.example.pages;

import com.microsoft.playwright.Page;
import org.example.config.ConfigManager;

public class LoginPage extends BasePage {

    public LoginPage(Page page) {
        super(page);
    }

    public LoginPage open() {
        page.navigate(ConfigManager.webBaseUrl());
        return this;
    }

    public InventoryPage login(String username, String password) {
        $("login.username").fill(username);
        $("login.password").fill(password);
        $("login.submit").click();
        return new InventoryPage(page);
    }
}
