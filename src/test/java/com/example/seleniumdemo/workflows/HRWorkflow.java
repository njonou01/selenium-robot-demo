package com.example.seleniumdemo.workflows;

import com.seleniumtests.uipage.PageObject;
import com.seleniumtests.util.logging.ScenarioLogger;

import com.example.seleniumdemo.webpage.OrangeHRMPage;
import com.example.seleniumdemo.custom.reporting.Workflow;
import com.example.seleniumdemo.custom.utils.Lazy;

public class HRWorkflow {

	// instanciee au premier appel plutot que dans le constructeur : sinon la PageObject se cree
	// avant qu'un step @Workflow soit actif, et son step "openPage" interne devient orphelin a la
	// racine du rapport au lieu de s'imbriquer dans le step en cours
	private final Lazy<OrangeHRMPage> page = new Lazy<>(OrangeHRMPage::new);

	@Workflow(name = "Étape 1: Connexion RH ${username}")
	public void step1_login(String username, String password) throws Exception {
		page.get().login(username, password);
	}

	@Workflow(name = "Étape 2: Naviguer vers ajout employé")
	public void step2_navigateToAddEmployee() throws Exception {
		page.get().navigateToAddEmployee();
	}

	@Workflow(name = "Étape 3: Ajouter employé ${firstName}")
	public void step3_addEmployee(String firstName, String lastName) throws Exception {
		page.get().addEmployee(firstName, lastName);
	}

	@Workflow(name = "Étape 4: Accéder aux feuilles de temps")
	public void step4_timesheet() throws Exception {
		page.get().navigateToTimesheet();
	}

	@Workflow(name = "Étape 5: Consulter My Info")
	public void step5_myInfo() throws Exception {
		page.get().navigateToMyInfo();
	}

	/**
	 * Illustre le chainage de resultats entre workflows: 'accountNumber' ne vient pas du site
	 * OrangeHRM (qui n'a pas de notion de compte bancaire lie a un employe) mais du resultat
	 * renvoye par 'banking.full', enchaine plus tot dans le meme scenario via
	 * '${result:banking.full.accountNumber}' dans le dataSet.
	 */
	@Workflow(name = "Étape 6: Confirmer le compte bancaire chaîné ${accountNumber}")
	public void step6_confirmBankAccountLinked(String accountNumber) throws Exception {
		ScenarioLogger.getScenarioLogger(HRWorkflow.class)
				.info("Compte bancaire chaine depuis banking.full: " + accountNumber);
	}

	@Workflow(name = "Workflow RH complet", code = "hr.full")
	public void fullHRFlow(Employee employee, String accountNumber) throws Exception {
		String username = PageObject.param("orangeHRM.username");
		String password = PageObject.param("orangeHRM.password");

		step1_login(username, password);
		step2_navigateToAddEmployee();
		step3_addEmployee(employee.firstName(), employee.lastName());
		step4_timesheet();
		step5_myInfo();
		step6_confirmBankAccountLinked(accountNumber);
	}
}
