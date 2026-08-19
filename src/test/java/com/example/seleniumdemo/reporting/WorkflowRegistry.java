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
 * Decouvre par reflexion toutes les methodes annotees @Workflow(code=...) dans les packages listes
 * par SCANNED_PACKAGES (scan recursif, sous-packages inclus), sans liste codee en dur des classes.
 * Point d'entree partage entre VariableDrivenScenarioTest (qui execute les workflows) et les outils
 * de documentation (qui listent juste les codes disponibles).
 *
 * SCANNED_PACKAGES est une liste blanche volontaire, pas tout gmfindem: le perimetre restreint sert
 * de garde-fou pour la separation Page (@Step) / Workflow (@Workflow) - une methode ne devient
 * pilotable depuis le serveur de variable que si sa classe vit dans un package explicitement
 * autorise ici. Ajouter un nouveau package = ajouter une ligne dans cette liste, jamais tout ouvrir.
 */
public final class WorkflowRegistry {

	private static final List<String> SCANNED_PACKAGES = List.of(
			"com.example.seleniumdemo.workflows"
	);

	private WorkflowRegistry() {
	}

	public record Entry(String code, Class<?> declaringClass, Method method, String name, String description) {
	}

	/**
	 * Parcourt recursivement chaque package de SCANNED_PACKAGES (sous-packages inclus, scan du
	 * dossier compile via le classloader, sans dependance externe) et ne retient que les classes
	 * qui declarent au moins une methode @Workflow(code=...) non vide. Aucune convention de nommage
	 * requise sur la classe: la selection se fait sur l'annotation reelle, pas sur un suffixe.
	 */
	public static List<Class<?>> discoverWorkflowClasses() throws Exception {
		List<Class<?>> classes = new ArrayList<>();
		for (String basePackage : SCANNED_PACKAGES) {
			String path = basePackage.replace('.', '/');
			Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
			while (resources.hasMoreElements()) {
				File root = new File(resources.nextElement().toURI());
				collectWorkflowClasses(root, basePackage, classes);
			}
		}
		return classes;
	}

	private static void collectWorkflowClasses(File dir, String packageName, List<Class<?>> out) throws Exception {
		File[] files = dir.listFiles();
		if (files == null) {
			return;
		}
		for (File file : files) {
			if (file.isDirectory()) {
				collectWorkflowClasses(file, packageName + "." + file.getName(), out);
			} else if (file.getName().endsWith(".class") && !file.getName().contains("$")) {
				String className = packageName + "." + file.getName().substring(0, file.getName().length() - ".class".length());
				Class<?> clazz = Class.forName(className);
				if (hasWorkflowCode(clazz)) {
					out.add(clazz);
				}
			}
		}
	}

	private static boolean hasWorkflowCode(Class<?> clazz) {
		for (Method method : clazz.getDeclaredMethods()) {
			Workflow annotation = method.getAnnotation(Workflow.class);
			if (annotation != null && !annotation.code().isBlank()) {
				return true;
			}
		}
		return false;
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
