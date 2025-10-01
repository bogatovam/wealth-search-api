package com.wealthsearch.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.wealthsearch")
public class WealthSearchEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(WealthSearchEngineApplication.class, args);
    }
}
