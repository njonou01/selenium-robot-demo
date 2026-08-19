package com.example.seleniumdemo.workflows;

import com.example.seleniumdemo.reporting.Workflow;
import com.example.seleniumdemo.webpage.WebFormPage;
import com.example.seleniumdemo.utils.Lazy;

public class FormWorkflow {

	// instanciee au premier appel plutot que dans le constructeur : sinon la PageObject se cree
	// avant qu'un step @Workflow soit actif, et son step "openPage" interne devient orphelin a la
	// racine du rapport au lieu de s'imbriquer dans le step en cours
	private final Lazy<WebFormPage> page = new Lazy<>(WebFormPage::new);

	@Workflow(name = "Initialiser le workflow de formulaire")
	public void initialize() throws Exception {
		page.get()
				.verifyTitle();
	}

	@Workflow(name = "Étape 1: Vérifier la page")
	public void step1_verifyPage() throws Exception {
		page.get().verifyTitle();
	}

	@Workflow(name = "Étape 2: Remplir le champ avec ${value}")
	public void step2_fillField(String value) throws Exception {
		page.get().fillTextField(value);
	}

	@Workflow(name = "Étape 3: Soumettre le formulaire")
	public void step3_submit() throws Exception {
		page.get()
				.submitForm();
	}

	@Workflow(name = "Processus complet: Déclaration de sinistre ${id}")
	public void processDeclareSinistre(String id) throws Exception {
		System.out.println("Declaration de sinistre: " + id);
		step1_verifyPage();
		step2_fillField(id);
		step3_submit();
	}

	@Workflow(name = "Workflow alternatif avec vérifications intermédiaires ${reference}")
	public void alternativeWorkflow(String reference) throws Exception {
		initialize();
		step2_fillField(reference + "_test");
		step1_verifyPage();
		step3_submit();
	}

	@Workflow(name = "Workflow long: Multi-étapes avec ${scenario}")
	public void complexWorkflow(String scenario) throws Exception {
		step1_verifyPage();
		step2_fillField("prefix_" + scenario);
		step2_fillField("modified_" + scenario);
		step3_submit();
		step1_verifyPage();
	}

	public WebFormPage getPage() throws Exception {
		return page.get();
	}
}
