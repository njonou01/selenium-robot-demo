package com.example.seleniumdemo.unitaire;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.seleniumdemo.custom.scenarios.JsonScenarioSource;
import com.example.seleniumdemo.custom.scenarios.ScenarioDef;
import com.example.seleniumdemo.custom.tests.ServerDrivenScenarioTest;

/**
 * Verifie les 2 controles a sec (avant navigateur) de 'ServerDrivenScenarioTest' au niveau ou
 * ils orchestrent vraiment ensemble ('validateDataSets' + 'WorkflowVariableScanner' + le
 * registre reel des workflows) - pas juste chaque brique isolee.
 */
public class ServerDrivenScenarioValidationTest {

	private List<ScenarioDef> parse(String json) {
		return new JsonScenarioSource().parse(json.getBytes(StandardCharsets.UTF_8));
	}

	@Test(expectedExceptions = IllegalStateException.class,
			expectedExceptionsMessageRegExp = ".*apparait plusieurs fois.*")
	public void rejectsDuplicateScenarioNames() {
		List<ScenarioDef> scenarios = parse(
				"[{\"name\":\"Meme Nom\",\"sinistre\":\"S1\",\"sbc\":false,\"steps\":[]},"
						+ "{\"name\":\"Meme Nom\",\"sinistre\":\"S2\",\"sbc\":false,\"steps\":[]}]");

		ServerDrivenScenarioTest.requireUniqueNames("workflow.scenarios", scenarios);
	}

	@Test
	public void acceptsDistinctScenarioNames() {
		List<ScenarioDef> scenarios = parse(
				"[{\"name\":\"A\",\"sinistre\":\"S1\",\"sbc\":false,\"steps\":[]},"
						+ "{\"name\":\"B\",\"sinistre\":\"S2\",\"sbc\":false,\"steps\":[]}]");

		ServerDrivenScenarioTest.requireUniqueNames("workflow.scenarios", scenarios);
	}

	@Test
	public void validateDataSetsFlagsMissingParameterBeforeAnyBrowserOpens() throws Exception {
		List<ScenarioDef> scenarios = parse(
				"[{\"name\":\"Sans Employee\",\"sinistre\":\"S1\",\"sbc\":false,\"steps\":[\"hr.full\"],\"dataSet\":{}}]");
		Map<String, String> errors = new LinkedHashMap<>();

		ServerDrivenScenarioTest.validateDataSets(scenarios, errors);

		Assert.assertTrue(errors.containsKey("Sans Employee"));
		Assert.assertTrue(errors.get("Sans Employee").contains("employee"));
	}

	@Test
	public void validateDataSetsFlagsResultReferenceToWorkflowThatRunsLater() throws Exception {
		List<ScenarioDef> scenarios = parse(
				"[{\"name\":\"Mauvais Ordre\",\"sinistre\":\"S1\",\"sbc\":false,"
						+ "\"steps\":[\"hr.full\",\"banking.full\"],"
						+ "\"dataSet\":{\"employee\":{\"firstName\":\"A\",\"lastName\":\"B\"},"
						+ "\"accountNumber\":\"${result:banking.full.accountNumber}\"}}]");
		Map<String, String> errors = new LinkedHashMap<>();

		ServerDrivenScenarioTest.validateDataSets(scenarios, errors);

		Assert.assertTrue(errors.containsKey("Mauvais Ordre"));
		Assert.assertTrue(errors.get("Mauvais Ordre").contains("AVANT"));
	}

	@Test
	public void validateDataSetsAcceptsValidChainedScenario() throws Exception {
		List<ScenarioDef> scenarios = parse(
				"[{\"name\":\"Bon Ordre\",\"sinistre\":\"S1\",\"sbc\":false,"
						+ "\"steps\":[\"banking.full\",\"hr.full\"],"
						+ "\"dataSet\":{\"employee\":{\"firstName\":\"A\",\"lastName\":\"B\"},"
						+ "\"accountNumber\":\"${result:banking.full.accountNumber}\"}}]");
		Map<String, String> errors = new LinkedHashMap<>();

		ServerDrivenScenarioTest.validateDataSets(scenarios, errors);

		Assert.assertTrue(errors.isEmpty(), "scenario valide, aucune erreur attendue: " + errors);
	}

	@Test
	public void parseErrorOnAScenarioIsIsolatedFromOtherScenarios() throws Exception {
		List<ScenarioDef> scenarios = parse(
				"[{\"name\":\"Casse\",\"sinistre\":\"S1\",\"sbc\":false,\"steps\":\"pas-un-tableau\"},"
						+ "{\"name\":\"OK\",\"sinistre\":\"S2\",\"sbc\":false,\"steps\":[]}]");
		Map<String, String> errors = new LinkedHashMap<>();

		ServerDrivenScenarioTest.validateDataSets(scenarios, errors);

		Assert.assertTrue(errors.containsKey("Casse"));
		Assert.assertFalse(errors.containsKey("OK"));
	}

	@Test
	public void unknownWorkflowCodeIsReportedPerScenario() throws Exception {
		List<ScenarioDef> scenarios = parse(
				"[{\"name\":\"Code Inconnu\",\"sinistre\":\"S1\",\"sbc\":false,\"steps\":[\"ne.existe.pas\"]}]");
		Map<String, String> errors = new LinkedHashMap<>();

		ServerDrivenScenarioTest.validateDataSets(scenarios, errors);

		Assert.assertTrue(errors.get("Code Inconnu").contains("inconnu"));
	}
}
