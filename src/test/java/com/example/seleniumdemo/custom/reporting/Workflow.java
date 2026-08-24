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

	/**
	 * Mapping optionnel nom metier -> nom Java pour les parametres consommes via 'dataSet'
	 * (format "nomMetier=nomJava", ex: {"prenom=firstName"}). Un parametre Java non liste ici
	 * garde son nom Java tel quel comme cle 'dataSet' - ce mapping ne sert que pour les
	 * parametres ou le vocabulaire metier (redacteur de scenario non-dev) doit differer du nom
	 * de variable Java.
	 */
	String[] params() default {};
}
