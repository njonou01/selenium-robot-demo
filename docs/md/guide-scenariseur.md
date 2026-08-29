# Guide du scénariseur

## Pourquoi cette documentation existe

Ce projet distingue deux publics : les rédacteurs de scénarios (testeurs manuels, chefs de
projet — pas forcément des devs) qui composent des campagnes depuis le serveur de variable, et
les devs qui ajoutent de nouveaux workflows pilotables ou font évoluer le moteur. Chaque page de
`docs/` s'adresse explicitement à l'un ou à l'autre pour éviter le piège classique : un document
qui essaie de parler aux deux à la fois finit par être inutile aux deux.

La règle qui tient tout le reste : une information que le code peut produire lui-même (liste des
workflows, leurs paramètres, les variables serveur qu'ils lisent) ne doit **jamais** être
recopiée à la main dans une doc — ça diverge du code réel dès le premier changement qu'on oublie
d'y répercuter. Le `WorkflowCatalogueGenerator` existe pour ça (voir
[`architecture.md`](architecture.md) pour le principe complet).

Cette page complète [`README-workflow-scenarios.md`](../../README-workflow-scenarios.md) (le
format des scénarios, en détail) plutôt que de le redire : elle répond à "pourquoi ce moteur
existe et à quoi sert chaque paramètre de configuration", pas "comment écrire un scénario
ligne par ligne".

## Objectif et rôle du scénariseur

Avant ce moteur, enchaîner plusieurs workflows (par exemple un parcours e-commerce puis un
parcours RH dans le même test) demandait d'écrire une classe Java dédiée à chaque combinaison —
voir `MasterWorkflow` pour un exemple de ce que ça donne à la main. Le scénariseur remplace ça
par une donnée : un scénario est un nom, une liste ordonnée de codes de workflow, et les données
dont ils ont besoin, le tout dans une variable du serveur `seleniumRobot-server`. Composer une
nouvelle campagne ne demande plus de recompiler ni de redéployer — juste éditer une variable.

Il vit dans l'architecture en couches du projet (`tests` → `workflows` → `webpage`, support
commun dans `custom`, voir [`architecture.md`](architecture.md)) sans la casser :
`ServerDrivenScenarioTest` reste un test — il orchestre, il n'ouvre jamais de page lui-même ; il
appelle les mêmes méthodes `@Workflow(code = ...)` qu'un test écrit à la main appellerait.

Deux moitiés du moteur, dans `custom/` :

- **`scenarios/`** — transforme le JSON ou l'Excel brut en `ScenarioDef` (nom, sinistre, sbc,
  steps, dataSet), avec isolation d'erreur par scénario : un scénario mal formé n'empêche jamais
  les autres de tourner.
- **`catalogue/`** — `WorkflowVariableScanner` résout par réflexion les paramètres attendus par
  chaque workflow et convertit les valeurs brutes du `dataSet` vers leur vrai type Java ; c'est
  le même mécanisme qui alimente à la fois l'exécution réelle et le catalogue généré.

## Guide d'utilisation rapide

1. Composer le scénario (JSON ou Excel — format complet dans
   [`README-workflow-scenarios.md`](../../README-workflow-scenarios.md)).
2. L'uploader dans la variable serveur `workflow.scenarios` (ou une autre, voir plus bas
   `-Dscenarios`), depuis l'admin du serveur de variable.
3. Lancer la suite `seleniumdemo-vds-only.xml` (ou `seleniumdemo.xml`, qui inclut aussi les
   autres tests du projet).
4. Lire le rapport : chaque scénario devient un test TestNG nommé d'après son `name`, chaque
   workflow de la chaîne apparaît comme une étape imbriquée.

Un scénario mal écrit remonte son erreur **avant** l'ouverture d'un navigateur — `dataSet`
incomplet, code de workflow inconnu, deux scénarios avec le même nom, référence
`${result:...}` vers un workflow qui n'a pas encore tourné : tout ça est détecté en quelques
secondes, pas après plusieurs minutes d'exécution Selenium.

## Les paramètres testng.xml

### Le test "VariableDrivenScenario" (`ServerDrivenScenarioTest`)

| Paramètre | Rôle |
|---|---|
| `seleniumRobotServerUrl` | URL du serveur de variable à interroger. |
| `seleniumRobotServerActive` | Active la lecture depuis le serveur de variable (`true`/`false`). |
| `seleniumRobotServerToken` | Volontairement absent des fichiers XML — vient de la variable d'environnement `SELENIUM_ROBOT_SERVER_TOKEN`, jamais commité en clair. |
| `testRetryCount` | Nombre de relances TestNG en cas d'échec d'un test. |
| `equipe` | Info affichée dans le rapport (listener `TeamInfoListener`), pas lue par le moteur de scénario. |
| `customReport` | Active le template de rapport custom (`CustomReportListener`). |

Un paramètre supplémentaire ne vient **pas** du XML mais de la ligne de commande :
`-Dscenarios=nom.de.variable` redirige la lecture vers une autre variable que
`workflow.scenarios` par défaut — utile pour donner à chaque équipe son propre jeu de
scénarios sur le même serveur, sans dupliquer de code.

### Le test "Documentation" (`WorkflowCatalogueGenerator`)

| Paramètre | Valeurs possibles | Rôle |
|---|---|---|
| `catalogueFields` | `code,name,method,description,class,parameters,variables` (liste, ordre libre) | Quelles colonnes apparaissent dans le catalogue généré, et dans quel ordre. Par défaut (absent) : `code,name,method,description,class`. |
| `catalogueLayout` | `sheets` / `blocks` / `json` / `matrix` | `sheets` = un classeur `.xlsx`, un onglet par classe Workflow. `blocks` = un seul onglet, un bloc de colonnes empilé par classe. `json` = un fichier `.json`, un objet par classe. `matrix` = un seul onglet, une ligne par code, classe fusionnée verticalement. Par défaut : `sheets`. |
| `catalogueStripPlaceholders` | `true` / `false` | Retire les `${xxx}` (placeholders runtime, ex: `${username}`) du champ `name` affiché dans le catalogue — un nom de step brut, pas templaté. Par défaut : `true`. |
| `catalogueKeepLocalCopy` | `true` / `false` | Le catalogue est **toujours** uploadé sur le serveur de variable (obligatoire, source de vérité). `true` garde **en plus** une copie locale dans `src/test/resources/workflows-catalogue.*` — pratique pour l'inspecter sans repasser par le serveur. Best-effort : ignoré silencieusement si le dossier n'existe pas (cas d'un jar Jenkins hors de l'arborescence source). Par défaut : `false`. |

Ces mêmes paramètres, avec les mêmes valeurs, sont déjà commentés directement dans
`seleniumdemo.xml` à côté de leur déclaration — cette page les regroupe pour qui veut la vue
d'ensemble sans ouvrir le XML.

## Pour aller plus loin

- Vision globale, ce qui est fait / ce qui manque : [`vision.md`](vision.md).
- Format complet des scénarios (JSON, Excel, alias, chaînage de résultats) :
  [`README-workflow-scenarios.md`](../../README-workflow-scenarios.md).
- API `Params` (`JsonParams`/`MapParams`) pour les données propres à un site :
  [`conventions/params.md`](conventions/params.md).
