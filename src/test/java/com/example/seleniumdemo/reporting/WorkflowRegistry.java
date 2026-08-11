package com.example.seleniumdemo.reporting;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Decouvre par reflexion toutes les methodes annotees @Workflow(code=...) du package "workflows",
 * sans liste codee en dur a maintenir. Point d'entree partage entre VariableDrivenScenarioTest (qui
 * execute les workflows) et les outils de documentation (qui listent juste les codes disponibles).
 */
public final class WorkflowRegistry {

	private static final String WORKFLOWS_PACKAGE = "com.example.seleniumdemo.workflows";

	private WorkflowRegistry() {
	}

	public record Entry(String code, Class<?> declaringClass, Method method, String name, String description) {
	}

	/**
	 * Decouvre toutes les classes du package "workflows" dont le nom finit par "Workflow", sans
	 * dependance externe (scan du dossier compile via le classloader). Les classes sans methode
	 * annotee @Workflow(code=...) (ex: MasterWorkflow, FormWorkflow) sont incluses sans consequence:
	 * scanEntries() ne retient que les methodes avec un code non vide.
	 */
	public static List<Class<?>> discoverWorkflowClasses() throws Exception {
		List<Class<?>> classes = new ArrayList<>();
		String path = WORKFLOWS_PACKAGE.replace('.', '/');
		Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
		while (resources.hasMoreElements()) {
			File dir = new File(resources.nextElement().toURI());
			File[] files = dir.listFiles((d, name) -> name.endsWith("Workflow.class"));
			if (files == null) {
				continue;
			}
			for (File file : files) {
				String className = WORKFLOWS_PACKAGE + "." + file.getName().replace(".class", "");
				classes.add(Class.forName(className));
			}
		}
		return classes;
	}

	public static List<Entry> scanEntries() throws Exception {
		List<Entry> entries = new ArrayList<>();
		for (Class<?> clazz : discoverWorkflowClasses()) {
			for (Method method : clazz.getDeclaredMethods()) {
				Workflow annotation = method.getAnnotation(Workflow.class);
				if (annotation != null && !annotation.code().isBlank()) {
					entries.add(new Entry(annotation.code(), clazz, method, annotation.name(), annotation.description()));
				}
			}
		}
		return entries;
	}

	/**
	 * Meme resultat que scanEntries(), indexe par code, pour un acces direct lors de l'execution.
	 */
	public static Map<String, Method> scanMethodsByCode() throws Exception {
		Map<String, Method> registry = new LinkedHashMap<>();
		for (Entry entry : scanEntries()) {
			registry.put(entry.code(), entry.method());
		}
		return registry;
	}
}
