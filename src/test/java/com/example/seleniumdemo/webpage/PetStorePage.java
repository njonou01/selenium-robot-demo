package com.example.seleniumdemo.webpage;

import org.openqa.selenium.By;
import org.testng.Assert;

import com.seleniumtests.core.Step;
import com.seleniumtests.uipage.ByC;
import com.seleniumtests.uipage.PageObject;
import com.seleniumtests.uipage.htmlelements.ButtonElement;
import com.seleniumtests.uipage.htmlelements.TextFieldElement;
import com.seleniumtests.uipage.htmlelements.LinkElement;
import com.seleniumtests.uipage.htmlelements.HtmlElement;

public class PetStorePage extends PageObject {

	private static final LinkElement SIGN_IN_LINK = new LinkElement("signIn", By.linkText("Sign In"));
	private static final TextFieldElement USERNAME_FIELD = new TextFieldElement("username", By.name("username"));
	private static final TextFieldElement PASSWORD_FIELD = new TextFieldElement("password", By.name("password"));
	private static final ButtonElement LOGIN_BUTTON = new ButtonElement("login", ByC.attribute("value", "Login"));

	private static final LinkElement PROCEED_CHECKOUT_LINK = new LinkElement("proceedCheckout", By.linkText("Proceed to Checkout"));
	private static final ButtonElement CONTINUE_BUTTON = new ButtonElement("continue", ByC.attribute("value", "Continue"));
	private static final ButtonElement CONFIRM_BUTTON = new ButtonElement("confirm", ByC.attribute("value", "Confirm"));
	private static final HtmlElement ORDER_SUBMITTED_MESSAGE = new HtmlElement("orderSubmitted", ByC.partialText("Thank you, your order has been submitted", "*"));

	public PetStorePage() throws Exception {
		super(SIGN_IN_LINK, "https://petstore.octoperf.com/actions/Catalog.action");
	}

	@Step(name = "Connexion: ${username}")
	public PetStorePage login(String username, String password) {
		SIGN_IN_LINK.click();
		USERNAME_FIELD.sendKeys(username);
		PASSWORD_FIELD.sendKeys(password);
		LOGIN_BUTTON.click();
		return this;
	}

	public PetStorePage browseCategory(String category) {
		LinkElement categoryLink = new LinkElement("category", By.linkText(category));
		categoryLink.click();
		return this;
	}

	public PetStorePage selectProduct(String productId) {
		LinkElement product = new LinkElement("product", By.linkText(productId));
		product.click();
		return this;
	}

	public PetStorePage selectItem(String itemId) {
		LinkElement addToCartLink = new LinkElement("addToCartForItem", By.xpath("//tr[td/a[text()='" + itemId + "']]//a[text()='Add to Cart']"));
		addToCartLink.click();
		return this;
	}

	@Step(name = "Procéder au checkout et valider paiement pré-rempli")
	public PetStorePage checkoutWithDefaultPayment() {
		PROCEED_CHECKOUT_LINK.click();
		CONTINUE_BUTTON.click();
		return this;
	}

	@Step(name = "Confirmer la commande")
	public PetStorePage confirmOrder() {
		CONFIRM_BUTTON.click();
		Assert.assertTrue(ORDER_SUBMITTED_MESSAGE.isDisplayedRetry(),
			"La confirmation de commande n'est pas affichée");
		return this;
	}
}
