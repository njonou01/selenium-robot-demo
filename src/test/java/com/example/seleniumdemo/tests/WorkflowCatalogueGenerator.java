package com.example.seleniumdemo.tests;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import java.awt.Color;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.ITestContext;
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
 * reflexion, via WorkflowRegistry - aucune liste codee en dur) et produit un catalogue a jour
 * dans un unique fichier, uploade sur le serveur de variable (via Unirest, deja une dependance
 * du framework) pour consultation par un testeur manuel sans acces au code.
 *
 * Trois parametres XML (bloc de test "Documentation") pilotent le rendu, sans toucher au code:
 * - catalogueFields: champs a afficher, dans l'ordre, separes par virgule (voir COLUMN_LABELS
 *   pour les cles valides)
 * - catalogueLayout: "sheets" (xlsx, 1 onglet par classe Workflow, nom sans "Workflow"),
 *   "blocks" (xlsx, 1 seule feuille, 1 bloc de colonnes par classe, empiles verticalement),
 *   "json" (1 objet par classe Workflow, cle "workflows" pour ses entrees), ou "matrix" (xlsx,
 *   1 seule feuille, 1 ligne par code, classe fusionnee verticalement sur ses lignes)
 * - catalogueStripPlaceholders: true/false (defaut true) - retire les "${xxx}" du champ "name"
 *   (placeholders interpoles a l'execution, sans valeur reelle dans un catalogue statique)
 *
 * En production, cette generation+upload serait un stage Jenkins (mvn test puis upload), pas un
 * script lance a la main - cette classe fonctionne pareil dans les deux cas.
 */
public class WorkflowCatalogueGenerator extends SeleniumTestPlan {

	private static final Logger logger = SeleniumRobotLogger.getLogger(WorkflowCatalogueGenerator.class);
	private static final String CATALOGUE_PATH = "src/test/resources/workflows-catalogue.xlsx";
	private static final String CATALOGUE_JSON_PATH = "src/test/resources/workflows-catalogue.json";
	private static final String VARIABLE_NAME = "workflow.catalogue";
	private static final List<String> DEFAULT_FIELDS = List.of("code", "name", "method", "description", "class");

	// cle -> (libelle colonne, extracteur de valeur depuis une Entry). LinkedHashMap: l'ordre
	// d'insertion sert de reference pour les cles valides dans les messages d'erreur.
	private static final Map<String, String> COLUMN_LABELS = new LinkedHashMap<>();
	private static final Map<String, Function<WorkflowRegistry.Entry, String>> COLUMN_EXTRACTORS = new LinkedHashMap<>();
	static {
		COLUMN_LABELS.put("code", "Code");
		COLUMN_EXTRACTORS.put("code", WorkflowRegistry.Entry::code);
		COLUMN_LABELS.put("name", "Libelle");
		COLUMN_EXTRACTORS.put("name", WorkflowRegistry.Entry::name);
		COLUMN_LABELS.put("method", "Methode");
		COLUMN_EXTRACTORS.put("method", e -> e.method().getName());
		COLUMN_LABELS.put("description", "Description");
		COLUMN_EXTRACTORS.put("description", WorkflowRegistry.Entry::description);
		COLUMN_LABELS.put("class", "Classe");
		COLUMN_EXTRACTORS.put("class", e -> e.declaringClass().getSimpleName());
	}

	/**
	 * Valeur du champ pour cette entree. Les libelles @Workflow(name=...) contiennent des
	 * placeholders "${xxx}" interpoles a l'execution par WorkflowAspect avec la valeur reelle de
	 * l'argument - sans valeur reelle disponible ici (catalogue statique), catalogueStripPlaceholders
	 * (XML) permet de les retirer plutot que de les afficher tels quels.
	 */
	private String resolveValue(String field, WorkflowRegistry.Entry entry, boolean stripPlaceholders) {
		String value = COLUMN_EXTRACTORS.get(field).apply(entry);
		if ("name".equals(field) && stripPlaceholders) {
			return value.replaceAll("\\s*\\$\\{[^}]*\\}", "").trim();
		}
		return value;
	}

	private record Styles(XSSFCellStyle header, XSSFCellStyle bodyEven, XSSFCellStyle bodyOdd) {
	}

	private enum CatalogueLayout {
		SHEETS, BLOCKS, JSON, MATRIX;

		static CatalogueLayout fromParameter(String raw) {
			if (raw == null || raw.isBlank()) {
				return SHEETS;
			}
			try {
				return valueOf(raw.trim().toUpperCase());
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException("Layout de catalogue inconnu '" + raw + "'. Valeurs possibles: "
						+ Arrays.stream(values()).map(v -> v.name().toLowerCase()).reduce((a, b) -> a + ", " + b).orElse(""));
			}
		}
	}

	@Test
	public void generateWorkflowCatalogue(ITestContext testContext) throws Exception {
		List<WorkflowRegistry.Entry> entries = WorkflowRegistry.scanEntries();

		System.out.println("Code                  | Classe                  | Methode                    | Libelle");
		System.out.println("-".repeat(100));
		for (WorkflowRegistry.Entry entry : entries) {
			System.out.printf("%-22s| %-25s| %-28s| %s%n",
					entry.code(), entry.declaringClass().getSimpleName(), entry.method().getName(), entry.name());
		}

		List<String> fields = readFields(testContext);
		CatalogueLayout layout = CatalogueLayout.fromParameter(testContext.getCurrentXmlTest().getParameter("catalogueLayout"));
		boolean stripPlaceholders = readBooleanParameter(testContext, "catalogueStripPlaceholders", true);

		Map<Class<?>, List<WorkflowRegistry.Entry>> entriesByClass = new LinkedHashMap<>();
		for (WorkflowRegistry.Entry entry : entries) {
			entriesByClass.computeIfAbsent(entry.declaringClass(), c -> new ArrayList<>()).add(entry);
		}

		switch (layout) {
			case SHEETS -> generateSingleWorkbookWithSheets(entriesByClass, fields, stripPlaceholders);
			case BLOCKS -> generateSingleWorkbookWithBlocks(entriesByClass, fields, stripPlaceholders);
			case JSON -> generateJsonCatalogue(entriesByClass, fields, stripPlaceholders);
			case MATRIX -> generateSingleWorkbookWithMatrix(entriesByClass, fields, stripPlaceholders);
		}
	}

	private List<String> readFields(ITestContext testContext) {
		String raw = testContext.getCurrentXmlTest().getParameter("catalogueFields");
		List<String> fields = (raw == null || raw.isBlank())
				? DEFAULT_FIELDS
				: Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();

		for (String field : fields) {
			if (!COLUMN_LABELS.containsKey(field)) {
				throw new IllegalArgumentException("Champ de catalogue inconnu '" + field + "'. Champs disponibles: "
						+ String.join(", ", COLUMN_LABELS.keySet()));
			}
		}
		return fields;
	}

	private boolean readBooleanParameter(ITestContext testContext, String name, boolean defaultValue) {
		String raw = testContext.getCurrentXmlTest().getParameter(name);
		return (raw == null || raw.isBlank()) ? defaultValue : Boolean.parseBoolean(raw.trim());
	}

	private String baseName(Class<?> clazz) {
		String name = clazz.getSimpleName();
		return name.endsWith("Workflow") ? name.substring(0, name.length() - "Workflow".length()) : name;
	}

	private void generateSingleWorkbookWithSheets(Map<Class<?>, List<WorkflowRegistry.Entry>> entriesByClass, List<String> fields,
			boolean stripPlaceholders) throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Styles styles = buildStyles(workbook);

			for (Map.Entry<Class<?>, List<WorkflowRegistry.Entry>> group : entriesByClass.entrySet()) {
				String name = baseName(group.getKey());
				XSSFSheet sheet = workbook.createSheet(name);
				writeBlock(sheet, styles, 0, 0, null, group.getValue(), fields, stripPlaceholders);
				sheet.createFreezePane(0, 1);
			}

			try (FileOutputStream fos = new FileOutputStream(CATALOGUE_PATH)) {
				workbook.write(fos);
			}
		}

		logger.info("Catalogue genere: " + CATALOGUE_PATH);
		uploadCatalogueToServer(new File(CATALOGUE_PATH), VARIABLE_NAME,
				"Catalogue des codes @Workflow disponibles, genere automatiquement (WorkflowCatalogueGenerator).");
	}

	private void generateSingleWorkbookWithBlocks(Map<Class<?>, List<WorkflowRegistry.Entry>> entriesByClass, List<String> fields,
			boolean stripPlaceholders) throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet("Workflows");
			Styles styles = buildStyles(workbook);

			int row = 0;
			for (Map.Entry<Class<?>, List<WorkflowRegistry.Entry>> group : entriesByClass.entrySet()) {
				String name = baseName(group.getKey());
				row = writeBlock(sheet, styles, row, 0, name, group.getValue(), fields, stripPlaceholders);
				row++; // ligne vide de separation avant le bloc suivant
			}

			try (FileOutputStream fos = new FileOutputStream(CATALOGUE_PATH)) {
				workbook.write(fos);
			}
		}

		logger.info("Catalogue genere: " + CATALOGUE_PATH);
		uploadCatalogueToServer(new File(CATALOGUE_PATH), VARIABLE_NAME,
				"Catalogue des codes @Workflow disponibles, genere automatiquement (WorkflowCatalogueGenerator).");
	}

	/**
	 * Catalogue en matrice: chaque code d'une classe Workflow occupe sa propre ligne (colonnes
	 * suivantes: les champs de catalogueFields); le nom de la classe (colonne A, sans "Workflow")
	 * n'apparait qu'une fois par classe, fusionne verticalement sur toutes ses lignes.
	 */
	private void generateSingleWorkbookWithMatrix(Map<Class<?>, List<WorkflowRegistry.Entry>> entriesByClass, List<String> fields,
			boolean stripPlaceholders) throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet("Workflows");
			Styles styles = buildStyles(workbook);

			XSSFRow headerRow = getOrCreateRow(sheet, 0);
			XSSFCell classHeaderCell = headerRow.createCell(0);
			classHeaderCell.setCellValue("Classe");
			classHeaderCell.setCellStyle(styles.header());
			for (int c = 0; c < fields.size(); c++) {
				XSSFCell cell = headerRow.createCell(c + 1);
				cell.setCellValue(COLUMN_LABELS.get(fields.get(c)));
				cell.setCellStyle(styles.header());
			}

			int row = 1;
			for (Map.Entry<Class<?>, List<WorkflowRegistry.Entry>> group : entriesByClass.entrySet()) {
				List<WorkflowRegistry.Entry> classEntries = group.getValue();
				int classStartRow = row;

				for (int i = 0; i < classEntries.size(); i++) {
					XSSFRow xssfRow = getOrCreateRow(sheet, row + i);
					XSSFCellStyle style = i % 2 == 0 ? styles.bodyEven() : styles.bodyOdd();
					for (int c = 0; c < fields.size(); c++) {
						XSSFCell cell = xssfRow.createCell(c + 1);
						cell.setCellValue(resolveValue(fields.get(c), classEntries.get(i), stripPlaceholders));
						cell.setCellStyle(style);
					}
				}

				XSSFCell classCell = getOrCreateRow(sheet, classStartRow).createCell(0);
				classCell.setCellValue(baseName(group.getKey()));
				classCell.setCellStyle(styles.header());
				if (classEntries.size() > 1) {
					sheet.addMergedRegion(new CellRangeAddress(classStartRow, classStartRow + classEntries.size() - 1, 0, 0));
				}

				row += classEntries.size();
			}

			sheet.createFreezePane(0, 1);
			for (int i = 0; i <= fields.size(); i++) {
				sheet.autoSizeColumn(i);
				sheet.setColumnWidth(i, sheet.getColumnWidth(i) + 800);
			}

			try (FileOutputStream fos = new FileOutputStream(CATALOGUE_PATH)) {
				workbook.write(fos);
			}
		}

		logger.info("Catalogue genere: " + CATALOGUE_PATH);
		uploadCatalogueToServer(new File(CATALOGUE_PATH), VARIABLE_NAME,
				"Catalogue des codes @Workflow disponibles, genere automatiquement (WorkflowCatalogueGenerator).");
	}

	/**
	 * Catalogue au format JSON: un objet par classe Workflow ("class": nom sans "Workflow",
	 * "workflows": liste d'entrees ne contenant que les champs demandes par catalogueFields).
	 */
	private void generateJsonCatalogue(Map<Class<?>, List<WorkflowRegistry.Entry>> entriesByClass, List<String> fields,
			boolean stripPlaceholders) throws Exception {
		JSONArray classes = new JSONArray();
		for (Map.Entry<Class<?>, List<WorkflowRegistry.Entry>> group : entriesByClass.entrySet()) {
			JSONObject classObject = new JSONObject();
			classObject.put("class", baseName(group.getKey()));

			JSONArray workflows = new JSONArray();
			for (WorkflowRegistry.Entry entry : group.getValue()) {
				JSONObject workflowObject = new JSONObject();
				for (String field : fields) {
					workflowObject.put(field, resolveValue(field, entry, stripPlaceholders));
				}
				workflows.put(workflowObject);
			}
			classObject.put("workflows", workflows);
			classes.put(classObject);
		}

		try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(new File(CATALOGUE_JSON_PATH).toPath()),
				StandardCharsets.UTF_8)) {
			writer.write(classes.toString(2));
		}

		logger.info("Catalogue genere: " + CATALOGUE_JSON_PATH);
		uploadCatalogueToServer(new File(CATALOGUE_JSON_PATH), VARIABLE_NAME,
				"Catalogue des codes @Workflow disponibles, genere automatiquement (WorkflowCatalogueGenerator).");
	}

	/**
	 * Ecrit un bloc de colonnes (titre optionnel + en-tetes + lignes de donnees) a partir de
	 * (startRow, startCol) et renvoie la premiere ligne libre apres ce bloc. Reutilise en mode
	 * "sheets" (un seul bloc, sans titre, par feuille) et en mode "blocks" (plusieurs blocs avec
	 * titre, empiles verticalement sur la meme feuille).
	 */
	private int writeBlock(XSSFSheet sheet, Styles styles, int startRow, int startCol, String title,
			List<WorkflowRegistry.Entry> entries, List<String> fields, boolean stripPlaceholders) {
		int row = startRow;

		if (title != null) {
			XSSFRow titleRow = getOrCreateRow(sheet, row);
			XSSFCell titleCell = titleRow.createCell(startCol);
			titleCell.setCellValue(title);
			titleCell.setCellStyle(styles.header());
			if (fields.size() > 1) {
				sheet.addMergedRegion(new CellRangeAddress(row, row, startCol, startCol + fields.size() - 1));
			}
			row++;
		}

		XSSFRow headerRow = getOrCreateRow(sheet, row);
		headerRow.setHeightInPoints(22);
		for (int i = 0; i < fields.size(); i++) {
			XSSFCell cell = headerRow.createCell(startCol + i);
			cell.setCellValue(COLUMN_LABELS.get(fields.get(i)));
			cell.setCellStyle(styles.header());
		}
		row++;

		for (int i = 0; i < entries.size(); i++) {
			XSSFRow dataRow = getOrCreateRow(sheet, row + i);
			XSSFCellStyle style = i % 2 == 0 ? styles.bodyEven() : styles.bodyOdd();
			WorkflowRegistry.Entry entry = entries.get(i);
			for (int c = 0; c < fields.size(); c++) {
				XSSFCell cell = dataRow.createCell(startCol + c);
				cell.setCellValue(resolveValue(fields.get(c), entry, stripPlaceholders));
				cell.setCellStyle(style);
			}
		}

		for (int i = 0; i < fields.size(); i++) {
			sheet.autoSizeColumn(startCol + i);
			sheet.setColumnWidth(startCol + i, sheet.getColumnWidth(startCol + i) + 800);
			if ("description".equals(fields.get(i))) {
				// colonne souvent vide actuellement: autoSize donnerait une largeur trop etroite
				// (basee juste sur le header) une fois la description reellement renseignee
				int minWidth = 45 * 256;
				if (sheet.getColumnWidth(startCol + i) < minWidth) {
					sheet.setColumnWidth(startCol + i, minWidth);
				}
			}
		}

		return row + entries.size();
	}

	private XSSFRow getOrCreateRow(XSSFSheet sheet, int rowIndex) {
		XSSFRow row = sheet.getRow(rowIndex);
		return row != null ? row : sheet.createRow(rowIndex);
	}

	private Styles buildStyles(XSSFWorkbook workbook) {
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

		return new Styles(headerStyle, bodyStyleEven, bodyStyleOdd);
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

	private void uploadCatalogueToServer(File file, String variableName, String description) {
		// seleniumRobotServerUrl/Token sont des parametres reserves du framework: geres par
		// SeleniumRobotServerContext, pas exposes via getConfiguration()/PageObject.param().
		String baseUrl = SeleniumTestsContextManager.getThreadContext().seleniumServer().getSeleniumRobotServerUrl();
		String token = SeleniumTestsContextManager.getThreadContext().seleniumServer().getSeleniumRobotServerToken();
		if (baseUrl == null || baseUrl.isEmpty() || token == null || token.isEmpty()) {
			logger.info("Upload ignore: 'seleniumRobotServerUrl'/'seleniumRobotServerToken' absents du testng.xml.");
			return;
		}

		String application = SeleniumTestsContextManager.getApplicationName();
		String environment = SeleniumTestsContextManager.getThreadContext().getTestEnv();
		String version = SeleniumTestsContextManager.getApplicationVersion();

		HttpResponse<String> searchResponse = Unirest.get(baseUrl + "/variable/api/variable/")
				.header("Authorization", "Token " + token)
				.queryString("application", application)
				.queryString("version", version)
				.queryString("environment", environment)
				.queryString("name", variableName)
				.asString();

		if (!searchResponse.isSuccess()) {
			throw new IllegalStateException(
					"Echec recherche de '" + variableName + "' (HTTP " + searchResponse.getStatus() + "): " + searchResponse.getBody());
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
					.field("name", variableName)
					.field("application", String.valueOf(applicationId))
					.field("environment", String.valueOf(environmentId))
					.field("description", description)
					.field("uploadFile", file)
					.asString();
		}

		if (!uploadResponse.isSuccess()) {
			throw new IllegalStateException(
					"Echec upload de '" + variableName + "' (HTTP " + uploadResponse.getStatus() + "): " + uploadResponse.getBody());
		}

		logger.info("Catalogue uploade sur le serveur de variable: " + variableName);
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
