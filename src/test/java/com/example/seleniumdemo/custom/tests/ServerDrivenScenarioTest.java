package com.example.seleniumdemo.custom.tests;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.seleniumtests.core.runner.SeleniumTestPlan;

import com.example.seleniumdemo.custom.reporting.WorkflowRegistry;
import com.example.seleniumdemo.custom.scenarios.ExcelScenarioSource;
import com.example.seleniumdemo.custom.scenarios.JsonScenarioSource;
import com.example.seleniumdemo.custom.scenarios.ScenarioDef;
import com.example.seleniumdemo.custom.scenarios.ScenarioSource;
import com.example.seleniumdemo.custom.server.VariableServerClient;
import com.example.seleniumdemo.custom.server.VariableServerConfig;
import com.example.seleniumdemo.workflows.FormWorkflow;

public class ServerDrivenScenarioTest extends SeleniumTestPlan {

	private static final String DEFAULT_VARIABLE_NAME = "workflow.scenarios";

	private String variableName = DEFAULT_VARIABLE_NAME;

	private String resolveVariableName() {
		String name = System.getProperty("scenarios");
		return name == null || name.isBlank() ? DEFAULT_VARIABLE_NAME : name;
	}

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
		if (method.getParameterCount() != 0) {
			throw new IllegalStateException("Le workflow '" + method.getDeclaringClass().getSimpleName() + "."
					+ method.getName() + "' attend " + method.getParameterCount()
					+ " parametre(s): non supporte par l'enchainement JSON actuel (\"steps\"), qui n'invoque"
					+ " que des methodes sans argument.");
		}

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

	private ScenarioSource resolveSource(String fileName) {
		if (fileName != null) {
			String lower = fileName.toLowerCase();
			if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
				return new ExcelScenarioSource();
			}
		}
		return new JsonScenarioSource();
	}

	private VariableServerClient.FetchedVariable fetchScenariosContent(ITestContext testContext) {
		VariableServerConfig config = VariableServerConfig.fromXmlTest(testContext);
		if (!config.isComplete()) {
			throw new IllegalStateException(
					"'seleniumRobotServerUrl' absent du testng.xml, ou aucun token trouve "
							+ "(ni SELENIUM_ROBOT_SERVER_TOKEN, ni parametre 'seleniumRobotServerToken').");
		}
		return new VariableServerClient(config).fetch(variableName);
	}

	private final Map<String, List<String>> stepsByScenarioName = new LinkedHashMap<>();
	private final Map<String, String> sinistresByScenarioName = new LinkedHashMap<>();

	@DataProvider(name = "scenarios")
	public Object[][] scenarios(ITestContext testContext) throws Exception {
		variableName = resolveVariableName();
		VariableServerClient.FetchedVariable payload = fetchScenariosContent(testContext);
		boolean blank = payload.content() == null || payload.content().length == 0
				|| (payload.fileName() == null && new String(payload.content(), StandardCharsets.UTF_8).isBlank());
		if (blank) {
			throw new IllegalStateException("Variable '" + variableName + "' vide sur le serveur de variable.");
		}
		List<ScenarioDef> scenarios = resolveSource(payload.fileName()).parse(payload.content());
		if (scenarios.isEmpty()) {
			throw new IllegalStateException("Variable '" + variableName + "' ne contient aucun scenario.");
		}
		Object[][] data = new Object[scenarios.size()][1];
		for (int i = 0; i < scenarios.size(); i++) {
			ScenarioDef scenario = scenarios.get(i);
			stepsByScenarioName.put(scenario.name(), scenario.steps());
			sinistresByScenarioName.put(scenario.name(), scenario.sinistre());
			data[i][0] = scenario.name();
		}
		return data;
	}

	@Test(dataProvider = "scenarios", testName = "${arg0}")
	public void testServerDrivenScenario(String scenarioName) throws Exception {
		Map<String, Method> registry = WorkflowRegistry.scanMethodsByCode();
		Map<Class<?>, Object> workflowInstances = new LinkedHashMap<>();

		String sinistre = sinistresByScenarioName.get(scenarioName);
		if (sinistre == null || sinistre.isBlank()) {
			throw new IllegalStateException("Scenario '" + scenarioName + "': champ 'sinistre' vide.");
		}
		((FormWorkflow) workflowInstance(workflowInstances, FormWorkflow.class)).processDeclareSinistre(sinistre);

		List<String> steps = stepsByScenarioName.get(scenarioName);
		if (steps == null || steps.isEmpty()) {
			throw new IllegalStateException("Scenario '" + scenarioName + "': liste de workflow vide.");
		}

		for (String key : steps) {
			Method method = registry.get(key.trim());
			if (method == null) {
				throw new IllegalArgumentException(
						"Scenario '" + scenarioName + "': code de workflow inconnu '" + key + "'. Codes disponibles: "
								+ String.join(", ", registry.keySet()));
			}

			invoke(workflowInstances, method);
		}
	}
}
