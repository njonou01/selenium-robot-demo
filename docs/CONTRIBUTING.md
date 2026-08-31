# Règles de code — <projet>

Guide pour écrire un nouveau workflow/test dans ce projet. Basé sur les décisions prises et
justifiées au fil du projet — pas des préférences arbitraires.

## Structure des packages

| Package | Rôle |
|---|---|
| `webpage/` | Page Objects bruts — locators + actions Selenium, une classe par site |
| `workflows/` | Orchestration métier — enchaîne les actions d'une PageObject, annoté `@Workflow` |
| `tests/` | Points d'entrée TestNG réels — tout ce qui est déclaré dans un `<class>` de `testng.xml` |
| `reporting/` | Mécanisme de rapport (`@Workflow`, listeners de rapport custom) |
| `testdata/` | Chargement de données depuis le serveur de variable (`JsonParams`, `MapParams`) |
| `utils/` | Helpers génériques réutilisables (`Lazy<T>`, attentes, données de test aléatoires) |

Ne pas mélanger les rôles. Une classe testée par `tests/` mais qui n'assert rien (générateur de
doc, script) reste dans `tests/` quand même — l'invariant du package c'est "déclaré dans un xml",
pas "fait des assertions".

## PageObject (`webpage/`)

- Une classe par site/appli, constructeur = `super(elementIdentifiant, url)`.
- Locators en `static final`, en haut de la classe.
- `@Step(name = "...")` sur chaque méthode publique qui doit apparaître dans le rapport.
- Retourner `this` (ou la page suivante) quand la méthode enchaîne naturellement avec d'autres
  appels — chaînage fluide, sauf si ça n'a pas de sens pour cette page.

## Workflow (`workflows/`)

- Une classe par PageObject/site (`EcommerceWorkflow` ↔ `SauceDemoPage`).
- **Ne jamais instancier la PageObject dans le constructeur ni dans un champ initialisé
  directement.** Le constructeur de `PageObject` navigue immédiatement (`openPage()`) — si ça
  arrive avant qu'un step `@Workflow` soit ouvert, ce step devient orphelin à la racine du
  rapport au lieu de s'imbriquer proprement.

  Toujours utiliser `Lazy<T>` (`utils/Lazy.java`) :

  ```java
  private final Lazy<SauceDemoPage> page = new Lazy<>(SauceDemoPage::new);

  @Workflow(name = "Étape 1: Login")
  public void step1_login(String user, String pwd) throws Exception {
      page.get().login(user, pwd);
  }
  ```

- `@Workflow(name = "...")` sur chaque step. Le nom est ce qui s'affiche dans le rapport —
  écrire pour un testeur manuel, pas pour un dev (français, clair, pas de jargon Java).
- Interpolation `${nom}` dans le `name` : le nom doit correspondre exactement à un paramètre de
  la méthode.
- Une méthode `fullXxxFlow()` avec `code = "xxx.full"` si le workflow doit être pilotable depuis
  le serveur de variable (`workflow.scenarios`) — voir `README-workflow-scenarios.md`. Exemple :
  `MissionAutoWorkflow.fullMissionAutoFlow()`, `code = "missionauto.full"`.

## Données et identifiants

- **Jamais de mot de passe/identifiant en dur dans le code.** Toujours `PageObject.param("cle")`.
- Données métier structurées (montants d'indemnisation, sinistres, adresses...) → variable
  serveur `xxx.params` (ex : `missionauto.params`), chargée via `JsonParams.load("xxx.params")`
  (JSON imbriqué, fichier uploadé) ou `MapParams.load("xxx.params")` (texte plat `cle=valeur`,
  variable inline) — pas les deux pour la même variable, choisir un format et s'y tenir par site.

## Nommage

- Identifiants Java (classes, méthodes, variables) : **anglais**, toujours.
- Libellés `@Workflow(name=...)`/`@Step(name=...)` : **français**, c'est ce que lit le testeur.
- Commentaires : français, mais seulement si le POURQUOI n'est pas évident (contournement d'un
  bug de site, piège du framework, raison d'un choix). Ne pas commenter ce que le code montre
  déjà (pas de `// login utilisateur` au-dessus de `login()`).

## Ce qu'on n'utilise PAS ici (et pourquoi)

Discuté et rejeté explicitement — ne pas réintroduire sans une vraie raison nouvelle :

- **AspectJ custom pour de l'injection/instanciation automatique** (`@LazyPage` + aspect sur
  `get()` pointcut) — testé, marchait, mais coût trop élevé pour le gain : faux positifs IDE
  (l'inspection "Spring AOP" se déclenche même sans Spring, dès que `spring-context` traîne sur
  le classpath via la dépendance Cucumber du framework), concept obscur pour qui ne connaît pas
  AspectJ. `Lazy<T>` fait exactement la même chose, en Java pur, sans ces coûts.
- **Classe abstraite générique pour factoriser le pattern workflow** — pas nécessaire tant que
  chaque workflow n'a qu'une page ; `Lazy<T>` suffit.
- **Bibliothèques externes non installées** (Lombok, Awaitility, DataFaker, AssertJ) — aucune
  n'est actuellement dans le projet. Le code est déjà raisonnablement compact (records Java,
  peu de getters/setters à factoriser) ; ne pas ajouter une dépendance pour gagner quelques
  lignes.

## Rapport

- `mvn test` → rapport standard du framework par défaut.
- `mvn test -DcustomReport=true` → template custom du projet (`report.test.vm`,
  `report.part.suiteSummary.vm`, dans `src/test/resources/custom/reporter/templates/`) — bandeau
  navbar + statut + badge équipe. Toggle fiable dans les deux sens, avec ou sans `mvn clean`
  entre deux runs (`CustomReportListener` nettoie lui-même les fichiers copiés si désactivé).
- Nom d'équipe affiché sur le rapport : paramètre `equipe` dans le `testng.xml` utilisé.

## Avant de commit

- `mvn clean test-compile` doit passer sans erreur.
- Si vous touchez à `report.test.vm` ou `report.part.suiteSummary.vm` : ce sont des templates
  Velocity, pas vérifiés par le compilateur Java. Une erreur de syntaxe `#if`/`#end` mal fermé ne
  se voit qu'à l'exécution — tester un vrai run avant de pousser.
