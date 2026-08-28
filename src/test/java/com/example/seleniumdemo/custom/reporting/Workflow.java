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

	/**
	 * Noms des variables serveur lues par la methode (ex: {"banking.params"}), en secours de
	 * la detection automatique par WorkflowVariableScanner.scan() (qui relit le code source
	 * .java sur disque - absent en execution packagee/jar, ex: Jenkins). Sert uniquement quand
	 * la detection automatique ne trouve rien: si scan() detecte deja des usages depuis le
	 * source, cette liste est ignoree. A ne renseigner que si le workflow doit rester
	 * documente correctement meme execute depuis un jar.
	 */
	String[] variables() default {};
}
