package com.example.seleniumdemo.workflows;

import com.seleniumtests.uipage.PageObject;

import com.example.seleniumdemo.webpage.PetStorePage;
import com.example.seleniumdemo.testdata.JsonParams;
import com.example.seleniumdemo.reporting.Workflow;
import com.example.seleniumdemo.utils.Lazy;

public class PetStoreWorkflow {

	// instanciee au premier appel plutot que dans le constructeur : sinon la PageObject se cree
	// avant qu'un step @Workflow soit actif, et son step "openPage" interne devient orphelin a la
	// racine du rapport au lieu de s'imbriquer dans le step en cours
	private final Lazy<PetStorePage> page = new Lazy<>(PetStorePage::new);

	@Workflow(name = "Étape 1: Connexion ${username}")
	public void step1_login(String username, String password) throws Exception {
		page.get().login(username, password);
	}

	@Workflow(name = "Étape 2: Parcourir ${category}, sélectionner ${productId} et ${itemId}")
	public void step2_browseSelectAndAdd(String category, String productId, String itemId) throws Exception {
		page.get().browseCategory(category);
		page.get().selectProduct(productId);
		page.get().selectItem(itemId);
	}

	@Workflow(name = "Étape 3: Checkout avec paiement par défaut")
	public void step3_checkout() throws Exception {
		page.get().checkoutWithDefaultPayment();
	}

	@Workflow(name = "Étape 4: Confirmer la commande")
	public void step4_confirmOrder() throws Exception {
		page.get().confirmOrder();
	}

	@Workflow(name = "Workflow animalerie complet", code = "petstore.full")
	public void fullPetStoreFlow() throws Exception {
		String username = PageObject.param("petStore.username");
		String password = PageObject.param("petStore.password");
		JsonParams params = JsonParams.load("petstore.params");

		step1_login(username, password);
		step2_browseSelectAndAdd(params.get("browse.category"), params.get("browse.productId"), params.get("browse.itemId"));
		step3_checkout();
		step4_confirmOrder();
	}
}
