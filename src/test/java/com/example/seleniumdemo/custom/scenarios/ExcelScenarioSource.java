package com.example.seleniumdemo.custom.scenarios;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
			Map<String, List<String>> stepsByScenario = new LinkedHashMap<>();

			String currentScenario = null;
			String currentSinistre = null;
			for (Row row : sheet) {
				if (row.getRowNum() <= headerRow) {
					continue;
				}
				String name = stringValue(row.getCell(0), formatter, evaluator);
				String sinistre = stringValue(row.getCell(1), formatter, evaluator);
				String step = stringValue(row.getCell(2), formatter, evaluator);

				if (name != null && !name.isBlank()) {
					if (!name.equals(currentScenario) && stepsByScenario.containsKey(name)) {
						throw new IllegalStateException("Feuille Excel 'workflow.scenarios': le scenario '" + name
								+ "' apparait dans 2 blocs distincts (ligne " + (row.getRowNum() + 1)
								+ "). Fusionner les 2 blocs ou renommer l'un des deux.");
					}
					currentScenario = name;
					currentSinistre = sinistre;
				}
				if (currentScenario == null) {
					throw new IllegalStateException("Feuille Excel 'workflow.scenarios': ligne " + (row.getRowNum() + 1)
							+ " sans scenario_name (ni fusionne depuis une ligne au-dessus).");
				}
				if (step == null || step.isBlank()) {
					continue;
				}

				sinistreByScenario.putIfAbsent(currentScenario, currentSinistre);
				stepsByScenario.computeIfAbsent(currentScenario, k -> new ArrayList<>()).add(step);
			}

			List<ScenarioDef> scenarios = new ArrayList<>();
			for (Map.Entry<String, List<String>> entry : stepsByScenario.entrySet()) {
				scenarios.add(new ScenarioDef(entry.getKey(), sinistreByScenario.get(entry.getKey()), entry.getValue()));
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

	private String stringValue(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
		if (cell == null || cell.getCellType() == CellType.BLANK) {
			return null;
		}
		String value = formatter.formatCellValue(cell, evaluator).trim();
		return value.isEmpty() ? null : value;
	}
}
