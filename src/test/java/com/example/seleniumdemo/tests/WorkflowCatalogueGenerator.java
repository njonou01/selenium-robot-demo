package com.example.seleniumdemo.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

import java.awt.Color;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.annotations.Test;

import com.seleniumtests.core.SeleniumTestsContextManager;
import com.seleniumtests.core.runner.SeleniumTestPlan;
import com.seleniumtests.util.logging.SeleniumRobotLogger;

import com.example.seleniumdemo.reporting.WorkflowRegistry;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;

/**
 * N'est pas un test au sens propre (rien n'est verifie/assert): c'est un GENERATEUR DE
 * DOCUMENTATION, pilote par TestNG (comme le reste du projet) pour rester lancable via
 * "mvn test" sans outillage separe. Scanne les codes @Workflow(code=...) disponibles (par
 * reflexion, via WorkflowRegistry - aucune liste codee en dur) et produit un catalogue a jour:
 * console + fichier Excel, uploade sur le serveur de variable (via Unirest, deja une dependance
 * du framework) pour consultation par un testeur manuel sans acces au code.
 *
 * En production, cette generation+upload serait un stage Jenkins (mvn test puis upload), pas un
 * script lance a la main - cette classe fonctionne pareil dans les deux cas.
 */
public class WorkflowCatalogueGenerator extends SeleniumTestPlan {

	private static final Logger logger = SeleniumRobotLogger.getLogger(WorkflowCatalogueGenerator.class);
	private static final String CATALOGUE_PATH = "src/test/resources/workflows-catalogue.xlsx";
	private static final String VARIABLE_NAME = "workflow.catalogue";

	@Test
	public void generateWorkflowCatalogue() throws Exception {
		List<WorkflowRegistry.Entry> entries = WorkflowRegistry.scanEntries();

		System.out.println("Code                  | Classe                  | Methode                    | Libelle");
		System.out.println("-".repeat(100));
		for (WorkflowRegistry.Entry entry : entries) {
			System.out.printf("%-22s| %-25s| %-28s| %s%n",
					entry.code(), entry.declaringClass().getSimpleName(), entry.method().getName(), entry.name());
		}

		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet("Workflows");

			XSSFColor headerBg = new XSSFColor(new Color(0x1F, 0x4E, 0x78), null);
			XSSFColor stripeBg = new XSSFColor(new Color(0xF2, 0xF6, 0xFA), null);
			XSSFColor borderColor = new XSSFColor(new Color(0xD9, 0xD9, 0xD9), null);

			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			headerFont.setColor(IndexedColors.WHITE.getIndex());
			headerFont.setFontHeightInPoints((short) 11);

			XSSFCellStyle headerStyle = workbook.createCellStyle();
			headerStyle.setFont(headerFont);
			headerStyle.setFillForegroundColor(headerBg);
			headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			headerStyle.setAlignment(HorizontalAlignment.CENTER);
			headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			applyThinBorders(headerStyle, borderColor);

			XSSFCellStyle bodyStyleEven = workbook.createCellStyle();
			bodyStyleEven.setVerticalAlignment(VerticalAlignment.CENTER);
			bodyStyleEven.setWrapText(true);
			applyThinBorders(bodyStyleEven, borderColor);

			XSSFCellStyle bodyStyleOdd = workbook.createCellStyle();
			bodyStyleOdd.cloneStyleFrom(bodyStyleEven);
			bodyStyleOdd.setFillForegroundColor(stripeBg);
			bodyStyleOdd.setFillPattern(FillPatternType.SOLID_FOREGROUND);

			String[] headers = { "Code", "Classe", "Methode", "Libelle", "Description" };
			XSSFRow headerRow = sheet.createRow(0);
			headerRow.setHeightInPoints(22);
			for (int i = 0; i < headers.length; i++) {
				XSSFCell cell = headerRow.createCell(i);
				cell.setCellValue(headers[i]);
				cell.setCellStyle(headerStyle);
			}

			int rowIndex = 1;
			for (WorkflowRegistry.Entry entry : entries) {
				XSSFRow row = sheet.createRow(rowIndex);
				// pas de hauteur fixe: laisse Excel recalculer selon le contenu (utile avec
				// wrapText si une description longue passe sur plusieurs lignes plus tard)
				XSSFCellStyle style = rowIndex % 2 == 0 ? bodyStyleEven : bodyStyleOdd;
				String[] values = { entry.code(), entry.declaringClass().getSimpleName(), entry.method().getName(),
						entry.name(), entry.description() };
				for (int i = 0; i < values.length; i++) {
					XSSFCell cell = row.createCell(i);
					cell.setCellValue(values[i]);
					cell.setCellStyle(style);
				}
				rowIndex++;
			}

			sheet.createFreezePane(0, 1);

			for (int i = 0; i < headers.length; i++) {
				sheet.autoSizeColumn(i);
				sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 800);
			}
			// colonne Description: largeur minimale fixe (actuellement vide partout, autoSize
			// se baserait juste sur le header "Description" sinon - trop etroit une fois remplie)
			int descriptionColumnIndex = headers.length - 1;
			int minDescriptionWidth = 45 * 256;
			if (sheet.getColumnWidth(descriptionColumnIndex) < minDescriptionWidth) {
				sheet.setColumnWidth(descriptionColumnIndex, minDescriptionWidth);
			}

			try (FileOutputStream fos = new FileOutputStream(CATALOGUE_PATH)) {
				workbook.write(fos);
			}
		}

		logger.info("Catalogue genere: " + CATALOGUE_PATH);

		uploadCatalogueToServer();
	}

	private void applyThinBorders(XSSFCellStyle style, XSSFColor color) {
		style.setBorderBottom(BorderStyle.THIN);
		style.setBorderTop(BorderStyle.THIN);
		style.setBorderLeft(BorderStyle.THIN);
		style.setBorderRight(BorderStyle.THIN);
		style.setBottomBorderColor(color);
		style.setTopBorderColor(color);
		style.setLeftBorderColor(color);
		style.setRightBorderColor(color);
	}

	private void uploadCatalogueToServer() {
		// seleniumRobotServerUrl/Token sont des parametres reserves du framework: geres par
		// SeleniumRobotServerContext, pas exposes via getConfiguration()/PageObject.param().
		String baseUrl = SeleniumTestsContextManager.getThreadContext().seleniumServer().getSeleniumRobotServerUrl();
		String token = SeleniumTestsContextManager.getThreadContext().seleniumServer().getSeleniumRobotServerToken();
		if (baseUrl == null || baseUrl.isEmpty() || token == null || token.isEmpty()) {
			logger.info("Upload ignore: 'seleniumRobotServerUrl'/'seleniumRobotServerToken' absents du testng.xml.");
			return;
		}

		File file = new File(CATALOGUE_PATH);

		String application = SeleniumTestsContextManager.getApplicationName();
		String environment = SeleniumTestsContextManager.getThreadContext().getTestEnv();
		String version = SeleniumTestsContextManager.getApplicationVersion();

		HttpResponse<String> searchResponse = Unirest.get(baseUrl + "/variable/api/variable/")
				.header("Authorization", "Token " + token)
				.queryString("application", application)
				.queryString("version", version)
				.queryString("environment", environment)
				.queryString("name", VARIABLE_NAME)
				.asString();

		if (!searchResponse.isSuccess()) {
			throw new IllegalStateException(
					"Echec recherche de '" + VARIABLE_NAME + "' (HTTP " + searchResponse.getStatus() + "): " + searchResponse.getBody());
		}

		JSONArray existing = new JSONArray(searchResponse.getBody());

		// PATCH si un enregistrement existe deja (met a jour juste le fichier, garde le meme id),
		// POST sinon. Le endpoint DELETE de ce serveur renvoie 404 (bug cote serveur, verifie via
		// curl direct: GET/PATCH fonctionnent sur le meme id, DELETE non) - on l'evite completement.
		HttpResponse<String> uploadResponse;
		if (existing.length() > 0) {
			int id = existing.getJSONObject(0).getInt("id");
			uploadResponse = Unirest.patch(baseUrl + "/variable/api/variable/" + id + "/")
					.header("Authorization", "Token " + token)
					.field("uploadFile", file)
					.asString();
		} else {
			int applicationId = lookupId(baseUrl, token, "/commons/api/application/", application);
			int environmentId = lookupId(baseUrl, token, "/commons/api/environment/", environment);

			uploadResponse = Unirest.post(baseUrl + "/variable/api/variable/")
					.header("Authorization", "Token " + token)
					.field("name", VARIABLE_NAME)
					.field("application", String.valueOf(applicationId))
					.field("environment", String.valueOf(environmentId))
					.field("description", "Catalogue des codes @Workflow disponibles, genere automatiquement (WorkflowCatalogueGenerator).")
					.field("uploadFile", file)
					.asString();
		}

		if (!uploadResponse.isSuccess()) {
			throw new IllegalStateException(
					"Echec upload de '" + VARIABLE_NAME + "' (HTTP " + uploadResponse.getStatus() + "): " + uploadResponse.getBody());
		}

		logger.info("Catalogue uploade sur le serveur de variable: " + VARIABLE_NAME);
	}

	/**
	 * Resout l'id numerique (cle primaire serveur) d'une application/environnement a partir de son
	 * nom - meme endpoint et meme forme de reponse (objet JSON unique, pas une liste) que ceux
	 * utilises en interne par le framework (SeleniumRobotServerConnector.getApplicationId()/
	 * getEnvironmentId()). Requis car l'API n'accepte que l'id (pas le nom) pour creer une nouvelle
	 * variable.
	 */
	private int lookupId(String baseUrl, String token, String apiPath, String name) {
		HttpResponse<String> response = Unirest.get(baseUrl + apiPath)
				.header("Authorization", "Token " + token)
				.queryString("name", name)
				.asString();

		if (!response.isSuccess()) {
			throw new IllegalStateException(
					"Echec recherche id pour '" + name + "' sur " + apiPath + " (HTTP " + response.getStatus() + "): " + response.getBody());
		}

		return new JSONObject(response.getBody()).getInt("id");
	}
}
