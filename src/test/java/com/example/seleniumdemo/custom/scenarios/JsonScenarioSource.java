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
		try {
			String json = new String(content, StandardCharsets.UTF_8);
			ObjectMapper mapper = new ObjectMapper();
			List<Map<String, Object>> raw = mapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
			});
			List<ScenarioDef> scenarios = new ArrayList<>();
			for (Map<String, Object> entry : raw) {
				String name = (String) entry.get("name");
				String sinistre = (String) entry.get("sinistre");
				List<String> steps = mapper.convertValue(entry.get("steps"), new TypeReference<List<String>>() {
				});
				boolean sbc = Boolean.TRUE.equals(entry.get("sbc"));
				Map<String, Object> dataSet = entry.get("dataSet") == null ? Map.of()
						: mapper.convertValue(entry.get("dataSet"), new TypeReference<Map<String, Object>>() {
						});
				scenarios.add(new ScenarioDef(name, sinistre, steps, sbc, dataSet));
			}
			return scenarios;
		} catch (Exception e) {
			throw new IllegalStateException("Contenu JSON de 'workflow.scenarios' illisible.", e);
		}
	}
}
