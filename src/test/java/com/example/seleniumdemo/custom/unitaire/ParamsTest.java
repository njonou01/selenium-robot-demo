package com.example.seleniumdemo.custom.unitaire;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.seleniumdemo.custom.testdata.JsonParams;
import com.example.seleniumdemo.custom.testdata.MapParams;

/**
 * 'AbstractParams' est abstraite - verifiee ici via ses deux implementations concretes,
 * construites directement (pas '.load(...)', qui appelle le serveur de variable) pour rester un
 * test pur, sans reseau.
 */
public class ParamsTest {

	@Test
	public void mapParamsParsesFlatKeyValuePairs() {
		MapParams params = new MapParams("assure.nom=Dupont;assure.prenom=Julie");

		Assert.assertEquals(params.get("assure.nom"), "Dupont");
		Assert.assertEquals(params.get("assure.prenom"), "Julie");
	}

	@Test
	public void mapParamsAcceptsColonAsKeyValueSeparatorToo() {
		MapParams params = new MapParams("cle:valeur");

		Assert.assertEquals(params.get("cle"), "valeur");
	}

	@Test
	public void mapParamsAllowsEmptyValue() {
		MapParams params = new MapParams("cle=");

		Assert.assertEquals(params.get("cle"), "");
	}

	@Test
	public void mapParamsLastDuplicateKeyWins() {
		MapParams params = new MapParams("cle=1;cle=2");

		Assert.assertEquals(params.get("cle"), "2");
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void mapParamsRejectsPairWithoutSeparator() {
		new MapParams("cleSansValeur");
	}

	@Test
	public void jsonParamsFlattensNestedObjectToDottedPaths() throws Exception {
		JsonParams params = new JsonParams("{\"dossier\":{\"id\":\"SIN-2026-001\"},\"incident\":{\"date\":\"2026-08-01\"}}");

		Assert.assertEquals(params.get("dossier.id"), "SIN-2026-001");
		Assert.assertEquals(params.get("incident.date"), "2026-08-01");
	}

	@Test(expectedExceptions = IllegalStateException.class)
	public void getOnMissingPathThrows() {
		new MapParams("a=1").get("b");
	}

	@Test
	public void getWithDefaultReturnsDefaultInsteadOfThrowing() {
		MapParams params = new MapParams("a=1");

		Assert.assertEquals(params.get("b", "fallback"), "fallback");
		Assert.assertEquals(params.get("a", "fallback"), "1");
	}

	@Test(expectedExceptions = IllegalStateException.class)
	public void getOnSubtreePrefixRedirectsToGetSubtreeInstead() {
		new MapParams("assure.nom=Dupont;assure.prenom=Julie").get("assure");
	}

	@Test
	public void getSubtreeReturnsRelativeKeysUnderPrefix() {
		MapParams params = new MapParams("assure.nom=Dupont;assure.prenom=Julie;dossier.id=X");

		Map<String, String> subtree = params.getSubtree("assure");

		Assert.assertEquals(subtree, Map.of("nom", "Dupont", "prenom", "Julie"));
	}

	@Test(expectedExceptions = IllegalStateException.class)
	public void getSubtreeOnEmptyPrefixThrows() {
		new MapParams("a=1").getSubtree("nothingHere");
	}

	@Test
	public void dollarPrefixSearchesBySuffixAcrossPaths() {
		MapParams params = new MapParams("assure.nom=Dupont;beneficiaire.nom=Martin");

		Assert.assertEquals(params.get("$.nom"), "Dupont", "le premier chemin trouve gagne, dans l'ordre d'insertion");
	}

	/**
	 * Le score Jaro-Winkler compare le chemin cherche COMPLET au DERNIER SEGMENT seul d'une cle
	 * stockee - la tolerance de faute de frappe ne marche donc que sur un identifiant simple
	 * (pas de point), pas sur un chemin pointe complet avec une faute dans un segment intermediaire.
	 */
	@Test
	public void getFuzzyToleratesTypoOnSingleSegmentIdentifier() {
		MapParams params = new MapParams("assure.nom=Dupont");

		Assert.assertEquals(params.getFuzzy("nomm"), "Dupont");
	}

	@Test(expectedExceptions = IllegalStateException.class)
	public void getFuzzyStillThrowsWhenNothingCloseEnough() {
		new MapParams("assure.nom=Dupont").getFuzzy("completelyUnrelatedKey12345");
	}
}
