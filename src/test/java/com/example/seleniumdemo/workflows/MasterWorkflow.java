package com.example.seleniumdemo.workflows;

import com.example.seleniumdemo.custom.reporting.Workflow;

public class MasterWorkflow {

	private EcommerceWorkflow ecommerce;
	private BankingWorkflow banking;
	private ElectronicsWorkflow electronics;
	private HRWorkflow hr;
	private PetStoreWorkflow petstore;

	public MasterWorkflow() throws Exception {
		this.ecommerce = new EcommerceWorkflow();
		this.banking = new BankingWorkflow();
		this.electronics = new ElectronicsWorkflow();
		this.hr = new HRWorkflow();
		this.petstore = new PetStoreWorkflow();
	}

	@Workflow(name = "Phase 1: Processus e-commerce ${scenario}")
	public void phase1_ecommerce(String scenario) throws Exception {
		ecommerce.step1_login("standard_user", "secret_sauce");
		ecommerce.step2_selectAndAddProduct("Sauce Labs Backpack");
		ecommerce.step3_cartAndCheckout("Jean", "Dupont", "75001");
		ecommerce.step4_finishOrder();
	}

	@Workflow(name = "Phase 2: Opérations bancaires longues ${amount}")
	public void phase2_banking(String amount) throws Exception {
		banking.step1_login("john", "demo");
		banking.step2_openNewAccount();
		banking.step3_transfer(0, 1, amount);
		banking.step4_payBill("EDF Electricite", "987654", "75");
		banking.step5_findTransaction("75");
		banking.step6_updateContact("310-447-9999");
		banking.step7_requestLoan("5000", "1000");
	}

	@Workflow(name = "Phase 3: Achat électronique ${productType}")
	public void phase3_electronics(String productType) throws Exception {
		electronics.step1_browseAndSelect("Phones", "Samsung galaxy s6");
		electronics.step2_addToCartAndCheckout();
		electronics.step3_fillOrderInfo("Pierre Martin", "France", "Paris", "4111111111111111", "06", "2027");
	}

	@Workflow(name = "Phase 4: Gestion RH ${department}")
	public void phase4_hr(String department) throws Exception {
		hr.step1_login("Admin", "admin123");
		hr.step2_navigateToAddEmployee();
		hr.step3_addEmployee("Sophie", "Bernard");
		hr.step4_timesheet();
		hr.step5_myInfo();
	}

	@Workflow(name = "Phase 5: Achats animalerie ${petType}")
	public void phase5_petstore(String petType) throws Exception {
		petstore.step1_login("j2ee", "j2ee");
		petstore.step2_browseSelectAndAdd("Fish", "Angelfish", "EST-1");
		petstore.step3_checkout();
		petstore.step4_confirmOrder();
	}

	@Workflow(name = "Orchestration complète: Scénario ${scenario}")
	public void fullMasterWorkflow(String scenario) throws Exception {
		phase1_ecommerce("shopping");
		phase2_banking("2500");
		phase3_electronics("smartphone");
		phase4_hr("IT");
		phase5_petstore("fish");
	}

	@Workflow(name = "Scénario alternatif: ${scenario} avec plusieurs itérations")
	public void alternativeMasterFlow(String scenario, int iterations) throws Exception {
		for (int i = 1; i <= iterations; i++) {
			phase1_ecommerce("iteration_" + i);
			phase3_electronics("product_" + i);
		}
		phase2_banking("5000");
		phase4_hr("HR");
	}

	@Workflow(name = "Workflow express: Phase 1 et 5 pour ${user}")
	public void expressWorkflow(String user) throws Exception {
		phase1_ecommerce("express_" + user);
		phase5_petstore(user + "_pets");
	}

	@Workflow(name = "Workflow intermédiaire: Banque -> HR -> E-commerce pour ${profile}")
	public void intermediateWorkflow(String profile) throws Exception {
		phase2_banking("1500");
		phase4_hr("Sales");
		phase1_ecommerce(profile + "_shopping");
		phase3_electronics("tablet");
	}
}
