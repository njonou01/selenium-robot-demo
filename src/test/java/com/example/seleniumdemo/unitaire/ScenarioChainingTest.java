package com.example.seleniumdemo.unitaire;

import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.seleniumdemo.custom.catalogue.WorkflowVariableScanner;
import com.example.seleniumdemo.custom.reporting.WorkflowResult;
import com.example.seleniumdemo.custom.scenarios.ExcelScenarioSource;
import com.example.seleniumdemo.custom.scenarios.JsonScenarioSource;
import com.example.seleniumdemo.custom.scenarios.ScenarioDef;
import com.example.seleniumdemo.workflows.BankingResult;
import com.example.seleniumdemo.workflows.BankingWorkflow;
import com.example.seleniumdemo.workflows.Employee;
import com.example.seleniumdemo.workflows.HRWorkflow;

/**
 * Verifie, sans serveur de variable ni navigateur (parsing/reflexion pur, deterministe), le
 * chainage de resultats ('${result:code.champ}') et les alias de scenario ('aliases').
 */
public class ScenarioChainingTest {

	@Test
	public void aliasesResolveStepsAndResultReferences() {
		String json = "[{\"name\":\"T1\",\"sinistre\":\"S1\",\"sbc\":false,"
				+ "\"aliases\":{\"b\":\"banking.full\",\"h\":\"hr.full\"},"
				+ "\"steps\":[\"b\",\"h\"],"
				+ "\"dataSet\":{\"employee\":{\"firstName\":\"Robert\",\"lastName\":\"Wilson\"},"
				+ "\"accountNumber\":\"${result:b.accountNumber}\"}}]";

		List<ScenarioDef> scenarios = new JsonScenarioSource().parse(json.getBytes(StandardCharsets.UTF_8));

		Assert.assertEquals(scenarios.size(), 1);
		ScenarioDef scenario = scenarios.get(0);
		Assert.assertNull(scenario.parseError(), "scenario ne doit pas etre en erreur: " + scenario.parseError());
		Assert.assertEquals(scenario.steps(), List.of("banking.full", "hr.full"),
				"'b'/'h' doivent etre resolus vers les vrais codes dans 'steps'");
		Assert.assertEquals(scenario.dataSet().get("accountNumber"), "${result:banking.full.accountNumber}",
				"l'alias 'b' dans '${result:b.accountNumber}' doit etre resolu vers 'banking.full'");
	}

	@Test
	public void aliasAppliesToTopLevelDataSetKeyButNotToNestedFields() {
		String json = "[{\"name\":\"T2\",\"sinistre\":\"S1\",\"sbc\":false,"
				+ "\"aliases\":{\"h\":\"hr.full\"},"
				+ "\"steps\":[\"h\"],"
				+ "\"dataSet\":{\"h\":{\"employee\":{\"firstName\":\"A\",\"lastName\":\"B\"}}}}]";

		ScenarioDef scenario = new JsonScenarioSource().parse(json.getBytes(StandardCharsets.UTF_8)).get(0);

		Assert.assertNull(scenario.parseError());
		Assert.assertTrue(scenario.dataSet().containsKey("hr.full"), "cle top-niveau 'h' doit devenir 'hr.full'");
		Assert.assertFalse(scenario.dataSet().containsKey("h"), "'h' brut ne doit plus apparaitre apres resolution");
		@SuppressWarnings("unchecked")
		Map<String, Object> hrScope = (Map<String, Object>) scenario.dataSet().get("hr.full");
		Assert.assertTrue(hrScope.containsKey("employee"), "champ imbrique doit rester intact");
	}

	@Test
	public void undeclaredAliasIsLeftLiteralNotSubstituted() {
		String json = "[{\"name\":\"T3\",\"sinistre\":\"S1\",\"sbc\":false,"
				+ "\"steps\":[\"x\"],\"dataSet\":{}}]";

		ScenarioDef scenario = new JsonScenarioSource().parse(json.getBytes(StandardCharsets.UTF_8)).get(0);

		Assert.assertEquals(scenario.steps(), List.of("x"), "sans 'aliases' declares, aucune substitution");
	}

	@Test
	public void resolveResultReferenceReadsRecordField() {
		Map<String, WorkflowResult> results = new LinkedHashMap<>();
		results.put("banking.full", new BankingResult("12345"));

		Object value = WorkflowVariableScanner.resolveResultReference("${result:banking.full.accountNumber}", results);

		Assert.assertEquals(value, "12345");
	}

	@Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*n'a pas encore ete execute.*")
	public void resolveResultReferenceFailsWhenSourceWorkflowNotYetExecuted() {
		WorkflowVariableScanner.resolveResultReference("${result:banking.full.accountNumber}", new LinkedHashMap<>());
	}

	@Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*n'a pas de champ.*")
	public void resolveResultReferenceFailsOnUnknownField() {
		Map<String, WorkflowResult> results = new LinkedHashMap<>();
		results.put("banking.full", new BankingResult("12345"));

		WorkflowVariableScanner.resolveResultReference("${result:banking.full.bogusField}", results);
	}

	@Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*doit apparaitre AVANT.*")
	public void validateResultReferenceRejectsForwardReference() throws NoSuchMethodException {
		Map<String, Method> registry = registry();
		List<String> steps = List.of("hr.full", "banking.full");

		WorkflowVariableScanner.validateResultReference("${result:banking.full.accountNumber}", steps, 0, registry);
	}

	@Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = ".*ne renvoie pas de WorkflowResult.*")
	public void validateResultReferenceRejectsNonWorkflowResultSource() throws NoSuchMethodException {
		Map<String, Method> registry = registry();
		List<String> steps = List.of("hr.full", "banking.full");

		WorkflowVariableScanner.validateResultReference("${result:hr.full.employee}", steps, 1, registry);
	}

	@Test
	public void validateResultReferenceAcceptsValidChain() throws NoSuchMethodException {
		Map<String, Method> registry = registry();
		List<String> steps = List.of("banking.full", "hr.full");

		WorkflowVariableScanner.validateResultReference("${result:banking.full.accountNumber}", steps, 1, registry);
	}

	private Map<String, Method> registry() throws NoSuchMethodException {
		Map<String, Method> registry = new LinkedHashMap<>();
		registry.put("banking.full", BankingWorkflow.class.getDeclaredMethod("fullBankingFlow"));
		registry.put("hr.full", HRWorkflow.class.getDeclaredMethod("fullHRFlow", Employee.class, String.class));
		return registry;
	}

	@Test
	public void excelAliasColumnResolvesStepsAndDataSet() throws Exception {
		byte[] workbook = buildAliasWorkbook();

		ScenarioDef scenario = new ExcelScenarioSource().parse(workbook).get(0);

		Assert.assertNull(scenario.parseError(), "scenario ne doit pas etre en erreur: " + scenario.parseError());
		Assert.assertEquals(scenario.steps(), List.of("banking.full", "hr.full"));
		Assert.assertEquals(scenario.dataSet().get("accountNumber"), "${result:banking.full.accountNumber}");
	}

	private byte[] buildAliasWorkbook() throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet("Scenarios");

			XSSFRow header = sheet.createRow(0);
			header.createCell(0).setCellValue("scenario_name");
			header.createCell(1).setCellValue("sinistre");
			header.createCell(2).setCellValue("step");
			header.createCell(3).setCellValue("sbc");
			header.createCell(4).setCellValue("dataSet");
			header.createCell(5).setCellValue("aliases");

			XSSFRow row1 = sheet.createRow(1);
			row1.createCell(0).setCellValue("Scenario Excel Alias");
			row1.createCell(1).setCellValue("S1");
			row1.createCell(2).setCellValue("b");
			row1.createCell(3).setCellValue("false");
			row1.createCell(4).setCellValue("accountNumber=${result:b.accountNumber}");
			row1.createCell(5).setCellValue("b=banking.full,h=hr.full");

			XSSFRow row2 = sheet.createRow(2);
			row2.createCell(2).setCellValue("h");

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			workbook.write(out);
			return out.toByteArray();
		}
	}
}
