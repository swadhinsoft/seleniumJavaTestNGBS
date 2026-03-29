package com.amazon.reporter;

import com.amazon.reporter.model.TestResultModel;
import com.amazon.utils.ConfigReader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Core singleton for the custom reporter.
 * No com.amazon.base.* imports — fully framework-agnostic.
 * Supports Selenium and Appium (both implement WebDriver).
 */
public class CustomReportManager {

    private static final CustomReportManager INSTANCE = new CustomReportManager();

    private final List<TestResultModel> results =
            Collections.synchronizedList(new ArrayList<>());
    private static final ThreadLocal<TestResultModel> current       = new ThreadLocal<>();
    private static final ThreadLocal<WebDriver>       driverHolder  = new ThreadLocal<>();
    private static final ThreadLocal<String>          browserHolder = new ThreadLocal<>();

    private static final String OUTPUT_DIR  = "target/custom-reports/";
    private static final String REPORT_FILE = OUTPUT_DIR + "report.html";
    private static final String TEMPLATE    = "report-template.html";

    private CustomReportManager() {}

    public static CustomReportManager getInstance() { return INSTANCE; }

    // ── Driver registration API ──────────────────────────────────────────────

    public void registerDriver(WebDriver driver) { driverHolder.set(driver); }

    public void unregisterDriver() {
        driverHolder.remove();
        browserHolder.remove();
    }

    public void setBrowser(String browser) { browserHolder.set(browser); }

    public WebDriver getRegisteredDriver() { return driverHolder.get(); }

    public String getRegisteredBrowser() {
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
        double passRate = total > 0 ? (passed * 100.0 / total) : 0;
        long totalMs    = results.stream().mapToLong(TestResultModel::getDurationMs).sum();
        String duration = totalMs >= 1000
                ? String.format("%.1fs", totalMs / 1000.0) : totalMs + "ms";
        String generated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String jsonData  = new ObjectMapper().writeValueAsString(results);

        // Logo — read from config path, embed as base64
        String logoElement = buildLogoElement();

        return tpl
                .replace("{{TITLE}}",          ConfigReader.getReportTitle())
                .replace("{{GENERATED_AT}}",   generated)
                .replace("{{ENVIRONMENT}}",    "Amazon.in")
                .replace("{{TOTAL}}",          String.valueOf(total))
                .replace("{{PASSED}}",         String.valueOf(passed))
                .replace("{{FAILED}}",         String.valueOf(failed))
                .replace("{{SKIPPED}}",        String.valueOf(skipped))
                .replace("{{PASS_RATE}}",      String.format("%.1f", passRate))
                .replace("{{TOTAL_DURATION}}", duration)
                .replace("{{LOGO_ELEMENT}}",   logoElement)
                .replace("{{TEST_DATA_JSON}}", jsonData);
    }

    /** Reads the logo from the path in config.json and returns an <img> HTML element,
     *  or an empty string if the path is blank or the file is missing. */
    private String buildLogoElement() {
        try {
            String path = ConfigReader.getLogoPath();
            if (path == null || path.trim().isEmpty()) return "";

            File file = new File(path);
            if (!file.isAbsolute()) file = new File(System.getProperty("user.dir"), path);
            if (!file.exists() || !file.isFile()) {
                System.err.println("[CustomReporter] Logo not found: " + file.getAbsolutePath());
                return "";
            }

            byte[] bytes = Files.readAllBytes(file.toPath());
            String name  = file.getName().toLowerCase();
            String mime  = name.endsWith(".svg")  ? "image/svg+xml"
                         : name.endsWith(".jpg") || name.endsWith(".jpeg") ? "image/jpeg"
                         : name.endsWith(".gif")  ? "image/gif"
                         : "image/png";
            String src = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
            return "<img src=\"" + src + "\" alt=\"Logo\" class=\"project-logo\"/>";
        } catch (Exception e) {
            System.err.println("[CustomReporter] Could not load logo: " + e.getMessage());
            return "";
        }
    }
}
