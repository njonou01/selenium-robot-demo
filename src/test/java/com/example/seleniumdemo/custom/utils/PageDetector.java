package com.example.seleniumdemo.custom.utils;

import com.seleniumtests.customexception.ScenarioException;
import com.seleniumtests.uipage.PageObject;
import com.seleniumtests.uipage.htmlelements.HtmlElement;
import com.seleniumtests.util.logging.ScenarioLogger;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class PageDetector {

	private static final ScenarioLogger LOGGER = ScenarioLogger.getScenarioLogger(PageDetector.class);

	private static final int DEFAULT_TIMEOUT_SECONDS = 30;
	private static final int DEFAULT_STABLE_CHECKS = 2;
	private static final long DEFAULT_POLL_INTERVAL_MILLIS = 200;

	private PageDetector() {
	}

	public static Map.Entry<HtmlElement, Supplier<? extends PageObject>> candidate(
			HtmlElement identifier, Supplier<? extends PageObject> supplier) {
		return Map.entry(identifier, supplier);
	}

	public static PageObject waitForOneOf(List<Map.Entry<HtmlElement, Supplier<? extends PageObject>>> candidates) {
		return waitForOneOf(DEFAULT_TIMEOUT_SECONDS, candidates, false);
	}

	public static PageObject waitForOneOf(int timeoutSeconds, List<Map.Entry<HtmlElement, Supplier<? extends PageObject>>> candidates) {
		return waitForOneOf(timeoutSeconds, candidates, false);
	}

	public static PageObject waitForOneOf(List<Map.Entry<HtmlElement, Supplier<? extends PageObject>>> candidates, boolean logDetection) {
		return waitForOneOf(DEFAULT_TIMEOUT_SECONDS, candidates, logDetection);
	}

	public static PageObject waitForOneOf(int timeoutSeconds, List<Map.Entry<HtmlElement, Supplier<? extends PageObject>>> candidates,
			boolean logDetection) {
		if (candidates == null || candidates.isEmpty()) {
			throw new ScenarioException("PageDetector: no candidate provided");
		}

		Map.Entry<HtmlElement, Supplier<? extends PageObject>> matched =
				resolveStableMatch(candidates, timeoutSeconds, DEFAULT_STABLE_CHECKS, DEFAULT_POLL_INTERVAL_MILLIS, logDetection);

		return matched.getValue().get();
	}

	/**
	 * Attend qu'exactement une candidate soit visible, de facon stable (meme candidate revue sur
	 * 'requiredStableChecks' controles consecutifs) avant de la retenir. Contrairement a "premier
	 * candidat visible trouve", ca evite un faux positif pendant une transition de page ou 2
	 * candidates sont visibles en meme temps un court instant (l'ancienne pas encore disparue, la
	 * nouvelle deja la): dans ce cas l'etat est traite comme ambigu et on reessaie, plutot que de
	 * trancher au hasard sur l'ordre de la liste.
	 */
	private static Map.Entry<HtmlElement, Supplier<? extends PageObject>> resolveStableMatch(
			List<Map.Entry<HtmlElement, Supplier<? extends PageObject>>> candidates,
			int timeoutSeconds, int requiredStableChecks, long pollIntervalMillis, boolean logDetection) {

		long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
		Map.Entry<HtmlElement, Supplier<? extends PageObject>> lastSeen = null;
		int stableCount = 0;

		while (System.currentTimeMillis() < deadline) {
			List<Map.Entry<HtmlElement, Supplier<? extends PageObject>>> visibleNow = candidates.stream()
					.filter(PageDetector::isDisplayedSafely)
					.toList();

			if (visibleNow.size() != 1) {
				if (logDetection && stableCount > 0) {
					LOGGER.log("PageDetector: etat ambigu (" + visibleNow.size()
							+ " candidate(s) visible(s) simultanement), reessai...");
				}
				stableCount = 0;
				lastSeen = null;
			} else if (visibleNow.get(0).equals(lastSeen)) {
				stableCount++;
				if (stableCount >= requiredStableChecks) {
					if (logDetection) {
						LOGGER.log("PageDetector: detected page identified by " + lastSeen.getKey());
					}
					return lastSeen;
				}
			} else {
				lastSeen = visibleNow.get(0);
				stableCount = 1;
			}

			sleep(pollIntervalMillis);
		}

		String identifiers = candidates.stream()
				.map(candidate -> candidate.getKey().toString())
				.reduce((a, b) -> a + ", " + b)
				.orElse("none");

		throw new ScenarioException("PageDetector: none of the expected pages appeared (in a stable, unambiguous state) within "
				+ timeoutSeconds + " seconds. Candidates: [" + identifiers + "]");
	}

	private static boolean isDisplayedSafely(Map.Entry<HtmlElement, Supplier<? extends PageObject>> candidate) {
		try {
			return candidate.getKey().isDisplayed();
		} catch (Exception e) {
			return false;
		}
	}

	private static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new ScenarioException("PageDetector: interrompu pendant l'attente.", e);
		}
	}
}
