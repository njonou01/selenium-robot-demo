package com.example.seleniumdemo.utils;

import java.time.Duration;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.seleniumtests.driver.WebUIDriver;
import com.seleniumtests.uipage.PageObject;

public class WindowsUtils {

	private WindowsUtils() {
	}

	public static void waitForPageLoadComplete() {
		waitForPageLoadComplete(Duration.ofSeconds(10));
	}

	public static void waitForPageLoadComplete(Duration timeout) {
		new WebDriverWait(WebUIDriver.getWebDriver(false), timeout)
			.until(driver -> "complete".equals(
				((JavascriptExecutor) driver).executeScript("return document.readyState")
			));
	}

	public static void closeWindowAfterLoad(PageObject page) {
		waitForPageLoadComplete();
		page.close();
	}

	public static <T extends PageObject> T closeWindowAfterLoad(PageObject page, Class<T> previousPage) {
		waitForPageLoadComplete();
		return page.close(previousPage);
	}

	public static void switchWaitAndClose(PageObject page) {
		page.switchToNewWindow();
		waitForPageLoadComplete();
		page.close();
	}

	public static <T extends PageObject> T switchWaitAndClose(PageObject page, Class<T> previousPage) {
		page.switchToNewWindow();
		waitForPageLoadComplete();
		return page.close(previousPage);
	}
}
