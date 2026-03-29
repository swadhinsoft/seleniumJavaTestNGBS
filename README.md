# seleniumJavaTestNGBS

A Selenium Java test automation framework for [Amazon.in](https://www.amazon.in) built with TestNG, supporting parallel execution, multi-browser testing locally, and cross-browser testing on BrowserStack — fully integrated with GitHub Actions CI/CD.

---

## Tech Stack

| Tool | Version | Purpose |
|------|---------|---------|
| Java | 11 | Language |
| Selenium WebDriver | 4.18.1 | Browser automation |
| TestNG | 7.9.0 | Test runner |
| WebDriverManager | 5.7.0 | Auto-manages local browser drivers |
| Maven | 3.x | Build & dependency management |
| BrowserStack Automate | — | Cloud cross-browser execution |
| GitHub Actions | — | CI/CD pipeline |

---

## Project Structure

```
seleniumJavaTestNGBS/
├── .github/
│   └── workflows/
│       └── browserstack.yml          # GitHub Actions CI/CD pipeline
├── src/
│   └── test/
│       ├── java/
│       │   └── com/amazon/
│       │       ├── base/
│       │       │   └── BaseTest.java          # Driver setup (local + BrowserStack)
│       │       ├── pages/
│       │       │   ├── HomePage.java
│       │       │   ├── SearchResultsPage.java
│       │       │   └── ProductPage.java
│       │       └── tests/
│       │           ├── HomePageTest.java       # TC01–TC03
│       │           └── SearchTest.java         # TC04–TC06
│       └── resources/
│           ├── testng.xml                      # Local parallel execution config
│           └── testng-browserstack.xml         # BrowserStack execution config
├── pom.xml
└── README.md
```

---

## Test Cases

| ID | Class | Description |
|----|-------|-------------|
| TC01 | HomePageTest | Verify home page title contains "Amazon" |
| TC02 | HomePageTest | Verify key UI elements (logo, search box, cart, sign-in) |
| TC03 | HomePageTest | Verify correct domain URL loads |
| TC04 | SearchTest | Search for "laptop" returns results |
| TC05 | SearchTest | Search query appears in results page URL |
| TC06 | SearchTest | Clicking first result opens a product page |

---

## Architecture

### Page Object Model (POM)

Tests are decoupled from UI interactions via page classes:

```
BaseTest  (driver lifecycle, browser factory)
    └── Tests (HomePageTest, SearchTest)
            └── Pages (HomePage, SearchResultsPage, ProductPage)
```

### Thread-Safe Driver

`BaseTest` uses `ThreadLocal<WebDriver>` so each parallel thread gets its own isolated driver instance. All test classes access the driver via `getDriver()`.

---

## Running Tests Locally

### Prerequisites

- Java 11+
- Maven 3.x
- Chrome and/or Firefox installed

### Run on Chrome + Firefox in parallel

```bash
mvn test
```

This uses `testng.xml` which runs 4 test groups in parallel (`thread-count="4"`):
- Chrome — Home Page Tests
- Chrome — Search Tests
- Firefox — Home Page Tests
- Firefox — Search Tests

### Run on a single browser

Pass the `browser` parameter at runtime:

```bash
mvn test -Dbrowser=chrome
mvn test -Dbrowser=firefox
mvn test -Dbrowser=edge
```

---

## Running Tests on BrowserStack

### Step 1 — Get BrowserStack credentials

1. Sign up at [browserstack.com](https://www.browserstack.com) (free trial available)
2. Go to **Account Settings** → copy your **Username** and **Access Key**

### Step 2 — Set environment variables

```bash
export BROWSERSTACK_USERNAME="your_username"
export BROWSERSTACK_ACCESS_KEY="your_access_key"
```

### Step 3 — Run

```bash
mvn test -P browserstack
```

This uses `testng-browserstack.xml` and runs 4 browser/OS combinations in parallel on BrowserStack:

| Browser | Version | OS | OS Version |
|---------|---------|----|------------|
| Chrome | Latest | Windows | 10 |
| Firefox | Latest | Windows | 10 |
| Edge | Latest | Windows | 11 |
| Safari | Latest | macOS | Ventura |

> **Free trial note:** The free trial allows only 1 parallel session. Set `thread-count="1"` in `testng-browserstack.xml` to run sequentially.

### BrowserStack capabilities

The framework sets the following `bstack:options` automatically:

| Capability | Value |
|------------|-------|
| `projectName` | Amazon.in Tests |
| `buildName` | Build-`$BUILD_ID` |
| `sessionName` | `<browser> <version> on <os> <os_version>` |

---

## GitHub Actions CI/CD

The pipeline is defined in `.github/workflows/browserstack.yml`.

### Triggers

| Event | Behaviour |
|-------|-----------|
| Push to `main` | Runs automatically |
| Pull Request to `main` | Runs on every PR — blocks merge on failure |
| Manual | **Actions** tab → select workflow → **Run workflow** |

### Pipeline Steps

```
Checkout → Java 11 setup → Maven cache → mvn test -P browserstack → Upload reports
```

### Setup (one-time)

Add BrowserStack credentials as GitHub repository secrets:

1. Go to your repo → **Settings** → **Secrets and variables** → **Actions**
2. Add the following secrets:

| Secret | Value |
|--------|-------|
| `BROWSERSTACK_USERNAME` | Your BrowserStack username |
| `BROWSERSTACK_ACCESS_KEY` | Your BrowserStack access key |

### Test Reports

TestNG XML reports are uploaded as artifacts after every run (retained 7 days).
Download from: **Actions** → select a run → **Artifacts** section.

---

## Quick Reference

```bash
# Local — parallel Chrome + Firefox
mvn test

# BrowserStack — cross-browser cloud
export BROWSERSTACK_USERNAME=xxx
export BROWSERSTACK_ACCESS_KEY=yyy
mvn test -P browserstack
```
