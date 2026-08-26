package com.example.seleniumdemo.custom.scenarios;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonScenarioSource implements ScenarioSource {

	@Override
	public List<ScenarioDef> parse(byte[] content) {
		List<Map<String, Object>> raw;
		try {
			String json = new String(content, StandardCharsets.UTF_8);
			raw = new ObjectMapper().readValue(json, new TypeReference<List<Map<String, Object>>>() {
			});
		} catch (Exception e) {
			throw new IllegalStateException("Contenu JSON de 'workflow.scenarios' illisible (structure globale invalide).", e);
		}

		List<ScenarioDef> scenarios = new ArrayList<>();
		int index = 0;
		for (Map<String, Object> entry : raw) {
			index++;
			String name = (String) entry.get("name");
			String fallbackName = name == null || name.isBlank() ? "Scenario JSON #" + index : name;
			try {
				ObjectMapper mapper = new ObjectMapper();
				String sinistre = (String) entry.get("sinistre");
				List<String> steps = mapper.convertValue(entry.get("steps"), new TypeReference<List<String>>() {
				});
				boolean sbc = Boolean.TRUE.equals(entry.get("sbc"));
				Map<String, Object> dataSet = entry.get("dataSet") == null ? Map.of()
						: mapper.convertValue(entry.get("dataSet"), new TypeReference<Map<String, Object>>() {
						});
				scenarios.add(new ScenarioDef(fallbackName, sinistre, steps, sbc, dataSet));
			} catch (Exception e) {
				scenarios.add(ScenarioDef.invalid(fallbackName,
						"Scenario '" + fallbackName + "' illisible dans 'workflow.scenarios': " + e.getMessage()));
			}
		}
		return scenarios;
	}
}
