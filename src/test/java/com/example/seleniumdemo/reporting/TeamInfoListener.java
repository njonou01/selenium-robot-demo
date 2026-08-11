package com.example.seleniumdemo.reporting;

import org.testng.ITestListener;
import org.testng.ITestResult;

import com.seleniumtests.core.utils.TestNGResultUtils;
import com.seleniumtests.reporter.info.StringInfo;

/**
 * Ajoute le nom de l'equipe (parametre testng.xml "equipe") comme info affichee sur chaque
 * rapport de test - permet de savoir qui est proprietaire d'un test sans ouvrir le code.
 *
 * Passe par TestNGResultUtils.setTestInfo() (meme mecanisme que BugTrackerReporter.java du
 * framework) plutot que par le Velocity context directement: aucune variable custom n'est
 * exposee au template par le framework, "testInfos" est le seul point d'extension prevu pour
 * afficher une donnee supplementaire sans toucher au reporter Java.
 */
public class TeamInfoListener implements ITestListener {

	// pas d'accent volontairement: le framework HTML-encode les cles de testInfos ("Équipe" devient
	// litteralement "&Eacute;quipe" dans le rapport genere), ce qui casse toute comparaison exacte
	// cote template Velocity. Cle ASCII = fiable ; le libelle affiche reste "Équipe" en dur dans le html.
	private static final String INFO_KEY = "Equipe";

	@Override
	public void onTestStart(ITestResult result) {
		String equipe = result.getTestContext().getCurrentXmlTest().getParameter("equipe");
		if (equipe == null || equipe.isBlank()) {
			return;
		}
		TestNGResultUtils.setTestInfo(result, INFO_KEY, new StringInfo(equipe));
	}
}
