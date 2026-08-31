# Vue d'ensemble

Le projet est organisé en couches séparées, chacune avec un rôle précis :

```text
Variables serveur → Tests → Workflows → Pages web
```

Les variables serveur pilotent l'exécution (quel scénario, quelles données). Les tests choisissent
le scénario et appellent le bon workflow. Le workflow porte et transforme la donnée métier puis
enchaîne les pages. Les pages exécutent les interactions UI. Un test ne parle jamais directement à
une page web — il passe toujours par un workflow.

Cette séparation ne sert à rien si elle n'est pas tenue : une page qui va lire une variable serveur
elle-même, un test qui porte le détail de la logique métier, ou `custom` qui devient une deuxième
couche métier cachée, et l'architecture perd son intérêt aussi vite qu'elle a été mise en place.

## Rôle de chaque couche

**Tests** — orchestrent les scénarios, appellent les workflows, vérifient le résultat attendu.
Pas de Selenium direct, pas de fabrication de la donnée métier du cas.

**Workflows** — portent la logique métier et les données du cas d'usage, enchaînent les pages,
appliquent les règles du domaine. Ils ne pilotent pas la campagne de test.

**Pages web** — cliquent, saisissent, lisent, attendent. Elles ne lisent pas les variables
serveur et ne portent pas de logique métier.

**Custom** — le support technique commun : catalogue des workflows, client du serveur de
variables, logs, helpers. Il ne porte jamais la logique métier du produit.

## Le serveur de variables, à deux niveaux différents

Deux couches parlent au serveur de variables (`seleniumRobot-server`), mais pas pour la même
raison. Confondre les deux, c'est le piège le plus fréquent quand on découvre le projet.

**Depuis `tests` — décider QUOI lancer.** `ServerDrivenScenarioTest` lit la variable
`workflow.scenarios` : elle liste les scénarios à exécuter, l'ordre dans lequel enchaîner les
workflows, et les données à injecter à chaque étape (`dataSet`). Cette lecture décide de la
**structure** du run — quels workflows tournent, dans quel ordre, combien de scénarios. Éditer
cette variable change ce qui s'exécute, sans toucher une ligne de code. C'est ce qui rend le
scénariseur possible : composer un parcours de test devient une donnée, pas du code compilé (voir
[`guide-scenariseur.md`](guide-scenariseur.md)).

**Depuis `workflows` — décider AVEC QUOI l'exécuter.** Un `Workflow` appelle
`JsonParams.load("xxx.params")` ou `MapParams.load("xxx.params")` pour lire des valeurs —
identifiants, montants, adresses — utilisées à l'intérieur d'une séquence d'étapes déjà fixée dans
le code. Cette lecture ne change jamais combien d'étapes s'exécutent ni dans quel ordre, seulement
les valeurs qui y circulent.

| Couche | Variable lue | Répond à | Éditer la variable change |
|---|---|---|---|
| `tests` | `workflow.scenarios` (ou `-Dscenarios=...`) | Quoi lancer, dans quel ordre | La structure du run : workflows appelés, séquence, nombre de scénarios |
| `workflows` | `xxx.params` (par site) | Avec quelles données | Les valeurs injectées dans une séquence déjà fixe — jamais la séquence elle-même |

Si un `Workflow` se mettait à lire `workflow.scenarios` pour décider quoi faire, il porterait de
l'orchestration — le rôle de `tests`. Si un `test` se mettait à lire `xxx.params` pour fabriquer
sa propre donnée métier, il porterait du métier — le rôle de `workflows`. Dans les deux cas, la
frontière entre "quoi lancer" et "avec quelles données" disparaît, et il faut deviner où chercher
pour comprendre ou modifier un comportement.

### Pourquoi la donnée de cas est un paramètre, pas une lecture interne

Une première approche possible pour un `Workflow` : le rendre entièrement autonome, il va
chercher lui-même toute sa donnée via `JsonParams`/`MapParams` dès qu'il en a besoin, sans rien
attendre de l'appelant.

```java
public class MissionAutoWorkflow {
    private final Lazy<PortailExpertiseAutoPage> page = new Lazy<>(PortailExpertiseAutoPage::new);

    @Workflow(name = "Mission auto complète", code = "missionauto.full")
    public void fullMissionAutoFlow() throws Exception {
        JsonParams params = JsonParams.load("missionauto.params");
        page.get().ouvrirMission(params.get("numeroSinistre"));
        page.get().saisirChiffrage(params.get("montantDevis"));
    }
}
```

Ça marche très bien pour un `Test` technique écrit à la main — il appelle `fullMissionAutoFlow()`
sans rien lui passer. Mais ça ferme la porte au scénariseur : `WorkflowVariableScanner` résout le
`dataSet` d'un scénario **par réflexion sur les paramètres de la méthode**. Une méthode sans
paramètre n'a rien à résoudre — le scénariseur ne peut piloter ni `numeroSinistre` ni
`montantDevis`. Tester deux sinistres différents obligerait à dupliquer toute la variable
`missionauto.params` par cas, exactement le genre de deuxième source de vérité que le projet
refuse ailleurs (catalogue, voir plus haut).

Ce n'est même pas juste gênant, c'est structurellement impossible dans une même exécution.
`JsonParams`/`MapParams` mettent en cache la variable **par nom**, pour toute la durée du run :

```java
// JsonParams.java
private static final Map<String, JsonParams> cache = new HashMap<>();

public static synchronized JsonParams load(String variableName) {
    return cache.computeIfAbsent(variableName, name -> {
        // ... récupère le fichier depuis le serveur, une seule fois
    });
}
```

Deux scénarios lancés dans la même suite (`Scenario Sinistre A` puis `Scenario Sinistre B`) qui
appellent tous les deux `JsonParams.load("missionauto.params")` reçoivent **la même instance**,
mise en cache par le premier appel — `computeIfAbsent` ne relit jamais le serveur une deuxième
fois pour le même nom de variable, quel que soit le scénario qui appelle. Le `numeroSinistre` lu
en interne serait donc identique pour les deux scénarios, sans aucun moyen de le faire varier —
pas une limite du scénariseur, une limite du cache lui-même.

Le `dataSet`, à l'inverse, n'est jamais mis en cache : `WorkflowVariableScanner` le résout à
chaque scénario, depuis le `ScenarioDef` propre à ce scénario. Deux scénarios qui appellent le
même `missionauto.full` avec des `dataSet["numeroSinistre"]` différents obtiennent chacun leur
propre valeur, sans rien partager entre eux.

La solution retenue : sortir de la méthode tout ce qui peut varier par cas, et ne garder en
lecture interne que ce qui ne varie jamais.

```java
public class MissionAutoWorkflow {
    private final Lazy<PortailExpertiseAutoPage> page = new Lazy<>(PortailExpertiseAutoPage::new);

    @Workflow(name = "Mission auto complète ${numeroSinistre}", code = "missionauto.full")
    public void fullMissionAutoFlow(String numeroSinistre, String montantDevis) throws Exception {
        page.get().ouvrirMission(numeroSinistre);
        page.get().saisirChiffrage(montantDevis);
    }
}
```

Cette version fonctionne à l'identique pour les deux appelants : un `Test` technique lui passe les
valeurs depuis un `dataProvider` CSV, le scénariseur les résout depuis le `dataSet` d'un scénario
— même méthode, aucune modification du `Workflow` entre les deux mondes. Ce qui reste appelé en
interne (`JsonParams.load("xxx.params")`) se limite alors à la config qui ne varie jamais par cas
— identifiants d'un compte de service, URL du site — puisque l'exposer en paramètre n'apporterait
rien : aucun scénario n'a besoin de la faire varier.

### Le coût de ce choix pour le rédacteur de scénario

Ce découpage a un prix, honnêtement : tout ce qui reste interne au `Workflow` devient invisible
depuis le scénario. Le rédacteur qui compose ce JSON voit exactement ce qu'il pilote, rien de
plus :

```json
{
  "name": "Scenario Sinistre Auto",
  "sinistre": "AUTO-2026-04871",
  "steps": ["missionauto.full"],
  "dataSet": {
    "numeroSinistre": "AUTO-2026-04871",
    "montantDevis": "1450.00"
  }
}
```

`missionauto.params` (identifiants, URL du site) n'apparaît nulle part dans ce fichier — ni son
nom, ni son contenu. Le catalogue généré liste `numeroSinistre` et `montantDevis` comme paramètres
de `missionauto.full` parce que ce sont des paramètres de méthode, résolus par réflexion ; il ne
liste jamais ce que le `Workflow` va chercher lui-même en interne, puisque `WorkflowCatalogueGenerator`
n'a aucune visibilité sur le corps de la méthode, seulement sur sa signature.

Concrètement, si le rédacteur veut savoir avec quel identifiant `missionauto.full` va se
connecter, ou pourquoi un scénario échoue à cause d'une config expirée côté site, la seule
réponse est côté serveur : ouvrir l'admin `seleniumRobot-server`, chercher la variable
`missionauto.params`, et lire son contenu directement — rien dans le scénario ni dans le
catalogue ne l'y renvoie. Pour chaque workflow de sa chaîne qui a sa propre config interne, c'est
un aller-retour serveur de plus, en dehors du fichier qu'il est en train d'écrire.

## En pratique

Un test peut choisir quel scénario lancer, mais ne doit jamais ouvrir une page lui-même. Un
workflow peut recevoir une donnée métier commune (un profil, un jeu de paramètres) et
l'utiliser pour enchaîner les pages. Une page reçoit un sélecteur ou une valeur, elle ne va pas
chercher elle-même sa configuration. Et le catalogue peut lister les codes de workflow, mais ne
doit jamais devenir une source de vérité tenue à la main si le code sait déjà produire cette
information tout seul — une liste de codes recopiée à la main dans une doc diverge du code réel
dès le premier workflow ajouté sans qu'on pense à la mettre à jour.

## Diagramme associé

[Diagramme Draw.io](architecture.drawio)
