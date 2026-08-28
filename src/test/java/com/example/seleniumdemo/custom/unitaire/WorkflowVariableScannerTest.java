package com.example.seleniumdemo.custom.unitaire;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.seleniumdemo.custom.catalogue.WorkflowVariableScanner;
import com.example.seleniumdemo.custom.reporting.Workflow;
import com.example.seleniumdemo.custom.reporting.WorkflowRegistry;

public class WorkflowVariableScannerTest {

	public enum Color {
		RED, GREEN, BLUE
	}

	public record Address(String street, String city) {
	}

	/**
	 * Fixture locale au test: expose un parametre de chaque famille de type supportee par
	 * 'convertDataSetValue', pour verifier la conversion sans dependre d'un vrai Workflow
	 * metier (qui pourrait changer de signature independamment de ce test).
	 */
	static class Fixture {
		@Workflow(params = { "prenom=firstName" })
		public void withMapping(String firstName, String lastName) {
		}

		public void withScalarTypes(String text, Color color, int number, boolean flag, LocalDate date) {
		}

		public void withRecord(Address address) {
		}

		public void withArray(String[] items) {
		}

		public void withList(List<String> items) {
		}

		// Corps volontairement vide (aucun JsonParams.load/MapParams.load/PageObject.param): le
		// scan AST du source ne trouve rien ici, quelle que soit la disponibilite du fichier
		// source sur disque - c'est exactement le meme "usages.isEmpty()" qui declenche le
		// repli sur 'variables()' en execution packagee/jar (WorkflowVariableScanner.scan()).
		@Workflow(code = "fixture.noBody", variables = { "site.params" })
		public void withDeclaredVariablesButNoScannableBody() {
		}
	}

	private Method method(String name, Class<?>... paramTypes) throws NoSuchMethodException {
		return Fixture.class.getDeclaredMethod(name, paramTypes);
	}

	@Test
	public void resolveDataSetKeysAppliesBusinessNameMapping() throws Exception {
		List<String> keys = WorkflowVariableScanner.resolveDataSetKeys(method("withMapping", String.class, String.class));

		Assert.assertEquals(keys, List.of("prenom", "lastName"),
				"'firstName' mappe vers 'prenom' via @Workflow(params=...), 'lastName' garde son nom Java brut");
	}

	@Test
	public void resolveRawValuePrefersWorkflowScopedOverride() {
		Map<String, Object> dataSet = Map.of(
				"address", "generic",
				"hr.full", Map.of("address", "specific"));

		Object scoped = WorkflowVariableScanner.resolveRawValue(dataSet, "hr.full", "address");
		Object generic = WorkflowVariableScanner.resolveRawValue(dataSet, "other.code", "address");

		Assert.assertEquals(scoped, "specific");
		Assert.assertEquals(generic, "generic");
	}

	@Test
	public void resolveRawValueReturnsNullWhenAbsent() {
		Assert.assertNull(WorkflowVariableScanner.resolveRawValue(Map.of(), "any.code", "missing"));
		Assert.assertNull(WorkflowVariableScanner.resolveRawValue(null, "any.code", "missing"));
	}

	@Test
	public void convertsScalarTypesFromRawJacksonValues() throws Exception {
		Parameter[] params = method("withScalarTypes", String.class, Color.class, int.class, boolean.class, LocalDate.class)
				.getParameters();

		Assert.assertEquals(WorkflowVariableScanner.convertDataSetValue("hello", params[0]), "hello");
		Assert.assertEquals(WorkflowVariableScanner.convertDataSetValue("GREEN", params[1]), Color.GREEN);
		Assert.assertEquals(WorkflowVariableScanner.convertDataSetValue(42, params[2]), 42);
		Assert.assertEquals(WorkflowVariableScanner.convertDataSetValue(true, params[3]), true);
		Assert.assertEquals(WorkflowVariableScanner.convertDataSetValue("2026-08-28", params[4]), LocalDate.of(2026, 8, 28));
	}

	@Test(expectedExceptions = IllegalStateException.class)
	public void unknownEnumValueFailsWithClearMessage() throws Exception {
		Parameter param = method("withScalarTypes", String.class, Color.class, int.class, boolean.class, LocalDate.class)
				.getParameters()[1];

		WorkflowVariableScanner.convertDataSetValue("PURPLE", param);
	}

	@Test
	public void convertsNestedRecordRecursively() throws Exception {
		Parameter param = method("withRecord", Address.class).getParameters()[0];
		Map<String, Object> raw = Map.of("street", "12 rue des Lilas", "city", "Lyon");

		Object result = WorkflowVariableScanner.convertDataSetValue(raw, param);

		Assert.assertEquals(result, new Address("12 rue des Lilas", "Lyon"));
	}

	@Test(expectedExceptions = IllegalStateException.class)
	public void recordConversionFailsWhenFieldMissing() throws Exception {
		Parameter param = method("withRecord", Address.class).getParameters()[0];

		WorkflowVariableScanner.convertDataSetValue(Map.of("street", "12 rue des Lilas"), param);
	}

	/**
	 * Preuve du repli jar-safety (WorkflowVariableScanner.scan()): quand le scan AST du source
	 * ne trouve rien, on retombe sur '@Workflow(variables = {...})' plutot que de laisser la
	 * colonne "Variables serveur" du catalogue vide sans explication.
	 */
	@Test
	public void scanFallsBackToDeclaredVariablesWhenSourceScanFindsNothing() throws Exception {
		Method method = method("withDeclaredVariablesButNoScannableBody");
		WorkflowRegistry.Entry entry = new WorkflowRegistry.Entry("fixture.noBody", Fixture.class, method, "Fixture", "");

		List<WorkflowVariableScanner.VariableUsage> usages = WorkflowVariableScanner.scan(entry);

		Assert.assertEquals(usages.size(), 1);
		Assert.assertEquals(usages.get(0).name(), "site.params");
		Assert.assertEquals(usages.get(0).kind(), WorkflowVariableScanner.Kind.SIMPLE,
				"le repli ne connait que le nom, pas le vrai type (JSON/MAP) - degrade a SIMPLE assumee");
	}

	@Test
	public void convertsArrayAndListOfStrings() throws Exception {
		Parameter arrayParam = method("withArray", String[].class).getParameters()[0];
		Parameter listParam = method("withList", List.class).getParameters()[0];
		List<Object> raw = List.of("A", "B", "C");

		Object array = WorkflowVariableScanner.convertDataSetValue(raw, arrayParam);
		Object list = WorkflowVariableScanner.convertDataSetValue(raw, listParam);

		Assert.assertEquals((String[]) array, new String[] { "A", "B", "C" });
		Assert.assertEquals(list, List.of("A", "B", "C"));
	}
}
