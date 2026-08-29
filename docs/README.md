# Documentation Selenium Demo

## Pages utiles

- [Structure du projet](md/structure-projet.md)
- [Vue d’ensemble](md/architecture.md)
- [Conventions](md/conventions/README.md)
- [Framework SeleniumRobot](md/framework.md)
- [Support du projet](md/support.md)
- [Scénariser avec JSON ou Excel](md/conventions/scenario.md)
- [Guide du scénariseur (pourquoi, objectif, paramètres testng.xml)](md/guide-scenariseur.md)
- [Vision — état des lieux et pistes](md/vision.md)

## Schémas

- [Diagramme principal](architecture.drawio)

## Comment lire le code

`tests` orchestre les scénarios, `workflows` porte la logique métier, `webpage` exécute les
actions Selenium, `custom` regroupe le support commun (reporting, chargement de données,
catalogue...).

## Pourquoi cette séparation

Le but, c'est qu'un test reste lisible même quand le projet grossit : les tests décident quoi
lancer, pas comment l'UI fonctionne ; les workflows portent le métier et la donnée du cas ; les
pages encapsulent l'UI et les sélecteurs. `custom` reste un support technique partagé — dès qu'on
commence à y mettre de la logique métier, la séparation perd son intérêt.

En pratique ça donne des tests plus courts, moins de code de navigation dupliqué entre deux
workflows qui passent par la même page, et un rapport qui reste lisible parce que chaque étape
raconte une action métier plutôt qu'un clic technique.

La contrepartie : ça fait plus de fichiers à comprendre au début, et la règle "les tests ne
touchent jamais directement aux pages" ne tient que si on la respecte vraiment — un test qui
appelle une `PageObject` directement pour "gagner du temps" casse l'intérêt de toute
l'architecture, pas juste ce test-là.
