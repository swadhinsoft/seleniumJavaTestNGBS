package com.amazon.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class SearchResultsPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Locators
    private final By resultItems      = By.cssSelector("[data-component-type='s-search-result']");
    private final By resultTitles     = By.cssSelector("h2.a-size-mini span, h2 .a-size-base-plus");
    private final By searchHeader     = By.cssSelector("h1.a-size-medium, .s-result-count");
    private final By noResultsMsg     = By.cssSelector(".s-no-outline");

    public SearchResultsPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public boolean hasResults() {
        try {
            List<WebElement> items = wait.until(
                ExpectedConditions.presenceOfAllElementsLocatedBy(resultItems));
            return !items.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public int getResultCount() {
        List<WebElement> items = driver.findElements(resultItems);
        return items.size();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public ProductPage clickFirstResult() {
        List<WebElement> titles = wait.until(
            ExpectedConditions.presenceOfAllElementsLocatedBy(resultTitles));
        titles.get(0).click();
        return new ProductPage(driver);
    }
}
