package com.amazon.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;

public class ConfigReader {

    private static final JsonNode config;

    static {
        try {
            InputStream is = ConfigReader.class.getClassLoader().getResourceAsStream("config.json");
            if (is == null) {
                throw new RuntimeException("config.json not found in resources");
            }
            config = new ObjectMapper().readTree(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config.json: " + e.getMessage(), e);
        }
    }

    public static String getBaseUrl() {
        return config.get("baseUrl").asText();
    }

    public static String getUsername() {
        return config.get("credentials").get("username").asText();
    }

    public static String getPassword() {
        return config.get("credentials").get("password").asText();
    }

    public static int getExplicitWait() {
        return config.get("timeouts").get("explicitWait").asInt();
    }

    public static int getPageLoad() {
        return config.get("timeouts").get("pageLoad").asInt();
    }

    public static String getReportTitle() {
        return config.get("report").get("title").asText();
    }

    public static String getReportName() {
        return config.get("report").get("name").asText();
    }

    public static String getLogoPath() {
        JsonNode node = config.path("report").path("logo");
        return node.isMissingNode() ? "" : node.asText("");
    }
}
