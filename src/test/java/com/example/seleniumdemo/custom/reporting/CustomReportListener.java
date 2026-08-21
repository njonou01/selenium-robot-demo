package com.example.seleniumdemo.custom.reporting;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.logging.log4j.Logger;
import org.testng.ISuite;
import org.testng.ISuiteListener;

import com.seleniumtests.util.logging.SeleniumRobotLogger;

public class CustomReportListener implements ISuiteListener {

	private static final Logger logger = SeleniumRobotLogger.getLogger(CustomReportListener.class);
	private static final String[] TEMPLATE_NAMES = { "report.test.vm", "report.part.suiteSummary.vm", "fonts.part.vm" };

	@Override
	public void onStart(ISuite suite) {
		String enabledParam = System.getProperty("customReport");
		if (enabledParam == null) {
			enabledParam = suite.getXmlSuite().getParameter("customReport");
		}
		boolean enabled = "true".equalsIgnoreCase(enabledParam);

		if (!enabled) {
			return;
		}

		try {
			Path tempDir = Files.createTempDirectory("seleniumdemo-custom-report-");
			Path templatesDir = tempDir.resolve("reporter/templates");
			Files.createDirectories(templatesDir);

			for (String templateName : TEMPLATE_NAMES) {
				try (InputStream in = CustomReportListener.class.getResourceAsStream("/custom/reporter/templates/" + templateName)) {
					if (in == null) {
						throw new IllegalStateException("Template custom introuvable sur le classpath: /custom/reporter/templates/" + templateName);
					}
					Files.copy(in, templatesDir.resolve(templateName), StandardCopyOption.REPLACE_EXISTING);
				}
			}

			// Le framework (Velocity/ClasspathResourceLoader) resout ses templates via
			// Thread.currentThread().getContextClassLoader().getResourceAsStream(...) en premier,
			// avant de retomber sur le jar. On remplace ce classloader par un qui regarde d'abord
			// notre dossier temporaire (toujours un vrai dossier disque, jar ou pas) avant de
			// deleguer au classloader d'origine. Aucune ecriture dans le jar, aucun "new File(uri)"
			// sur une URL jar: - donc ca marche identiquement en exploded et en jar packagee.
			//
			// Volontairement jamais restaure (pas de onFinish qui remet l'ancien classloader):
			// le rapport HTML est genere en plusieurs passes par le framework (au moins une passe
			// intermediaire puis une finale), et la derniere passe - celle qui compte, ecrite en
			// dernier sur le disque - peut arriver apres n'importe quel hook de fin de suite. Un
			// classloader "delegue au parent" ne change rien au comportement du reste du programme.
			ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
			ClassLoader overrideClassLoader = new ChildFirstClassLoader(
					new URL[] { tempDir.toUri().toURL() }, originalClassLoader);
			Thread.currentThread().setContextClassLoader(overrideClassLoader);

			logger.info("Rapport custom active depuis: {}", templatesDir);
		} catch (IOException | RuntimeException e) {
			throw new IllegalStateException("Impossible d'activer le rapport custom", e);
		}
	}

	/**
	 * Java delegue par defaut au classloader parent en premier. On inverse l'ordre pour ce
	 * classloader precis: on cherche d'abord dans nos propres URLs (le dossier temporaire des
	 * templates custom), et seulement si rien n'est trouve on retombe sur le parent (le vrai
	 * classloader de l'application, jar ou exploded).
	 */
	private static final class ChildFirstClassLoader extends URLClassLoader {

		ChildFirstClassLoader(URL[] urls, ClassLoader parent) {
			super(urls, parent);
		}

		@Override
		public URL getResource(String name) {
			URL own = findResource(name);
			return own != null ? own : super.getResource(name);
		}
	}
}
