package com.example.seleniumdemo.custom.testdata;

import java.util.LinkedHashMap;
import java.util.Map;

import net.ricecode.similarity.JaroWinklerStrategy;
import net.ricecode.similarity.SimilarityStrategy;

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
			throw new IllegalStateException("Parametre '" + path + "' introuvable. Chemins disponibles: " + flatValues.keySet()
					+ ". Pour une recherche par nom approximatif (deconseillee), utiliser getFuzzy(\"" + path + "\").");
		}

		String suffix = path.substring(2);
		for (Map.Entry<String, String> entry : flatValues.entrySet()) {
			if (entry.getKey().equals(suffix) || entry.getKey().endsWith("." + suffix)) {
				return entry.getValue();
			}
		}
		throw new IllegalStateException(
				"Aucun parametre ne se termine par '" + suffix + "'. Chemins disponibles: " + flatValues.keySet());
	}

	public String getFuzzy(String path) {
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
		} else {
			String suffix = path.substring(2);
			for (Map.Entry<String, String> entry : flatValues.entrySet()) {
				if (entry.getKey().equals(suffix) || entry.getKey().endsWith("." + suffix)) {
					return entry.getValue();
				}
			}
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

		throw new IllegalStateException("Parametre '" + path + "' introuvable (meme en recherche floue). Chemins disponibles: " + flatValues.keySet());
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
