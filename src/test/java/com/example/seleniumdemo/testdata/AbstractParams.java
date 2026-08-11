package com.example.seleniumdemo.testdata;

import java.util.LinkedHashMap;
import java.util.Map;

import net.ricecode.similarity.JaroWinklerStrategy;
import net.ricecode.similarity.SimilarityStrategy;

/**
 * Porte la logique de recherche PARTAGEE entre les implementations de {@link Params}, quel que
 * soit le format d'origine (JSON imbrique, "cle=valeur"...). Chaque sous-classe se contente de
 * remplir {@link #flatValues} depuis son propre format, via son propre constructeur.
 *
 * Si le chemin donne correspond a un noeud PARENT (plusieurs sous-valeurs, ex: "payBill" pour
 * {"payBill": {"payeeName": ..., "amount": ...}}) plutot qu'a une valeur unique, {@link #get(String)}
 * leve une erreur explicite au lieu de deviner - utiliser {@link #getSubtree(String)} a la place.
 *
 * 4 niveaux de recherche pour {@link #get(String)}, du plus strict au plus permissif:
 * - chemin complet: "transfer.amount"
 * - chemin court (prefixe "$."): "$.amount" -> premier chemin qui SE TERMINE par "amount", peu
 *   importe ce qu'il y a avant
 * - recherche floue exacte/contient (juste un nom, sans point ni $): "amount" -> dernier segment
 *   du chemin qui correspond (exact insensible a la casse, sinon "contient")
 * - tolerance aux fautes de frappe (dernier recours, Jaro-Winkler, seuil {@link #TYPO_TOLERANCE_THRESHOLD}):
 *   "amout" -> dernier segment le plus proche s'il depasse le seuil. Risque assume: un typo cote
 *   appelant peut silencieusement retourner la mauvaise valeur si deux cles se ressemblent - seuil
 *   volontairement haut pour limiter ce risque, ne remplace pas les 3 niveaux stricts precedents.
 */
public abstract class AbstractParams implements Params {

	private static final double TYPO_TOLERANCE_THRESHOLD = 0.85;
	private static final SimilarityStrategy SIMILARITY = new JaroWinklerStrategy();

	protected final Map<String, String> flatValues = new LinkedHashMap<>();

	@Override
	public String get(String path) {
		if (path == null || path.isBlank()) {
			throw new IllegalArgumentException("Chemin vide.");
		}

		if (flatValues.containsKey(path)) {
			return flatValues.get(path);
		}

		if (!path.startsWith("$.")) {
			String subtreePrefix = path + ".";
			boolean isSubtreeNode = flatValues.keySet().stream().anyMatch(key -> key.startsWith(subtreePrefix));
			if (isSubtreeNode) {
				throw new IllegalStateException(
						"'" + path + "' est un sous-arbre (plusieurs valeurs), pas une valeur unique. Utiliser getSubtree(\"" + path + "\") a la place.");
			}
		}

		if (path.startsWith("$.")) {
			String suffix = path.substring(2);
			for (Map.Entry<String, String> entry : flatValues.entrySet()) {
				if (entry.getKey().equals(suffix) || entry.getKey().endsWith("." + suffix)) {
					return entry.getValue();
				}
			}
			throw new IllegalStateException(
					"Aucun parametre ne se termine par '" + suffix + "'. Chemins disponibles: " + flatValues.keySet());
		}

		for (Map.Entry<String, String> entry : flatValues.entrySet()) {
			String lastSegment = entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1);
			if (lastSegment.equalsIgnoreCase(path)) {
				return entry.getValue();
			}
		}
		for (Map.Entry<String, String> entry : flatValues.entrySet()) {
			if (entry.getKey().toLowerCase().contains(path.toLowerCase())) {
				return entry.getValue();
			}
		}

		String bestMatch = null;
		double bestScore = TYPO_TOLERANCE_THRESHOLD;
		for (Map.Entry<String, String> entry : flatValues.entrySet()) {
			String lastSegment = entry.getKey().substring(entry.getKey().lastIndexOf('.') + 1);
			double score = SIMILARITY.score(path.toLowerCase(), lastSegment.toLowerCase());
			if (score > bestScore) {
				bestScore = score;
				bestMatch = entry.getKey();
			}
		}
		if (bestMatch != null) {
			return flatValues.get(bestMatch);
		}

		throw new IllegalStateException("Parametre '" + path + "' introuvable. Chemins disponibles: " + flatValues.keySet());
	}

	@Override
	public String get(String path, String defaultValue) {
		try {
			return get(path);
		} catch (IllegalStateException e) {
			return defaultValue;
		}
	}

	@Override
	public Map<String, String> getSubtree(String path) {
		if (path == null || path.isBlank()) {
			throw new IllegalArgumentException("Chemin vide.");
		}
		String prefix = path + ".";
		Map<String, String> subtree = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : flatValues.entrySet()) {
			if (entry.getKey().startsWith(prefix)) {
				subtree.put(entry.getKey().substring(prefix.length()), entry.getValue());
			}
		}
		if (subtree.isEmpty()) {
			throw new IllegalStateException(
					"Aucun sous-arbre pour '" + path + "'. Chemins disponibles: " + flatValues.keySet());
		}
		return subtree;
	}
}
