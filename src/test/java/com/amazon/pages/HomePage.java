package com.amazon.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class HomePage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Locators
    private final By searchBox        = By.id("twotabsearchtextbox");
    private final By searchButton     = By.id("nav-search-submit-button");
    private final By cartCount        = By.id("nav-cart-count");
    private final By amazonLogo       = By.id("nav-logo");
    private final By signInButton     = By.id("nav-link-accountList");
    private final By departmentsMenu  = By.id("nav-hamburger-menu");
    private final By pageTitle        = By.tagName("title");

    public HomePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public String getTitle() {
        return driver.getTitle();
    }

    public boolean isSearchBoxDisplayed() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(searchBox)).isDisplayed();
    }

    public SearchResultsPage searchFor(String query) {
        WebElement box = wait.until(ExpectedConditions.elementToBeClickable(searchBox));
        box.clear();
        box.sendKeys(query);
        driver.findElement(searchButton).click();
        return new SearchResultsPage(driver);
    }

    public boolean isCartIconVisible() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(cartCount)).isDisplayed();
    }

    public boolean isLogoVisible() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(amazonLogo)).isDisplayed();
    }

    public boolean isSignInButtonVisible() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(signInButton)).isDisplayed();
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }
}
