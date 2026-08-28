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
import com.example.seleniumdemo.custom.reporting.Workflow;
import com.example.seleniumdemo.custom.reporting.WorkflowRegistry;
import com.example.seleniumdemo.workflows.BankingWorkflow;
import com.example.seleniumdemo.workflows.ElectronicsWorkflow;

public class WorkflowCatalogueGeneratorTest {

	public enum Fruit {
		APPLE, PEAR
	}

	// 'toString()' override deliberement DIFFERENT du nom, pour prouver que
	// 'describeParameterHint' utilise '.name()' et jamais 'toString()' - le dataSet attend le
	// nom brut (Enum.valueOf), pas ce texte-la, meme si un dev l'a surcharge pour autre chose.
	public enum Status {
		APPROVED("A"), REJECTED("R");

		private final String code;

		Status(String code) {
			this.code = code;
		}

		@Override
		public String toString() {
			return "Statut " + code;
		}
	}

	public record Point(int x, int y) {
	}

	public record Employee(String firstName, String lastName) {
	}

	public record Claim(String reference, Status status) {
	}

	static class Fixture {
		public void withEnum(Fruit fruit) {
		}

		public void withRichEnum(Status status) {
		}

		public void withRecord(Point point) {
		}

		// mapping declare sur le champ Java 'firstName' du record - le hint doit afficher
		// 'prenom', pas 'firstName', puisque c'est ce que le dataSet attend reellement.
		@Workflow(params = { "prenom=employee.firstName" })
		public void withMappedRecordField(Employee employee) {
		}

		// enum imbriquee dans un record - le hint doit recurser et lister ses valeurs, pas
		// juste dire "record: reference, status" sans dire ce que 'status' accepte.
		public void withRecordContainingEnum(Claim claim) {
		}

		public void withArray(String[] items) {
		}

		public void withArrayOfEnum(Status[] items) {
		}

		public void withList(java.util.List<String> items) {
		}

		public void withDate(java.time.LocalDate date) {
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

	/**
	 * Le dataSet attend '.name()' (Enum.valueOf), jamais un 'toString()' custom ni un champ
	 * interne comme 'code' - le catalogue doit montrer exactement ce qui est utilisable dans le
	 * dataSet, pas la representation "humaine" de l'enum.
	 */
	@Test
	public void describeParameterHintUsesEnumNameNeverToStringOrInternalFields() throws Exception {
		String hint = WorkflowCatalogueGenerator.describeParameterHint(firstParam("withRichEnum", Status.class));

		Assert.assertEquals(hint, "APPROVED | REJECTED");
	}

	@Test
	public void describeParameterHintListsRecordComponents() throws Exception {
		String hint = WorkflowCatalogueGenerator.describeParameterHint(firstParam("withRecord", Point.class));

		Assert.assertEquals(hint, "record: x, y");
	}

	/**
	 * Le champ Java 'firstName' est mappe vers le nom metier 'prenom' via
	 * '@Workflow(params = {"prenom=employee.firstName"})' - c'est 'prenom' que le dataSet
	 * attend reellement (WorkflowVariableScanner.convertDataSetValue fait la meme resolution),
	 * le catalogue doit montrer ca, pas le nom Java brut.
	 */
	@Test
	public void describeParameterHintUsesBusinessNameForMappedRecordField() throws Exception {
		String hint = WorkflowCatalogueGenerator.describeParameterHint(firstParam("withMappedRecordField", Employee.class));

		Assert.assertEquals(hint, "record: prenom, lastName");
	}

	/**
	 * Un champ record qui est lui-meme un enum doit etre decrit recursivement (ses valeurs
	 * valides), pas juste liste par son nom - sinon le catalogue dit "il y a un champ status"
	 * sans dire ce qu'on peut y mettre.
	 */
	@Test
	public void describeParameterHintRecursesIntoEnumFieldInsideRecord() throws Exception {
		String hint = WorkflowCatalogueGenerator.describeParameterHint(firstParam("withRecordContainingEnum", Claim.class));

		Assert.assertEquals(hint, "record: reference, status (APPROVED | REJECTED)");
	}

	@Test
	public void describeParameterHintRecursesIntoArrayOfEnum() throws Exception {
		String hint = WorkflowCatalogueGenerator.describeParameterHint(firstParam("withArrayOfEnum", Status[].class));

		Assert.assertEquals(hint, "tableau de Status (APPROVED | REJECTED)");
	}

	@Test
	public void describeParameterHintShowsExpectedDateFormat() throws Exception {
		String hint = WorkflowCatalogueGenerator.describeParameterHint(firstParam("withDate", java.time.LocalDate.class));

		Assert.assertEquals(hint, "format AAAA-MM-JJ");
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
