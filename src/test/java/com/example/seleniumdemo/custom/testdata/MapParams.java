package com.example.seleniumdemo.custom.testdata;

import java.util.HashMap;
import java.util.Map;

import com.seleniumtests.uipage.PageObject;

public class MapParams extends AbstractParams {

	private static final Map<String, MapParams> cache = new HashMap<>();

	public MapParams(String raw) {
		if (raw == null || raw.isBlank()) {
			return;
		}
		for (String pair : raw.split(";")) {
			if (pair.isBlank()) {
				continue;
			}
			String[] parts = pair.split("[=:]", 2);
			if (parts.length != 2) {
				throw new IllegalArgumentException("Paire invalide (attendu cle=valeur ou cle:valeur): '" + pair.trim() + "'");
			}
			String key = parts[0].trim();
			if (key.isEmpty() || key.contains("=") || key.contains(":")) {
				throw new IllegalArgumentException(
						"Cle invalide '" + key + "' dans la paire '" + pair.trim() + "' - une cle ne doit pas contenir '=' ni ':'.");
			}
			flatValues.put(key, parts[1].trim());
		}
	}

	public static synchronized MapParams load(String variableName) {
		return cache.computeIfAbsent(variableName, name -> {
			try {
				return new MapParams(PageObject.param(name));
			} catch (Exception e) {
				throw new IllegalStateException("Impossible de charger '" + name + "' depuis le serveur de variable.", e);
			}
		});
	}
}
