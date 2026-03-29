package com.amazon.reporter.adapter;

import com.amazon.reporter.CustomReportManager;
import com.amazon.reporter.model.TestResultModel;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/**
 * JUnit 5 adapter for the custom reporter.
 * Hooks into JUnit 5's extension API.
 *
 * Usage — annotate your test class:
 *   @ExtendWith(JUnit5Extension.class)
 *   public class MyTest { ... }
 *
 * In @BeforeEach, call:
 *   CustomReportManager.getInstance().registerDriver(driver)
 *   CustomReportManager.getInstance().setBrowser("chrome")
 *
 * In @AfterEach, call:
 *   CustomReportManager.getInstance().unregisterDriver()
 *
 * Works with Selenium WebDriver and Appium AppiumDriver.
 */
public class JUnit5Extension
        implements BeforeEachCallback, AfterEachCallback, TestWatcher, AfterAllCallback {

    private final CustomReportManager manager = CustomReportManager.getInstance();

    @Override
    public void beforeEach(ExtensionContext ctx) {
        String methodName = ctx.getRequiredTestMethod().getName();
        String description = ctx.getDisplayName();
        String suiteName   = ctx.getRequiredTestClass().getSimpleName();
        String browser     = manager.getRegisteredBrowser();

        manager.startTest(methodName, description, suiteName, browser, "");
        manager.getCurrentTest().addLog("JUnit 5 test started");
        manager.getCurrentTest().addLog("Browser: " + browser);
    }

    @Override
    public void afterEach(ExtensionContext ctx) {
        // finishTest is called by TestWatcher methods below;
        // this hook is used only if no watcher result fires (shouldn't happen normally)
    }

    // ── TestWatcher ──────────────────────────────────────────────────────────

    @Override
    public void testSuccessful(ExtensionContext ctx) {
        TestResultModel m = manager.getCurrentTest();
        if (m != null) m.addLog("Test passed successfully");
        manager.finishTest("PASSED");
    }

    @Override
    public void testFailed(ExtensionContext ctx, Throwable cause) {
        TestResultModel m = manager.getCurrentTest();
        if (m == null) return;

        m.addLog("Test FAILED — " + cause.getMessage());
        m.setErrorMessage(cause.getMessage());
        m.setStackTrace(toStackTrace(cause));

        WebDriver driver = manager.getRegisteredDriver();
        if (driver != null) {
            try {
                m.setScreenshotBase64("data:image/png;base64,"
                        + ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64));
                m.addLog("Screenshot captured");
            } catch (Exception e) {
                m.addLog("Screenshot capture failed: " + e.getMessage());
            }
        }

        manager.finishTest("FAILED");
    }

    @Override
    public void testAborted(ExtensionContext ctx, Throwable cause) {
        TestResultModel m = manager.getCurrentTest();
        if (m != null) m.addLog("Test aborted — " + (cause != null ? cause.getMessage() : ""));
        manager.finishTest("SKIPPED");
    }

    @Override
    public void testDisabled(ExtensionContext ctx, Optional<String> reason) {
        // startTest may not have been called if the test was disabled before beforeEach
        String methodName = ctx.getTestMethod().map(java.lang.reflect.Method::getName).orElse("unknown");
        String suiteName  = ctx.getTestClass().map(Class::getSimpleName).orElse("unknown");
        manager.startTest(methodName, ctx.getDisplayName(), suiteName,
                manager.getRegisteredBrowser(), "");
        TestResultModel m = manager.getCurrentTest();
        if (m != null) m.addLog("Test disabled — " + reason.orElse("no reason"));
        manager.finishTest("SKIPPED");
    }

    // ── AfterAllCallback — flush report after the test class completes ────────

    @Override
    public void afterAll(ExtensionContext ctx) {
        manager.generateReport();
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private String toStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
