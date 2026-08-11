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

public class SauceDemoPage extends PageObject {

	private static final HtmlElement LOGIN_CONTAINER = new HtmlElement("loginContainer", By.className("login_container"));
	private static final TextFieldElement USERNAME_FIELD = new TextFieldElement("username", By.id("user-name"));
	private static final TextFieldElement PASSWORD_FIELD = new TextFieldElement("password", By.id("password"));
	private static final ButtonElement LOGIN_BUTTON = new ButtonElement("login", By.id("login-button"));
	private static final ButtonElement ADD_TO_CART_BUTTON = new ButtonElement("addToCart", ByC.partialText("Add to cart", "button"));
	private static final LinkElement CART_ICON = new LinkElement("cart", By.className("shopping_cart_link"));
	private static final ButtonElement CHECKOUT_BUTTON = new ButtonElement("checkout", By.id("checkout"));
	private static final TextFieldElement FIRST_NAME_FIELD = new TextFieldElement("firstName", By.id("first-name"));
	private static final TextFieldElement LAST_NAME_FIELD = new TextFieldElement("lastName", By.id("last-name"));
	private static final TextFieldElement ZIP_CODE_FIELD = new TextFieldElement("zipCode", By.id("postal-code"));
	private static final ButtonElement CONTINUE_BUTTON = new ButtonElement("continue", By.id("continue"));
	private static final ButtonElement FINISH_BUTTON = new ButtonElement("finish", By.id("finish"));
	private static final HtmlElement ORDER_COMPLETE_HEADER = new HtmlElement("orderComplete", By.className("complete-header"));

	public SauceDemoPage() throws Exception {
		super(LOGIN_CONTAINER, "https://www.saucedemo.com");
	}

	@Step(name = "Authentification: ${username}")
	public SauceDemoPage login(String username, String password) {
		USERNAME_FIELD.sendKeys(username);
		PASSWORD_FIELD.sendKeys(password);
		LOGIN_BUTTON.click();
		return this;
	}

	public SauceDemoPage selectProduct(String productName) {
		HtmlElement product = new HtmlElement("product", ByC.partialText(productName, "div"));
		product.click();
		return this;
	}

	public SauceDemoPage addToCart() {
		ADD_TO_CART_BUTTON.click();
		return this;
	}

	@Step(name = "Gestion panier et checkout")
	public SauceDemoPage goToCartAndCheckout(String firstName, String lastName, String zipCode) {
		CART_ICON.click();
		CHECKOUT_BUTTON.click();
		FIRST_NAME_FIELD.sendKeys(firstName);
		LAST_NAME_FIELD.sendKeys(lastName);
		ZIP_CODE_FIELD.sendKeys(zipCode);
		CONTINUE_BUTTON.click();
		return this;
	}

	@Step(name = "Finalisation commande")
	public SauceDemoPage finishOrder() {
		FINISH_BUTTON.click();
		Assert.assertEquals(ORDER_COMPLETE_HEADER.getText(), "Thank you for your order!",
			"La confirmation de commande n'est pas affichée");
		return this;
	}
}
