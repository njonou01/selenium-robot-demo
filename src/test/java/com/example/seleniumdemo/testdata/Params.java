package com.example.seleniumdemo.testdata;

import java.util.Map;

/**
 * Contrat commun pour une source de parametres cle/valeur chargee depuis le serveur de variable,
 * quel que soit le format de serialisation. Permet d'ecrire {@code Params p = JsonParams.load(...)}
 * ou {@code Params p = MapParams.load(...)} indifferemment.
 */
public interface Params {

	/**
	 * @param path chemin complet ("transfer.amount"), court ("$.amount") ou nom flou ("amount")
	 * @return la valeur associee
	 * @throws IllegalStateException si aucun parametre ne correspond
	 */
	String get(String path);

	/**
	 * Comme {@link #get(String)}, mais retourne defaultValue au lieu de lever une exception si rien
	 * ne correspond.
	 */
	String get(String path, String defaultValue);

	/**
	 * @param path chemin exact d'un noeud parent (ex: "payBill") - pas de recherche floue ici,
	 *             contrairement a {@link #get(String)}
	 * @return les entrees dont le chemin complet commence par "path.", cles relativisees a path
	 *         (ex: "payBill.payeeName" -> "payeeName")
	 * @throws IllegalStateException si aucune entree ne commence par "path."
	 */
	Map<String, String> getSubtree(String path);
}
