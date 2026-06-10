package org.example;

import org.example.config.ConfigManager;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AppTest {

    @Test
    public void configLoads() {
        Assert.assertNotNull(ConfigManager.env(), "env not loaded from config");
    }
}
