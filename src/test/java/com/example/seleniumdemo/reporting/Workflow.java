package com.example.seleniumdemo.reporting;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.seleniumtests.core.Step.RootCause;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Workflow {

	String name() default "";

	/**
	 * Cle symbolique stable permettant de piloter cette methode depuis le serveur de variable
	 * (variable "workflow.scenarios"), sans exposer de nom de classe/methode Java. Vide = non pilotable.
	 */
	String code() default "";

	String description() default "";

	String expectedResult() default "";

	RootCause errorCause() default RootCause.NONE;

	String errorCauseDetails() default "";

	boolean disableBugTracker() default false;
}
