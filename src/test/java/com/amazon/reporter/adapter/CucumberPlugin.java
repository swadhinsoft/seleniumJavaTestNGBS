package com.amazon.reporter.adapter;

import com.amazon.reporter.CustomReportManager;
import com.amazon.reporter.model.TestResultModel;
import io.cucumber.plugin.EventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.TestCaseFinished;
import io.cucumber.plugin.event.TestCaseStarted;
import io.cucumber.plugin.event.TestRunFinished;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Cucumber 7+ adapter for the custom reporter.
 * Implements io.cucumber.plugin.EventListener.
 *
 * Usage — add to @CucumberOptions:
 *   @CucumberOptions(plugin = {"com.amazon.reporter.adapter.CucumberPlugin"})
 *
 * In your Cucumber @Before hook, call:
 *   CustomReportManager.getInstance().registerDriver(driver)
 *   CustomReportManager.getInstance().setBrowser("chrome")
 *
 * In your Cucumber @After hook, call:
 *   CustomReportManager.getInstance().unregisterDriver()
 *
 * Works with Selenium WebDriver and Appium AppiumDriver.
 */
public class CucumberPlugin implements EventListener {

    private final CustomReportManager manager = CustomReportManager.getInstance();

    @Override
    public void setEventPublisher(EventPublisher publisher) {
        publisher.registerHandlerFor(TestCaseStarted.class,  this::onTestCaseStarted);
        publisher.registerHandlerFor(TestCaseFinished.class, this::onTestCaseFinished);
        publisher.registerHandlerFor(TestRunFinished.class,  e -> manager.generateReport());
    }

    // ── Event handlers ───────────────────────────────────────────────────────

    private void onTestCaseStarted(TestCaseStarted event) {
        String name    = event.getTestCase().getName();
        String browser = manager.getRegisteredBrowser();
        String tags    = event.getTestCase().getTags().toString();

        manager.startTest(name, name, "Cucumber", browser, "");
        manager.getCurrentTest().addLog("Scenario: " + name);
        manager.getCurrentTest().addLog("Browser: " + browser);
        if (!tags.isEmpty() && !tags.equals("[]")) {
            manager.getCurrentTest().addLog("Tags: " + tags);
        }
    }

    private void onTestCaseFinished(TestCaseFinished event) {
        TestResultModel m = manager.getCurrentTest();
        String rawStatus  = event.getResult().getStatus().name(); // PASSED/FAILED/SKIPPED/PENDING/UNDEFINED

        String status;
        switch (rawStatus) {
            case "PASSED":
                status = "PASSED";
                if (m != null) m.addLog("Scenario passed");
                break;
            case "FAILED":
                status = "FAILED";
                if (m != null && event.getResult().getError() != null) {
                    Throwable error = event.getResult().getError();
                    m.addLog("Scenario FAILED — " + error.getMessage());
                    m.setErrorMessage(error.getMessage());
                    m.setStackTrace(toStackTrace(error));

                    // Screenshot
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
                }
                break;
            default: // SKIPPED, PENDING, UNDEFINED, AMBIGUOUS
                status = "SKIPPED";
                if (m != null) m.addLog("Scenario " + rawStatus.toLowerCase());
                break;
        }

        manager.finishTest(status);
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private String toStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }
}
