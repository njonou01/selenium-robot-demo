package com.example.seleniumdemo.custom.catalogue;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONObject;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;

import com.example.seleniumdemo.custom.reporting.WorkflowRegistry;

public final class WorkflowVariableScanner {

	public enum Kind {
		SIMPLE, JSON, MAP
	}

	public record VariableUsage(String name, Kind kind, Set<String> paths) {

		public String structure() {
			return switch (kind) {
				case SIMPLE -> null;
				case JSON -> buildJsonExample(paths);
				case MAP -> buildMapExample(paths);
			};
		}
	}

	private static final Set<String> LOAD_SCOPES = Set.of("JsonParams", "MapParams");

	private WorkflowVariableScanner() {
	}

	public static String buildStructure(Kind kind, Set<String> paths) {
		return switch (kind) {
			case SIMPLE -> null;
			case JSON -> buildJsonExample(paths);
			case MAP -> buildMapExample(paths);
		};
	}

	public static List<VariableUsage> scan(WorkflowRegistry.Entry entry) {
		File sourceFile = new File("src/test/java/" + entry.declaringClass().getName().replace('.', '/') + ".java");
		if (!sourceFile.exists()) {
			return List.of();
		}

		CompilationUnit unit;
		try {
			unit = StaticJavaParser.parse(sourceFile);
		} catch (Exception e) {
			return List.of();
		}

		List<VariableUsage> usages = new ArrayList<>();
		unit.findAll(MethodDeclaration.class).stream()
				.filter(m -> m.getNameAsString().equals(entry.method().getName()))
				.findFirst()
				.ifPresent(method -> {
					for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
						String scopeName = scopeName(call);
						String literalArg = firstStringLiteralArg(call);
						if (scopeName == null || literalArg == null) {
							continue;
						}

						if ("PageObject".equals(scopeName) && "param".equals(call.getNameAsString())) {
							usages.add(new VariableUsage(literalArg, Kind.SIMPLE, Set.of()));
						} else if (LOAD_SCOPES.contains(scopeName) && "load".equals(call.getNameAsString())) {
							Kind kind = "JsonParams".equals(scopeName) ? Kind.JSON : Kind.MAP;
							usages.add(new VariableUsage(literalArg, kind, collectGetPaths(method, call)));
						}
					}
				});

		return usages;
	}

	private static Set<String> collectGetPaths(MethodDeclaration method, MethodCallExpr loadCall) {
		Set<String> paths = new LinkedHashSet<>();
		loadCall.findAncestor(VariableDeclarator.class).ifPresent(declarator -> {
			String localVarName = declarator.getNameAsString();
			for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
				if (!"get".equals(call.getNameAsString())) {
					continue;
				}
				if (call.getScope().isEmpty() || !(call.getScope().get() instanceof NameExpr scope)) {
					continue;
				}
				if (!localVarName.equals(scope.getNameAsString())) {
					continue;
				}
				String literalArg = firstStringLiteralArg(call);
				if (literalArg != null) {
					paths.add(literalArg);
				}
			}
		});
		return paths;
	}

	private static String scopeName(MethodCallExpr call) {
		if (call.getScope().isEmpty() || !(call.getScope().get() instanceof NameExpr scope)) {
			return null;
		}
		return scope.getNameAsString();
	}

	private static String firstStringLiteralArg(MethodCallExpr call) {
		if (call.getArguments().isEmpty()) {
			return null;
		}
		Expression firstArg = call.getArguments().get(0);
		return firstArg instanceof StringLiteralExpr literal ? literal.asString() : null;
	}

	private static String buildJsonExample(Set<String> paths) {
		JSONObject root = new JSONObject();
		for (String path : paths) {
			String[] segments = path.split("\\.");
			JSONObject current = root;
			for (int i = 0; i < segments.length - 1; i++) {
				Object existing = current.opt(segments[i]);
				if (!(existing instanceof JSONObject)) {
					current.put(segments[i], new JSONObject());
				}
				current = current.getJSONObject(segments[i]);
			}
			current.put(segments[segments.length - 1], "...");
		}
		return root.toString(2);
	}

	private static String buildMapExample(Set<String> paths) {
		return paths.stream().map(p -> p + "=...").collect(Collectors.joining(";"));
	}
}
