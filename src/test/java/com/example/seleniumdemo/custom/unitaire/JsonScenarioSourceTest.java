package com.example.seleniumdemo.custom.unitaire;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.seleniumdemo.custom.scenarios.JsonScenarioSource;
import com.example.seleniumdemo.custom.scenarios.ScenarioDef;

public class JsonScenarioSourceTest {

	private List<ScenarioDef> parse(String json) {
		return new JsonScenarioSource().parse(json.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void parsesBasicScenario() {
		String json = "[{\"name\":\"S1\",\"sinistre\":\"REF1\",\"sbc\":true,"
				+ "\"steps\":[\"ecommerce.full\"],\"dataSet\":{\"foo\":\"bar\"}}]";

		ScenarioDef scenario = parse(json).get(0);

		Assert.assertNull(scenario.parseError());
		Assert.assertEquals(scenario.name(), "S1");
		Assert.assertEquals(scenario.sinistre(), "REF1");
		Assert.assertTrue(scenario.sbc());
		Assert.assertEquals(scenario.steps(), List.of("ecommerce.full"));
		Assert.assertEquals(scenario.dataSet().get("foo"), "bar");
	}

	@Test
	public void missingDataSetDefaultsToEmptyMap() {
		String json = "[{\"name\":\"S1\",\"sinistre\":\"REF1\",\"sbc\":false,\"steps\":[]}]";

		ScenarioDef scenario = parse(json).get(0);

		Assert.assertNull(scenario.parseError());
		Assert.assertTrue(scenario.dataSet().isEmpty());
	}

	@Test
	public void blankNameFallsBackToPositionalLabel() {
		String json = "[{\"sinistre\":\"REF1\",\"sbc\":false,\"steps\":[]}]";

		ScenarioDef scenario = parse(json).get(0);

		Assert.assertEquals(scenario.name(), "Scenario JSON #1");
	}

	/**
	 * Coeur de l'isolation d'erreur par scenario: un scenario mal forme (ici 'steps' n'est pas
	 * un tableau) ne doit pas empecher le suivant, valide, d'etre parse normalement.
	 */
	@Test
	public void malformedEntryIsIsolatedNotFatalToSiblings() {
		String json = "["
				+ "{\"name\":\"Casse\",\"sinistre\":\"REF1\",\"sbc\":false,\"steps\":\"pas-un-tableau\"},"
				+ "{\"name\":\"OK\",\"sinistre\":\"REF2\",\"sbc\":false,\"steps\":[\"hr.full\"]}"
				+ "]";

		List<ScenarioDef> scenarios = parse(json);

		Assert.assertEquals(scenarios.size(), 2);
		Assert.assertNotNull(scenarios.get(0).parseError(), "le scenario casse doit porter une erreur");
		Assert.assertTrue(scenarios.get(0).parseError().contains("Casse"));
		Assert.assertNull(scenarios.get(1).parseError(), "le scenario valide ne doit pas etre affecte");
		Assert.assertEquals(scenarios.get(1).steps(), List.of("hr.full"));
	}

	@Test(expectedExceptions = IllegalStateException.class)
	public void globallyInvalidJsonFailsFast() {
		parse("{ceci n'est pas un tableau JSON valide");
	}

	@Test
	public void nestedRecordAndListDataSetValuesArePreserved() {
		String json = "[{\"name\":\"S1\",\"sinistre\":\"REF1\",\"sbc\":false,\"steps\":[],"
				+ "\"dataSet\":{\"employee\":{\"firstName\":\"Robert\",\"lastName\":\"Wilson\"},"
				+ "\"produits\":[\"A\",\"B\",\"C\"]}}]";

		ScenarioDef scenario = parse(json).get(0);

		@SuppressWarnings("unchecked")
		var employee = (java.util.Map<String, Object>) scenario.dataSet().get("employee");
		Assert.assertEquals(employee.get("firstName"), "Robert");
		Assert.assertEquals(scenario.dataSet().get("produits"), List.of("A", "B", "C"));
	}
}
