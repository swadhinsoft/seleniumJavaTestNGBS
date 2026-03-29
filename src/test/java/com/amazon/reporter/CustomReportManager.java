package com.amazon.reporter;

import com.amazon.reporter.model.TestResultModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Core singleton for the custom reporter.
 * No com.amazon.* imports — fully framework-agnostic.
 * Supports Selenium and Appium (both implement WebDriver).
 */
public class CustomReportManager {

    private static final CustomReportManager INSTANCE = new CustomReportManager();

    // ── Test result collection ───────────────────────────────────────────────
    private final List<TestResultModel> results =
            Collections.synchronizedList(new ArrayList<>());
    private static final ThreadLocal<TestResultModel> current = new ThreadLocal<>();

    // ── Driver / browser registration (pushed by the test framework) ─────────
    private static final ThreadLocal<WebDriver> driverHolder  = new ThreadLocal<>();
    private static final ThreadLocal<String>    browserHolder = new ThreadLocal<>();

    private static final String OUTPUT_DIR  = "target/custom-reports/";
    private static final String REPORT_FILE = OUTPUT_DIR + "report.html";
    private static final String TEMPLATE    = "report-template.html";

    private CustomReportManager() {}

    public static CustomReportManager getInstance() { return INSTANCE; }

    // ── Driver registration API (called by any test base class / hook) ────────

    /** Register the WebDriver for the current thread (Selenium or Appium). */
    public void registerDriver(WebDriver driver)   { driverHolder.set(driver); }

    /** Unregister driver and browser after the test/teardown completes. */
    public void unregisterDriver() {
        driverHolder.remove();
        browserHolder.remove();
    }

    /** Set the browser name for the current thread (e.g. "chrome", "firefox"). */
    public void setBrowser(String browser) { browserHolder.set(browser); }

    // ── Package-private: used by adapters ────────────────────────────────────

    WebDriver getRegisteredDriver() { return driverHolder.get(); }

    String getRegisteredBrowser() {
        String b = browserHolder.get();
        return (b != null && !b.isEmpty()) ? b : System.getProperty("browser", "unknown");
    }

    // ── Test lifecycle ────────────────────────────────────────────────────────

    public void startTest(String methodName, String description,
                          String suiteName, String browser, String os) {
        TestResultModel m = new TestResultModel();
        m.setTestName(methodName);
        m.setDescription(description != null && !description.isEmpty() ? description : methodName);
        m.setSuiteName(suiteName);
        m.setBrowser(browser != null ? browser : "unknown");
        m.setOs(os != null ? os : "");
        m.setStartTime(System.currentTimeMillis());
        m.addLog("Test started");
        current.set(m);
    }

    public TestResultModel getCurrentTest() { return current.get(); }

    public void finishTest(String status) {
        TestResultModel m = current.get();
        if (m == null) return;
        m.setStatus(status);
        m.setDurationMs(System.currentTimeMillis() - m.getStartTime());
        results.add(m);
        current.remove();
    }

    // ── Report generation ────────────────────────────────────────────────────

    public synchronized void generateReport() {
        try {
            String html = fillTemplate(readTemplate());
            File dir = new File(OUTPUT_DIR);
            if (!dir.exists()) dir.mkdirs();
            Files.write(new File(REPORT_FILE).toPath(), html.getBytes(StandardCharsets.UTF_8));

            System.out.println("\n╔══════════════════════════════════════════════╗");
            System.out.println("║  Custom Report → " + REPORT_FILE + "  ║");
            System.out.println("╚══════════════════════════════════════════════╝\n");
        } catch (Exception e) {
            System.err.println("Custom report generation failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String readTemplate() throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(TEMPLATE);
        if (is == null) throw new IllegalStateException(TEMPLATE + " not found on classpath");
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private String fillTemplate(String tpl) throws Exception {
        long passed  = results.stream().filter(r -> "PASSED".equals(r.getStatus())).count();
        long failed  = results.stream().filter(r -> "FAILED".equals(r.getStatus())).count();
        long skipped = results.stream().filter(r -> "SKIPPED".equals(r.getStatus())).count();
        long total   = results.size();
        double passRate  = total > 0 ? (passed * 100.0 / total) : 0;
        long totalMs     = results.stream().mapToLong(TestResultModel::getDurationMs).sum();
        String duration  = totalMs >= 1000
                ? String.format("%.1fs", totalMs / 1000.0) : totalMs + "ms";
        String generated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String jsonData  = new ObjectMapper().writeValueAsString(results);

        return tpl
                .replace("{{TITLE}}",          "Amazon.in Test Report")
                .replace("{{GENERATED_AT}}",   generated)
                .replace("{{ENVIRONMENT}}",    "Amazon.in")
                .replace("{{TOTAL}}",          String.valueOf(total))
                .replace("{{PASSED}}",         String.valueOf(passed))
                .replace("{{FAILED}}",         String.valueOf(failed))
                .replace("{{SKIPPED}}",        String.valueOf(skipped))
                .replace("{{PASS_RATE}}",      String.format("%.1f", passRate))
                .replace("{{TOTAL_DURATION}}", duration)
                .replace("{{TEST_DATA_JSON}}", jsonData);
    }
}
