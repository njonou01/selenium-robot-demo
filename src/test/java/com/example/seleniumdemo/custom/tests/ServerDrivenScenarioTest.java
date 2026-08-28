package com.example.seleniumdemo.custom.tests;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.ITestContext;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.seleniumtests.core.runner.SeleniumTestPlan;

import com.example.seleniumdemo.custom.catalogue.WorkflowVariableScanner;
import com.example.seleniumdemo.custom.reporting.WorkflowRegistry;
import com.example.seleniumdemo.custom.reporting.WorkflowResult;
import com.example.seleniumdemo.custom.scenarios.ExcelScenarioSource;
import com.example.seleniumdemo.custom.scenarios.JsonScenarioSource;
import com.example.seleniumdemo.custom.scenarios.ScenarioDef;
import com.example.seleniumdemo.custom.scenarios.ScenarioSource;
import com.example.seleniumdemo.custom.server.VariableServerClient;
import com.example.seleniumdemo.custom.server.VariableServerConfig;
import com.example.seleniumdemo.workflows.FormWorkflow;
import com.example.seleniumdemo.workflows.SbcWorkflow;

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

	private void invoke(Map<Class<?>, Object> workflowInstances, String workflowCode, Method method, Map<String, Object> dataSet,
			Map<String, WorkflowResult> results) {
		Object[] args;
		if (method.getParameterCount() == 0) {
			args = new Object[0];
		} else {
			List<String> dataSetKeys = WorkflowVariableScanner.resolveDataSetKeys(method);
			Parameter[] parameters = method.getParameters();
			args = new Object[dataSetKeys.size()];
			for (int i = 0; i < dataSetKeys.size(); i++) {
				String paramName = dataSetKeys.get(i);
				Object rawValue = WorkflowVariableScanner.resolveRawValue(dataSet, workflowCode, paramName);
				if (rawValue == null) {
					throw new IllegalStateException("Le workflow '" + method.getDeclaringClass().getSimpleName() + "."
							+ method.getName() + "' attend le parametre '" + paramName + "', absent du 'dataSet' du scenario ("
							+ "dataSet disponible: " + (dataSet == null ? "{}" : dataSet.keySet()) + ").");
				}
				if (rawValue instanceof String text && WorkflowVariableScanner.isResultReference(text)) {
					rawValue = WorkflowVariableScanner.resolveResultReference(text, results);
				}
				args[i] = WorkflowVariableScanner.convertDataSetValue(rawValue, parameters[i]);
			}
		}

		Object instance = workflowInstance(workflowInstances, method.getDeclaringClass());
		Object returnValue;
		try {
			returnValue = method.invoke(instance, args);
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

		if (returnValue instanceof WorkflowResult workflowResult) {
			results.put(workflowCode, workflowResult);
		}
	}

	/**
	 * Verifie, pour chaque scenario, que chaque workflow reference existe et que son 'dataSet'
	 * couvre bien tous les parametres attendus - avant meme d'ouvrir un navigateur, plutot que de
	 * decouvrir un 'dataSet' incomplet apres 1-2 minutes d'execution Selenium. Les problemes sont
	 * rattaches au scenario concerne (via 'errorByScenarioName') plutot que de faire planter le
	 * chargement de TOUS les scenarios: un 'dataSet' casse dans le scenario A ne doit pas empecher
	 * le scenario B (correct) de s'executer.
	 */
	public static void validateDataSets(List<ScenarioDef> scenarios, Map<String, String> errorByScenarioName) throws Exception {
		Map<String, Method> registry = WorkflowRegistry.scanMethodsByCode();

		for (ScenarioDef scenario : scenarios) {
			if (scenario.parseError() != null) {
				errorByScenarioName.put(scenario.name(), scenario.parseError());
				continue;
			}

			List<String> problems = new ArrayList<>();
			List<String> steps = scenario.steps();
			for (int stepIndex = 0; stepIndex < steps.size(); stepIndex++) {
				String code = steps.get(stepIndex).trim();
				Method method = registry.get(code);
				if (method == null) {
					problems.add("code de workflow inconnu '" + code + "'.");
					continue;
				}
				if (method.getParameterCount() == 0) {
					continue;
				}
				List<String> dataSetKeys = WorkflowVariableScanner.resolveDataSetKeys(method);
				Parameter[] parameters = method.getParameters();
				for (int i = 0; i < dataSetKeys.size(); i++) {
					String paramName = dataSetKeys.get(i);
					Object rawValue = WorkflowVariableScanner.resolveRawValue(scenario.dataSet(), code, paramName);
					if (rawValue == null) {
						problems.add("le workflow '" + code + "' attend le parametre '" + paramName
								+ "', absent du 'dataSet' (dataSet disponible: "
								+ (scenario.dataSet() == null ? "{}" : scenario.dataSet().keySet()) + ").");
						continue;
					}
					try {
						if (rawValue instanceof String text && WorkflowVariableScanner.isResultReference(text)) {
							WorkflowVariableScanner.validateResultReference(text, steps, stepIndex, registry);
						} else {
							WorkflowVariableScanner.convertDataSetValue(rawValue, parameters[i]);
						}
					} catch (RuntimeException e) {
						problems.add("workflow '" + code + "', parametre '" + paramName + "' - " + e.getMessage());
					}
				}
			}

			if (!problems.isEmpty()) {
				errorByScenarioName.put(scenario.name(), "'dataSet' invalide pour le scenario '" + scenario.name()
						+ "', avant meme d'ouvrir un navigateur:\n" + String.join("\n", problems));
			}
		}
	}

	/**
	 * 'name' sert de cle dans toutes les maps indexees par scenario (stepsByScenarioName,
	 * dataSetByScenarioName, ...) et dans le nom du test TestNG genere par le DataProvider: un
	 * doublon ecraserait silencieusement le premier scenario par le second (meme 'name', meme
	 * cle) et TestNG afficherait 2 tests identiques dans le rapport, l'un d'eux executant en
	 * realite les donnees de l'autre. Fatal pour tout le fichier plutot qu'isole par scenario:
	 * ce n'est pas l'erreur d'UN scenario, c'est une relation entre au moins 2 d'entre eux.
	 */
	public static void requireUniqueNames(String variableName, List<ScenarioDef> scenarios) {
		Set<String> seen = new HashSet<>();
		for (ScenarioDef scenario : scenarios) {
			if (!seen.add(scenario.name())) {
				throw new IllegalStateException("Variable '" + variableName + "': le nom de scenario '" + scenario.name()
						+ "' apparait plusieurs fois. Chaque scenario doit avoir un nom unique - il sert de cle "
						+ "dans le rapport et dans l'execution.");
			}
		}
	}

	public static ScenarioSource resolveSource(String fileName) {
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
	private final Map<String, Boolean> sbcByScenarioName = new LinkedHashMap<>();
	private final Map<String, Map<String, Object>> dataSetByScenarioName = new LinkedHashMap<>();
	private final Map<String, String> errorByScenarioName = new LinkedHashMap<>();

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
		requireUniqueNames(variableName, scenarios);
		validateDataSets(scenarios, errorByScenarioName);
		Object[][] data = new Object[scenarios.size()][1];
		for (int i = 0; i < scenarios.size(); i++) {
			ScenarioDef scenario = scenarios.get(i);
			stepsByScenarioName.put(scenario.name(), scenario.steps());
			sinistresByScenarioName.put(scenario.name(), scenario.sinistre());
			sbcByScenarioName.put(scenario.name(), scenario.sbc());
			dataSetByScenarioName.put(scenario.name(), scenario.dataSet());
			data[i][0] = scenario.name();
		}
		return data;
	}

	@Test(dataProvider = "scenarios", testName = "${arg0}")
	public void testServerDrivenScenario(String scenarioName) throws Exception {
		String error = errorByScenarioName.get(scenarioName);
		if (error != null) {
			throw new IllegalStateException(error);
		}

		Map<String, Method> registry = WorkflowRegistry.scanMethodsByCode();
		Map<Class<?>, Object> workflowInstances = new LinkedHashMap<>();
		Map<String, WorkflowResult> results = new LinkedHashMap<>();

		Map<String, Object> dataSet = dataSetByScenarioName.get(scenarioName);

		if (Boolean.TRUE.equals(sbcByScenarioName.get(scenarioName))) {
			((SbcWorkflow) workflowInstance(workflowInstances, SbcWorkflow.class)).run();
		}

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

			invoke(workflowInstances, key.trim(), method, dataSet, results);
		}
	}
}
