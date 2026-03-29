package com.amazon.utils;

import com.amazon.base.BaseTest;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import org.testng.ISuite;
import org.testng.ISuiteListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class ExtentTestListener implements ITestListener, ISuiteListener {

    // ── Test lifecycle ───────────────────────────────────────────────────────

    @Override
    public void onTestStart(ITestResult result) {
        String description = result.getMethod().getDescription();
        String name = (description != null && !description.isEmpty())
                ? description
                : result.getMethod().getMethodName();

        ExtentTest test = ExtentManager.getInstance()
                .createTest(name, result.getTestClass().getName());

        test.assignCategory(result.getTestContext().getName());  // browser/test group label
        ExtentManager.setTest(test);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        if (test != null) {
            test.log(Status.PASS, "Test passed");
        }
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        if (test == null) return;

        test.log(Status.FAIL, result.getThrowable());

        // Attach screenshot
        Object instance = result.getInstance();
        if (instance instanceof BaseTest) {
            String screenshotPath = ScreenshotUtil.capture(
                    ((BaseTest) instance).getDriver(),
                    result.getName()
            );
            if (screenshotPath != null) {
                test.addScreenCaptureFromPath(screenshotPath, "Failure Screenshot");
            }
        }
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest test = ExtentManager.getTest();
        if (test != null) {
            test.log(Status.SKIP, result.getThrowable() != null
                    ? result.getThrowable().getMessage()
                    : "Test skipped");
        }
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) { }

    @Override
    public void onStart(ITestContext context) { }

    @Override
    public void onFinish(ITestContext context) {
        ExtentManager.removeTest();
    }

    // ── Suite lifecycle — flush once after all parallel tests finish ─────────

    @Override
    public void onStart(ISuite suite) { }

    @Override
    public void onFinish(ISuite suite) {
        ExtentManager.getInstance().flush();
    }
}
