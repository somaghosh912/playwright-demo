package org.example.driver;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.example.config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

public final class PlaywrightFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PlaywrightFactory.class);

    private static final ThreadLocal<Playwright> PLAYWRIGHT = new ThreadLocal<>();
    private static final ThreadLocal<Browser> BROWSER = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Page> PAGE = new ThreadLocal<>();
    private static final ThreadLocal<String> VIDEO_PATH = new ThreadLocal<>();

    private PlaywrightFactory() {}

    public static Page startBrowser() {
        Playwright pw = Playwright.create();
        PLAYWRIGHT.set(pw);

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(ConfigManager.headless());
        Browser browser = switch (ConfigManager.browser().toLowerCase()) {
            case "firefox" -> pw.firefox().launch(options);
            case "webkit"  -> pw.webkit().launch(options);
            default        -> pw.chromium().launch(options);
        };
        BROWSER.set(browser);

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setRecordVideoDir(Paths.get("target/videos"));
        BrowserContext context = browser.newContext(contextOptions);
        context.setDefaultTimeout(ConfigManager.playwrightTimeout());
        CONTEXT.set(context);

        Page page = context.newPage();
        PAGE.set(page);
        LOG.info("Started Playwright browser={} headless={} with video recording", ConfigManager.browser(), ConfigManager.headless());
        return page;
    }

    public static Page page() {
        return PAGE.get();
    }

    public static String getVideoPath() {
        Page page = PAGE.get();

        if (page != null) {
            try {
                return page.video().path().toString();
            } catch (Exception e) {
                LOG.warn("Could not get video path: {}", e.getMessage());
            }
        }
        return null;
    }

    public static void quit() {
        try {
            if (PAGE.get() != null)       PAGE.get().close();
            if (CONTEXT.get() != null)    CONTEXT.get().close();
            if (BROWSER.get() != null)    BROWSER.get().close();
            if (PLAYWRIGHT.get() != null) PLAYWRIGHT.get().close();
        } catch (Exception e) {
            LOG.warn("Error while closing Playwright resources: {}", e.getMessage());
        } finally {
            PAGE.remove();
            CONTEXT.remove();
            BROWSER.remove();
            PLAYWRIGHT.remove();
            VIDEO_PATH.remove();
        }
    }
}
