# Support du projet

Cette page regroupe les briques transverses qui ne sont ni des pages, ni des workflows, ni des tests.

## Variables serveur

Les variables serveur servent à piloter l’exécution et à fournir les données nécessaires aux flux métier.

Elles servent à :

- décider quels scénarios lancer ;
- fournir des données d’entrée aux workflows ;
- porter la configuration d’environnement ;
- mutualiser des valeurs communes.

Exemples :

- `workflow.scenarios` peut dire quels cas sont actifs ;
- une variable de profil peut indiquer un environnement ou un mode d’exécution ;
- une valeur partagée peut éviter de dupliquer la même donnée dans plusieurs workflows.

Qui les lit :

- les tests pour orchestrer ;
- les flux métier pour lire et transformer la donnée.

Qui ne les lit pas :

- les pages web.

## Catalogue des codes

Le catalogue des codes sert à référencer les workflows de manière stable et lisible.

Règles :

- un seul code par workflow ;
- pas de doublon ;
- code stable dans le temps ;
- source de vérité unique.

Rôle :

- routage ;
- documentation ;
- lecture du projet ;
- diagnostic.

Point important :

- si un catalogue est généré, il doit refléter l’existant réel ;
- il ne doit pas devenir une seconde vérité à maintenir à la main.

## Reporting et diagnostic

Ce qu’on veut tracer :

- les logs ;
- les captures d’écran ;
- le catalogue des workflows ;
- les helpers et les utilitaires techniques.

Ce qu’on doit pouvoir comprendre :

- quel scénario a été lancé ;
- quel workflow a été appelé ;
- quelle donnée a été utilisée ;
- où l’échec s’est produit ;
- si le souci vient du métier, de l’IU ou de la configuration.

Règle de nommage pour les rapports :

- le rapport doit rester lisible sans aller fouiller dans le code ;
- le nom de la page et le label des éléments doivent aider à comprendre vite.

## Points techniques

- `custom/reporting/` : listeners, aspects et suivi d’exécution ;
- `custom/catalogue/` : génération et lecture du catalogue ;
- `custom/scenarios/` : lecture des scénarios JSON / Excel ;
- `custom/server/` : client et configuration du serveur de variables ;
- `custom/testdata/` : objets de données de test ;
- `custom/tests/` : tests techniques ou de pilotage ;
- `custom/utils/` : utilitaires transverses.

