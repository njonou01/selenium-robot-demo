package com.example.seleniumdemo.custom.catalogue;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

import com.example.seleniumdemo.custom.reporting.Workflow;
import com.example.seleniumdemo.custom.reporting.WorkflowRegistry;
import com.example.seleniumdemo.custom.reporting.WorkflowResult;

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

	/**
	 * Resout les vrais noms de parametres d'une methode via reflexion (bytecode), pas via lecture
	 * du code source: fonctionne aussi bien depuis un jar empaquete (Jenkins) qu'en local, tant que
	 * la compilation utilise le flag '-parameters' (deja active dans le pom pour maven-compiler-plugin
	 * et aspectj-maven-plugin).
	 */
	public static List<String> resolveParameterNames(Method method) {
		List<String> names = new ArrayList<>();
		for (Parameter parameter : method.getParameters()) {
			if (!parameter.isNamePresent()) {
				throw new IllegalStateException("Noms de parametres absents du bytecode pour '"
						+ method.getDeclaringClass().getSimpleName() + "." + method.getName()
						+ "' - la compilation doit utiliser le flag '-parameters' "
						+ "(maven-compiler-plugin et aspectj-maven-plugin dans le pom.xml).");
			}
			names.add(parameter.getName());
		}
		return names;
	}

	/**
	 * Parse '@Workflow(params = {"nomMetier=cheminJava"})' en Map cheminJava -> nomMetier.
	 * 'cheminJava' est soit un nom de parametre simple ('firstName'), soit un chemin pointe dans
	 * un record ('address.street') pour nommer un champ interne. Partage entre resolveDataSetKeys
	 * (niveau parametre) et convertDataSetValue (niveau champ de record) pour eviter 2 lectures
	 * divergentes de la meme annotation.
	 */
	public static Map<String, String> parseParamsMapping(Method method) {
		Workflow annotation = method.getAnnotation(Workflow.class);
		String[] rawMappings = annotation == null ? new String[0] : annotation.params();
		Map<String, String> javaPathToBusiness = new LinkedHashMap<>();
		for (String rawMapping : rawMappings) {
			String[] parts = rawMapping.split("=", 2);
			if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
				throw new IllegalStateException("Mapping de parametre invalide '" + rawMapping + "' sur '"
						+ method.getDeclaringClass().getSimpleName() + "." + method.getName()
						+ "' - format attendu: '@Workflow(params = {\"nomMetier=cheminJava\"})'.");
			}
			javaPathToBusiness.put(parts[1].trim(), parts[0].trim());
		}
		return javaPathToBusiness;
	}

	/**
	 * Resout, pour chaque parametre de la methode (dans l'ordre de la signature), la cle
	 * top-niveau a chercher dans 'dataSet': le nom metier declare via
	 * '@Workflow(params = {"nomMetier=nomJava"})' s'il existe pour ce parametre, sinon le nom
	 * Java brut (resolveParameterNames) tel quel. Aucun mapping declare = comportement inchange.
	 * Le mapping des champs internes d'un record ('address.street') est resolu separement, plus
	 * bas dans la recursion de convertDataSetValue.
	 */
	public static List<String> resolveDataSetKeys(Method method) {
		List<String> javaNames = resolveParameterNames(method);
		Map<String, String> javaPathToBusiness = parseParamsMapping(method);

		List<String> keys = new ArrayList<>(javaNames.size());
		for (String javaName : javaNames) {
			keys.add(javaPathToBusiness.getOrDefault(javaName, javaName));
		}
		return keys;
	}

	/**
	 * Resout la valeur brute a utiliser pour le parametre 'key' d'un workflow 'workflowCode':
	 * priorite au bloc specialise dataSet[workflowCode][key] s'il existe (objet JSON imbrique
	 * sous le code du workflow), sinon repli sur dataSet[key] (valeur generique partagee entre
	 * workflows). Evite qu'un meme nom de parametre (ex: 'address') partage entre 2 workflows
	 * force la meme valeur pour les deux.
	 */
	public static Object resolveRawValue(Map<String, Object> dataSet, String workflowCode, String key) {
		if (dataSet == null) {
			return null;
		}
		Object scoped = dataSet.get(workflowCode);
		if (scoped instanceof Map<?, ?> scopedMap && scopedMap.containsKey(key)) {
			return scopedMap.get(key);
		}
		return dataSet.get(key);
	}

	// Groupe 1 gourmand ('.+', pas '[^.}]+') : un code de workflow contient toujours au moins un
	// point ('banking.full'), donc la coupure code/champ doit se faire sur le DERNIER point, pas
	// le premier.
	private static final Pattern RESULT_REFERENCE = Pattern.compile("^\\$\\{result:(.+)\\.([^.}]+)\\}$");

	public static boolean isResultReference(String rawValue) {
		return RESULT_REFERENCE.matcher(rawValue).matches();
	}

	/**
	 * Resout '${result:code.champ}': lit le champ 'champ' (par reflexion sur les
	 * RecordComponent) du WorkflowResult renvoye par le workflow 'code', deja execute plus tot
	 * dans le meme scenario. 'results' est local a l'execution du scenario en cours (pas un
	 * champ partage) - un workflow non encore execute n'y figure simplement pas.
	 */
	public static Object resolveResultReference(String rawValue, Map<String, WorkflowResult> results) {
		Matcher matcher = RESULT_REFERENCE.matcher(rawValue);
		if (!matcher.matches()) {
			throw new IllegalStateException("'" + rawValue + "' n'est pas une reference '${result:code.champ}' valide.");
		}
		String workflowCode = matcher.group(1);
		String fieldName = matcher.group(2);

		WorkflowResult result = results.get(workflowCode);
		if (result == null) {
			throw new IllegalStateException("'" + rawValue + "': le workflow '" + workflowCode
					+ "' n'a pas encore ete execute (ou n'a pas renvoye de resultat) a ce stade du scenario.");
		}
		return readRecordField(result, fieldName, rawValue);
	}

	private static Object readRecordField(Object record, String fieldName, String rawValue) {
		for (RecordComponent component : record.getClass().getRecordComponents()) {
			if (component.getName().equals(fieldName)) {
				try {
					return component.getAccessor().invoke(record);
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException("'" + rawValue + "': impossible de lire le champ '" + fieldName
							+ "' de '" + record.getClass().getSimpleName() + "'.", e);
				}
			}
		}
		throw new IllegalStateException("'" + rawValue + "': '" + record.getClass().getSimpleName()
				+ "' n'a pas de champ '" + fieldName + "'. Champs disponibles: "
				+ Arrays.stream(record.getClass().getRecordComponents()).map(RecordComponent::getName)
						.collect(Collectors.joining(", ")));
	}

	/**
	 * Verifie a sec (avant ouverture du navigateur) qu'une reference '${result:code.champ}' est
	 * exploitable: le workflow 'code' existe, apparait dans 'steps' AVANT l'etape courante
	 * (sinon son resultat n'existera pas encore a l'execution), renvoie bien un WorkflowResult,
	 * et ce type a un champ 'champ'.
	 */
	public static void validateResultReference(String rawValue, List<String> steps, int currentStepIndex, Map<String, Method> registry) {
		Matcher matcher = RESULT_REFERENCE.matcher(rawValue);
		if (!matcher.matches()) {
			throw new IllegalStateException("'" + rawValue + "' n'est pas une reference '${result:code.champ}' valide.");
		}
		String workflowCode = matcher.group(1);
		String fieldName = matcher.group(2);

		int referencedIndex = -1;
		for (int i = 0; i < steps.size(); i++) {
			if (steps.get(i).trim().equals(workflowCode)) {
				referencedIndex = i;
				break;
			}
		}
		if (referencedIndex < 0) {
			throw new IllegalStateException("'" + rawValue + "' reference le workflow '" + workflowCode
					+ "', absent de 'steps'.");
		}
		if (referencedIndex >= currentStepIndex) {
			throw new IllegalStateException("'" + rawValue + "' reference le workflow '" + workflowCode
					+ "' (position " + referencedIndex + " dans 'steps'), qui doit apparaitre AVANT l'etape courante"
					+ " (position " + currentStepIndex + ") pour que son resultat existe deja.");
		}

		Method referencedMethod = registry.get(workflowCode);
		if (referencedMethod == null) {
			throw new IllegalStateException("'" + rawValue + "' reference le workflow '" + workflowCode
					+ "', code de workflow inconnu.");
		}
		Class<?> returnType = referencedMethod.getReturnType();
		if (!WorkflowResult.class.isAssignableFrom(returnType)) {
			throw new IllegalStateException("'" + rawValue + "': le workflow '" + workflowCode
					+ "' ne renvoie pas de WorkflowResult (type de retour: " + returnType.getSimpleName() + ").");
		}
		boolean fieldExists = Arrays.stream(returnType.getRecordComponents()).anyMatch(c -> c.getName().equals(fieldName));
		if (!fieldExists) {
			throw new IllegalStateException("'" + rawValue + "': '" + returnType.getSimpleName()
					+ "' n'a pas de champ '" + fieldName + "'. Champs disponibles: "
					+ Arrays.stream(returnType.getRecordComponents()).map(RecordComponent::getName)
							.collect(Collectors.joining(", ")));
		}
	}

	/**
	 * Point d'entree pour un parametre de methode: connait le mapping metier declare sur
	 * '@Workflow' et le type generique reel du parametre (necessaire pour 'List&lt;T&gt;', dont
	 * l'effacement de type rend 'Class&lt;?&gt;' seul insuffisant). Delegue ensuite a la conversion
	 * recursive (record/tableau/scalaire).
	 */
	public static Object convertDataSetValue(Object rawValue, Parameter parameter) {
		Method method = (Method) parameter.getDeclaringExecutable();
		Map<String, String> javaPathToBusiness = parseParamsMapping(method);
		Class<?> targetType = parameter.getType();

		if (List.class.isAssignableFrom(targetType)) {
			return convertList(rawValue, resolveListComponentType(parameter), javaPathToBusiness, parameter.getName());
		}
		return convertDataSetValue(rawValue, targetType, javaPathToBusiness, parameter.getName());
	}

	private static Class<?> resolveListComponentType(Parameter parameter) {
		if (parameter.getParameterizedType() instanceof ParameterizedType parameterizedType
				&& parameterizedType.getActualTypeArguments().length == 1
				&& parameterizedType.getActualTypeArguments()[0] instanceof Class<?> componentType) {
			return componentType;
		}
		throw new IllegalStateException("Impossible de determiner le type des elements de la List '"
				+ parameter.getName() + "' (type generique non resolu - eviter les wildcards/types bornes).");
	}

	/**
	 * Convertit une valeur brute de 'dataSet' (String, Number, Boolean, ou Map/List imbriquee -
	 * cadeau de Jackson quand le JSON source a un objet/tableau imbrique) vers le type reel
	 * attendu. Supporte String, enum (Enum.valueOf), int/long/double/boolean (primitifs et
	 * wrappers), LocalDate (format ISO AAAA-MM-JJ), tableau (T[]) et record (reconstruit champ par
	 * champ, recursivement - un champ record peut lui-meme etre un enum, un tableau ou un autre
	 * record). 'javaPathToBusiness'/'javaPath' permettent de nommer les champs internes d'un
	 * record via '@Workflow(params = {"nomMetier=cheminJava.champ"})', avec repli sur le nom Java
	 * brut si rien n'est mappe pour ce chemin precis.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object convertDataSetValue(Object rawValue, Class<?> targetType, Map<String, String> javaPathToBusiness,
			String javaPath) {
		if (targetType.isRecord()) {
			if (!(rawValue instanceof Map<?, ?> fields)) {
				throw new IllegalStateException("Le parametre de type record '" + targetType.getSimpleName()
						+ "' attend un objet JSON (champ/valeur) dans 'dataSet', recu: '" + rawValue + "'.");
			}
			RecordComponent[] components = targetType.getRecordComponents();
			Class<?>[] componentTypes = new Class<?>[components.length];
			Object[] componentValues = new Object[components.length];
			for (int i = 0; i < components.length; i++) {
				componentTypes[i] = components[i].getType();
				String componentPath = javaPath + "." + components[i].getName();
				String lookupKey = javaPathToBusiness.getOrDefault(componentPath, components[i].getName());
				Object fieldRaw = fields.get(lookupKey);
				if (fieldRaw == null) {
					throw new IllegalStateException("Champ '" + lookupKey + "' manquant pour le record '"
							+ targetType.getSimpleName() + "' (champs disponibles: " + fields.keySet() + ").");
				}
				componentValues[i] = convertDataSetValue(fieldRaw, componentTypes[i], javaPathToBusiness, componentPath);
			}
			try {
				Constructor<?> canonicalConstructor = targetType.getDeclaredConstructor(componentTypes);
				canonicalConstructor.setAccessible(true);
				return canonicalConstructor.newInstance(componentValues);
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("Impossible de construire le record '" + targetType.getSimpleName() + "'.", e);
			}
		}
		if (targetType.isArray()) {
			List<Object> convertedElements = convertList(rawValue, targetType.getComponentType(), javaPathToBusiness, javaPath);
			Object nativeArray = Array.newInstance(targetType.getComponentType(), convertedElements.size());
			for (int i = 0; i < convertedElements.size(); i++) {
				Array.set(nativeArray, i, convertedElements.get(i));
			}
			return nativeArray;
		}
		if (targetType == String.class) {
			return requireRawType(rawValue, String.class, targetType);
		}
		if (targetType.isEnum()) {
			String rawString = requireRawType(rawValue, String.class, targetType);
			try {
				return Enum.valueOf((Class<? extends Enum>) targetType, rawString);
			} catch (IllegalArgumentException e) {
				throw new IllegalStateException("Valeur '" + rawString + "' invalide pour l'enum '"
						+ targetType.getSimpleName() + "' - valeurs possibles: "
						+ Arrays.toString(targetType.getEnumConstants()));
			}
		}
		if (targetType == Integer.class || targetType == int.class) {
			if (rawValue instanceof Number number) {
				return number.intValue();
			}
			return parseOrFail(rawValue, targetType, Integer::parseInt);
		}
		if (targetType == Long.class || targetType == long.class) {
			if (rawValue instanceof Number number) {
				return number.longValue();
			}
			return parseOrFail(rawValue, targetType, Long::parseLong);
		}
		if (targetType == Double.class || targetType == double.class) {
			if (rawValue instanceof Number number) {
				return number.doubleValue();
			}
			return parseOrFail(rawValue, targetType, Double::parseDouble);
		}
		if (targetType == Boolean.class || targetType == boolean.class) {
			if (rawValue instanceof Boolean bool) {
				return bool;
			}
			if (rawValue instanceof String rawString) {
				return Boolean.parseBoolean(rawString.trim());
			}
			throw new IllegalStateException("Valeur '" + rawValue + "' invalide pour un parametre de type boolean.");
		}
		if (targetType == LocalDate.class) {
			String rawString = requireRawType(rawValue, String.class, targetType);
			try {
				return LocalDate.parse(rawString.trim());
			} catch (DateTimeParseException e) {
				throw new IllegalStateException("Valeur '" + rawString + "' invalide pour un parametre de type LocalDate "
						+ "(format attendu: AAAA-MM-JJ).");
			}
		}
		throw new IllegalStateException("Type de parametre '" + targetType.getSimpleName()
				+ "' non supporte par 'dataSet'.");
	}

	private static List<Object> convertList(Object rawValue, Class<?> componentType, Map<String, String> javaPathToBusiness,
			String javaPath) {
		if (!(rawValue instanceof List<?> rawList)) {
			throw new IllegalStateException("Le parametre '" + javaPath
					+ "' attend un tableau JSON dans 'dataSet', recu: '" + rawValue + "'.");
		}
		List<Object> converted = new ArrayList<>(rawList.size());
		for (Object element : rawList) {
			converted.add(convertDataSetValue(element, componentType, javaPathToBusiness, javaPath + "[]"));
		}
		return converted;
	}

	private static <T> T requireRawType(Object rawValue, Class<T> expectedRawType, Class<?> targetType) {
		if (expectedRawType.isInstance(rawValue)) {
			return expectedRawType.cast(rawValue);
		}
		throw new IllegalStateException("Valeur '" + rawValue + "' invalide pour un parametre de type "
				+ targetType.getSimpleName() + " (type recu: "
				+ (rawValue == null ? "null" : rawValue.getClass().getSimpleName()) + ").");
	}

	private static Object parseOrFail(Object rawValue, Class<?> targetType, Function<String, Object> parser) {
		if (rawValue instanceof String rawString) {
			try {
				return parser.apply(rawString.trim());
			} catch (NumberFormatException e) {
				throw new IllegalStateException(
						"Valeur '" + rawValue + "' invalide pour un parametre de type " + targetType.getSimpleName() + ".");
			}
		}
		throw new IllegalStateException(
				"Valeur '" + rawValue + "' invalide pour un parametre de type " + targetType.getSimpleName() + ".");
	}

	/**
	 * Detection automatique par lecture du source .java sur disque - absent en execution
	 * packagee/jar (Jenkins), auquel cas on retombe sur '@Workflow(variables = {...})' declare
	 * a la main sur la methode (nom seul, sans Kind/paths - degrade mais au moins non vide).
	 */
	public static List<VariableUsage> scan(WorkflowRegistry.Entry entry) {
		List<VariableUsage> usages = scanSource(entry);
		if (!usages.isEmpty()) {
			return usages;
		}

		Workflow annotation = entry.method().getAnnotation(Workflow.class);
		String[] declared = annotation == null ? new String[0] : annotation.variables();
		List<VariableUsage> fallback = new ArrayList<>();
		for (String name : declared) {
			fallback.add(new VariableUsage(name, Kind.SIMPLE, Set.of()));
		}
		return fallback;
	}

	private static List<VariableUsage> scanSource(WorkflowRegistry.Entry entry) {
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
