package com.example.seleniumdemo.custom.unitaire;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import com.example.seleniumdemo.custom.reporting.TeamInfoListener;
import com.seleniumtests.reporter.info.Info;

public class TeamInfoListenerTest {

	// meme cle privee que TestNGResultUtils.TEST_INFO - stockage de la Map<String, Info> sur
	// l'attribut ITestResult, jamais expose publiquement autrement que par ce nom de cle.
	private static final String TEST_INFO_ATTRIBUTE = "testInfo";

	/**
	 * 'getAttribute(TEST_INFO)' renvoie directement 'storedInfo' (jamais null) : TestNGResultUtils.
	 * setTestInfo() recupere cette Map par reference et la mute en place (testInfo.put(...)) avant
	 * de la re-poser via setAttribute() - inutile d'intercepter setAttribute() separement, la
	 * mutation est deja visible sur 'storedInfo' directement.
	 */
	private Map<String, Info> runOnTestStart(String equipeParameter) {
		XmlTest xmlTest = new XmlTest(new XmlSuite());
		if (equipeParameter != null) {
			xmlTest.addParameter("equipe", equipeParameter);
		}
		ITestContext context = Mockito.mock(ITestContext.class);
		Mockito.when(context.getCurrentXmlTest()).thenReturn(xmlTest);

		Map<String, Info> storedInfo = new HashMap<>();
		ITestResult result = Mockito.mock(ITestResult.class);
		Mockito.when(result.getTestContext()).thenReturn(context);
		Mockito.when(result.getAttribute(TEST_INFO_ATTRIBUTE)).thenReturn(storedInfo);

		new TeamInfoListener().onTestStart(result);
		return storedInfo;
	}

	@Test
	public void setsEquipeInfoWhenParameterPresent() {
		Map<String, Info> info = runOnTestStart("Selenium Demo");

		Assert.assertEquals(info.get("Equipe").getInfo(), "Selenium Demo");
	}

	@Test
	public void doesNotSetEquipeInfoWhenParameterAbsent() {
		Map<String, Info> info = runOnTestStart(null);

		Assert.assertFalse(info.containsKey("Equipe"), "aucun parametre 'equipe' -> pas d'entree 'Equipe'");
	}

	@Test
	public void doesNotSetEquipeInfoWhenParameterBlank() {
		Map<String, Info> info = runOnTestStart("   ");

		Assert.assertFalse(info.containsKey("Equipe"), "parametre 'equipe' vide/blanc -> pas d'entree 'Equipe'");
	}

	@Test
	public void alwaysSetsCampagneInfoToCurrentMonthAndYearInFrench() {
		Map<String, Info> info = runOnTestStart(null);

		LocalDate today = LocalDate.now();
		String mois = today.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
		String expected = Character.toUpperCase(mois.charAt(0)) + mois.substring(1) + " " + today.getYear();

		Assert.assertEquals(info.get("Campagne").getInfo(), expected);
	}
}
