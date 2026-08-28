package com.example.seleniumdemo.unitaire;

import java.lang.reflect.Method;
import java.util.List;

import org.testng.annotations.Test;

import com.example.seleniumdemo.custom.reporting.WorkflowRegistry;
import com.example.seleniumdemo.workflows.BankingWorkflow;
import com.example.seleniumdemo.workflows.EcommerceWorkflow;

public class WorkflowRegistryTest {

	@Test(expectedExceptions = IllegalStateException.class,
			expectedExceptionsMessageRegExp = ".*Code de workflow duplique 'duplicate.code'.*")
	public void shouldRejectDuplicateWorkflowCodes() throws Exception {
		Method firstMethod = EcommerceWorkflow.class.getDeclaredMethod("fullEcommerceFlow");
		Method secondMethod = BankingWorkflow.class.getDeclaredMethod("fullBankingFlow");

		List<WorkflowRegistry.Entry> entries = List.of(
				new WorkflowRegistry.Entry("duplicate.code", EcommerceWorkflow.class, firstMethod, "Ecommerce", ""),
				new WorkflowRegistry.Entry("duplicate.code", BankingWorkflow.class, secondMethod, "Banking", "")
		);

		WorkflowRegistry.indexMethodsByCode(entries);
	}
}
