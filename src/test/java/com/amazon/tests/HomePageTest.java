package com.amazon.tests;

import com.amazon.base.BaseTest;
import com.amazon.pages.HomePage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class HomePageTest extends BaseTest {

    /**
     * TC01 - Verify amazon.in home page loads with the correct title
     */
    @Test(description = "Verify Amazon.in home page title")
    public void testHomePageTitle() {
        HomePage homePage = new HomePage(getDriver());
        String title = homePage.getTitle();
        Assert.assertTrue(title.toLowerCase().contains("amazon"),
            "Page title should contain 'Amazon'. Actual: " + title);
    }

    /**
     * TC02 - Verify key UI elements are visible on the home page
     */
    @Test(description = "Verify key UI elements are present on the home page")
    public void testHomePageUIElements() {
        HomePage homePage = new HomePage(getDriver());
        Assert.assertTrue(homePage.isLogoVisible(),       "Amazon logo should be visible");
        Assert.assertTrue(homePage.isSearchBoxDisplayed(), "Search box should be visible");
        Assert.assertTrue(homePage.isCartIconVisible(),   "Cart icon should be visible");
        Assert.assertTrue(homePage.isSignInButtonVisible(), "Sign-In button should be visible");
    }

    /**
     * TC03 - Verify the URL loads the correct domain
     */
    @Test(description = "Verify Amazon.in URL is loaded")
    public void testHomePageUrl() {
        HomePage homePage = new HomePage(getDriver());
        String url = homePage.getCurrentUrl();
        Assert.assertTrue(url.contains("amazon.in"),
            "URL should contain 'amazon.in'. Actual: " + url);
    }
}
