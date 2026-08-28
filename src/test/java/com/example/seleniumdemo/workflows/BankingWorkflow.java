package com.example.seleniumdemo.workflows;

import com.seleniumtests.uipage.PageObject;

import com.example.seleniumdemo.webpage.ParabankPage;
import com.example.seleniumdemo.custom.testdata.JsonParams;
import com.example.seleniumdemo.custom.reporting.Workflow;
import com.example.seleniumdemo.custom.utils.Lazy;

public class BankingWorkflow {

	// instanciee au premier appel plutot que dans le constructeur : sinon la PageObject se cree
	// avant qu'un step @Workflow soit actif, et son step "openPage" interne devient orphelin a la
	// racine du rapport au lieu de s'imbriquer dans le step en cours
	private final Lazy<ParabankPage> page = new Lazy<>(ParabankPage::new);

	@Workflow(name = "Étape 1: Authentification bancaire ${username}")
	public void step1_login(String username, String password) throws Exception {
		page.get().login(username, password);
	}

	@Workflow(name = "Étape 2: Ouvrir un nouveau compte")
	public String step2_openNewAccount() throws Exception {
		page.get().openNewAccount("CHECKING");
		return page.get().getNewAccountNumber();
	}

	@Workflow(name = "Étape 3: Effectuer virement de ${amount}")
	public void step3_transfer(int fromAccountIndex, int toAccountIndex, String amount) throws Exception {
		page.get().transferFunds(fromAccountIndex, toAccountIndex, amount);
	}

	@Workflow(name = "Étape 4: Payer une facture")
	public void step4_payBill(String payeeName, String accountNumber, String amount) throws Exception {
		page.get().payBill(payeeName, accountNumber, amount);
	}

	@Workflow(name = "Étape 5: Rechercher une transaction")
	public void step5_findTransaction(String amount) throws Exception {
		page.get().findTransactionByAmount(amount);
	}

	@Workflow(name = "Étape 6: Mettre à jour le profil")
	public void step6_updateContact(String phone) throws Exception {
		page.get().updateContactPhone(phone);
	}

	@Workflow(name = "Étape 7: Demander un prêt")
	public void step7_requestLoan(String loanAmount, String downPayment) throws Exception {
		page.get().requestLoan(loanAmount, downPayment);
	}

	@Workflow(name = "Workflow bancaire complet et long", code = "banking.full")
	public BankingResult fullBankingFlow() throws Exception {
		String username = PageObject.param("parabank.username");
		String password = PageObject.param("parabank.password");
		JsonParams params = JsonParams.load("banking.params");

		step1_login(username, password);
		String accountNumber = step2_openNewAccount();
		step3_transfer(0, 1, params.get("transfer.amount"));
		step4_payBill(params.get("payBill.payeeName"), params.get("payBill.accountNumber"), params.get("payBill.amount"));
		step5_findTransaction(params.get("findTransaction.amount"));
		step6_updateContact(params.get("updateContact.phone"));
		step7_requestLoan(params.get("requestLoan.loanAmount"), params.get("requestLoan.downPayment"));

		return new BankingResult(accountNumber);
	}
}
