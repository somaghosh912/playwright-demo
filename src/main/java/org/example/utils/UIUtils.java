package org.example.utils;

import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UIUtils {

    private static final Logger LOG = LoggerFactory.getLogger(UIUtils.class);

    private final Page page;

    public UIUtils(Page page) {
        this.page = page;
    }

    // Scroll operations
    public void scrollToElement(Locator locator) {
        try {
            locator.scrollIntoViewIfNeeded();
            LOG.info("Scrolled to element: {}", locator);
        } catch (Exception e) {
            LOG.warn("Failed to scroll to element: {}", e.getMessage());
        }
    }

    public void scrollToElement(String selector) {
        try {
            page.locator(selector).scrollIntoViewIfNeeded();
            LOG.info("Scrolled to element: {}", selector);
        } catch (Exception e) {
            LOG.warn("Failed to scroll to element {}: {}", selector, e.getMessage());
        }
    }

    public void scrollToTop() {
        try {
            page.evaluate("window.scrollTo(0, 0);");
            LOG.info("Scrolled to top of page");
        } catch (Exception e) {
            LOG.warn("Failed to scroll to top: {}", e.getMessage());
        }
    }

    public void scrollToBottom() {
        try {
            page.evaluate("window.scrollTo(0, document.body.scrollHeight);");
            LOG.info("Scrolled to bottom of page");
        } catch (Exception e) {
            LOG.warn("Failed to scroll to bottom: {}", e.getMessage());
        }
    }

    public void scrollByPixels(int pixels) {
        try {
            page.evaluate("window.scrollBy(0, " + pixels + ");");
            LOG.info("Scrolled by {} pixels", pixels);
        } catch (Exception e) {
            LOG.warn("Failed to scroll by {} pixels: {}", pixels, e.getMessage());
        }
    }

    public void scrollHorizontally(int pixels) {
        try {
            page.evaluate("window.scrollBy(" + pixels + ", 0);");
            LOG.info("Scrolled horizontally by {} pixels", pixels);
        } catch (Exception e) {
            LOG.warn("Failed to scroll horizontally: {}", e.getMessage());
        }
    }

    // Frame operations
    public Frame getFrame(String frameSelector) {
        try {
            Locator frameLocator = page.locator(frameSelector);
            Frame frame = (Frame) frameLocator.frameLocator("internal").owner().contentFrame();
            LOG.info("Got frame: {}", frameSelector);
            return frame;
        } catch (Exception e) {
            LOG.warn("Failed to get frame {}: {}", frameSelector, e.getMessage());
            return null;
        }
    }

    public Frame getFrameByName(String frameName) {
        try {
            Frame frame = (Frame) page.frameLocator("iframe[name='" + frameName + "']").owner().contentFrame();
            LOG.info("Got frame by name: {}", frameName);
            return frame;
        } catch (Exception e) {
            LOG.warn("Failed to get frame by name {}: {}", frameName, e.getMessage());
            return null;
        }
    }

    public Frame getFrameByIndex(int index) {
        try {
            java.util.List<Frame> frames = page.frames();
            if (index < frames.size()) {
                LOG.info("Got frame at index: {}", index);
                return frames.get(index);
            }
            LOG.warn("Frame index {} out of bounds", index);
            return null;
        } catch (Exception e) {
            LOG.warn("Failed to get frame by index {}: {}", index, e.getMessage());
            return null;
        }
    }

    // Wait operations
    public void waitForElement(String selector) {
        try {
            page.locator(selector).waitFor();
            LOG.info("Element appeared: {}", selector);
        } catch (Exception e) {
            LOG.warn("Timeout waiting for element {}: {}", selector, e.getMessage());
        }
    }

    public void waitForElement(Locator locator) {
        try {
            locator.waitFor();
            LOG.info("Element appeared");
        } catch (Exception e) {
            LOG.warn("Timeout waiting for element: {}", e.getMessage());
        }
    }

    public void waitForElementToHide(String selector) {
        try {
            page.locator(selector).waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
            LOG.info("Element hidden: {}", selector);
        } catch (Exception e) {
            LOG.warn("Timeout waiting for element to hide {}: {}", selector, e.getMessage());
        }
    }

    public void waitForNavigation() {
        try {
            page.waitForNavigation(this::waitForNavigation);
            LOG.info("Navigation completed");
        } catch (Exception e) {
            LOG.warn("Timeout waiting for navigation: {}", e.getMessage());
        }
    }

    public void waitForLoadState() {
        try {
            page.waitForLoadState();
            LOG.info("Page load state: load");
        } catch (Exception e) {
            LOG.warn("Timeout waiting for load state: {}", e.getMessage());
        }
    }

    // Visibility and interaction checks
    public boolean isElementVisible(String selector) {
        try {
            return page.locator(selector).isVisible();
        } catch (Exception e) {
            LOG.warn("Failed to check visibility of {}: {}", selector, e.getMessage());
            return false;
        }
    }

    public boolean isElementVisible(Locator locator) {
        try {
            return locator.isVisible();
        } catch (Exception e) {
            LOG.warn("Failed to check element visibility: {}", e.getMessage());
            return false;
        }
    }

    public boolean isElementEnabled(String selector) {
        try {
            return page.locator(selector).isEnabled();
        } catch (Exception e) {
            LOG.warn("Failed to check if element {} is enabled: {}", selector, e.getMessage());
            return false;
        }
    }

    public boolean isElementEnabled(Locator locator) {
        try {
            return locator.isEnabled();
        } catch (Exception e) {
            LOG.warn("Failed to check if element is enabled: {}", e.getMessage());
            return false;
        }
    }

    public boolean isElementChecked(String selector) {
        try {
            return page.locator(selector).isChecked();
        } catch (Exception e) {
            LOG.warn("Failed to check if element {} is checked: {}", selector, e.getMessage());
            return false;
        }
    }

    // Click operations
    public void click(String selector) {
        try {
            page.locator(selector).click();
            LOG.info("Clicked on element: {}", selector);
        } catch (Exception e) {
            LOG.warn("Failed to click on {}: {}", selector, e.getMessage());
        }
    }

    public void click(Locator locator) {
        try {
            locator.click();
            LOG.info("Clicked on element");
        } catch (Exception e) {
            LOG.warn("Failed to click on element: {}", e.getMessage());
        }
    }

    public void doubleClick(String selector) {
        try {
            page.locator(selector).dblclick();
            LOG.info("Double clicked on element: {}", selector);
        } catch (Exception e) {
            LOG.warn("Failed to double click on {}: {}", selector, e.getMessage());
        }
    }

    public void rightClick(String selector) {
        try {
            page.locator(selector).click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
            LOG.info("Right clicked on element: {}", selector);
        } catch (Exception e) {
            LOG.warn("Failed to right click on {}: {}", selector, e.getMessage());
        }
    }

    // Text and input operations
    public void fill(String selector, String text) {
        try {
            page.locator(selector).fill(text);
            LOG.info("Filled {} with text: {}", selector, text);
        } catch (Exception e) {
            LOG.warn("Failed to fill {} with text: {}", selector, e.getMessage());
        }
    }

    public void fill(Locator locator, String text) {
        try {
            locator.fill(text);
            LOG.info("Filled element with text");
        } catch (Exception e) {
            LOG.warn("Failed to fill element with text: {}", e.getMessage());
        }
    }

    public void clearAndFill(String selector, String text) {
        try {
            page.locator(selector).clear();
            page.locator(selector).fill(text);
            LOG.info("Cleared and filled {} with text: {}", selector, text);
        } catch (Exception e) {
            LOG.warn("Failed to clear and fill {}: {}", selector, e.getMessage());
        }
    }

    public String getText(String selector) {
        try {
            return page.locator(selector).textContent();
        } catch (Exception e) {
            LOG.warn("Failed to get text from {}: {}", selector, e.getMessage());
            return null;
        }
    }

    public String getText(Locator locator) {
        try {
            return locator.textContent();
        } catch (Exception e) {
            LOG.warn("Failed to get text from element: {}", e.getMessage());
            return null;
        }
    }

    public String getAttribute(String selector, String attributeName) {
        try {
            return page.locator(selector).getAttribute(attributeName);
        } catch (Exception e) {
            LOG.warn("Failed to get attribute {} from {}: {}", attributeName, selector, e.getMessage());
            return null;
        }
    }

    // Hover operations
    public void hover(String selector) {
        try {
            page.locator(selector).hover();
            LOG.info("Hovered on element: {}", selector);
        } catch (Exception e) {
            LOG.warn("Failed to hover on {}: {}", selector, e.getMessage());
        }
    }

    public void hover(Locator locator) {
        try {
            locator.hover();
            LOG.info("Hovered on element");
        } catch (Exception e) {
            LOG.warn("Failed to hover on element: {}", e.getMessage());
        }
    }

    // Element existence checks
    public boolean elementExists(String selector) {
        try {
            return page.locator(selector).count() > 0;
        } catch (Exception e) {
            LOG.warn("Failed to check if element {} exists: {}", selector, e.getMessage());
            return false;
        }
    }

    public int getElementCount(String selector) {
        try {
            return page.locator(selector).count();
        } catch (Exception e) {
            LOG.warn("Failed to get count of element {}: {}", selector, e.getMessage());
            return 0;
        }
    }

    // Select operations
    public void selectByValue(String selector, String value) {
        try {
            page.locator(selector).selectOption(value);
            LOG.info("Selected value {} in {}", value, selector);
        } catch (Exception e) {
            LOG.warn("Failed to select value {} in {}: {}", value, selector, e.getMessage());
        }
    }

    public void selectByLabel(String selector, String label) {
        try {
            page.locator(selector).selectOption(new com.microsoft.playwright.options.SelectOption().setLabel(label));
            LOG.info("Selected label {} in {}", label, selector);
        } catch (Exception e) {
            LOG.warn("Failed to select label {} in {}: {}", label, selector, e.getMessage());
        }
    }

    // Drag and drop
    public void dragAndDrop(String sourceSelector, String targetSelector) {
        try {
            page.locator(sourceSelector).dragTo(page.locator(targetSelector));
            LOG.info("Dragged {} to {}", sourceSelector, targetSelector);
        } catch (Exception e) {
            LOG.warn("Failed to drag and drop: {}", e.getMessage());
        }
    }

    // JavaScript execution
    public Object executeScript(String script) {
        try {
            return page.evaluate(script);
        } catch (Exception e) {
            LOG.warn("Failed to execute script: {}", e.getMessage());
            return null;
        }
    }

    public Object executeScript(String script, Object argument) {
        try {
            return page.evaluate(script, argument);
        } catch (Exception e) {
            LOG.warn("Failed to execute script with argument: {}", e.getMessage());
            return null;
        }
    }

    // Keyboard operations
    public void press(String key) {
        try {
            page.keyboard().press(key);
            LOG.info("Pressed key: {}", key);
        } catch (Exception e) {
            LOG.warn("Failed to press key {}: {}", key, e.getMessage());
        }
    }

    public void type(String text) {
        try {
            page.keyboard().type(text);
            LOG.info("Typed text: {}", text);
        } catch (Exception e) {
            LOG.warn("Failed to type text: {}", e.getMessage());
        }
    }

    // Refresh and navigation
    public void refresh() {
        try {
            page.reload();
            LOG.info("Page refreshed");
        } catch (Exception e) {
            LOG.warn("Failed to refresh page: {}", e.getMessage());
        }
    }

    public void goBack() {
        try {
            page.goBack();
            LOG.info("Navigated back");
        } catch (Exception e) {
            LOG.warn("Failed to go back: {}", e.getMessage());
        }
    }

    public void goForward() {
        try {
            page.goForward();
            LOG.info("Navigated forward");
        } catch (Exception e) {
            LOG.warn("Failed to go forward: {}", e.getMessage());
        }
    }

    // Alert/Dialog handling
    public void acceptAlert() {
        try {
            page.onDialog(dialog -> {
                dialog.accept();
                LOG.info("Alert accepted");
            });
        } catch (Exception e) {
            LOG.warn("Failed to accept alert: {}", e.getMessage());
        }
    }

    public void dismissAlert() {
        try {
            page.onDialog(dialog -> {
                dialog.dismiss();
                LOG.info("Alert dismissed");
            });
        } catch (Exception e) {
            LOG.warn("Failed to dismiss alert: {}", e.getMessage());
        }
    }

    // Visibility operations
    public void show(String selector) {
        try {
            page.evaluate("document.querySelector('" + selector + "').style.display = 'block';");
            LOG.info("Made element visible: {}", selector);
        } catch (Exception e) {
            LOG.warn("Failed to show element {}: {}", selector, e.getMessage());
        }
    }

    public void hide(String selector) {
        try {
            page.evaluate("document.querySelector('" + selector + "').style.display = 'none';");
            LOG.info("Hidden element: {}", selector);
        } catch (Exception e) {
            LOG.warn("Failed to hide element {}: {}", selector, e.getMessage());
        }
    }

    // Focus operations
    public void focus(String selector) {
        try {
            page.locator(selector).focus();
            LOG.info("Focused on element: {}", selector);
        } catch (Exception e) {
            LOG.warn("Failed to focus on {}: {}", selector, e.getMessage());
        }
    }

    public void blur(String selector) {
        try {
            page.evaluate("document.querySelector('" + selector + "').blur();");
            LOG.info("Blurred element: {}", selector);
        } catch (Exception e) {
            LOG.warn("Failed to blur element {}: {}", selector, e.getMessage());
        }
    }
}
