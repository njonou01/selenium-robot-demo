package com.example.seleniumdemo.custom.unitaire;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.seleniumdemo.custom.utils.PageDetector;
import com.seleniumtests.customexception.ScenarioException;
import com.seleniumtests.uipage.PageObject;
import com.seleniumtests.uipage.htmlelements.HtmlElement;

/**
 * 'PageDetector' n'a jamais ete teste sans navigateur reel malgre une logique non triviale
 * (stabilite sur plusieurs controles avant de trancher). Ici, HtmlElement/PageObject mockes
 * (Mockito - ni l'un ni l'autre n'est final) pour verifier la logique pure de
 * 'resolveStableMatch' independamment de Selenium.
 */
public class PageDetectorTest {

	private Map.Entry<HtmlElement, Supplier<? extends PageObject>> candidateAlwaysVisible(String label) {
		HtmlElement element = Mockito.mock(HtmlElement.class);
		Mockito.when(element.isDisplayed()).thenReturn(true);
		Mockito.when(element.toString()).thenReturn(label);
		PageObject page = Mockito.mock(PageObject.class);
		return PageDetector.candidate(element, () -> page);
	}

	private Map.Entry<HtmlElement, Supplier<? extends PageObject>> candidateNeverVisible(String label) {
		HtmlElement element = Mockito.mock(HtmlElement.class);
		Mockito.when(element.isDisplayed()).thenReturn(false);
		Mockito.when(element.toString()).thenReturn(label);
		return PageDetector.candidate(element, () -> Mockito.mock(PageObject.class));
	}

	@Test
	public void resolvesTheOnlyCandidateThatStaysStablyVisible() {
		PageObject expected = Mockito.mock(PageObject.class);
		HtmlElement element = Mockito.mock(HtmlElement.class);
		Mockito.when(element.isDisplayed()).thenReturn(true);

		PageObject resolved = PageDetector.waitForOneOf(5, List.of(PageDetector.candidate(element, () -> expected)));

		Assert.assertSame(resolved, expected);
	}

	@Test(expectedExceptions = ScenarioException.class, expectedExceptionsMessageRegExp = ".*no candidate provided.*")
	public void rejectsEmptyCandidateList() {
		PageDetector.waitForOneOf(List.of());
	}

	@Test(expectedExceptions = ScenarioException.class, expectedExceptionsMessageRegExp = ".*none of the expected pages appeared.*")
	public void timesOutWhenNoCandidateIsEverVisible() {
		PageDetector.waitForOneOf(1, List.of(candidateNeverVisible("jamaisVisible")));
	}

	/**
	 * 2 candidates visibles EN MEME TEMPS en permanence: etat ambigu tout du long, jamais
	 * "exactement une candidate visible" - doit finir par timeout, pas trancher au hasard sur
	 * l'ordre de la liste.
	 */
	@Test(expectedExceptions = ScenarioException.class, expectedExceptionsMessageRegExp = ".*none of the expected pages appeared.*")
	public void timesOutWhenTwoCandidatesStayVisibleSimultaneously() {
		PageDetector.waitForOneOf(1, List.of(candidateAlwaysVisible("A"), candidateAlwaysVisible("B")));
	}

	/**
	 * Une exception levee par isDisplayed() (ex: element detache du DOM pendant une transition
	 * de page) doit etre traitee comme "pas visible", pas propagee et pas faire planter la
	 * detection en cours.
	 */
	@Test
	public void treatsExceptionFromIsDisplayedAsNotVisibleRatherThanPropagating() {
		PageObject expected = Mockito.mock(PageObject.class);
		HtmlElement flaky = Mockito.mock(HtmlElement.class);
		Mockito.when(flaky.isDisplayed()).thenThrow(new org.openqa.selenium.StaleElementReferenceException("detached"));
		HtmlElement stable = Mockito.mock(HtmlElement.class);
		Mockito.when(stable.isDisplayed()).thenReturn(true);

		PageObject resolved = PageDetector.waitForOneOf(5, List.of(
				PageDetector.candidate(flaky, () -> Mockito.mock(PageObject.class)),
				PageDetector.candidate(stable, () -> expected)));

		Assert.assertSame(resolved, expected);
	}

	/**
	 * Visible puis invisible puis visible a nouveau (candidate change d'etat) avant de se
	 * stabiliser: le compteur de stabilite doit repartir de zero a chaque changement, pas
	 * accumuler across une interruption.
	 */
	@Test
	public void restartsStabilityCountAfterATemporaryDisappearance() {
		PageObject expected = Mockito.mock(PageObject.class);
		HtmlElement element = Mockito.mock(HtmlElement.class);
		// visible, invisible, puis visible en continu - jamais 2 controles VRAIMENT consecutifs
		// stables avant le 3eme "true"
		Mockito.when(element.isDisplayed()).thenReturn(true, false, true, true, true, true);

		PageObject resolved = PageDetector.waitForOneOf(5, List.of(PageDetector.candidate(element, () -> expected)));

		Assert.assertSame(resolved, expected);
	}
}
