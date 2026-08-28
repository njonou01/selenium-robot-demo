package com.example.seleniumdemo.custom.unitaire;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.example.seleniumdemo.custom.server.VariableServerConfig;

public class VariableServerConfigTest {

	@Test
	public void isCompleteWhenUrlAndTokenBothPresent() {
		VariableServerConfig config = new VariableServerConfig("http://localhost:8000", "token", "app", "1.0", "DEV");

		Assert.assertTrue(config.isComplete());
	}

	@Test
	public void isNotCompleteWhenUrlMissing() {
		VariableServerConfig config = new VariableServerConfig(null, "token", "app", "1.0", "DEV");

		Assert.assertFalse(config.isComplete());
	}

	@Test
	public void isNotCompleteWhenUrlBlank() {
		VariableServerConfig config = new VariableServerConfig("", "token", "app", "1.0", "DEV");

		Assert.assertFalse(config.isComplete());
	}

	@Test
	public void isNotCompleteWhenTokenMissing() {
		VariableServerConfig config = new VariableServerConfig("http://localhost:8000", null, "app", "1.0", "DEV");

		Assert.assertFalse(config.isComplete());
	}

	@Test
	public void isNotCompleteWhenTokenBlank() {
		VariableServerConfig config = new VariableServerConfig("http://localhost:8000", "", "app", "1.0", "DEV");

		Assert.assertFalse(config.isComplete());
	}

	@Test
	public void applicationVersionEnvironmentDoNotAffectCompleteness() {
		// 'isComplete()' ne regarde que url/token - application/version/environment absents
		// n'empechent pas de considerer la config comme "complete" a ce niveau.
		VariableServerConfig config = new VariableServerConfig("http://localhost:8000", "token", null, null, null);

		Assert.assertTrue(config.isComplete());
	}
}
