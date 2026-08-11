package com.example.seleniumdemo.workflows;

import com.example.seleniumdemo.webpage.DemoblazePage;
import com.example.seleniumdemo.testdata.JsonParams;
import com.example.seleniumdemo.reporting.Workflow;
import com.example.seleniumdemo.utils.Lazy;

public class ElectronicsWorkflow {

	// instanciee au premier appel plutot que dans le constructeur : sinon la PageObject se cree
	// avant qu'un step @Workflow soit actif, et son step "openPage" interne devient orphelin a la
	// racine du rapport au lieu de s'imbriquer dans le step en cours
	private final Lazy<DemoblazePage> page = new Lazy<>(DemoblazePage::new);

	@Workflow(name = "Étape 1: Parcourir ${category} et sélectionner ${product}")
	public void step1_browseAndSelect(String category, String product) throws Exception {
		page.get().selectCategory(category);
		page.get().selectProduct(product);
	}

	@Workflow(name = "Étape 2: Ajouter au panier et commander")
	public void step2_addToCartAndCheckout() throws Exception {
		page.get().addToCartAndCheckout();
	}

	@Workflow(name = "Étape 3: Renseigner paiement ${cardName}")
	public void step3_fillOrderInfo(String name, String country, String city, String card, String month, String year) throws Exception {
		page.get().fillOrderInfo(name, country, city, card, month, year);
	}

	@Workflow(name = "Workflow électronique", code = "electronics.full")
	public void fullElectronicsFlow() throws Exception {
		JsonParams params = JsonParams.load("electronics.params");

		step1_browseAndSelect(params.get("browse.category"), params.get("browse.product"));
		step2_addToCartAndCheckout();
		step3_fillOrderInfo(params.get("order.name"), params.get("order.country"), params.get("order.city"),
				params.get("order.card"), params.get("order.month"), params.get("order.year"));
	}
}
