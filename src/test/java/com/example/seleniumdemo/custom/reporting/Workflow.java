package com.example.seleniumdemo.custom.reporting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.seleniumtests.core.Step.RootCause;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Workflow {

	String name() default "";

	String code() default "";

	String description() default "";

	String expectedResult() default "";

	RootCause errorCause() default RootCause.NONE;

	String errorCauseDetails() default "";

	boolean disableBugTracker() default false;
}
