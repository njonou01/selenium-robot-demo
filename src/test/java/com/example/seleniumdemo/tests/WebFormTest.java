package com.example.seleniumdemo.tests;

import org.testng.annotations.Test;

import com.seleniumtests.core.runner.SeleniumTestPlan;

import com.example.seleniumdemo.workflows.FormWorkflow;

public class WebFormTest extends SeleniumTestPlan {

	@Test
	public void testCompleteWorkflowChain() throws Exception {
		FormWorkflow workflow = new FormWorkflow();

		workflow.step1_verifyPage();
		workflow.step2_fillField("SINISTRE-2026-001");
		workflow.step1_verifyPage();
		workflow.step2_fillField("modified_value");
		workflow.step3_submit();
	}

	@Test
	public void testAllFormWorkflowMethods() throws Exception {
		new FormWorkflow().initialize();
		new FormWorkflow().processDeclareSinistre("SINISTRE-2026-002");
		new FormWorkflow().alternativeWorkflow("REF-99999");
		new FormWorkflow().complexWorkflow("scenario-complet");
	}
}
