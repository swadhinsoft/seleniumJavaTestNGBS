package com.amazon.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ScreenshotUtil {

    private static final String SCREENSHOT_DIR = "target/extent-reports/screenshots/";

    /**
     * Captures a screenshot and saves it to the screenshots folder.
     *
     * @return path relative to the Extent report HTML file, or null on failure
     */
    public static String capture(WebDriver driver, String testName) {
        try {
            String fileName = sanitize(testName) + "_" + System.currentTimeMillis() + ".png";
            File dest = new File(SCREENSHOT_DIR + fileName);
            dest.getParentFile().mkdirs();

            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);

            // relative to target/extent-reports/index.html
            return "screenshots/" + fileName;
        } catch (IOException e) {
            System.err.println("Screenshot capture failed for [" + testName + "]: " + e.getMessage());
            return null;
        }
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
