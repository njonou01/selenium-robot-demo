package com.example.seleniumdemo.custom.scenarios;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class ExcelScenarioSource implements ScenarioSource {

	@Override
	public List<ScenarioDef> parse(byte[] content) {
		try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(content))) {
			Sheet sheet = workbook.getSheetAt(0);
			DataFormatter formatter = new DataFormatter();
			FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

			int headerRow = findHeaderRow(sheet, formatter, evaluator);

			Map<String, String> sinistreByScenario = new LinkedHashMap<>();
			Map<String, Boolean> sbcByScenario = new LinkedHashMap<>();
			Map<String, Map<String, Object>> dataSetByScenario = new LinkedHashMap<>();
			Map<String, List<String>> stepsByScenario = new LinkedHashMap<>();
			// scenario dont la cellule 'dataSet' est illisible: isole (n'echoue qu'a l'execution de
			// CE scenario), plutot que de faire planter la lecture de tout le fichier Excel.
			Map<String, String> poisonByScenario = new LinkedHashMap<>();

			String currentScenario = null;
			String currentSinistre = null;
			boolean currentSbc = false;
			Map<String, Object> currentDataSet = Map.of();
			for (Row row : sheet) {
				if (row.getRowNum() <= headerRow) {
					continue;
				}
				String name = stringValue(row.getCell(0), formatter, evaluator);
				String sinistre = stringValue(row.getCell(1), formatter, evaluator);
				String step = stringValue(row.getCell(2), formatter, evaluator);
				String sbc = stringValue(row.getCell(3), formatter, evaluator);
				String dataSet = stringValue(row.getCell(4), formatter, evaluator);

				if (name != null && !name.isBlank()) {
					if (!name.equals(currentScenario) && stepsByScenario.containsKey(name)) {
						throw new IllegalStateException("Feuille Excel 'workflow.scenarios': le scenario '" + name
								+ "' apparait dans 2 blocs distincts (ligne " + (row.getRowNum() + 1)
								+ "). Fusionner les 2 blocs ou renommer l'un des deux.");
					}
					currentScenario = name;
					currentSinistre = sinistre;
					currentSbc = "true".equalsIgnoreCase(sbc) || "1".equals(sbc) || "oui".equalsIgnoreCase(sbc);
					try {
						currentDataSet = parseDataSet(dataSet);
					} catch (Exception e) {
						poisonByScenario.put(currentScenario, "Scenario '" + currentScenario
								+ "' illisible dans la feuille Excel 'workflow.scenarios': " + e.getMessage());
						currentDataSet = Map.of();
					}
				}
				if (currentScenario == null) {
					throw new IllegalStateException("Feuille Excel 'workflow.scenarios': ligne " + (row.getRowNum() + 1)
							+ " sans scenario_name (ni fusionne depuis une ligne au-dessus).");
				}
				if (step == null || step.isBlank()) {
					continue;
				}

				sinistreByScenario.putIfAbsent(currentScenario, currentSinistre);
				sbcByScenario.putIfAbsent(currentScenario, currentSbc);
				dataSetByScenario.putIfAbsent(currentScenario, currentDataSet);
				stepsByScenario.computeIfAbsent(currentScenario, k -> new ArrayList<>()).add(step);
			}

			List<ScenarioDef> scenarios = new ArrayList<>();
			for (Map.Entry<String, List<String>> entry : stepsByScenario.entrySet()) {
				String scenarioName = entry.getKey();
				if (poisonByScenario.containsKey(scenarioName)) {
					scenarios.add(ScenarioDef.invalid(scenarioName, poisonByScenario.get(scenarioName)));
					continue;
				}
				scenarios.add(new ScenarioDef(scenarioName, sinistreByScenario.get(scenarioName), entry.getValue(),
						sbcByScenario.get(scenarioName), dataSetByScenario.get(scenarioName)));
			}
			return scenarios;
		} catch (Exception e) {
			throw new IllegalStateException("Fichier Excel de 'workflow.scenarios' illisible.", e);
		}
	}

	private int findHeaderRow(Sheet sheet, DataFormatter formatter, FormulaEvaluator evaluator) {
		int lastRow = Math.min(sheet.getLastRowNum(), 9);
		for (int i = 0; i <= lastRow; i++) {
			Row row = sheet.getRow(i);
			if (row == null) {
				continue;
			}
			if ("scenario_name".equalsIgnoreCase(stringValue(row.getCell(0), formatter, evaluator))) {
				return i;
			}
		}
		throw new IllegalStateException(
				"Feuille Excel 'workflow.scenarios': ligne d'en-tete introuvable (1ere colonne doit valoir 'scenario_name').");
	}

	/**
	 * Parse une cellule 'dataSet' au format "cle=valeur,cle=valeur,...". Une valeur peut aussi
	 * etre du JSON brut ('{...}' ou '[...]') pour representer un record/tableau - dans ce cas elle
	 * est parsee en Map/List (comme le ferait Jackson depuis un vrai fichier JSON), pas gardee
	 * comme String. Le split respecte la profondeur d'accolades/crochets pour ne pas couper sur
	 * une virgule ou un '=' se trouvant a l'interieur d'un fragment JSON.
	 */
	private Map<String, Object> parseDataSet(String raw) {
		if (raw == null || raw.isBlank()) {
			return Map.of();
		}
		Map<String, Object> dataSet = new LinkedHashMap<>();
		for (String pair : splitTopLevel(raw, ',')) {
			if (pair.isBlank()) {
				continue;
			}
			int equalIndex = pair.indexOf('=');
			if (equalIndex < 0) {
				throw new IllegalStateException(
						"Feuille Excel 'workflow.scenarios': paire 'dataSet' invalide (attendu cle=valeur): '" + pair.trim() + "'");
			}
			String key = pair.substring(0, equalIndex).trim();
			String rawFieldValue = pair.substring(equalIndex + 1).trim();
			dataSet.put(key, parseDataSetValue(rawFieldValue));
		}
		return dataSet;
	}

	private Object parseDataSetValue(String rawFieldValue) {
		if (!rawFieldValue.startsWith("{") && !rawFieldValue.startsWith("[")) {
			return rawFieldValue;
		}
		try {
			return new ObjectMapper().readValue(rawFieldValue, Object.class);
		} catch (Exception e) {
			throw new IllegalStateException(
					"Feuille Excel 'workflow.scenarios': JSON invalide dans une valeur 'dataSet': '" + rawFieldValue + "'", e);
		}
	}

	/**
	 * Coupe 'raw' sur chaque occurrence de 'delimiter' situee au niveau d'imbrication 0 (compte
	 * les '{'/'[' et '}'/']' pour ignorer les occurrences a l'interieur d'un fragment JSON).
	 */
	private List<String> splitTopLevel(String raw, char delimiter) {
		List<String> parts = new ArrayList<>();
		int depth = 0;
		StringBuilder current = new StringBuilder();
		for (char c : raw.toCharArray()) {
			if (c == '{' || c == '[') {
				depth++;
			} else if (c == '}' || c == ']') {
				depth--;
			}
			if (c == delimiter && depth == 0) {
				parts.add(current.toString());
				current.setLength(0);
			} else {
				current.append(c);
			}
		}
		parts.add(current.toString());
		return parts;
	}

	private String stringValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			return null;
		}
		String value = formatter.formatCellValue(cell, evaluator).trim();
		return value.isEmpty() ? null : value;
	}
}
