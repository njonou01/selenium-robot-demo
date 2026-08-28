# Vision — état des lieux et pistes

Ce document répond à une question différente de celle du [`guide-scenariseur.md`](guide-scenariseur.md) :
pas "comment ça marche", mais "où on en est, qu'est-ce qui est solide, qu'est-ce qui manque".
Destiné à quiconque doit décider quoi construire ensuite, ou juger si le moteur est prêt pour
tel usage.

## Ce qui est fait

**Architecture en couches, tenue dans tout le code.** `tests` orchestre, `workflows` porte le
métier, `webpage` exécute l'UI, `custom` reste un support technique sans logique produit. Cette
règle n'est pas juste écrite dans `architecture.md` — une relecture classe par classe de tout
`custom/`, `workflows/`, `webpage/` et `tests/` n'a trouvé aucune violation.

**Scénarios pilotés par variable serveur**, sans toucher au code : JSON ou Excel, détecté à
l'extension, avec :

- isolation d'erreur par scénario (un scénario cassé n'empêche jamais les autres de tourner,
  côté JSON comme côté Excel) ;
- validation à sec avant ouverture d'un navigateur (dataSet incomplet, code inconnu, nom de
  scénario dupliqué, référence de chaînage mal ordonnée — tout détecté en quelques secondes) ;
- override de dataSet par workflow (`dataSet[code][clé]`) et mapping nom métier / nom Java
  (`@Workflow(params = {...})`) ;
- chaînage de résultats entre workflows (`${result:code.champ}`, interface `WorkflowResult`,
  records typés — pas de `Map` générique) ;
- alias de scénario optionnels, portée à un seul scénario, JSON et Excel.

Chaînage et alias prouvés en conditions réelles (navigateur, serveur de variable réel), **dans
les deux formats** : `banking.full` ouvre un vrai compte Parabank, extrait le vrai numéro, le
transmet à `hr.full` via l'alias `b`→`banking.full` — identique en JSON et en `.xlsx`.

**Catalogue auto-généré**, jamais tenu à la main : `WorkflowCatalogueGenerator` scanne les
classes `*Workflow`, produit 4 formats de sortie (`sheets`/`blocks`/`json`/`matrix`), et reste
lisible même en exécution packagée (repli `@Workflow(variables = {...})` quand la détection
automatique par lecture du source échoue en jar).

**Données de site** via `JsonParams`/`MapParams` : résolution par chemin pointé, recherche par
suffixe (`$.`), recherche approximative tolérante aux fautes de frappe (`getFuzzy`, en dépannage
seulement), extraction de sous-arbre (`getSubtree`).

**55 tests automatisés**, répartis en deux familles nouvelles (`unitaire/`, `integration/`) :
parsing JSON/Excel et isolation d'erreur, résolution et conversion de types, override par
workflow, chaînage et alias, validation à sec au niveau orchestration, et un test d'intégration
HTTP réel contre le serveur de variable. Avant cette session, ce moteur n'avait aucun test
automatisé — seulement des vérifications manuelles au coup par coup.

## Les bonnes choses

**Aucune deuxième source de vérité tenue à la main.** Le catalogue en est la preuve vivante — un
ancien `README-workflow-scenarios.md` listait les codes à la main et a fini par mentir dès le
premier workflow ajouté sans y penser. Le projet a tiré la leçon et l'a écrite noir sur blanc
dans `architecture.md`.

**Les erreurs métier disent quoi et où**, jamais une stack trace brute jetée au visage d'un
rédacteur de scénario non-dev : "le workflow 'hr.full' attend le parametre 'employee', absent
du dataSet" plutôt qu'une `NullPointerException` à la ligne 47.

**Le code documente le pourquoi, jamais le quoi.** Vérifié classe par classe cette session :
zéro commentaire qui reformule ce que le code fait déjà, chaque commentaire existant justifie
un choix non évident (pourquoi `Lazy`, pourquoi un classloader inversé, pourquoi la validation
`${result:...}` se fait à deux endroits différents).

## Ce qui manque

**`JsonParams`/`MapParams` ont divergé entre `gmfindem` (le POC) et `seleniumdemo` (ce dépôt).**
La version `seleniumdemo` est plus riche (`getFuzzy`, `getSubtree`, recherche par suffixe
`$.`) — jamais réconciliées, jamais décidé laquelle doit devenir la référence commune si les
deux projets doivent un jour reconverger.

**Pas de rapport de couverture catalogue ↔ scénarios.** Le catalogue liste tous les workflows
codés ; rien ne dit lesquels ne sont référencés par aucun scénario actif dans
`workflow.scenarios`. Un workflow codé, jamais branché, reste invisible sans grep manuel — alors
que le mécanisme pour le détecter existe déjà (même pattern que `aggregateVariables`, qui fait
exactement ce genre de croisement pour les variables serveur).

**Le serveur de variable est fragile à froid.** Aucune application/version/environnement ne
s'auto-enregistre : chaque nouvel environnement de dev (ou de CI) demande une initialisation
manuelle en base avant le premier run — vécu concrètement cette session, deux allers-retours
pour trouver la bonne version ("1.0" résolu à l'exécution, pas "1.0.0-SNAPSHOT" comme on
l'attendrait du `pom.xml`).

**Aucune détection de flakiness.** Les runs réels de cette session l'illustrent directement :
sur plusieurs exécutions, l'approbation de prêt Parabank échoue de façon non déterministe (rien
à voir avec le code — le même site refuse ou approuve le même prêt selon les essais) — rien
dans le moteur ne fait aujourd'hui la différence entre "ce site est instable" et "on a cassé
quelque chose". Chaque échec est traité pareil.

**Pas d'éditeur visuel.** Composer un scénario demande de connaître le format JSON/Excel à la
main — pas de formulaire généré depuis le catalogue qui garantirait les bons types sans
connaître la syntaxe.

**Le retry par workflow peut se cumuler avec le retry TestNG.** Un site réellement en panne peut
prendre plusieurs minutes avant que le test abandonne — connu, documenté, jamais traité.

## Pistes, pas commencées

Discutées mais pas construites, par ordre de coût croissant :

1. **Coverage report catalogue ↔ scénarios** — le plus proche d'être prêt à spécifier, pattern
   déjà existant dans le code à réutiliser.
2. **Détection de flakiness** — historique d'exécution par scénario, distinction site-instable
   vs régression réelle.
3. **Score de santé par scénario** — combinaison stabilité historique + fraîcheur des données +
   couverture, affiché dans le rapport.
4. **Éditeur visuel de scénario** — le plus gros chantier, celui qui changerait le plus la vie
   d'un rédacteur non-dev, mais qui demande une vraie interface, pas juste du code serveur.
