package com.example.seleniumdemo.reporting;

import java.io.File;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.logging.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;

import com.seleniumtests.util.logging.SeleniumRobotLogger;

/**
 * Active/desactive les templates de rapport HTML custom, au choix via "-DcustomReport=true" ou
 * <parameter name="customReport" value="true" /> dans le testng.xml:
 * - report.test.vm : rapport detaille d'un test
 * - report.part.suiteSummary.vm : page resume (SeleniumTestReport.html)
 *
 * Le framework charge toujours ses templates via un chemin classpath fixe
 * (SeleniumTestsReporter2.java) - aucun parametre du framework ne permet de les rendre optionnels.
 * Les templates custom vivent donc hors de ces chemins
 * (src/test/resources/custom/reporter/templates/*.vm) pour ne JAMAIS les ecraser par defaut, et ne
 * sont copies a l'emplacement attendu qu'au demarrage de la suite, si demande.
 *
 * IMPORTANT: un "mvn test" (sans "clean") ne vide pas target/test-classes - un fichier copie par
 * un run precedent avec customReport=true y reste physiquement. Sans le bloc else ci-dessous, un
 * run suivant avec customReport=false reutiliserait silencieusement l'ancien template au lieu de
 * revenir a celui du framework. On supprime donc explicitement le fichier copie quand desactive,
 * pour que le flag soit fiable dans les deux sens peu importe l'etat du run precedent.
 */
public class CustomReportListener implements ISuiteListener {

	private static final Logger logger = SeleniumRobotLogger.getLogger(CustomReportListener.class);
	private static final String[] TEMPLATE_NAMES = { "report.test.vm", "report.part.suiteSummary.vm" };

	@Override
	public void onStart(ISuite suite) {
		String enabledParam = System.getProperty("customReport");
		if (enabledParam == null) {
			enabledParam = suite.getXmlSuite().getParameter("customReport");
		}
		boolean enabled = "true".equalsIgnoreCase(enabledParam);

		try {
			URL classesRoot = CustomReportListener.class.getResource("/");
			if (classesRoot == null) {
				throw new IllegalStateException("Racine du classpath introuvable");
			}
			File classesRootDir = new File(classesRoot.toURI());

			for (String templateName : TEMPLATE_NAMES) {
				File targetFile = new File(classesRootDir, "reporter/templates/" + templateName);

				if (enabled) {
					targetFile.getParentFile().mkdirs();
					try (InputStream in = CustomReportListener.class.getResourceAsStream("/custom/reporter/templates/" + templateName)) {
						if (in == null) {
							throw new IllegalStateException("Template custom introuvable sur le classpath: /custom/reporter/templates/" + templateName);
						}
						Files.copy(in, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
					logger.info("Rapport custom active: " + targetFile);
				} else if (targetFile.exists()) {
					Files.delete(targetFile.toPath());
					logger.info("Rapport custom desactive, template framework restaure: " + targetFile);
				}
			}
		} catch (URISyntaxException | RuntimeException | java.io.IOException e) {
			throw new IllegalStateException("Impossible de (des)activer le rapport custom", e);
		}
	}
}
