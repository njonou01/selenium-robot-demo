package com.example.seleniumdemo.unitaire;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.seleniumdemo.custom.scenarios.ExcelScenarioSource;
import com.example.seleniumdemo.custom.scenarios.ScenarioDef;

public class ExcelScenarioSourceTest {

	private XSSFWorkbook newWorkbookWithHeader() {
		XSSFWorkbook workbook = new XSSFWorkbook();
		XSSFSheet sheet = workbook.createSheet("Scenarios");
		XSSFRow header = sheet.createRow(0);
		header.createCell(0).setCellValue("scenario_name");
		header.createCell(1).setCellValue("sinistre");
		header.createCell(2).setCellValue("step");
		header.createCell(3).setCellValue("sbc");
		header.createCell(4).setCellValue("dataSet");
		return workbook;
	}

	private byte[] toBytes(XSSFWorkbook workbook) throws Exception {
		try (workbook; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			workbook.write(out);
			return out.toByteArray();
		}
	}

	@Test
	public void parsesMultiRowBlockIntoOneScenario() throws Exception {
		XSSFWorkbook workbook = newWorkbookWithHeader();
		XSSFSheet sheet = workbook.getSheetAt(0);

		XSSFRow row1 = sheet.createRow(1);
		row1.createCell(0).setCellValue("Scenario Auto");
		row1.createCell(1).setCellValue("AUTO-1");
		row1.createCell(2).setCellValue("auto.declaration");
		row1.createCell(3).setCellValue("false");
		row1.createCell(4).setCellValue("montantDevis=1450.00");

		XSSFRow row2 = sheet.createRow(2);
		row2.createCell(2).setCellValue("auto.chiffrage");

		List<ScenarioDef> scenarios = new ExcelScenarioSource().parse(toBytes(workbook));

		Assert.assertEquals(scenarios.size(), 1);
		ScenarioDef scenario = scenarios.get(0);
		Assert.assertNull(scenario.parseError());
		Assert.assertEquals(scenario.steps(), List.of("auto.declaration", "auto.chiffrage"));
		Assert.assertEquals(scenario.dataSet().get("montantDevis"), "1450.00");
	}

	/**
	 * Le dataSet ne se lit qu'une fois, sur la 1ere ligne du bloc - une valeur mise sur une
	 * ligne suivante (colonne 'dataSet' remplie a nouveau) est ignoree.
	 */
	@Test
	public void dataSetIsReadOnlyOnFirstRowOfBlock() throws Exception {
		XSSFWorkbook workbook = newWorkbookWithHeader();
		XSSFSheet sheet = workbook.getSheetAt(0);

		XSSFRow row1 = sheet.createRow(1);
		row1.createCell(0).setCellValue("S1");
		row1.createCell(1).setCellValue("REF1");
		row1.createCell(2).setCellValue("step.one");
		row1.createCell(4).setCellValue("key=fromFirstRow");

		XSSFRow row2 = sheet.createRow(2);
		row2.createCell(2).setCellValue("step.two");
		row2.createCell(4).setCellValue("key=ignoredBecauseSecondRow");

		ScenarioDef scenario = new ExcelScenarioSource().parse(toBytes(workbook)).get(0);

		Assert.assertEquals(scenario.dataSet().get("key"), "fromFirstRow");
	}

	@Test
	public void jsonFragmentInCellIsParsedAsStructuredValue() throws Exception {
		XSSFWorkbook workbook = newWorkbookWithHeader();
		XSSFSheet sheet = workbook.getSheetAt(0);
		XSSFRow row1 = sheet.createRow(1);
		row1.createCell(0).setCellValue("S1");
		row1.createCell(1).setCellValue("REF1");
		row1.createCell(2).setCellValue("hr.full");
		row1.createCell(4).setCellValue("employee={\"firstName\":\"Robert\",\"lastName\":\"Wilson\"},montantDevis=1450.00");

		ScenarioDef scenario = new ExcelScenarioSource().parse(toBytes(workbook)).get(0);

		@SuppressWarnings("unchecked")
		var employee = (java.util.Map<String, Object>) scenario.dataSet().get("employee");
		Assert.assertEquals(employee.get("firstName"), "Robert");
		Assert.assertEquals(scenario.dataSet().get("montantDevis"), "1450.00");
	}

	@Test
	public void duplicateScenarioNameInTwoSeparateBlocksFailsFast() throws Exception {
		XSSFWorkbook workbook = newWorkbookWithHeader();
		XSSFSheet sheet = workbook.getSheetAt(0);

		XSSFRow row1 = sheet.createRow(1);
		row1.createCell(0).setCellValue("S1");
		row1.createCell(2).setCellValue("step.a");

		XSSFRow row2 = sheet.createRow(2);
		row2.createCell(0).setCellValue("Other");
		row2.createCell(2).setCellValue("step.b");

		XSSFRow row3 = sheet.createRow(3);
		row3.createCell(0).setCellValue("S1");
		row3.createCell(2).setCellValue("step.c");

		Assert.assertThrows(IllegalStateException.class, () -> new ExcelScenarioSource().parse(toBytes(workbook)));
	}

	@Test
	public void missingHeaderRowFailsFast() throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			workbook.createSheet("Scenarios");
			byte[] bytes;
			try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
				workbook.write(out);
				bytes = out.toByteArray();
			}
			Assert.assertThrows(IllegalStateException.class, () -> new ExcelScenarioSource().parse(bytes));
		}
	}

	/**
	 * Une cellule 'dataSet' cassee (JSON invalide) n'affecte que CE scenario, pas les autres
	 * blocs du meme fichier - meme isolation d'erreur que le format JSON.
	 */
	@Test
	public void malformedDataSetCellIsIsolatedNotFatalToSiblings() throws Exception {
		XSSFWorkbook workbook = newWorkbookWithHeader();
		XSSFSheet sheet = workbook.getSheetAt(0);

		XSSFRow row1 = sheet.createRow(1);
		row1.createCell(0).setCellValue("Casse");
		row1.createCell(2).setCellValue("step.a");
		row1.createCell(4).setCellValue("champSansEgal");

		XSSFRow row2 = sheet.createRow(2);
		row2.createCell(0).setCellValue("OK");
		row2.createCell(2).setCellValue("step.b");

		List<ScenarioDef> scenarios = new ExcelScenarioSource().parse(toBytes(workbook));

		Assert.assertEquals(scenarios.size(), 2);
		ScenarioDef casse = scenarios.stream().filter(s -> s.name().equals("Casse")).findFirst().orElseThrow();
		ScenarioDef ok = scenarios.stream().filter(s -> s.name().equals("OK")).findFirst().orElseThrow();
		Assert.assertNotNull(casse.parseError());
		Assert.assertNull(ok.parseError());
	}
}
