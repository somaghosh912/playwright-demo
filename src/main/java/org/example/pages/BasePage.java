package org.example.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.example.locators.ORManager;
import org.example.utils.UIUtils;

public abstract class BasePage {

    protected final Page page;
    protected final UIUtils ui;

    protected BasePage(Page page) {
        this.page = page;
        this.ui = new UIUtils(page);
    }

    protected Locator $(String key) {
        return page.locator(ORManager.get(key));
    }
}
