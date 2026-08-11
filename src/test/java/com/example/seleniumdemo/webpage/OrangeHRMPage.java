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

public class OrangeHRMPage extends PageObject {

	// NOTE: OrangeHRM (Vue.js) entoure ses textes de commentaires <!----> qui cassent
	// XPath text()/ByC.text (le node-set retourné prend le 1er noeud texte, souvent vide).
	// On utilise donc des sélecteurs stables (class, href) plutôt que le texte affiché.

	private static final HtmlElement LOGIN_FORM = new HtmlElement("loginForm", By.className("orangehrm-login-form"));
	private static final TextFieldElement USERNAME_FIELD = new TextFieldElement("username", By.name("username"));
	private static final TextFieldElement PASSWORD_FIELD = new TextFieldElement("password", By.name("password"));
	private static final ButtonElement LOGIN_BUTTON = new ButtonElement("login", By.className("orangehrm-login-button"));

	private static final LinkElement PIM_MENU = new LinkElement("pim", ByC.attribute("href*", "pim/viewPimModule"));
	private static final LinkElement ADD_EMPLOYEE_TAB = new LinkElement("addEmployeeTab", By.linkText("Add Employee"));
	private static final TextFieldElement FIRST_NAME_FIELD = new TextFieldElement("firstName", By.name("firstName"));
	private static final TextFieldElement LAST_NAME_FIELD = new TextFieldElement("lastName", By.name("lastName"));
	private static final ButtonElement SAVE_BUTTON = new ButtonElement("save", ByC.attribute("type", "submit"));
	private static final HtmlElement SUCCESS_TOAST = new HtmlElement("successToast", By.className("oxd-toast"));

	private static final LinkElement TIME_MENU = new LinkElement("time", ByC.attribute("href*", "time/viewTimeModule"));

	private static final LinkElement MY_INFO_MENU = new LinkElement("myInfo", ByC.attribute("href*", "pim/viewMyDetails"));

	public OrangeHRMPage() throws Exception {
		super(LOGIN_FORM, "https://opensource-demo.orangehrmlive.com");
	}

	@Step(name = "Connexion RH: ${username}")
	public OrangeHRMPage login(String username, String password) {
		USERNAME_FIELD.sendKeys(username);
		PASSWORD_FIELD.sendKeys(password);
		LOGIN_BUTTON.click();
		return this;
	}

	@Step(name = "Naviguer vers PIM puis Ajouter Employé")
	public OrangeHRMPage navigateToAddEmployee() {
		PIM_MENU.click();
		ADD_EMPLOYEE_TAB.click();
		return this;
	}

	@Step(name = "Ajouter employé: ${firstName} ${lastName}")
	public OrangeHRMPage addEmployee(String firstName, String lastName) {
		FIRST_NAME_FIELD.clear();
		FIRST_NAME_FIELD.sendKeys(firstName);
		LAST_NAME_FIELD.clear();
		LAST_NAME_FIELD.sendKeys(lastName);
		SAVE_BUTTON.click();
		Assert.assertTrue(SUCCESS_TOAST.isDisplayedRetry(),
			"Le toast de confirmation d'ajout d'employé n'est pas affiché");
		return this;
	}

	@Step(name = "Naviguer vers Time / Timesheets")
	public OrangeHRMPage navigateToTimesheet() {
		TIME_MENU.click();
		return this;
	}

	@Step(name = "Consulter My Info")
	public OrangeHRMPage navigateToMyInfo() {
		MY_INFO_MENU.click();
		return this;
	}
}
