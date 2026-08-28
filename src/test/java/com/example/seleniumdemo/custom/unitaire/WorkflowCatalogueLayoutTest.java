package com.example.seleniumdemo.custom.unitaire;

import java.util.List;

import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.seleniumdemo.custom.catalogue.WorkflowCatalogueGenerator;

/**
 * Verifie que la mise en page du catalogue reste lisible meme en cas de "surinfo" (un hint de
 * parametre tres charge - record avec plusieurs champs enum imbriques, par exemple): largeur
 * de colonne plafonnee (pas de colonne demesuree) et hauteur de ligne calculee explicitement
 * (pas de texte coupe faute d'ajustement automatique dans tous les lecteurs).
 */
public class WorkflowCatalogueLayoutTest {

	@Test
	public void columnWidthIsCappedEvenForAVeryLongWrappedValue() throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet("Test");
			XSSFRow row = sheet.createRow(0);
			row.createCell(0).setCellValue("x".repeat(500));

			WorkflowCatalogueGenerator.sizeColumn(sheet, 0, true);

			Assert.assertEquals(sheet.getColumnWidth(0), WorkflowCatalogueGenerator.TEXT_COLUMN_MAX_WIDTH,
					"une valeur tres longue ne doit jamais depasser la largeur plafond");
		}
	}

	@Test
	public void columnWidthRespectsMinimumForAShortWrappedValue() throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet("Test");
			XSSFRow row = sheet.createRow(0);
			row.createCell(0).setCellValue("court");

			WorkflowCatalogueGenerator.sizeColumn(sheet, 0, true);

			Assert.assertEquals(sheet.getColumnWidth(0), WorkflowCatalogueGenerator.TEXT_COLUMN_MIN_WIDTH,
					"une valeur courte ne doit jamais rendre la colonne plus etroite que le minimum lisible");
		}
	}

	@Test
	public void nonWrappedColumnIsNotClampedByTextColumnMaxWidth() throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet("Test");
			XSSFRow row = sheet.createRow(0);
			// assez long pour depasser TEXT_COLUMN_MAX_WIDTH, pas assez pour taper le plafond
			// dur d'Excel (255 caracteres) - le but est de prouver l'absence du plafond "texte
			// long" ici, pas de retester le filet de securite Excel (deja couvert par le test
			// suivant).
			row.createCell(0).setCellValue("x".repeat(150));

			WorkflowCatalogueGenerator.sizeColumn(sheet, 0, false);

			Assert.assertTrue(sheet.getColumnWidth(0) > WorkflowCatalogueGenerator.TEXT_COLUMN_MAX_WIDTH,
					"une colonne non-'wrappedTextColumn' (code/name/method/class) n'a pas de plafond 'texte long' - non concernee par 'surinfo'");
		}
	}

	/**
	 * Meme sans etre une colonne "texte long" designee, une valeur extreme ne doit jamais faire
	 * planter la generation du catalogue - filet de securite independant de TEXT_COLUMN_MAX_WIDTH.
	 */
	@Test
	public void evenNonWrappedColumnNeverExceedsExcelsHardLimit() throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet("Test");
			XSSFRow row = sheet.createRow(0);
			row.createCell(0).setCellValue("x".repeat(1000));

			WorkflowCatalogueGenerator.sizeColumn(sheet, 0, false);

			Assert.assertEquals(sheet.getColumnWidth(0), 255 * 256,
					"une valeur extreme doit etre plafonnee a la limite dure d'Excel, jamais lever d'exception");
		}
	}

	@Test
	public void countWrappedLinesAccountsForExistingLineBreaks() {
		// 3 segments deja separes par des retours a la ligne (ex: JSON pretty-imprime) - meme
		// si chaque segment est court, ca fait bien 3 lignes, pas 1.
		int lines = WorkflowCatalogueGenerator.countWrappedLines("a\nb\nc", 50);

		Assert.assertEquals(lines, 3);
	}

	@Test
	public void countWrappedLinesWrapsALongSegmentAcrossMultipleLines() {
		int lines = WorkflowCatalogueGenerator.countWrappedLines("x".repeat(100), 25);

		Assert.assertEquals(lines, 4);
	}

	@Test
	public void countWrappedLinesCombinesExistingBreaksAndWrapping() {
		// 1 segment court + 1 segment qui doit lui-meme se replier sur plusieurs lignes
		String value = "court\n" + "x".repeat(100);

		int lines = WorkflowCatalogueGenerator.countWrappedLines(value, 25);

		Assert.assertEquals(lines, 1 + 4);
	}

	/**
	 * Reproduit un cas de "surinfo" realiste: un hint de parametre tres charge (record avec
	 * plusieurs champs enum) dans une colonne "parameters" plafonnee - la hauteur de ligne doit
	 * augmenter en consequence, pas rester a 1 ligne avec du texte qui deborde invisiblement.
	 */
	@Test
	public void rowHeightGrowsWhenAWrappedFieldIsVeryLong() throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet("Test");
			List<String> fields = List.of("code", "parameters");

			XSSFRow row = sheet.createRow(0);
			row.createCell(0).setCellValue("banking.full");
			XSSFCell parametersCell = row.createCell(1);
			parametersCell.setCellValue(
					"record: reference, status (APPROVED | REJECTED), payee (record: name, address (record: street, city, zipCode))");

			WorkflowCatalogueGenerator.sizeColumn(sheet, 0, false);
			WorkflowCatalogueGenerator.sizeColumn(sheet, 1, true);
			float defaultHeight = sheet.getDefaultRowHeightInPoints();

			WorkflowCatalogueGenerator.autoFitRowHeight(row, sheet, fields, 0);

			Assert.assertTrue(row.getHeightInPoints() > defaultHeight,
					"un hint tres charge doit faire grandir la ligne, pas rester a la hauteur par defaut");
		}
	}

	@Test
	public void rowHeightStaysDefaultWhenContentIsShort() throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet("Test");
			List<String> fields = List.of("code", "parameters");

			XSSFRow row = sheet.createRow(0);
			row.createCell(0).setCellValue("banking.full");
			row.createCell(1).setCellValue("APPROVED | REJECTED");

			WorkflowCatalogueGenerator.sizeColumn(sheet, 0, false);
			WorkflowCatalogueGenerator.sizeColumn(sheet, 1, true);
			float defaultHeight = sheet.getDefaultRowHeightInPoints();

			WorkflowCatalogueGenerator.autoFitRowHeight(row, sheet, fields, 0);

			Assert.assertEquals(row.getHeightInPoints(), defaultHeight,
					"un contenu court ne doit pas gonfler la ligne inutilement");
		}
	}
}
