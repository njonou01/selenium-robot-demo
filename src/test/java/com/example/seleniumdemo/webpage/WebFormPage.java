package com.example.seleniumdemo.webpage;

import org.openqa.selenium.By;

import com.seleniumtests.core.Step;
import com.seleniumtests.uipage.PageObject;
import com.seleniumtests.uipage.htmlelements.ButtonElement;
import com.seleniumtests.uipage.htmlelements.HtmlElement;
import com.seleniumtests.uipage.htmlelements.TextFieldElement;

public class WebFormPage extends PageObject {

	private static final HtmlElement TITRE_FORMULAIRE = new HtmlElement("Titre du formulaire", By.tagName("h1"));
	private static final TextFieldElement CHAMP_SINISTRE = new TextFieldElement("Numéro de sinistre", By.id("my-text-id"));
	private static final ButtonElement BOUTON_SOUMETTRE = new ButtonElement("Bouton soumettre", By.cssSelector("button[type='submit']"));

	public WebFormPage() throws Exception {
		super(TITRE_FORMULAIRE, "https://www.selenium.dev/selenium/web/web-form.html");
	}

	@Step(name = "Vérifier titre de la page")
	public void verifyTitle() {
		if (!TITRE_FORMULAIRE.isDisplayed()) {
			throw new AssertionError("Title not found");
		}
	}

	@Step(name = "Remplir le champ avec ${value}")
	public void fillTextField(String value) {
		CHAMP_SINISTRE.clear();
		CHAMP_SINISTRE.sendKeys(value);
	}

	@Step(name = "Soumettre le formulaire")
	public void submitForm() {
		BOUTON_SOUMETTRE.click();
	}

	@Step(name = "Remplir et soumettre le formulaire avec ${value}")
	public void fillAndSubmit(String value) {
		verifyTitle();
		fillTextField(value);
		submitForm();
	}
}
