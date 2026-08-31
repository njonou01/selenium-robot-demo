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

Cette page complète [`README-workflow-scenarios.md`](README-workflow-scenarios.md) (le
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

## Quel circuit choisir

Un même besoin — "je veux tester ce parcours" — peut se couvrir de deux façons radicalement
différentes. Se tromper de circuit coûte cher dans les deux sens : forcer du code pour un cas que
le scénariseur couvrait déjà, ou forcer un scénario compliqué dans un format qui ne peut pas
l'exprimer (comportement métier qui n'existe pas encore).

### L'image du restaurant

Le **circuit technique**, c'est la cuisine. Un dev est le cuisinier : il écrit une recette fixe —
une classe `Workflow`, des étapes `@Workflow` dans un ordre précis, compilée et testée. Changer la
recette demande de rouvrir le code, de recompiler, de redéployer.

Le **scénariseur**, c'est le menu et la commande du client. Le rédacteur de scénario (testeur
manuel, chef de projet) compose sa commande à partir des plats que la cuisine sait déjà faire :
quels workflows, dans quel ordre, avec quelles données. Il ne rentre jamais en cuisine — il ne
touche jamais au code. Composer une nouvelle commande, c'est éditer une variable serveur, pas
recompiler.

Un menu ne peut proposer que ce que la cuisine sait cuisiner. Le scénariseur ne peut chaîner et
paramétrer que des workflows **déjà codés** par un dev — il n'invente jamais de comportement
nouveau, il orchestre l'existant.

### Circuit 1 — technique (dev)

À utiliser quand le besoin demande un comportement qui n'existe pas encore dans aucun workflow :
un nouveau site, un nouveau parcours métier, une nouvelle règle.

1. Un besoin arrive : un parcours ou un site jamais couvert (ex : le portail expertise auto,
   jamais testé jusque-là).
2. Le dev écrit (ou complète) une `PageObject` dans `webpage/` — sélecteurs et actions Selenium
   pour ce site (ex : `PortailExpertiseAutoPage`).
3. Le dev écrit (ou complète) un `Workflow` dans `workflows/` — enchaîne les pages, porte la
   logique métier, une étape `@Workflow` par action significative pour le rapport. Exemple :
   `MissionAutoWorkflow`, avec des étapes comme `step1_ouvrirMission`, `step2_saisirChiffrage`.
4. Si ce parcours doit un jour être pilotable depuis un scénario, la méthode `fullXxxFlow()` porte
   un `code` stable (`xxx.full` — ex : `MissionAutoWorkflow.fullMissionAutoFlow()`,
   `code = "missionauto.full"`) — voir [`CONTRIBUTING.md`](CONTRIBUTING.md) pour les règles
   précises (nommage, `Lazy<T>`, où mettre quoi).
5. Si le site a besoin de données propres (identifiants, config d'environnement), elles passent
   par `JsonParams`/`MapParams` — jamais en dur dans le code.
6. `mvn clean test-compile` doit passer ; le workflow apparaît automatiquement dans le catalogue
   généré, sans rien recopier à la main.

Dans ce circuit, un "jeu de données" éventuel (`xxx.params`, ex : `missionauto.params`) est une
**config de site partagée** (URL, identifiants d'environnement) — pas une variation de cas de
test. C'est le rôle de `JsonParams`/`MapParams`.

```text
Besoin (parcours jamais codé)
        │
        ▼
PageObject (webpage/) ─── sélecteurs, actions Selenium
        │
        ▼
Workflow (workflows/) ─── étapes @Workflow, logique métier
        │
        ▼
mvn clean test-compile
        │
        ▼
Catalogue mis à jour automatiquement (aucune doc à recopier à la main)
        │
        ▼
Comportement disponible — testable directement, ou branché au scénariseur si un `code` a été posé
```

**Concrètement, pour un non-testeur (le dev) :** le besoin arrive sous forme de ticket ou de
demande métier ("il faut pouvoir tester l'ouverture d'une mission auto"). Il ouvre son IDE, pas
d'admin serveur, pas de fichier Excel. Il écrit ou complète une `PageObject` puis un `Workflow`
(`MissionAutoWorkflow`), compile, vérifie que ça passe. Il ne touche à aucun scénario JSON/Excel
existant — s'il faut que ce nouveau parcours soit combinable plus tard, il pose juste un `code`
stable (`missionauto.full`) sur la méthode `fullMissionAutoFlow()`, et s'arrête là : c'est au
rédacteur de scénario, ensuite, de décider s'il l'utilise et comment.

### Circuit 2 — scénariseur (non-dev)

À utiliser quand le besoin se couvre en combinant des workflows **qui existent déjà**, avec des
données différentes selon le cas.

1. Un besoin arrive : un enchaînement de parcours déjà codés (ex : ouvrir une mission d'expertise
   auto, puis engager un recours si le tiers est identifié).
2. Le rédacteur compose un scénario JSON ou Excel : un nom, une liste ordonnée de codes de
   workflow existants (`missionauto.full`, `recours.full`), et les données (`dataSet`) dont chaque
   workflow a besoin.
3. Optionnel : alias pour donner un nom métier au scénario, chaînage de résultat entre deux
   workflows (`${result:code.champ}`) si l'un doit réutiliser une valeur produite par un autre
   (ex : le numéro de sinistre ouvert par `missionauto.full`, réutilisé par `recours.full`).
4. Le scénario est uploadé dans la variable serveur `workflow.scenarios` (ou une variable dédiée
   via `-Dscenarios=`).
5. Lancer la suite `<projet>-vds-only.xml` (ou la suite complète).
6. Le moteur valide tout **avant** d'ouvrir un navigateur (dataSet incomplet, code de workflow
   inconnu, nom de scénario dupliqué, chaînage mal ordonné), puis exécute — chaque scénario
   devient un test nommé d'après son `name`, chaque workflow une étape imbriquée dans le rapport.

Dans ce circuit, le `dataSet` est une **variation par cas** (trois sinistres différents, avec des
garanties ou des montants différents, passés dans le même enchaînement de workflows, par exemple)
— pas une config de site.

```text
Besoin (combiner des workflows déjà codés)
        │
        ▼
Composer le scénario (JSON ou Excel) ─── nom, codes de workflow, dataSet, alias
        │
        ▼
Uploader sur la variable serveur `workflow.scenarios` (admin serveur de variable)
        │
        ▼
Lancer la suite `<projet>-vds-only.xml`
        │
        ▼
Validation à sec ─── AVANT tout navigateur (dataSet incomplet, code inconnu, nom dupliqué...)
        │
        ▼
Exécution ─── un scénario = un test nommé, un workflow = une étape imbriquée
        │
        ▼
Rapport HTML lisible ─── quoi a tourné, avec quelle donnée, où ça a cassé si ça casse
```

**Concrètement, pour le testeur manuel :** il n'ouvre jamais l'IDE, ne voit jamais une ligne de
Java. Le besoin arrive ("je veux tester l'ouverture d'une mission auto suivie d'un recours") ; il
consulte le catalogue généré pour savoir quels codes de workflow existent (`missionauto.full`,
`recours.full`) et quels paramètres ils attendent, compose son scénario dans un fichier JSON ou
une feuille Excel, l'uploade dans la variable serveur, puis lance la suite (localement ou via
Jenkins selon l'équipe). Le rapport lui dit directement, en français, ce qui a tourné et où ça a
cassé — jamais une stack trace Java brute. S'il veut tester trois sinistres différents sur le
même enchaînement, il ajoute trois entrées au `dataSet` : pas besoin de redemander au dev, pas
besoin de recompiler.

### Ce qui est commun aux deux circuits

`JsonParams`/`MapParams` existent dans les deux mondes avec le même rôle : des réglages de site
partagés, indépendants du cas de test en cours — ni le code d'un `Workflow`, ni le `dataSet` d'un
scénario ne devraient porter une donnée qui ne change jamais d'un cas à l'autre.

### Décider vite

| Situation | Circuit |
|---|---|
| Site ou parcours métier jamais codé | Technique (dev) |
| Combiner des workflows déjà codés, sans toucher au code | Scénariseur |
| Multiplier les cas (sinistres, montants d'indemnisation, jeux de données) sur un enchaînement déjà codé | Scénariseur (`dataSet`) |
| Un comportement métier nouveau à tester, même une seule fois | Technique (dev) — éventuellement branché au scénariseur ensuite, une fois le `code` posé |

Détail complet de chaque circuit : [`CONTRIBUTING.md`](CONTRIBUTING.md) pour le technique,
[`README-workflow-scenarios.md`](README-workflow-scenarios.md) pour le scénariseur.

## Guide d'utilisation rapide

1. Composer le scénario (JSON ou Excel — format complet dans
   [`README-workflow-scenarios.md`](README-workflow-scenarios.md)).
2. L'uploader dans la variable serveur `workflow.scenarios` (ou une autre, voir plus bas
   `-Dscenarios`), depuis l'admin du serveur de variable.
3. Lancer la suite `<projet>-vds-only.xml` (ou `<projet>.xml`, qui inclut aussi les
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
`<projet>.xml` à côté de leur déclaration — cette page les regroupe pour qui veut la vue
d'ensemble sans ouvrir le XML.

## Pour aller plus loin

- Vision globale, ce qui est fait / ce qui manque : [`vision.md`](vision.md).
- Format complet des scénarios (JSON, Excel, alias, chaînage de résultats) :
  [`README-workflow-scenarios.md`](README-workflow-scenarios.md).
- API `Params` (`JsonParams`/`MapParams`) pour les données propres à un site :
  [`conventions/params.md`](conventions/params.md).
