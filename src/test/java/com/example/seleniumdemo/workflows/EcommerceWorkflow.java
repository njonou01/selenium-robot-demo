package com.example.seleniumdemo.workflows;

import com.seleniumtests.uipage.PageObject;

import com.example.seleniumdemo.webpage.SauceDemoPage;
import com.example.seleniumdemo.testdata.JsonParams;
import com.example.seleniumdemo.reporting.Workflow;
import com.example.seleniumdemo.utils.Lazy;

public class EcommerceWorkflow {

	// instanciee au premier appel plutot que dans le constructeur : sinon la PageObject se cree
	// avant qu'un step @Workflow soit actif, et son step "openPage" interne devient orphelin a la
	// racine du rapport au lieu de s'imbriquer dans le step en cours
	private final Lazy<SauceDemoPage> page = new Lazy<>(SauceDemoPage::new);

	@Workflow(name = "Étape 1: Login avec ${username}")
	public void step1_login(String username, String password) throws Exception {
		page.get().login(username, password);
	}

	@Workflow(name = "Étape 2: Sélectionner produit ${productName} et ajouter au panier")
	public void step2_selectAndAddProduct(String productName) throws Exception {
		page.get().selectProduct(productName);
		page.get().addToCart();
	}

	@Workflow(name = "Étape 3: Panier et checkout pour ${firstName}")
	public void step3_cartAndCheckout(String firstName, String lastName, String zipCode) throws Exception {
		page.get().goToCartAndCheckout(firstName, lastName, zipCode);
	}

	@Workflow(name = "Étape 4: Valider la commande")
	public void step4_finishOrder() throws Exception {
		page.get().finishOrder();
	}

	@Workflow(name = "Processus e-commerce complet", code = "ecommerce.full")
	public void fullEcommerceFlow() throws Exception {
		String username = PageObject.param("sauceDemo.username");
		String password = PageObject.param("sauceDemo.password");
		JsonParams params = JsonParams.load("ecommerce.params");

		step1_login(username, password);
		step2_selectAndAddProduct(params.get("product.name"));
		step3_cartAndCheckout(params.get("checkout.firstName"), params.get("checkout.lastName"), params.get("checkout.zipCode"));
		step4_finishOrder();
	}
}
