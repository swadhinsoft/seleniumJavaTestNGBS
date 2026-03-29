package com.amazon.reporter.adapter;

import com.amazon.reporter.CustomReportManager;
import com.amazon.reporter.model.TestResultModel;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * TestNG adapter for the custom reporter.
 * Hooks into TestNG's ITestListener + ISuiteListener event system.
 *
 * Register in testng.xml:
 *   <listener class-name="com.amazon.reporter.adapter.TestNGListener"/>
 *
 * The test base class must call:
 *   CustomReportManager.getInstance().registerDriver(driver)  — in @BeforeMethod
 *   CustomReportManager.getInstance().unregisterDriver()      — in @AfterMethod
 */
public class TestNGListener implements ITestListener, ISuiteListener {

    private final CustomReportManager manager = CustomReportManager.getInstance();

    // ── ITestListener ────────────────────────────────────────────────────────

    @Override
    public void onTestStart(ITestResult result) {
        // Browser: prefer TestNG parameter, fall back to registered value
        String browser = param(result, "browser");
        if (browser.isEmpty()) browser = manager.getRegisteredBrowser();

        String os    = param(result, "os");
        String suite = result.getTestContext().getName();
        String desc  = result.getMethod().getDescription();

        manager.startTest(result.getMethod().getMethodName(), desc, suite, browser, os);
        manager.getCurrentTest().addLog("Browser: " + browser);
        if (!os.isEmpty()) manager.getCurrentTest().addLog("OS: " + os);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        TestResultModel m = manager.getCurrentTest();
        if (m != null) m.addLog("Test passed successfully");
        manager.finishTest("PASSED");
    }

    @Override
    public void onTestFailure(ITestResult result) {
        TestResultModel m = manager.getCurrentTest();
        if (m == null) return;

        m.addLog("Test FAILED — " + result.getThrowable().getMessage());
        m.setErrorMessage(result.getThrowable().getMessage());
        m.setStackTrace(toStackTrace(result.getThrowable()));

        // Screenshot — driver pushed by the test base class, no BaseTest import needed
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
    public void onTestSkipped(ITestResult result) {
        TestResultModel m = manager.getCurrentTest();
        if (m != null) {
            String reason = result.getThrowable() != null
                    ? result.getThrowable().getMessage() : "No reason provided";
            m.addLog("Test skipped — " + reason);
        }
        manager.finishTest("SKIPPED");
    }

    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult r) {}
    @Override public void onStart(ITestContext ctx)  {}
    @Override public void onFinish(ITestContext ctx) {}

    // ── ISuiteListener — flush once after all parallel tests finish ──────────

    @Override public void onStart(ISuite suite)  {}

    @Override
    public void onFinish(ISuite suite) {
        manager.generateReport();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String param(ITestResult result, String name) {
        String val = result.getTestContext().getCurrentXmlTest().getParameter(name);
        return val != null ? val : "";
    }

    private String toStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
