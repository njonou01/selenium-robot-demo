package com.example.seleniumdemo.tests;

import org.testng.annotations.Test;

import com.seleniumtests.core.runner.SeleniumTestPlan;

import com.example.seleniumdemo.workflows.MasterWorkflow;

public class MasterWorkflowTest extends SeleniumTestPlan {

	@Test
	public void testFullOrchestration() throws Exception {
		MasterWorkflow master = new MasterWorkflow();
		master.fullMasterWorkflow("complete_scenario");
	}

	@Test
	public void testAlternativeWithIterations() throws Exception {
		MasterWorkflow master = new MasterWorkflow();
		master.alternativeMasterFlow("multi_iteration", 2);
	}

	@Test
	public void testExpressFlow() throws Exception {
		MasterWorkflow master = new MasterWorkflow();
		master.expressWorkflow("john_doe");
	}

	@Test
	public void testIntermediateFlow() throws Exception {
		MasterWorkflow master = new MasterWorkflow();
		master.intermediateWorkflow("premium_user");
	}

	@Test
	public void testCombinedLongScenario() throws Exception {
		MasterWorkflow master = new MasterWorkflow();
		master.phase1_ecommerce("shop_1");
		master.phase2_banking("1000");
		master.phase1_ecommerce("shop_2");
		master.phase3_electronics("electronics_1");
		master.phase4_hr("department_x");
		master.phase5_petstore("pets_1");
	}
}
