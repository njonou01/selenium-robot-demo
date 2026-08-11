package com.example.seleniumdemo.utils;

import java.time.Duration;
import java.util.function.BooleanSupplier;

import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.seleniumtests.driver.WebUIDriver;

public class WaitUtils {

	private WaitUtils() {
	}

	public static boolean waitUntil(BooleanSupplier condition, Duration timeout) {
		long deadline = System.currentTimeMillis() + timeout.toMillis();
		while (System.currentTimeMillis() < deadline) {
			if (condition.getAsBoolean()) {
				return true;
			}
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return false;
			}
		}
		return false;
	}

	public static boolean waitForAlertPresent(Duration timeout) {
		try {
			new WebDriverWait(WebUIDriver.getWebDriver(false), timeout)
				.until(ExpectedConditions.alertIsPresent());
			return true;
		} catch (TimeoutException e) {
			return false;
		}
	}

	public static void acceptAlertIfPresent(Duration timeout) {
		if (waitForAlertPresent(timeout)) {
			WebUIDriver.getWebDriver(false).switchTo().alert().accept();
		}
	}

	public static void waitForUrlContains(String fragment, Duration timeout) {
		new WebDriverWait(WebUIDriver.getWebDriver(false), timeout)
			.until(ExpectedConditions.urlContains(fragment));
	}
}
