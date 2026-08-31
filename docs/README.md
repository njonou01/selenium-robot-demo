# Documentation `<projet>`

> **Placeholders.** `<projet>` et `<package-interne>` apparaissent dans plusieurs pages à la
> place du vrai nom de projet et du vrai package Java — à remplacer par les valeurs réelles avant
> usage. C'est volontaire, pas un oubli.

## Par où commencer

Deux publics, deux points d'entrée. Inutile de tout lire — choisis ta ligne selon ce que tu dois
faire.

**Je compose ou j'adapte un scénario de test, je ne touche pas au code Java :**

1. [`guide-scenariseur.md`](guide-scenariseur.md) — "quel circuit choisir", vue d'ensemble du
   moteur, paramètres de lancement.
2. [`conventions/scenario.md`](conventions/scenario.md) — où sont les scénarios, quel test
   lancer.
3. [`README-workflow-scenarios.md`](README-workflow-scenarios.md) — format complet JSON/Excel,
   chaînage de résultats, alias.

**J'ajoute ou je modifie du code (page, workflow, test) :**

1. [`architecture.md`](architecture.md) — les couches et leur rôle.
2. [`structure-projet.md`](structure-projet.md) — où mettre quelle classe.
3. [`CONTRIBUTING.md`](CONTRIBUTING.md) — règles concrètes d'écriture (PageObject, Workflow,
   nommage, ce qu'on n'utilise pas et pourquoi).
4. [`REGLES_EQUIPE.md`](REGLES_EQUIPE.md) — nommage déterministe des classes (Page, Workflow,
   Test), règle de la page de synthèse en fin de workflow complet.
5. [`conventions/README.md`](conventions/README.md) — conventions détaillées par brique
   (page, workflow, test, params).
6. [`framework.md`](framework.md) — ce qui vient de SeleniumRobot, pas du projet.

**Je veux l'état des lieux, ce qui est solide, ce qui manque :**

- [`vision.md`](vision.md) — pas "comment ça marche", mais "où on en est".

## Toutes les pages

| Page | Public | Contenu |
|---|---|---|
| [`guide-scenariseur.md`](guide-scenariseur.md) | Non-dev + dev | Pourquoi le scénariseur existe, quel circuit choisir, paramètres testng.xml |
| [`conventions/scenario.md`](conventions/scenario.md) | Non-dev | Où sont les scénarios, quel test lancer |
| [`README-workflow-scenarios.md`](README-workflow-scenarios.md) | Non-dev + dev | Format complet des scénarios (JSON, Excel, alias, chaînage) |
| [`architecture.md`](architecture.md) | Dev | Vue d'ensemble des couches |
| [`structure-projet.md`](structure-projet.md) | Dev | Arborescence, où va une nouvelle classe |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Dev | Règles d'écriture concrètes |
| [`REGLES_EQUIPE.md`](REGLES_EQUIPE.md) | Dev | Nommage déterministe (Page/Workflow/Test), règle de la page de synthèse |
| [`conventions/README.md`](conventions/README.md) | Dev | Index des conventions (page, workflow, test, params) |
| [`framework.md`](framework.md) | Dev | Ce qui appartient à SeleniumRobot |
| [`support.md`](support.md) | Dev | Briques transverses (`custom/`), reporting, diagnostic |
| [`vision.md`](vision.md) | Décideur / dev | État des lieux, ce qui manque, pistes non commencées |

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
