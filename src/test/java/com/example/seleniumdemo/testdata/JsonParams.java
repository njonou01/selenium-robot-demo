package com.example.seleniumdemo.testdata;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.seleniumtests.uipage.PageObject;

/**
 * Charge une variable serveur JSON imbriquee, uploadee comme FICHIER (pas de texte inline), et
 * l'aplatit en chemins points dans {@link #flatValues} (herite de {@link AbstractParams}, qui
 * porte la recherche par chemin). Le nom de variable passe a {@link #load(String)} est utilise
 * tel quel (aucun suffixe ajoute), mise en cache separement par nom.
 */
public class JsonParams extends AbstractParams {

	private static final Map<String, JsonParams> cache = new HashMap<>();

	public JsonParams(String json) throws Exception {
		JsonNode root = new ObjectMapper().readTree(json);
		flatten("", root);
	}

	/**
	 * Charge (une seule fois par run et par variable) le fichier de la variable serveur nommee
	 * exactement "variableName", via PageObject.paramFile - donc uniquement utilisable une fois le
	 * contexte de test pret (contrairement au DataProvider qui lit "workflow.scenarios").
	 */
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
