package com.amazon.reporter;

import com.amazon.reporter.model.TestResultModel;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

public class CustomReportManager {

    private static final CustomReportManager INSTANCE = new CustomReportManager();
    private final List<TestResultModel>      results  =
            Collections.synchronizedList(new ArrayList<>());
    private static final ThreadLocal<TestResultModel> current = new ThreadLocal<>();

    private static final String OUTPUT_DIR  = "target/custom-reports/";
    private static final String REPORT_FILE = OUTPUT_DIR + "report.html";
    private static final String TEMPLATE    = "report-template.html";

    private CustomReportManager() {}

    public static CustomReportManager getInstance() { return INSTANCE; }

    // ── Thread-local helpers ─────────────────────────────────────────────────

    public void startTest(String methodName, String description,
                          String suiteName, String browser, String os) {
        TestResultModel m = new TestResultModel();
        m.setTestName(methodName);
        m.setDescription(description != null && !description.isEmpty()
                ? description : methodName);
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
            String template = readTemplate();
            String html     = fillTemplate(template);

            File dir = new File(OUTPUT_DIR);
            if (!dir.exists()) dir.mkdirs();
            Files.write(new File(REPORT_FILE).toPath(),
                    html.getBytes(StandardCharsets.UTF_8));

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
        double passRate   = total > 0 ? (passed * 100.0 / total) : 0;
        long totalMs      = results.stream().mapToLong(TestResultModel::getDurationMs).sum();
        String duration   = totalMs >= 1000
                ? String.format("%.1fs", totalMs / 1000.0) : totalMs + "ms";
        String generatedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String jsonData    = new ObjectMapper().writeValueAsString(results);

        return tpl
                .replace("{{TITLE}}",          "Amazon.in Test Report")
                .replace("{{GENERATED_AT}}",   generatedAt)
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
