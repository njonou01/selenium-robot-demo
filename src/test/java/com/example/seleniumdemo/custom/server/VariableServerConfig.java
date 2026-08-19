package com.example.seleniumdemo.custom.server;

import org.testng.ITestContext;

import com.seleniumtests.core.SeleniumTestsContext;
import com.seleniumtests.core.SeleniumTestsContextManager;

public record VariableServerConfig(String baseUrl, String token, String application, String version, String environment) {

	public static VariableServerConfig fromRunningContext() {
		String baseUrl = SeleniumTestsContextManager.getThreadContext().seleniumServer().getSeleniumRobotServerUrl();
		String token = SeleniumTestsContextManager.getThreadContext().seleniumServer().getSeleniumRobotServerToken();
		String application = SeleniumTestsContextManager.getApplicationName();
		String environment = SeleniumTestsContextManager.getThreadContext().getTestEnv();
		String version = SeleniumTestsContextManager.getApplicationVersion();
		return new VariableServerConfig(baseUrl, token, application, version, environment);
	}

	public static VariableServerConfig fromXmlTest(ITestContext testContext) {
		String baseUrl = testContext.getCurrentXmlTest().getParameter("seleniumRobotServerUrl");

		String token = System.getenv("SELENIUM_ROBOT_SERVER_TOKEN");
		String tokenFromConfig = testContext.getCurrentXmlTest().getParameter("seleniumRobotServerToken");
		if (tokenFromConfig == null) {
			tokenFromConfig = System.getProperty("seleniumRobotServerToken");
		}
		if (tokenFromConfig != null) {
			token = tokenFromConfig;
		}

		String application = SeleniumTestsContextManager.getApplicationName();
		String environment = testContext.getCurrentXmlTest().getParameter("env");
		if (environment == null) {
			environment = SeleniumTestsContext.DEFAULT_TEST_ENV;
		}
		String version = SeleniumTestsContextManager.getApplicationVersion();

		return new VariableServerConfig(baseUrl, token, application, version, environment);
	}

	public boolean isComplete() {
		return baseUrl != null && !baseUrl.isEmpty() && token != null && !token.isEmpty();
	}
}
