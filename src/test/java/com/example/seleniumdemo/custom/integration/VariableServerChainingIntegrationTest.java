package com.example.seleniumdemo.custom.integration;

import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.seleniumdemo.custom.scenarios.ScenarioDef;
import com.example.seleniumdemo.custom.server.VariableServerClient;
import com.example.seleniumdemo.custom.server.VariableServerConfig;
import com.example.seleniumdemo.custom.tests.ServerDrivenScenarioTest;

/**
 * Integration reelle avec le serveur de variable (HTTP, pas de bouchon): a besoin d'un serveur
 * seleniumRobot-server qui tourne sur 'seleniumRobotServerUrl' (localhost:8000 par defaut) avec
 * l'application 'seleniumdemo' / version '1.0' / environnement 'DEV' deja enregistres, du token
 * dans 'SELENIUM_ROBOT_SERVER_TOKEN', et la variable 'workflow.scenarios' deja uploadee (JSON ou
 * Excel, detecte a l'extension comme en production). Pas de navigateur ici - seulement la couche
 * HTTP + parsing + resolution alias/chainage, jusqu'a la validation a sec (avant ouverture d'un
 * navigateur). La preuve navigateur reelle (chainage effectivement recu et logue cote HRWorkflow,
 * en JSON et en Excel) a ete faite manuellement, voir le bilan
 * de session.
 */
public class VariableServerChainingIntegrationTest {

	private static final String SERVER_URL = "http://localhost:8000";
	private static final String APPLICATION = "seleniumdemo";
	private static final String VERSION = "1.0";
	private static final String ENVIRONMENT = "DEV";

	private VariableServerClient client() {
		String token = System.getenv("SELENIUM_ROBOT_SERVER_TOKEN");
		VariableServerConfig config = new VariableServerConfig(SERVER_URL, token, APPLICATION, VERSION, ENVIRONMENT);
		return new VariableServerClient(config);
	}

	@Test
	public void fetchesAndParsesRealScenarioVariableFromRunningServer() {
		VariableServerClient.FetchedVariable payload = client().fetch("workflow.scenarios");

		Assert.assertNotNull(payload.content());
		Assert.assertTrue(payload.content().length > 0);

		List<ScenarioDef> scenarios = ServerDrivenScenarioTest.resolveSource(payload.fileName()).parse(payload.content());

		Assert.assertFalse(scenarios.isEmpty(), "le serveur doit renvoyer au moins un scenario");
		for (ScenarioDef scenario : scenarios) {
			Assert.assertNull(scenario.parseError(), "scenario '" + scenario.name() + "' casse: " + scenario.parseError());
		}
	}

	/**
	 * Reproduit, contre le vrai serveur, exactement ce que 'ServerDrivenScenarioTest' fait
	 * avant d'ouvrir un navigateur: fetch + parse + noms uniques + dataSet valide (y compris
	 * les references '${result:...}' de chainage, resolues d'apres l'ordre reel de 'steps').
	 */
	@Test
	public void realUploadedScenarioPassesFullPreFlightValidation() throws Exception {
		VariableServerClient.FetchedVariable payload = client().fetch("workflow.scenarios");
		List<ScenarioDef> scenarios = ServerDrivenScenarioTest.resolveSource(payload.fileName()).parse(payload.content());

		ServerDrivenScenarioTest.requireUniqueNames("workflow.scenarios", scenarios);

		Map<String, String> errors = new java.util.LinkedHashMap<>();
		ServerDrivenScenarioTest.validateDataSets(scenarios, errors);

		Assert.assertTrue(errors.isEmpty(), "le(s) scenario(s) reel(s) sur le serveur devraient passer la validation a sec: " + errors);
	}
}
