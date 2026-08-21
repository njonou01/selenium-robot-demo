# Pourquoi le rapport custom plantait sur Jenkins (et ce qu'on a durci autour)

Cette page explique un incident réel et son correctif, pour qu'on se souvienne du "pourquoi"
la prochaine fois qu'on touche à `CustomReportListener`, `WorkflowRegistry` ou
`WorkflowCatalogueGenerator`.

## Le symptôme

Sur Jenkins, le build échouait tout de suite au démarrage du suite, avec :

```text
java.lang.IllegalStateException: Impossible de (des)activer le rapport custom
Caused by: java.lang.IllegalArgumentException: URI is not hierarchical
	at java.base/java.io.File.<init>(File.java:420)
	at ...CustomReportListener.onStart(CustomReportListener.java:34)
```

En local (`mvn test`), rien de tout ça ne se produisait — d'où la difficulté à le reproduire au
début.

## La cause

Jenkins lance les tests directement contre un `.jar` déjà construit
(`org.testng.TestNG ... test-indem.xml`), pas via `mvn test`. En local, nos classes et
ressources existent comme de vrais fichiers sur disque (`target/test-classes/...`). Dans un
`.jar`, tout est compressé dans une seule archive — il n'y a plus de "vrai dossier" à pointer.

Deux endroits du code supposaient à tort qu'il y avait toujours un dossier réel derrière :

- `CustomReportListener` essayait de calculer "le dossier racine du classpath"
  (`new File(getClass().getResource("/").toURI())`) pour y copier ses modèles de rapport.
- `WorkflowRegistry.discoverWorkflowClasses()` essayait de lister les fichiers `.class` du
  package `workflows` avec `dir.listFiles()`, sur ce même genre de chemin.

Les deux plantent avec `URI is not hierarchical` dès que ce chemin pointe vers l'intérieur d'un
jar (`jar:file:/....jar!/...`) plutôt que vers un vrai dossier.

## Le correctif

Aucun changement dans `seleniumRobot` (le framework) ni dans le build (`pom.xml`) — les deux
fixes vivent entièrement dans le code de `seleniumdemo`, dans les fichiers concernés, et marchent
identiquement en local et en jar.

### `WorkflowRegistry`

On distingue maintenant le cas jar du cas dossier réel avant d'essayer de lister les classes :

- dossier réel → comportement inchangé (`listFiles()`) ;
- jar → on ouvre le jar comme une archive (`JarFile`) et on liste ses entrées directement,
  au lieu d'essayer de le traiter comme un dossier.

### `CustomReportListener`

Plus aucune tentative d'écrire à l'intérieur du classpath d'origine (impossible dans un jar en
cours d'exécution). À la place :

1. on crée un vrai dossier temporaire sur disque (`Files.createTempDirectory`, marche toujours,
   jar ou pas) et on y copie les 3 modèles custom ;
2. le framework (Velocity) cherche toujours ses modèles via le classloader du **thread courant**
   en premier, avant de retomber sur le jar (`ClasspathResourceLoader`, code du framework,
   inchangé) — ce classloader est modifiable pendant l'exécution, contrairement au contenu du
   jar. On remplace le classloader du thread par un qui regarde d'abord notre dossier temporaire,
   puis retombe sur l'original si rien n'y est trouvé.

Résultat : si `customReport=true`, le rapport custom est actif, en local comme en jar. Si
`customReport=false`, rien ne change par rapport à avant.

### Piège trouvé en vérifiant (pas juste en lisant le log)

Première version du fix : `onStart()` posait le classloader custom, `onFinish()` le remettait
comme avant "pour nettoyer". Le log disait bien "Rapport custom active", zéro crash — mais en
inspectant le HTML généré (pas juste le log), c'était quand même le template par défaut qui
sortait.

Cause : le rapport HTML se génère en **plusieurs passes** (au moins une intermédiaire, une
finale) — la dernière passe, celle qui écrase le fichier sur le disque, peut arriver **après**
`onFinish()`. Donc au moment où ça comptait, le classloader avait déjà été remis à l'ancien.

Fix : `onFinish()` ne remet plus rien du tout. Le process se termine juste après de toute façon,
et un classloader qui délègue à l'original pour tout sauf 3 fichiers connus ne casse rien à
laisser vivre.

Leçon : un log qui dit "ça a marché" et l'absence de crash ne prouvent pas que le bon contenu a
été produit — il faut inspecter le résultat final lui-même (ici : comparer le HTML généré au
template attendu, pas juste lire les logs).

## `WorkflowCatalogueGenerator` — même famille de piège, en préventif

Ce fichier n'avait pas de bug de crash (`FileOutputStream` marche très bien en jar, contrairement
à `new File(uri)`). Mais il écrivait le classeur (`workflows-catalogue.xlsx`/`.json`) sur un
chemin **relatif fixe** (`src/test/resources/...`), résolu par rapport au dossier de lancement du
process. Ça marche tant que ce dossier existe et contient l'arborescence source à cet endroit-là —
vrai aujourd'hui sur Jenkins (le workspace contient le vrai checkout, pas juste le jar), mais une
dépendance implicite et fragile qui n'a aucune raison d'exister: ce fichier ne sert qu'à être
uploadé sur le serveur de variable juste après, rien d'autre dans le code ne le relit.

Changement : le classeur est maintenant construit dans un **fichier temporaire**
(`Files.createTempFile`, toujours un vrai chemin disque valide, peu importe d'où on lance le
process), uploadé sur le serveur, puis supprimé. Zéro dépendance à l'arborescence source pour le
comportement obligatoire (création + upload).

Une copie locale reste possible si besoin (inspecter le catalogue sans aller sur le serveur),
via le paramètre `catalogueKeepLocalCopy` (`testng.xml`, defaut `false`). Si activé mais que le
dossier `src/test/resources` n'existe pas à l'endroit attendu (ex: lancement isolé, jar seul sans
l'arborescence source), c'est un avertissement dans les logs, jamais un échec du test — la copie
serveur (obligatoire) reste la source de vérité.

Vérifié en conditions extrêmes : jar + dépendances copiés seuls dans un dossier vide (aucun
`src/`, aucun `pom.xml`, rien d'autre que le `.jar` et le fichier `testng.xml`) — création,
upload et rapport custom fonctionnent tous les trois sans rien d'autre présent sur disque.

## Ce qu'il faut retenir pour la suite

- Ne plus jamais faire `new File(url.toURI())` sur une ressource classpath sans vérifier d'abord
  `url.getProtocol()` — `"jar"` casse toujours cette conversion.
- Toute logique qui doit fonctionner à la fois en `mvn test` (dossier) et en exécution
  packagée (jar, comme sur Jenkins) doit être testée dans les deux modes avant d'être considérée
  finie. Un run `mvn test` qui passe ne garantit rien sur le comportement en jar.
