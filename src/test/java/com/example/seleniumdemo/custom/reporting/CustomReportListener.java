package com.example.seleniumdemo.custom.reporting;

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
					logger.info("Rapport custom active: {}", targetFile);
				} else if (targetFile.exists()) {
					Files.delete(targetFile.toPath());
					logger.info("Rapport custom désactivé, template framework restauré: {}", targetFile);
				}
			}
		} catch (URISyntaxException | RuntimeException | java.io.IOException e) {
			throw new IllegalStateException("Impossible de (des)activer le rapport custom", e);
		}
	}
}
