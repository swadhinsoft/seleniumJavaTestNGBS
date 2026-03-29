package com.amazon.tests;

import com.amazon.base.BaseTest;
import com.amazon.pages.HomePage;
import com.amazon.pages.ProductPage;
import com.amazon.pages.SearchResultsPage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SearchTest extends BaseTest {

    /**
     * TC04 - Verify search returns results for a valid product query
     */
    @Test(description = "Search for 'laptop' and verify results are returned")
    public void testSearchReturnsResults() {
        HomePage homePage = new HomePage(getDriver());
        SearchResultsPage resultsPage = homePage.searchFor("laptop");

        Assert.assertTrue(resultsPage.hasResults(),
            "Search results should be displayed for 'laptop'");
        Assert.assertTrue(resultsPage.getResultCount() > 0,
            "At least one result should be present");
    }

    /**
     * TC05 - Verify search URL contains the query keyword
     */
    @Test(description = "Verify search query appears in results page URL")
    public void testSearchUrlContainsQuery() {
        HomePage homePage = new HomePage(getDriver());
        SearchResultsPage resultsPage = homePage.searchFor("mobile phone");

        String url = resultsPage.getCurrentUrl();
        Assert.assertTrue(url.contains("mobile") || url.contains("phone") || url.contains("k="),
            "Results URL should reflect the search query. Actual URL: " + url);
    }

    /**
     * TC06 - Verify clicking first search result opens a product page
     */
    @Test(description = "Click first search result and verify product page loads")
    public void testClickFirstSearchResultOpensProduct() {
        HomePage homePage = new HomePage(getDriver());
        SearchResultsPage resultsPage = homePage.searchFor("headphones");

        Assert.assertTrue(resultsPage.hasResults(), "There should be results for 'headphones'");

        ProductPage productPage = resultsPage.clickFirstResult();

        Assert.assertTrue(productPage.isProductTitleDisplayed(),
            "Product title should be visible on the product page");
        Assert.assertFalse(productPage.getProductTitle().isEmpty(),
            "Product title should not be empty");
    }
}
