package com.example.seleniumdemo.webpage;

import org.openqa.selenium.By;
import org.testng.Assert;

import com.seleniumtests.core.Step;
import com.seleniumtests.uipage.ByC;
import com.seleniumtests.uipage.PageObject;
import com.seleniumtests.uipage.htmlelements.ButtonElement;
import com.seleniumtests.uipage.htmlelements.TextFieldElement;
import com.seleniumtests.uipage.htmlelements.SelectList;
import com.seleniumtests.uipage.htmlelements.LinkElement;
import com.seleniumtests.uipage.htmlelements.HtmlElement;

public class ParabankPage extends PageObject {

	private static final HtmlElement LOGIN_PANEL = new HtmlElement("loginPanel", By.className("login"));
	private static final TextFieldElement USERNAME_FIELD = new TextFieldElement("username", By.name("username"));
	private static final TextFieldElement PASSWORD_FIELD = new TextFieldElement("password", By.name("password"));
	private static final ButtonElement LOGIN_BUTTON = new ButtonElement("login", ByC.attribute("value", "Log In"));

	private static final LinkElement OPEN_NEW_ACCOUNT_MENU = new LinkElement("openNewAccount", By.linkText("Open New Account"));
	private static final SelectList ACCOUNT_TYPE_SELECT = new SelectList("accountType", By.id("type"));
	private static final ButtonElement OPEN_ACCOUNT_BUTTON = new ButtonElement("openAccount", ByC.attribute("value", "Open New Account"));
	private static final HtmlElement NEW_ACCOUNT_ID = new HtmlElement("newAccountId", By.id("newAccountId"));

	private static final LinkElement TRANSFER_MENU = new LinkElement("transfer", By.linkText("Transfer Funds"));
	private static final TextFieldElement AMOUNT_FIELD = new TextFieldElement("amount", By.id("amount"));
	private static final SelectList FROM_ACCOUNT_SELECT = new SelectList("fromAccount", By.id("fromAccountId"));
	private static final SelectList TO_ACCOUNT_SELECT = new SelectList("toAccount", By.id("toAccountId"));
	private static final ButtonElement TRANSFER_BUTTON = new ButtonElement("transfer", ByC.attribute("value", "Transfer"));
	private static final HtmlElement TRANSFER_COMPLETE_HEADER = new HtmlElement("transferComplete", ByC.partialText("Transfer Complete", "*"));

	private static final LinkElement BILL_PAY_MENU = new LinkElement("billPay", By.linkText("Bill Pay"));
	private static final TextFieldElement PAYEE_NAME_FIELD = new TextFieldElement("payeeName", By.name("payee.name"));
	private static final TextFieldElement PAYEE_STREET_FIELD = new TextFieldElement("payeeStreet", By.name("payee.address.street"));
	private static final TextFieldElement PAYEE_CITY_FIELD = new TextFieldElement("payeeCity", By.name("payee.address.city"));
	private static final TextFieldElement PAYEE_STATE_FIELD = new TextFieldElement("payeeState", By.name("payee.address.state"));
	private static final TextFieldElement PAYEE_ZIP_FIELD = new TextFieldElement("payeeZip", By.name("payee.address.zipCode"));
	private static final TextFieldElement PAYEE_PHONE_FIELD = new TextFieldElement("payeePhone", By.name("payee.phoneNumber"));
	private static final TextFieldElement PAYEE_ACCOUNT_FIELD = new TextFieldElement("payeeAccount", By.name("payee.accountNumber"));
	private static final TextFieldElement VERIFY_ACCOUNT_FIELD = new TextFieldElement("verifyAccount", By.name("verifyAccount"));
	private static final TextFieldElement BILL_AMOUNT_FIELD = new TextFieldElement("billAmount", By.name("amount"));
	private static final ButtonElement SEND_PAYMENT_BUTTON = new ButtonElement("sendPayment", ByC.attribute("value", "Send Payment"));

	private static final LinkElement FIND_TRANSACTIONS_MENU = new LinkElement("findTransactions", By.linkText("Find Transactions"));
	private static final TextFieldElement FIND_BY_AMOUNT_FIELD = new TextFieldElement("findByAmount", By.id("amount"));
	private static final ButtonElement FIND_BY_AMOUNT_BUTTON = new ButtonElement("findByAmountButton", By.id("findByAmount"));

	private static final LinkElement UPDATE_CONTACT_MENU = new LinkElement("updateContact", By.linkText("Update Contact Info"));
	private static final TextFieldElement PROFILE_PHONE_FIELD = new TextFieldElement("profilePhone", By.id("customer.phoneNumber"));
	private static final ButtonElement UPDATE_PROFILE_BUTTON = new ButtonElement("updateProfile", ByC.attribute("value", "Update Profile"));

	private static final LinkElement REQUEST_LOAN_MENU = new LinkElement("requestLoan", By.linkText("Request Loan"));
	private static final TextFieldElement LOAN_AMOUNT_FIELD = new TextFieldElement("loanAmount", By.id("amount"));
	private static final TextFieldElement DOWN_PAYMENT_FIELD = new TextFieldElement("downPayment", By.id("downPayment"));
	private static final ButtonElement APPLY_LOAN_BUTTON = new ButtonElement("applyLoan", ByC.attribute("value", "Apply Now"));
	private static final HtmlElement LOAN_STATUS_APPROVED = new HtmlElement("loanStatusApproved", ByC.partialText("Approved", "*"));

	public ParabankPage() throws Exception {
		super(LOGIN_PANEL, "https://parabank.parasoft.com");
	}

	@Step(name = "Connexion bancaire: ${username}")
	public ParabankPage login(String username, String password) {
		USERNAME_FIELD.sendKeys(username);
		PASSWORD_FIELD.sendKeys(password);
		LOGIN_BUTTON.click();
		return this;
	}

	@Step(name = "Ouvrir un nouveau compte de type ${accountType}")
	public ParabankPage openNewAccount(String accountType) {
		OPEN_NEW_ACCOUNT_MENU.click();
		ACCOUNT_TYPE_SELECT.selectByText(accountType);
		OPEN_ACCOUNT_BUTTON.click();
		Assert.assertTrue(NEW_ACCOUNT_ID.isDisplayedRetry(),
			"Le numero du nouveau compte n'est pas affiché");
		return this;
	}

	public String getNewAccountNumber() {
		return NEW_ACCOUNT_ID.getText().trim();
	}

	@Step(name = "Effectuer virement de ${amount}")
	public ParabankPage transferFunds(int fromAccountIndex, int toAccountIndex, String amount) {
		TRANSFER_MENU.click();
		AMOUNT_FIELD.sendKeys(amount);
		FROM_ACCOUNT_SELECT.selectByIndex(fromAccountIndex);
		TO_ACCOUNT_SELECT.selectByIndex(toAccountIndex);
		TRANSFER_BUTTON.click();
		Assert.assertTrue(TRANSFER_COMPLETE_HEADER.isDisplayedRetry(),
			"La confirmation de virement n'est pas affichée");
		return this;
	}

	@Step(name = "Payer facture au fournisseur ${payeeName}")
	public ParabankPage payBill(String payeeName, String accountNumber, String amount) {
		BILL_PAY_MENU.click();
		PAYEE_NAME_FIELD.sendKeys(payeeName);
		PAYEE_STREET_FIELD.sendKeys("1 avenue Energie");
		PAYEE_CITY_FIELD.sendKeys("Lyon");
		PAYEE_STATE_FIELD.sendKeys("ARA");
		PAYEE_ZIP_FIELD.sendKeys("69000");
		PAYEE_PHONE_FIELD.sendKeys("0405060708");
		PAYEE_ACCOUNT_FIELD.sendKeys(accountNumber);
		VERIFY_ACCOUNT_FIELD.sendKeys(accountNumber);
		BILL_AMOUNT_FIELD.sendKeys(amount);
		SEND_PAYMENT_BUTTON.click();
		return this;
	}

	@Step(name = "Rechercher transaction par montant ${amount}")
	public ParabankPage findTransactionByAmount(String amount) {
		FIND_TRANSACTIONS_MENU.click();
		FIND_BY_AMOUNT_FIELD.sendKeys(amount);
		FIND_BY_AMOUNT_BUTTON.click();
		return this;
	}

	@Step(name = "Mettre à jour téléphone de contact: ${phone}")
	public ParabankPage updateContactPhone(String phone) {
		UPDATE_CONTACT_MENU.click();
		PROFILE_PHONE_FIELD.clear();
		PROFILE_PHONE_FIELD.sendKeys(phone);
		UPDATE_PROFILE_BUTTON.click();
		return this;
	}

	@Step(name = "Demander un prêt de ${loanAmount}")
	public ParabankPage requestLoan(String loanAmount, String downPayment) {
		REQUEST_LOAN_MENU.click();
		LOAN_AMOUNT_FIELD.sendKeys(loanAmount);
		DOWN_PAYMENT_FIELD.sendKeys(downPayment);
		APPLY_LOAN_BUTTON.click();
		Assert.assertTrue(LOAN_STATUS_APPROVED.isDisplayedRetry(),
			"Le prêt n'a pas été approuvé ou la confirmation n'est pas affichée");
		return this;
	}
}
