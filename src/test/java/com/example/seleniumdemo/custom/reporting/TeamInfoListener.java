package com.example.seleniumdemo.custom.reporting;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import org.testng.ITestListener;
import org.testng.ITestResult;

import com.seleniumtests.core.utils.TestNGResultUtils;
import com.seleniumtests.reporter.info.StringInfo;

public class TeamInfoListener implements ITestListener {

	private static final String EQUIPE_INFO_KEY = "Equipe";
	private static final String CAMPAGNE_INFO_KEY = "Campagne";

	@Override
	public void onTestStart(ITestResult result) {
		String equipe = result.getTestContext().getCurrentXmlTest().getParameter("equipe");
		if (equipe != null && !equipe.isBlank()) {
			TestNGResultUtils.setTestInfo(result, EQUIPE_INFO_KEY, new StringInfo(equipe));
		}

		LocalDate today = LocalDate.now();
		String mois = today.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
		String campagne = Character.toUpperCase(mois.charAt(0)) + mois.substring(1) + " " + today.getYear();
		TestNGResultUtils.setTestInfo(result, CAMPAGNE_INFO_KEY, new StringInfo(campagne));
	}
}
