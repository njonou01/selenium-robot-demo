package com.example.seleniumdemo.workflows;

import com.seleniumtests.uipage.PageObject;

import com.example.seleniumdemo.webpage.OrangeHRMPage;
import com.example.seleniumdemo.testdata.MapParams;
import com.example.seleniumdemo.reporting.Workflow;
import com.example.seleniumdemo.utils.Lazy;

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

	@Workflow(name = "Workflow RH complet", code = "hr.full")
	public void fullHRFlow() throws Exception {
		String username = PageObject.param("orangeHRM.username");
		String password = PageObject.param("orangeHRM.password");
		MapParams params = MapParams.load("hr.params");

		step1_login(username, password);
		step2_navigateToAddEmployee();
		step3_addEmployee(params.get("employee.firstName"), params.get("employee.lastName"));
		step4_timesheet();
		step5_myInfo();
	}
}
