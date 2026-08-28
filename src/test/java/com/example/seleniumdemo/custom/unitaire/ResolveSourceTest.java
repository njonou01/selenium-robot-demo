package com.example.seleniumdemo.custom.unitaire;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.seleniumdemo.custom.scenarios.ExcelScenarioSource;
import com.example.seleniumdemo.custom.scenarios.JsonScenarioSource;
import com.example.seleniumdemo.custom.tests.ServerDrivenScenarioTest;

public class ResolveSourceTest {

	@Test
	public void xlsxExtensionSelectsExcelSource() {
		Assert.assertTrue(ServerDrivenScenarioTest.resolveSource("workflow-scenarios.xlsx") instanceof ExcelScenarioSource);
	}

	@Test
	public void xlsExtensionSelectsExcelSource() {
		Assert.assertTrue(ServerDrivenScenarioTest.resolveSource("workflow-scenarios.xls") instanceof ExcelScenarioSource);
	}

	@Test
	public void extensionMatchIsCaseInsensitive() {
		Assert.assertTrue(ServerDrivenScenarioTest.resolveSource("Workflow-Scenarios.XLSX") instanceof ExcelScenarioSource);
	}

	@Test
	public void jsonExtensionSelectsJsonSource() {
		Assert.assertTrue(ServerDrivenScenarioTest.resolveSource("workflow-scenarios.json") instanceof JsonScenarioSource);
	}

	@Test
	public void nullFileNameDefaultsToJsonSource() {
		// variable texte (pas fichier attache) -> pas de nom de fichier -> JSON par defaut
		Assert.assertTrue(ServerDrivenScenarioTest.resolveSource(null) instanceof JsonScenarioSource);
	}

	@Test
	public void unrelatedExtensionDefaultsToJsonSource() {
		Assert.assertTrue(ServerDrivenScenarioTest.resolveSource("workflow-scenarios.txt") instanceof JsonScenarioSource);
	}
}
