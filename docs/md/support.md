# Support du projet

Les briques transverses qui ne sont ni des pages, ni des workflows, ni des tests métier — tout
ce qui vit dans `custom/` (détail des sous-dossiers dans
[`structure-projet.md`](structure-projet.md)).

## Variables serveur

Pilotent l'exécution et fournissent les données aux workflows : quels scénarios lancer
(`workflow.scenarios`), la configuration d'environnement, des valeurs partagées entre plusieurs
workflows pour éviter de dupliquer la même donnée partout. Les tests les lisent pour orchestrer,
les workflows pour transformer la donnée — les pages web n'y touchent jamais directement. Le
détail complet (format, exemples, override par workflow) est dans
`README-workflow-scenarios.md` à la racine du projet.

## Catalogue des codes

Référence les workflows de façon stable : un seul code par workflow, pas de doublon, stable dans
le temps. Il sert au routage, à la documentation, et au diagnostic. Le point qui compte vraiment :
s'il est généré depuis le code, il doit refléter l'existant réel — jamais devenu une deuxième
vérité tenue à la main en parallèle du code (voir la note dans
[`architecture.md`](architecture.md) sur ce qui arrive quand on maintient ça à la main).

## Reporting et diagnostic

Ce qu'on trace : logs, captures d'écran, catalogue des workflows, helpers techniques. Ce qu'on
doit pouvoir reconstituer à la lecture du rapport, sans aller fouiller le code : quel scénario a
tourné, quel workflow a été appelé, quelle donnée a été utilisée, où ça a cassé, et si le souci
vient du métier, de l'UI, ou de la configuration. Le nom de la page et le label des éléments sont
ce qui rend ça possible ou pas — voir [`conventions/page.md`](conventions/page.md).

### Le rapport custom

Le rapport HTML par défaut de seleniumRobot vient avec sa propre mise en forme (AdminLTE). Le
projet en a une version repeinte — mêmes données, même structure, habillage visuel propre : une
barre de navigation dédiée, une charte de couleurs cohérente, la police forcée en clair (jamais
de bascule sombre selon le système du lecteur), un correctif explicite sur un défaut d'AdminLTE
qui limitait la hauteur de l'en-tête et forçait une police cursive peu lisible sur les cartes de
statut.

Activé par le paramètre `customReport` (`true`/`false`) dans le XML de suite — désactivé, le
rapport standard du framework s'affiche sans rien de custom. 3 gabarits Velocity sont remplacés :
`report.test.vm` (page de détail d'un test), `report.part.suiteSummary.vm` (page de résumé de
suite), `fonts.part.vm` (police Inter embarquée en base64 — le rapport reste lisible et bien mis
en forme même ouvert sans connexion internet ou hors de son dossier de sortie standard).

Techniquement, c'est `CustomReportListener` qui bascule le classloader du thread courant vers un
dossier temporaire contenant ces 3 gabarits, avant que le framework ne génère le rapport — voir
[`jenkins-jar-fixes.md`](jenkins-jar-fixes.md) pour l'incident qui a façonné cette implémentation
(et pourquoi elle marche identiquement en local et en jar packagé).
