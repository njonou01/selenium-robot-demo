package com.example.seleniumdemo.custom.reporting;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WorkflowRegistry {

	private static final List<String> SCANNED_PACKAGES = List.of(
			"com.example.seleniumdemo.workflows"
	);

	private WorkflowRegistry() {
	}

	public record Entry(String code, Class<?> declaringClass, Method method, String name, String description) {
	}

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
		validateUniqueCodes(entries);
		return entries;
	}

	public static Map<String, Method> scanMethodsByCode() throws Exception {
		return indexMethodsByCode(scanEntries());
	}

	static Map<String, Method> indexMethodsByCode(List<Entry> entries) {
		validateUniqueCodes(entries);

		Map<String, Method> registry = new LinkedHashMap<>();
		for (Entry entry : entries) {
			registry.put(entry.code(), entry.method());
		}
		return registry;
	}

	private static void validateUniqueCodes(List<Entry> entries) {
		Map<String, Entry> firstEntryByCode = new LinkedHashMap<>();
		for (Entry entry : entries) {
			Entry previous = firstEntryByCode.putIfAbsent(entry.code(), entry);
			if (previous != null) {
				throw new IllegalStateException(
						"Code de workflow duplique '" + entry.code() + "': "
								+ formatEntry(previous) + " et " + formatEntry(entry));
			}
		}
	}

	private static String formatEntry(Entry entry) {
		return entry.declaringClass().getSimpleName() + "." + entry.method().getName();
	}
}
