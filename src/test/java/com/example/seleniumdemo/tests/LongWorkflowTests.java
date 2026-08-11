package com.example.seleniumdemo.tests;

import org.testng.annotations.Test;

import com.seleniumtests.core.runner.SeleniumTestPlan;

import com.example.seleniumdemo.workflows.EcommerceWorkflow;
import com.example.seleniumdemo.workflows.BankingWorkflow;
import com.example.seleniumdemo.workflows.ElectronicsWorkflow;
import com.example.seleniumdemo.workflows.HRWorkflow;
import com.example.seleniumdemo.workflows.PetStoreWorkflow;

public class LongWorkflowTests extends SeleniumTestPlan {


	@Test
	public void testAllFullFlows() throws Exception {
		new EcommerceWorkflow().fullEcommerceFlow();
		new BankingWorkflow().fullBankingFlow();
		new ElectronicsWorkflow().fullElectronicsFlow();
	}
}
