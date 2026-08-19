package com.example.seleniumdemo.custom.reporting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;

import com.seleniumtests.core.SeleniumTestsContextManager;
import com.seleniumtests.core.TestStepManager;
import com.seleniumtests.core.TestTasks;
import com.seleniumtests.core.aspects.LogAction;
import com.seleniumtests.driver.CustomEventFiringWebDriver;
import com.seleniumtests.driver.WebUIDriver;
import com.seleniumtests.reporter.logger.TestStep;
import com.seleniumtests.util.logging.ScenarioLogger;
import com.seleniumtests.util.video.VideoRecorder;

@Aspect
public class WorkflowAspect {

    private static final ScenarioLogger scenarioLogger = ScenarioLogger.getScenarioLogger(WorkflowAspect.class);

    @Around(
            "execution(@com.example.seleniumdemo.custom.reporting.Workflow * *(..)) "
                    + "&& @annotation(workflow)"
    )
    public Object aroundWorkflow(
            ProceedingJoinPoint joinPoint,
            Workflow workflow) throws Throwable {

        List<String> pwdToReplace = new ArrayList<>();
        Map<String, String> arguments = new HashMap<>();

        LogAction.buildArgString(
                joinPoint,
                pwdToReplace,
                arguments);

        String stepName = interpolate(workflow.name(), arguments);
        String description = interpolate(workflow.description(), arguments);
        String expectedResult = interpolate(workflow.expectedResult(), arguments);

        TestStep currentStep = new TestStep(
                stepName,
                stepName,
                joinPoint.getTarget() != null
                        ? joinPoint.getTarget().getClass()
                        : joinPoint.getSignature().getDeclaringType(),
                Reporter.getCurrentTestResult(),
                pwdToReplace,
                SeleniumTestsContextManager.getThreadContext().getMaskedPassword(),
                workflow.errorCause(),
                workflow.errorCauseDetails(),
                workflow.disableBugTracker(),
                description,
                expectedResult);

        boolean rootStep = false;
        TestStep previousParent = null;

        VideoRecorder videoRecorder = WebUIDriver.getThreadVideoRecorder();

        if (TestStepManager.getCurrentRootTestStep() == null) {

            TestStepManager.setCurrentRootTestStep(currentStep);
            rootStep = true;

            if (videoRecorder != null) {
                long duration = CustomEventFiringWebDriver.displayStepOnScreen(currentStep.getName(),
                        SeleniumTestsContextManager.getThreadContext().getRunMode(),
                        SeleniumTestsContextManager.getThreadContext().getSeleniumGridConnector(),
                        videoRecorder);
                currentStep.setVideoTimeStamp(currentStep.getVideoTimeStamp() + Math.max(0, duration - 5));
                currentStep.setDurationToExclude(duration);
            }

        } else {

            previousParent = TestStepManager.getParentTestStep();

            if (previousParent != null) {
                previousParent.addStep(currentStep);
            }

            TestStepManager.setParentTestStep(currentStep);
        }

        currentStep.setStartDate();

        try {

            return joinPoint.proceed(joinPoint.getArgs());

        } catch (Throwable e) {

            currentStep.setFailed(true);
            currentStep.setActionException(e);

            MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
            if (methodSignature.getMethod().getAnnotation(AfterMethod.class) != null) {
                scenarioLogger.error(String.format("Error in @AfterMethod %s: %s", methodSignature, e.getMessage()));
            } else {
                throw e;
            }

        } finally {

            if (rootStep) {
				try {
					TestTasks.capturePageSnapshot();
				} catch (Exception ignored) {
				}
			}

            currentStep.updateDuration();

            if (rootStep) {
                TestStepManager.logTestStep(currentStep);
            } else {
                TestStepManager.setParentTestStep(previousParent);
            }
        }

        return null;
    }

    private String interpolate(String text, Map<String, String> arguments) {
        String result = text;
        for (Map.Entry<String, String> entry : arguments.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
