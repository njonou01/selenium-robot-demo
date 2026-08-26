package com.example.seleniumdemo.custom.scenarios;

import java.util.List;
import java.util.Map;

/**
 * 'parseError' non-null signale un scenario dont le parsing (JSON/Excel) a echoue: au lieu de
 * faire planter tout le fichier (et donc tous les AUTRES scenarios, valides), ce scenario est
 * conserve avec son erreur et echouera individuellement a l'execution - les autres scenarios du
 * meme fichier continuent de tourner normalement.
 */
public record ScenarioDef(String name, String sinistre, List<String> steps, boolean sbc, Map<String, Object> dataSet,
		String parseError) {

	public ScenarioDef(String name, String sinistre, List<String> steps, boolean sbc, Map<String, Object> dataSet) {
		this(name, sinistre, steps, sbc, dataSet, null);
	}

	public static ScenarioDef invalid(String name, String error) {
		return new ScenarioDef(name, null, List.of(), false, Map.of(), error);
	}
}
