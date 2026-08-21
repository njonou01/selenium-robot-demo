# Documentation Selenium Demo

Bienvenue dans la documentation du projet.

## Pages utiles

- [Structure du projet](md/structure-projet.md)
- [Vue d’ensemble](md/architecture.md)
- [Conventions](md/conventions/README.md)
- [Framework SeleniumRobot](md/framework.md)
- [Support du projet](md/support.md)
- [Scénariser avec JSON ou Excel](md/conventions/scenario.md)
- [Pourquoi le rapport custom plantait sur Jenkins (et ce qu'on a durci autour)](md/jenkins-jar-fixes.md)

## Schémas

- [Diagramme principal](architecture.drawio)

## Lecture rapide

- `tests` orchestre les scénarios ;
- `workflows` porte la logique métier ;
- `webpage` exécute les actions Selenium ;
- `custom` contient le support commun.

## Pourquoi cette architecture

On a choisi cette architecture pour séparer clairement les responsabilités et garder le projet lisible quand il grandit.

L’idée est simple :

- les tests décident quoi lancer, pas comment l’UI fonctionne ;
- les workflows portent le métier et la donnée du cas ;
- les pages encapsulent l’IU et les sélecteurs ;
- le support commun reste dans `custom`, sans mélanger la logique métier.

## Ce que ça apporte

- des tests plus courts et plus lisibles ;
- moins de duplication de navigation ;
- des pages réutilisables ;
- une meilleure traçabilité dans les rapports ;
- une source de vérité plus claire pour les données métier.

## Les limites à garder en tête

- il faut respecter les conventions de nommage et de couches ;
- il y a plus de fichiers à comprendre au début ;
- si on mélange les rôles, l’architecture perd vite son intérêt ;
- `custom` doit rester stable : on ne lui ajoute pas de nouvelle responsabilité métier.

## Règle de fond

- les tests ne touchent jamais directement aux pages ;
- les workflows lisent les données et enchaînent les pages ;
- les pages restent la couche UI ;
- `custom` reste un support technique partagé, pas une couche métier cachée.
