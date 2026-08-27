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
