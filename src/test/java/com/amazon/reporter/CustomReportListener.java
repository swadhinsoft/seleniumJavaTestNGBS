package com.amazon.reporter;

import com.amazon.base.BaseTest;
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

public class CustomReportListener implements ITestListener, ISuiteListener {

    private final CustomReportManager manager = CustomReportManager.getInstance();

    // ── ITestListener ────────────────────────────────────────────────────────

    @Override
    public void onTestStart(ITestResult result) {
        String browser = param(result, "browser");
        String os      = param(result, "os");
        String suite   = result.getTestContext().getName();
        String desc    = result.getMethod().getDescription();

        manager.startTest(result.getMethod().getMethodName(), desc, suite, browser, os);
        manager.getCurrentTest().addLog("Browser: " + (browser.isEmpty() ? "chrome" : browser));
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

        // Error details
        m.setErrorMessage(result.getThrowable().getMessage());
        m.setStackTrace(stackTrace(result.getThrowable()));

        // Screenshot as base64 (self-contained report)
        Object instance = result.getInstance();
        if (instance instanceof BaseTest) {
            WebDriver driver = ((BaseTest) instance).getDriver();
            if (driver != null) {
                try {
                    String base64 = "data:image/png;base64,"
                            + ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
                    m.setScreenshotBase64(base64);
                    m.addLog("Screenshot captured");
                } catch (Exception e) {
                    m.addLog("Screenshot capture failed: " + e.getMessage());
                }
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

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {}

    @Override
    public void onStart(ITestContext context) {}

    @Override
    public void onFinish(ITestContext context) {}

    // ── ISuiteListener — flush once after all parallel tests finish ──────────

    @Override
    public void onStart(ISuite suite) {}

    @Override
    public void onFinish(ISuite suite) {
        manager.generateReport();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String param(ITestResult result, String name) {
        String val = result.getTestContext().getCurrentXmlTest().getParameter(name);
        return val != null ? val : "";
    }

    private String stackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
