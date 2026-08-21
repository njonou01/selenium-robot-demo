package com.example.seleniumdemo.custom.reporting;

import java.util.ArrayList;

import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;

import com.seleniumtests.core.SeleniumTestsContextManager;
import com.seleniumtests.core.Step.RootCause;
import com.seleniumtests.core.TestStepManager;
import com.seleniumtests.reporter.logger.TestStep;

/**
 * Regroupe tous les appels @Workflow d'une meme methode @Test sous une seule step racine
 * (nommee d'apres le test), au lieu de laisser chaque appel @Workflow devenir sa propre
 * racine independante (comportement par defaut de WorkflowAspect: TestStepManager reinitialise
 * la racine apres chaque step racine loguee, donc plusieurs appels @Workflow a la suite dans un
 * meme test ne se regroupent pas entre eux).
 */
public class WorkflowGroupingListener implements IInvokedMethodListener {

	@Override
	public void beforeInvocation(IInvokedMethod method, ITestResult testResult) {
		if (!method.isTestMethod()) {
			return;
		}

		String testName = testResult.getMethod().getMethodName();

		TestStep rootStep = new TestStep(
				testName,
				testName,
				testResult.getTestClass().getRealClass(),
				testResult,
				new ArrayList<>(),
				SeleniumTestsContextManager.getThreadContext().getMaskedPassword(),
				RootCause.NONE,
				"",
				false,
				"",
				"");

		rootStep.setStartDate();
		TestStepManager.setCurrentRootTestStep(rootStep);
	}

	@Override
	public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
		if (!method.isTestMethod()) {
			return;
		}

		TestStep rootStep = TestStepManager.getCurrentRootTestStep();
		if (rootStep == null) {
			return;
		}

		rootStep.updateDuration();
		TestStepManager.logTestStep(rootStep);
	}
}
