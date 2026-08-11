package com.example.seleniumdemo.utils;

import java.util.concurrent.Callable;

/**
 * Conteneur generique pour une valeur creee a la demande, au premier appel de {@link #get()}, puis
 * reutilisee ensuite (jamais recreee). Utile notamment pour une PageObject dans une classe Workflow:
 * instanciee au premier step reellement execute plutot que dans le constructeur, pour que son step
 * interne "openPage" s'imbrique dans le step @Workflow en cours au lieu de devenir orphelin a la
 * racine du rapport.
 *
 * Usage:
 * <pre>
 * private final Lazy&lt;SauceDemoPage&gt; page = new Lazy&lt;&gt;(SauceDemoPage::new);
 *
 * &#64;Workflow(...)
 * public void step1_login(String user, String pwd) throws Exception {
 *     page.get().login(user, pwd);
 * }
 * </pre>
 */
public class Lazy<T> {

	private final Callable<T> factory;
	private T value;

	public Lazy(Callable<T> factory) {
		this.factory = factory;
	}

	public T get() throws Exception {
		if (value == null) {
			value = factory.call();
		}
		return value;
	}
}
