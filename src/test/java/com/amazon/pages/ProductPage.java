package com.amazon.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class ProductPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Locators
    private final By productTitle   = By.id("productTitle");
    private final By productPrice   = By.cssSelector(".a-price .a-offscreen, #priceblock_ourprice, #apex_desktop_newAccordionRow .a-price");
    private final By addToCartBtn   = By.id("add-to-cart-button");
    private final By buyNowBtn      = By.id("buy-now-button");
    private final By breadcrumb     = By.id("wayfinding-breadcrumbs_feature_div");

    public ProductPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    public String getProductTitle() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(productTitle)).getText().trim();
    }

    public boolean isProductTitleDisplayed() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(productTitle)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isAddToCartButtonVisible() {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(addToCartBtn)).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    public String getPageTitle() {
        return driver.getTitle();
    }
}
