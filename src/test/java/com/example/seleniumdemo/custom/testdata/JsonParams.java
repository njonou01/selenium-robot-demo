package com.example.seleniumdemo.custom.testdata;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seleniumtests.uipage.PageObject;

public class JsonParams extends AbstractParams {

	private static final Map<String, JsonParams> cache = new HashMap<>();

	public JsonParams(String json) throws Exception {
		JsonNode root = new ObjectMapper().readTree(json);
		flatten("", root);
	}

	public static synchronized JsonParams load(String variableName) {
		return cache.computeIfAbsent(variableName, name -> {
			try {
				File file = PageObject.paramFile(name);
				return new JsonParams(Files.readString(file.toPath()));
			} catch (Exception e) {
				throw new IllegalStateException("Impossible de charger le fichier de '" + name + "' depuis le serveur de variable.", e);
			}
		});
	}

	private void flatten(String prefix, JsonNode node) {
		if (node.isObject()) {
			node.properties().forEach(entry -> {
				String path = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
				flatten(path, entry.getValue());
			});
		} else if (node.isValueNode()) {
			flatValues.put(prefix, node.asText());
		}
	}
}
