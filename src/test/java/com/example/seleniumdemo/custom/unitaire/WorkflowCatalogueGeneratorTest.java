package com.example.seleniumdemo.custom.unitaire;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.Test;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import com.example.seleniumdemo.custom.catalogue.WorkflowCatalogueGenerator;
import com.example.seleniumdemo.custom.reporting.WorkflowRegistry;
import com.example.seleniumdemo.workflows.BankingWorkflow;
import com.example.seleniumdemo.workflows.ElectronicsWorkflow;

public class WorkflowCatalogueGeneratorTest {

	public enum Fruit {
		APPLE, PEAR
	}

	// Pas de 'toString()' override volontairement: 'describeParameterHint' doit lire 'code' et
	// 'weight' par reflexion, jamais via toString() (sinon ces champs restent invisibles des
	// qu'un dev oublie l'override).
	public enum Status {
		APPROVED("A", 1), REJECTED("R", 0);

		private final String code;
		private final int weight;

		Status(String code, int weight) {
			this.code = code;
			this.weight = weight;
		}
	}

	public record Point(int x, int y) {
	}

	static class Fixture {
		public void withEnum(Fruit fruit) {
		}

		public void withRichEnum(Status status) {
		}

		public void withRecord(Point point) {
		}

		public void withArray(String[] items) {
		}

		public void withList(java.util.List<String> items) {
		}

		public void withPlainString(String value) {
		}
	}

	private Parameter firstParam(String method, Class<?>... types) throws NoSuchMethodException {
		return Fixture.class.getDeclaredMethod(method, types).getParameters()[0];
	}

	@Test
	public void describeParameterHintListsEnumConstants() throws Exception {
		String hint = WorkflowCatalogueGenerator.describeParameterHint(firstParam("withEnum", Fruit.class));

		Assert.assertEquals(hint, "APPLE | PEAR");
	}

	@Test
	public void describeParameterHintExposesInternalEnumFieldsWithoutRelyingOnToString() throws Exception {
		String hint = WorkflowCatalogueGenerator.describeParameterHint(firstParam("withRichEnum", Status.class));

		Assert.assertEquals(hint, "APPROVED (code=A, weight=1) | REJECTED (code=R, weight=0)");
	}

	@Test
	public void describeParameterHintListsRecordComponents() throws Exception {
		String hint = WorkflowCatalogueGenerator.describeParameterHint(firstParam("withRecord", Point.class));

		Assert.assertEquals(hint, "record: x, y");
	}

	@Test
	public void describeParameterHintDescribesArrayComponentType() throws Exception {
		String hint = WorkflowCatalogueGenerator.describeParameterHint(firstParam("withArray", String[].class));

		Assert.assertEquals(hint, "tableau de String");
	}

	@Test
	public void describeParameterHintDescribesListComponentType() throws Exception {
		Method method = Fixture.class.getDeclaredMethod("withList", java.util.List.class);
		String hint = WorkflowCatalogueGenerator.describeParameterHint(method.getParameters()[0]);

		Assert.assertEquals(hint, "liste de String");
	}

	@Test
	public void describeParameterHintIsEmptyForPlainScalarTypes() throws Exception {
		String hint = WorkflowCatalogueGenerator.describeParameterHint(firstParam("withPlainString", String.class));

		Assert.assertEquals(hint, "");
	}

	@Test
	public void catalogueLayoutDefaultsToSheetsWhenBlank() {
		Assert.assertEquals(WorkflowCatalogueGenerator.CatalogueLayout.fromParameter(null),
				WorkflowCatalogueGenerator.CatalogueLayout.SHEETS);
		Assert.assertEquals(WorkflowCatalogueGenerator.CatalogueLayout.fromParameter("  "),
				WorkflowCatalogueGenerator.CatalogueLayout.SHEETS);
	}

	@Test
	public void catalogueLayoutParsesEachKnownValueCaseInsensitively() {
		Assert.assertEquals(WorkflowCatalogueGenerator.CatalogueLayout.fromParameter("blocks"),
				WorkflowCatalogueGenerator.CatalogueLayout.BLOCKS);
		Assert.assertEquals(WorkflowCatalogueGenerator.CatalogueLayout.fromParameter("JSON"),
				WorkflowCatalogueGenerator.CatalogueLayout.JSON);
		Assert.assertEquals(WorkflowCatalogueGenerator.CatalogueLayout.fromParameter("Matrix"),
				WorkflowCatalogueGenerator.CatalogueLayout.MATRIX);
	}

	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*Layout de catalogue inconnu.*")
	public void catalogueLayoutRejectsUnknownValueWithClearMessage() {
		WorkflowCatalogueGenerator.CatalogueLayout.fromParameter("bogus");
	}

	@Test
	public void resolveValueStripsRuntimePlaceholdersFromNameWhenRequested() throws Exception {
		// 'Entry.name()' est lue telle quelle (pas re-derivee de l'annotation) - une entree
		// synthetique avec un placeholder suffit, pas besoin qu'elle corresponde a un vrai
		// workflow code (aucun workflow code aujourd'hui n'a de placeholder dans son nom, ce
		// qui aurait rendu ce test aveugle s'il avait reutilise une entree reelle).
		Method method = BankingWorkflow.class.getDeclaredMethod("fullBankingFlow");
		WorkflowRegistry.Entry entry = new WorkflowRegistry.Entry("fixture.code", BankingWorkflow.class, method,
				"Étape: Authentification ${username}", "");

		String stripped = WorkflowCatalogueGenerator.resolveValue("name", entry, true);
		String kept = WorkflowCatalogueGenerator.resolveValue("name", entry, false);

		Assert.assertEquals(stripped, "Étape: Authentification");
		Assert.assertEquals(kept, "Étape: Authentification ${username}");
	}

	@Test
	public void resolveValuePassesThroughNonNameFieldsUnchanged() throws Exception {
		WorkflowRegistry.Entry entry = entryFor(BankingWorkflow.class, "fullBankingFlow");

		Assert.assertEquals(WorkflowCatalogueGenerator.resolveValue("code", entry, true), "banking.full");
	}

	@Test
	public void baseNameStripsTrailingWorkflowSuffix() {
		Assert.assertEquals(WorkflowCatalogueGenerator.baseName(BankingWorkflow.class), "Banking");
		Assert.assertEquals(WorkflowCatalogueGenerator.baseName(ElectronicsWorkflow.class), "Electronics");
	}

	@Test
	public void readFieldsDefaultsWhenParameterAbsent() {
		ITestContext context = fakeContext(null);

		Assert.assertEquals(WorkflowCatalogueGenerator.readFields(context),
				java.util.List.of("code", "name", "method", "description", "class"));
	}

	@Test
	public void readFieldsParsesCommaSeparatedListInOrder() {
		ITestContext context = fakeContext("class, code , variables");

		Assert.assertEquals(WorkflowCatalogueGenerator.readFields(context), java.util.List.of("class", "code", "variables"));
	}

	@Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = ".*Champ de catalogue inconnu.*")
	public void readFieldsRejectsUnknownFieldName() {
		WorkflowCatalogueGenerator.readFields(fakeContext("code,bogusField"));
	}

	@Test
	public void readBooleanParameterUsesDefaultWhenAbsent() {
		Assert.assertTrue(WorkflowCatalogueGenerator.readBooleanParameter(fakeContext(null), "catalogueKeepLocalCopy", true));
		Assert.assertFalse(WorkflowCatalogueGenerator.readBooleanParameter(fakeContext(null), "catalogueKeepLocalCopy", false));
	}

	@Test
	public void readBooleanParameterParsesExplicitValue() {
		XmlTest xmlTest = new XmlTest(new XmlSuite());
		xmlTest.addParameter("catalogueStripPlaceholders", "false");
		ITestContext context = Mockito.mock(ITestContext.class);
		Mockito.when(context.getCurrentXmlTest()).thenReturn(xmlTest);

		Assert.assertFalse(WorkflowCatalogueGenerator.readBooleanParameter(context, "catalogueStripPlaceholders", true));
	}

	private ITestContext fakeContext(String catalogueFieldsValue) {
		XmlTest xmlTest = new XmlTest(new XmlSuite());
		if (catalogueFieldsValue != null) {
			xmlTest.addParameter("catalogueFields", catalogueFieldsValue);
		}
		ITestContext context = Mockito.mock(ITestContext.class);
		Mockito.when(context.getCurrentXmlTest()).thenReturn(xmlTest);
		return context;
	}

	private WorkflowRegistry.Entry entryFor(Class<?> declaringClass, String methodName) throws Exception {
		for (WorkflowRegistry.Entry entry : WorkflowRegistry.scanEntries()) {
			if (entry.declaringClass().equals(declaringClass) && entry.method().getName().equals(methodName)) {
				return entry;
			}
		}
		throw new AssertionError("Entree introuvable pour " + declaringClass.getSimpleName() + "." + methodName);
	}
}
