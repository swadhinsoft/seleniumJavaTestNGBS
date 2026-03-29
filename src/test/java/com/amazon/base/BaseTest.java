package com.amazon.base;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import java.net.URL;
import java.util.HashMap;

public class BaseTest {

    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    protected static final String BASE_URL = "https://www.amazon.in";
    private static final String BS_HUB = "https://hub-cloud.browserstack.com/wd/hub";

    protected WebDriver getDriver() {
        return driverThreadLocal.get();
    }

    @Parameters({"browser", "browser_version", "os", "os_version", "execution"})
    @BeforeMethod
    public void setUp(
            @Optional("chrome")      String browser,
            @Optional("latest")      String browserVersion,
            @Optional("")            String os,
            @Optional("")            String osVersion,
            @Optional("local")       String execution
    ) throws Exception {
        WebDriver driver;
        if ("browserstack".equalsIgnoreCase(execution)) {
            driver = createBrowserStackDriver(browser, browserVersion, os, osVersion);
        } else {
            driver = createLocalDriver(browser);
        }
        driverThreadLocal.set(driver);
        driver.get(BASE_URL);
    }

    @AfterMethod
    public void tearDown() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            driver.quit();
            driverThreadLocal.remove();
        }
    }

    // ── Local driver factory ────────────────────────────────────────────────

    private WebDriver createLocalDriver(String browser) {
        switch (browser.toLowerCase()) {
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                firefoxOptions.addArguments("--start-maximized");
                return new FirefoxDriver(firefoxOptions);
            case "edge":
                WebDriverManager.edgedriver().setup();
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments("--start-maximized");
                edgeOptions.addArguments("--disable-notifications");
                return new EdgeDriver(edgeOptions);
            default: // chrome
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("--start-maximized");
                chromeOptions.addArguments("--disable-notifications");
                return new ChromeDriver(chromeOptions);
        }
    }

    // ── BrowserStack remote driver factory ─────────────────────────────────

    private WebDriver createBrowserStackDriver(
            String browser, String browserVersion, String os, String osVersion
    ) throws Exception {
        String username  = System.getenv("BROWSERSTACK_USERNAME");
        String accessKey = System.getenv("BROWSERSTACK_ACCESS_KEY");
        if (username == null || accessKey == null) {
            throw new IllegalStateException(
                "BROWSERSTACK_USERNAME and BROWSERSTACK_ACCESS_KEY environment variables must be set.");
        }

        HashMap<String, Object> bstackOptions = new HashMap<>();
        bstackOptions.put("userName", username);
        bstackOptions.put("accessKey", accessKey);
        bstackOptions.put("projectName", "Amazon.in Tests");
        bstackOptions.put("buildName", "Build-" + System.getenv().getOrDefault("BUILD_ID", "local"));
        bstackOptions.put("sessionName", browser + " " + browserVersion + " on " + os + " " + osVersion);
        if (!os.isEmpty())        bstackOptions.put("os", os);
        if (!osVersion.isEmpty()) bstackOptions.put("osVersion", osVersion);
        bstackOptions.put("browserVersion", browserVersion);

        MutableCapabilities caps;
        switch (browser.toLowerCase()) {
            case "firefox":
                FirefoxOptions ffOpts = new FirefoxOptions();
                ffOpts.setCapability("bstack:options", bstackOptions);
                caps = ffOpts;
                break;
            case "safari":
                SafariOptions safariOpts = new SafariOptions();
                safariOpts.setCapability("bstack:options", bstackOptions);
                caps = safariOpts;
                break;
            case "edge":
                EdgeOptions edgeOpts = new EdgeOptions();
                edgeOpts.setCapability("bstack:options", bstackOptions);
                caps = edgeOpts;
                break;
            default: // chrome
                ChromeOptions chromeOpts = new ChromeOptions();
                chromeOpts.setCapability("bstack:options", bstackOptions);
                caps = chromeOpts;
                break;
        }

        return new RemoteWebDriver(new URL(BS_HUB), caps);
    }
}
