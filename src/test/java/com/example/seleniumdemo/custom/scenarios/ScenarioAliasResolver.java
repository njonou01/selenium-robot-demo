package com.example.seleniumdemo.custom.scenarios;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Alias de code de workflow, optionnel, portee a UN scenario ('aliases' declare dans ce
 * scenario, JSON ou Excel). Resolu entierement au parsing, avant construction du ScenarioDef:
 * le reste du moteur (ScenarioDef, WorkflowVariableScanner, ServerDrivenScenarioTest) ne sait
 * meme pas qu'un alias a existe, il ne voit que des vrais codes. Un alias inconnu/mal ecrit
 * n'est PAS valide ici - il ressort simplement comme "code de workflow inconnu" plus loin dans
 * le pipeline existant (meme message que pour un vrai code mal ecrit).
 */
final class ScenarioAliasResolver {

	// Groupe 1 gourmand ('.+', pas '[^.}]+') : le code/alias reference peut lui-meme contenir un
	// point ('banking.full'), la coupure code/champ doit se faire sur le DERNIER point.
	private static final Pattern RESULT_REFERENCE = Pattern.compile("^\\$\\{result:(.+)\\.([^.}]+)\\}$");

	private ScenarioAliasResolver() {
	}

	static List<String> resolveSteps(List<String> steps, Map<String, String> aliases) {
		if (aliases.isEmpty() || steps == null) {
			return steps;
		}
		List<String> resolved = new ArrayList<>(steps.size());
		for (String step : steps) {
			resolved.add(aliases.getOrDefault(step, step));
		}
		return resolved;
	}

	/**
	 * Substitue les alias dans le dataSet: sur les cles de premier niveau (portee 'per-workflow'
	 * du dataSet, ex: dataSet["b"] -> dataSet["banking.full"]) et dans les valeurs
	 * '${result:alias.champ}' a n'importe quelle profondeur. Les cles des niveaux plus profonds
	 * (champs d'un record imbrique, ex: 'employee.firstName') ne sont jamais touchees - un alias
	 * ne represente qu'un code de workflow, jamais un nom de champ.
	 */
	static Map<String, Object> resolveDataSet(Map<String, Object> dataSet, Map<String, String> aliases) {
		if (aliases.isEmpty() || dataSet == null) {
			return dataSet;
		}
		Map<String, Object> resolved = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : dataSet.entrySet()) {
			String key = aliases.getOrDefault(entry.getKey(), entry.getKey());
			resolved.put(key, resolveValue(entry.getValue(), aliases));
		}
		return resolved;
	}

	private static Object resolveValue(Object value, Map<String, String> aliases) {
		if (value instanceof Map<?, ?> map) {
			Map<String, Object> resolved = new LinkedHashMap<>();
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				resolved.put(String.valueOf(entry.getKey()), resolveValue(entry.getValue(), aliases));
			}
			return resolved;
		}
		if (value instanceof List<?> list) {
			List<Object> resolved = new ArrayList<>(list.size());
			for (Object element : list) {
				resolved.add(resolveValue(element, aliases));
			}
			return resolved;
		}
		if (value instanceof String text) {
			Matcher matcher = RESULT_REFERENCE.matcher(text);
			if (matcher.matches() && aliases.containsKey(matcher.group(1))) {
				return "${result:" + aliases.get(matcher.group(1)) + "." + matcher.group(2) + "}";
			}
		}
		return value;
	}
}
