package com.example.seleniumdemo.webpage;

import java.time.Duration;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import com.seleniumtests.core.Step;
import com.seleniumtests.driver.WebUIDriver;
import com.seleniumtests.uipage.ByC;
import com.seleniumtests.uipage.PageObject;
import com.seleniumtests.uipage.htmlelements.ButtonElement;
import com.seleniumtests.uipage.htmlelements.LinkElement;
import com.seleniumtests.uipage.htmlelements.HtmlElement;

public class DemoblazePage extends PageObject {

	private static final HtmlElement CATEGORY_LIST = new HtmlElement("categories", By.id("cat"));
	private static final LinkElement ADD_TO_CART_BTN = new LinkElement("addToCart", ByC.partialText("Add to cart", "a"));
	private static final LinkElement CART_LINK = new LinkElement("cart", By.id("cartur"));
	private static final ButtonElement PLACE_ORDER_BTN = new ButtonElement("placeOrder", ByC.partialText("Place Order", "button"));
	private static final ButtonElement PURCHASE_BTN = new ButtonElement("purchase", ByC.partialText("Purchase", "button"));
	private static final HtmlElement PURCHASE_CONFIRMATION = new HtmlElement("purchaseConfirmation", ByC.partialText("Thank you for your purchase", "*"));

	public DemoblazePage() throws Exception {
		super(CATEGORY_LIST, "https://demoblaze.com");
	}

	public DemoblazePage selectCategory(String category) {
		LinkElement categoryBtn = new LinkElement("category", By.linkText(category));
		categoryBtn.click();
		return this;
	}

	public DemoblazePage selectProduct(String productName) {
		LinkElement product = new LinkElement("product", By.linkText(productName));
		product.click();
		return this;
	}

	@Step(name = "Ajouter au panier et accéder à la commande")
	public DemoblazePage addToCartAndCheckout() {
		ADD_TO_CART_BTN.click();
		acceptNativeAlertIfPresent();
		CART_LINK.click();
		PLACE_ORDER_BTN.click();
		return this;
	}

	private void acceptNativeAlertIfPresent() {
		try {
			new WebDriverWait(WebUIDriver.getWebDriver(false), Duration.ofSeconds(3))
				.until(ExpectedConditions.alertIsPresent());
			WebUIDriver.getWebDriver(false).switchTo().alert().accept();
		} catch (TimeoutException e) {
			// pas d'alerte apparue dans le délai, rien à faire
		}
	}

	@Step(name = "Renseigner infos de paiement: ${name}")
	public DemoblazePage fillOrderInfo(String name, String country, String city, String card, String month, String year) {
		((JavascriptExecutor) WebUIDriver.getWebDriver(false)).executeScript(
			"document.getElementById('name').value = arguments[0];"
			+ "document.getElementById('country').value = arguments[1];"
			+ "document.getElementById('city').value = arguments[2];"
			+ "document.getElementById('card').value = arguments[3];"
			+ "document.getElementById('month').value = arguments[4];"
			+ "document.getElementById('year').value = arguments[5];",
			name, country, city, card, month, year
		);
		PURCHASE_BTN.click();
		Assert.assertTrue(PURCHASE_CONFIRMATION.isDisplayedRetry(),
			"La confirmation d'achat n'est pas affichée");
		return this;
	}
}
