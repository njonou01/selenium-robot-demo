package com.example.seleniumdemo.tests;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seleniumtests.core.SeleniumTestsContext;
import com.seleniumtests.core.SeleniumTestsContextManager;
import com.seleniumtests.core.runner.SeleniumTestPlan;

import com.example.seleniumdemo.reporting.WorkflowRegistry;

/**
 * Les scenarios a jouer sont pilotes depuis la variable serveur "workflow.scenarios" (JSON
 * [{"name": "...", "chain": "code1,code2,..."}]), pas depuis ce code Java. Chaque entree devient
 * une execution TestNG distincte, nommee dans le rapport via un DataProvider. Les codes de chaine
 * sont resolus par reflexion sur @Workflow(code=...) via WorkflowRegistry: ajouter/retirer un code
 * se fait dans la classe Workflow concernee, jamais ici.
 *
 * Un DataProvider s'execute avant que TestNG ne cree le ITestResult qui declenche la connexion
 * standard au serveur de variable: ce fichier appelle donc directement l'API REST, en lisant
 * l'URL/le token depuis les parametres XML (disponibles immediatement).
 */
public class VariableDrivenScenarioTest extends SeleniumTestPlan {

	private static final String VARIABLE_NAME = "workflow.scenarios";

	private Object workflowInstance(Map<Class<?>, Object> workflowInstances, Class<?> clazz) {
		return workflowInstances.computeIfAbsent(clazz, c -> {
			try {
				return c.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException("Impossible de demarrer " + c.getSimpleName(), e);
			}
		});
	}

	private void invoke(Map<Class<?>, Object> workflowInstances, Method method) {
		Object instance = workflowInstance(workflowInstances, method.getDeclaringClass());
		try {
			method.invoke(instance);
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			if (e.getCause() instanceof Error error) {
				throw error;
			}
			throw new RuntimeException(e.getCause());
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private String fetchScenariosJson(ITestContext testContext) throws Exception {
		String baseUrl = testContext.getCurrentXmlTest().getParameter("seleniumRobotServerUrl");

		// meme ordre de priorite que SeleniumRobotServerContext.init() du framework: variable d'env
		// d'abord, ecrasee si un token est configure explicitement (xml ou system property).
		String token = System.getenv("SELENIUM_ROBOT_SERVER_TOKEN");
		String tokenFromConfig = testContext.getCurrentXmlTest().getParameter("seleniumRobotServerToken");
		if (tokenFromConfig == null) {
			tokenFromConfig = System.getProperty("seleniumRobotServerToken");
		}
		if (tokenFromConfig != null) {
			token = tokenFromConfig;
		}

		if (baseUrl == null || token == null) {
			throw new IllegalStateException(
					"'seleniumRobotServerUrl' absent du testng.xml, ou aucun token trouve "
							+ "(ni SELENIUM_ROBOT_SERVER_TOKEN, ni parametre 'seleniumRobotServerToken').");
		}

		String application = SeleniumTestsContextManager.getApplicationName();
		String environment = testContext.getCurrentXmlTest().getParameter("env");
		if (environment == null) {
			environment = SeleniumTestsContext.DEFAULT_TEST_ENV;
		}

		String version = SeleniumTestsContextManager.getApplicationVersion();
		String url = baseUrl + "/variable/api/variable/?application=" + application + "&version=" + version
				+ "&environment=" + environment + "&name=" + VARIABLE_NAME;
		try (HttpClient client = HttpClient.newHttpClient()) {
			HttpRequest request = HttpRequest.newBuilder()
					.uri(URI.create(url))
					.header("Authorization", "Token " + token)
					.GET()
					.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new IllegalStateException(
						"Echec recuperation de '" + VARIABLE_NAME + "' (HTTP " + response.statusCode() + "): " + response.body());
			}

			for (JsonNode node : new ObjectMapper().readTree(response.body())) {
				if (!VARIABLE_NAME.equals(node.path("name").asText())) {
					continue;
				}
				// la variable peut etre un texte inline ("value") ou un fichier uploade ("uploadFile"):
				// dans ce dernier cas, on telecharge le contenu du fichier depuis l'API.
				if (!node.path("uploadFile").isMissingNode() && !node.path("uploadFile").isNull()
						&& !node.path("uploadFile").asText().isBlank()) {
					String fileUrl = baseUrl + String.format("/variable/api/variable/%d/file", node.path("id").asInt());
					HttpRequest fileRequest = HttpRequest.newBuilder()
							.uri(URI.create(fileUrl))
							.header("Authorization", "Token " + token)
							.GET()
							.build();
					HttpResponse<String> fileResponse = client.send(fileRequest, HttpResponse.BodyHandlers.ofString());
					if (fileResponse.statusCode() != 200) {
						throw new IllegalStateException(
								"Echec telechargement du fichier '" + VARIABLE_NAME + "' (HTTP " + fileResponse.statusCode() + ")");
					}
					return fileResponse.body();
				}
				return node.path("value").asText();
			}
			throw new IllegalStateException("Variable '" + VARIABLE_NAME + "' absente du serveur de variable.");
		}
	}

	// rempli par le DataProvider, relu par le test: evite de passer "chainValue" comme 2e parametre de
	// methode, ce qui ferait apparaitre la chaine de codes (bruit) a cote du nom dans le rapport
	// ("with params: (...)" liste tous les arguments bruts de la methode, quel que soit testName).
	private final Map<String, String> chainsByScenarioName = new LinkedHashMap<>();

	@DataProvider(name = "scenarios")
	public Object[][] scenarios(ITestContext testContext) throws Exception {
		String json = fetchScenariosJson(testContext);
		if (json == null || json.isBlank()) {
			throw new IllegalStateException("Variable '" + VARIABLE_NAME + "' vide sur le serveur de variable.");
		}
		List<Map<String, String>> raw = new ObjectMapper().readValue(json, new TypeReference<List<Map<String, String>>>() {
		});
		Object[][] data = new Object[raw.size()][1];
		for (int i = 0; i < raw.size(); i++) {
			String name = raw.get(i).get("name");
			chainsByScenarioName.put(name, raw.get(i).get("chain"));
			data[i][0] = name;
		}
		return data;
	}

	@Test(dataProvider = "scenarios", testName = "${arg0}")
	public void testServerDrivenScenario(String scenarioName) throws Exception {
		Map<String, Method> registry = WorkflowRegistry.scanMethodsByCode();
		// cree localement a chaque execution de la methode: un retry TestNG reutilise la meme instance
		// de classe de test, donc un cache en champ d'instance survivrait a la fermeture du driver
		// precedent et invoquerait des workflows lies a un driver deja mort.
		Map<Class<?>, Object> workflowInstances = new LinkedHashMap<>();

		String chainValue = chainsByScenarioName.get(scenarioName);
		if (chainValue == null || chainValue.isBlank()) {
			throw new IllegalStateException("Scenario '" + scenarioName + "': chaine de workflow vide.");
		}

		for (String rawKey : chainValue.split(",")) {
			String key = rawKey.trim();
			Method method = registry.get(key);
			if (method == null) {
				throw new IllegalArgumentException(
						"Scenario '" + scenarioName + "': code de workflow inconnu '" + key + "'. Codes disponibles: "
								+ String.join(", ", registry.keySet()));
			}

			invoke(workflowInstances, method);
		}
	}
}
