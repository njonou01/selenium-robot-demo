package com.example.seleniumdemo.testdata;

import java.util.HashMap;
import java.util.Map;

import com.seleniumtests.uipage.PageObject;

/**
 * Charge une variable serveur EN TEXTE INLINE au format plat "cle1=valeur1;cle2:valeur2;..." (pas
 * de fichier, pas de JSON) directement dans {@link #flatValues} (herite de {@link AbstractParams},
 * qui porte la recherche par chemin). Pas de parsing recursif necessaire: le format est deja plat.
 * Paires separees par ";", cle et valeur separees par "=" OU ":" (au choix, y compris mixe dans la
 * meme variable).
 */
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
			// zero-risque: si la cle contient encore "=" ou ":", la coupure a eu lieu au mauvais
			// endroit (cle mal formee) - on refuse au lieu de stocker une donnee fausse en silence.
			if (key.isEmpty() || key.contains("=") || key.contains(":")) {
				throw new IllegalArgumentException(
						"Cle invalide '" + key + "' dans la paire '" + pair.trim() + "' - une cle ne doit pas contenir '=' ni ':'.");
			}
			flatValues.put(key, parts[1].trim());
		}
	}

	/**
	 * Charge (une seule fois par run et par variable) la variable texte serveur nommee exactement
	 * "variableName", via PageObject.param - donc uniquement utilisable une fois le contexte de test
	 * pret.
	 */
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
