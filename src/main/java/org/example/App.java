package org.example;

import org.example.config.ConfigManager;

public final class App {

    public static void main(String[] args) {
        System.out.println("playwright-demo BDD framework — env=" + ConfigManager.env());
    }
}
