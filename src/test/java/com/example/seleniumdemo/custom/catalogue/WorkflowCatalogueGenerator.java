package com.example.seleniumdemo.custom.catalogue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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

import com.seleniumtests.core.runner.SeleniumTestPlan;
import com.seleniumtests.util.logging.SeleniumRobotLogger;

import com.example.seleniumdemo.custom.reporting.WorkflowRegistry;
import com.example.seleniumdemo.custom.server.VariableServerClient;
import com.example.seleniumdemo.custom.server.VariableServerConfig;

public class WorkflowCatalogueGenerator extends SeleniumTestPlan {

	private static final Logger logger = SeleniumRobotLogger.getLogger(WorkflowCatalogueGenerator.class);
	private static final String VARIABLE_NAME = "workflow.catalogue";
	private static final List<String> DEFAULT_FIELDS = List.of("code", "name", "method", "description", "class");

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
		COLUMN_LABELS.put("variables", "Variables serveur");
		COLUMN_EXTRACTORS.put("variables", WorkflowCatalogueGenerator::describeVariables);
		COLUMN_LABELS.put("parameters", "Parametres dataSet");
		COLUMN_EXTRACTORS.put("parameters", WorkflowCatalogueGenerator::describeParameters);
	}

	private static String describeVariables(WorkflowRegistry.Entry entry) {
		List<WorkflowVariableScanner.VariableUsage> usages = WorkflowVariableScanner.scan(entry);
		if (usages.isEmpty()) {
			return "";
		}
		return usages.stream().map(WorkflowVariableScanner.VariableUsage::name).collect(Collectors.joining(", "));
	}

	private static String describeParameters(WorkflowRegistry.Entry entry) {
		try {
			List<String> keys = WorkflowVariableScanner.resolveDataSetKeys(entry.method());
			if (keys.isEmpty()) {
				return "(aucun)";
			}
			Parameter[] parameters = entry.method().getParameters();
			List<String> described = new ArrayList<>(keys.size());
			for (int i = 0; i < keys.size(); i++) {
				String hint = describeParameterHint(parameters[i]);
				described.add(hint.isEmpty() ? keys.get(i) : keys.get(i) + " (" + hint + ")");
			}
			return String.join(", ", described);
		} catch (Exception e) {
			return "(erreur: " + e.getMessage() + ")";
		}
	}

	/**
	 * Indice affiche entre parentheses a cote du nom du parametre dans le catalogue: valeurs
	 * possibles pour un enum, champs pour un record, type d'element pour un tableau/List. Vide
	 * (String, int, etc.) si le type ne demande pas de precision supplementaire.
	 */
	private static String describeParameterHint(Parameter parameter) {
		Class<?> type = parameter.getType();
		if (type.isEnum()) {
			return Arrays.stream(type.getEnumConstants()).map(Object::toString).collect(Collectors.joining(" | "));
		}
		if (type.isRecord()) {
			return "record: " + Arrays.stream(type.getRecordComponents())
					.map(RecordComponent::getName).collect(Collectors.joining(", "));
		}
		if (type.isArray()) {
			return "tableau de " + type.getComponentType().getSimpleName();
		}
		if (List.class.isAssignableFrom(type) && parameter.getParameterizedType() instanceof ParameterizedType parameterizedType
				&& parameterizedType.getActualTypeArguments().length == 1) {
			return "liste de " + ((Class<?>) parameterizedType.getActualTypeArguments()[0]).getSimpleName();
		}
		return "";
	}

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
		boolean keepLocalCopy = readBooleanParameter(testContext, "catalogueKeepLocalCopy", false);

		Map<Class<?>, List<WorkflowRegistry.Entry>> entriesByClass = new LinkedHashMap<>();
		for (WorkflowRegistry.Entry entry : entries) {
			entriesByClass.computeIfAbsent(entry.declaringClass(), c -> new ArrayList<>()).add(entry);
		}

		switch (layout) {
			case SHEETS -> generateSingleWorkbookWithSheets(entriesByClass, entries, fields, stripPlaceholders, keepLocalCopy);
			case BLOCKS -> generateSingleWorkbookWithBlocks(entriesByClass, entries, fields, stripPlaceholders, keepLocalCopy);
			case JSON -> generateJsonCatalogue(entriesByClass, entries, fields, stripPlaceholders, keepLocalCopy);
			case MATRIX -> generateSingleWorkbookWithMatrix(entriesByClass, entries, fields, stripPlaceholders, keepLocalCopy);
		}
	}

	private record VariableAggregate(String name, WorkflowVariableScanner.Kind kind, String structure, Set<String> usedBy) {
	}

	private List<VariableAggregate> aggregateVariables(List<WorkflowRegistry.Entry> allEntries) {
		Map<String, WorkflowVariableScanner.Kind> kindByVariable = new LinkedHashMap<>();
		Map<String, Set<String>> pathsByVariable = new LinkedHashMap<>();
		Map<String, Set<String>> codesByVariable = new LinkedHashMap<>();

		for (WorkflowRegistry.Entry entry : allEntries) {
			for (WorkflowVariableScanner.VariableUsage usage : WorkflowVariableScanner.scan(entry)) {
				kindByVariable.put(usage.name(), usage.kind());
				pathsByVariable.computeIfAbsent(usage.name(), k -> new LinkedHashSet<>()).addAll(usage.paths());
				codesByVariable.computeIfAbsent(usage.name(), k -> new LinkedHashSet<>()).add(entry.code());
			}
		}

		List<VariableAggregate> aggregates = new ArrayList<>();
		for (String name : kindByVariable.keySet()) {
			WorkflowVariableScanner.Kind kind = kindByVariable.get(name);
			String structure = WorkflowVariableScanner.buildStructure(kind, pathsByVariable.get(name));
			aggregates.add(new VariableAggregate(name, kind, structure, codesByVariable.get(name)));
		}
		return aggregates;
	}

	private void addVariablesSheet(XSSFWorkbook workbook, Styles styles, List<WorkflowRegistry.Entry> allEntries) {
		List<VariableAggregate> aggregates = aggregateVariables(allEntries);

		XSSFSheet sheet = workbook.createSheet("Variables");
		XSSFCellStyle accentHeaderStyle = workbook.createCellStyle();
		accentHeaderStyle.cloneStyleFrom(styles.header());
		accentHeaderStyle.setFillForegroundColor(new XSSFColor(new Color(0x1d, 0xaa, 0xe2), null));

		String[] headers = { "Nom", "Type", "Structure", "Utilisee par" };
		XSSFRow headerRow = getOrCreateRow(sheet, 0);
		for (int c = 0; c < headers.length; c++) {
			XSSFCell cell = headerRow.createCell(c);
			cell.setCellValue(headers[c]);
			cell.setCellStyle(accentHeaderStyle);
		}

		int row = 1;
		for (VariableAggregate aggregate : aggregates) {
			XSSFRow dataRow = getOrCreateRow(sheet, row);
			XSSFCellStyle style = (row - 1) % 2 == 0 ? styles.bodyEven() : styles.bodyOdd();

			XSSFCell nameCell = dataRow.createCell(0);
			nameCell.setCellValue(aggregate.name());
			nameCell.setCellStyle(style);

			XSSFCell kindCell = dataRow.createCell(1);
			kindCell.setCellValue(switch (aggregate.kind()) {
				case SIMPLE -> "texte";
				case JSON -> "JSON";
				case MAP -> "texte cle=valeur";
			});
			kindCell.setCellStyle(style);

			XSSFCell structureCell = dataRow.createCell(2);
			structureCell.setCellValue(aggregate.structure() == null ? "" : aggregate.structure());
			structureCell.setCellStyle(style);

			XSSFCell usedByCell = dataRow.createCell(3);
			usedByCell.setCellValue(String.join(", ", aggregate.usedBy()));
			usedByCell.setCellStyle(style);

			row++;
		}

		sheet.createFreezePane(0, 1);
		for (int c = 0; c < headers.length; c++) {
			sheet.autoSizeColumn(c);
		}
		int minStructureWidth = 45 * 256;
		if (sheet.getColumnWidth(2) < minStructureWidth) {
			sheet.setColumnWidth(2, minStructureWidth);
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

	private void generateSingleWorkbookWithSheets(Map<Class<?>, List<WorkflowRegistry.Entry>> entriesByClass,
			List<WorkflowRegistry.Entry> allEntries, List<String> fields, boolean stripPlaceholders,
			boolean keepLocalCopy) throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			Styles styles = buildStyles(workbook);

			for (Map.Entry<Class<?>, List<WorkflowRegistry.Entry>> group : entriesByClass.entrySet()) {
				String name = baseName(group.getKey());
				XSSFSheet sheet = workbook.createSheet(name);
				writeBlock(sheet, styles, 0, 0, null, group.getValue(), fields, stripPlaceholders);
				sheet.createFreezePane(0, 1);
			}
			addVariablesSheet(workbook, styles, allEntries);

			File catalogueFile = createTempCatalogueFile(".xlsx");
			try (FileOutputStream fos = new FileOutputStream(catalogueFile)) {
				workbook.write(fos);
			}
			logger.info("Catalogue genere: " + catalogueFile);
			uploadCatalogueToServer(catalogueFile, VARIABLE_NAME,
					"Catalogue des codes @Workflow disponibles, genere automatiquement (WorkflowCatalogueGenerator).");
			copyToLocalIfRequested(catalogueFile, "src/test/resources/workflows-catalogue.xlsx", keepLocalCopy);
			catalogueFile.delete();
		}
	}

	private void generateSingleWorkbookWithBlocks(Map<Class<?>, List<WorkflowRegistry.Entry>> entriesByClass,
			List<WorkflowRegistry.Entry> allEntries, List<String> fields, boolean stripPlaceholders,
			boolean keepLocalCopy) throws Exception {
		try (XSSFWorkbook workbook = new XSSFWorkbook()) {
			XSSFSheet sheet = workbook.createSheet("Workflows");
			Styles styles = buildStyles(workbook);

			int row = 0;
			for (Map.Entry<Class<?>, List<WorkflowRegistry.Entry>> group : entriesByClass.entrySet()) {
				String name = baseName(group.getKey());
				row = writeBlock(sheet, styles, row, 0, name, group.getValue(), fields, stripPlaceholders);
				row++;
			}
			addVariablesSheet(workbook, styles, allEntries);

			File catalogueFile = createTempCatalogueFile(".xlsx");
			try (FileOutputStream fos = new FileOutputStream(catalogueFile)) {
				workbook.write(fos);
			}
			logger.info("Catalogue genere: " + catalogueFile);
			uploadCatalogueToServer(catalogueFile, VARIABLE_NAME,
					"Catalogue des codes @Workflow disponibles, genere automatiquement (WorkflowCatalogueGenerator).");
			copyToLocalIfRequested(catalogueFile, "src/test/resources/workflows-catalogue.xlsx", keepLocalCopy);
			catalogueFile.delete();
		}
	}

	private void generateSingleWorkbookWithMatrix(Map<Class<?>, List<WorkflowRegistry.Entry>> entriesByClass,
			List<WorkflowRegistry.Entry> allEntries, List<String> fields, boolean stripPlaceholders,
			boolean keepLocalCopy) throws Exception {
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
			addVariablesSheet(workbook, styles, allEntries);

			File catalogueFile = createTempCatalogueFile(".xlsx");
			try (FileOutputStream fos = new FileOutputStream(catalogueFile)) {
				workbook.write(fos);
			}
			logger.info("Catalogue genere: " + catalogueFile);
			uploadCatalogueToServer(catalogueFile, VARIABLE_NAME,
					"Catalogue des codes @Workflow disponibles, genere automatiquement (WorkflowCatalogueGenerator).");
			copyToLocalIfRequested(catalogueFile, "src/test/resources/workflows-catalogue.xlsx", keepLocalCopy);
			catalogueFile.delete();
		}
	}

	private void generateJsonCatalogue(Map<Class<?>, List<WorkflowRegistry.Entry>> entriesByClass,
			List<WorkflowRegistry.Entry> allEntries, List<String> fields, boolean stripPlaceholders,
			boolean keepLocalCopy) throws Exception {
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

		JSONArray variables = new JSONArray();
		for (VariableAggregate aggregate : aggregateVariables(allEntries)) {
			JSONObject variableObject = new JSONObject();
			variableObject.put("name", aggregate.name());
			variableObject.put("type", switch (aggregate.kind()) {
				case SIMPLE -> "texte";
				case JSON -> "JSON";
				case MAP -> "texte cle=valeur";
			});
			variableObject.put("structure", switch (aggregate.kind()) {
				case SIMPLE -> JSONObject.NULL;
				case JSON -> new JSONObject(aggregate.structure());
				case MAP -> aggregate.structure();
			});
			variableObject.put("usedBy", new JSONArray(aggregate.usedBy()));
			variables.put(variableObject);
		}

		JSONObject root = new JSONObject();
		root.put("classes", classes);
		root.put("variables", variables);

		File catalogueFile = createTempCatalogueFile(".json");
		try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(catalogueFile.toPath()),
				StandardCharsets.UTF_8)) {
			writer.write(root.toString(2));
		}

		logger.info("Catalogue genere: " + catalogueFile);
		uploadCatalogueToServer(catalogueFile, VARIABLE_NAME,
				"Catalogue des codes @Workflow disponibles, genere automatiquement (WorkflowCatalogueGenerator).");
		copyToLocalIfRequested(catalogueFile, "src/test/resources/workflows-catalogue.json", keepLocalCopy);
		catalogueFile.delete();
	}

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
			if ("description".equals(fields.get(i)) || "variables".equals(fields.get(i))) {
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
		XSSFColor headerBg = new XSSFColor(new Color(0x15, 0x3a, 0x4c), null);
		XSSFColor stripeBg = new XSSFColor(new Color(0xf5, 0xf6, 0xf8), null);
		XSSFColor borderColor = new XSSFColor(new Color(0xe3, 0xe6, 0xea), null);

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
		VariableServerConfig config = VariableServerConfig.fromRunningContext();
		if (!config.isComplete()) {
			logger.info("Upload ignore: 'seleniumRobotServerUrl'/'seleniumRobotServerToken' absents du testng.xml.");
			return;
		}

		new VariableServerClient(config).uploadFile(variableName, file, description);

		logger.info("Catalogue uploade sur le serveur de variable: " + variableName);
	}

	/**
	 * Le catalogue n'a besoin d'exister que le temps de l'upload vers le serveur de variable -
	 * rien d'autre ne le relit depuis le disque. Un fichier temporaire evite toute dependance
	 * au dossier de lancement du process (contrairement a un chemin relatif fixe type
	 * "src/test/resources/...") et n'ajoute pas d'artefact genere dans l'arborescence source.
	 */
	private File createTempCatalogueFile(String suffix) throws IOException {
		File file = File.createTempFile("workflows-catalogue-", suffix);
		file.deleteOnExit();
		return file;
	}

	/**
	 * Copie optionnelle vers un chemin relatif fixe (pratique pour inspecter le catalogue en
	 * local sans aller le chercher sur le serveur de variable). Best-effort: si le dossier
	 * cible n'existe pas (ex: lancement depuis un jar hors de l'arborescence source, sur
	 * Jenkins), on logue un avertissement et on continue - ca ne doit jamais faire echouer le
	 * test, la copie serveur (obligatoire, faite avant l'appel) reste la source de verite.
	 */
	private void copyToLocalIfRequested(File sourceFile, String localRelativePath, boolean keepLocalCopy) {
		if (!keepLocalCopy) {
			return;
		}
		try {
			File localFile = new File(localRelativePath);
			File parent = localFile.getParentFile();
			if (parent != null && !parent.isDirectory()) {
				logger.warn("Copie locale du catalogue ignoree: dossier introuvable '{}' (normal si le process "
						+ "tourne hors de l'arborescence source, ex: jar Jenkins).", parent);
				return;
			}
			Files.copy(sourceFile.toPath(), localFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			logger.info("Copie locale du catalogue: " + localRelativePath);
		} catch (IOException e) {
			logger.warn("Copie locale du catalogue ignoree: {}", e.getMessage());
		}
	}
}
