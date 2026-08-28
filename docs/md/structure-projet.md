# Structure du projet

```text
seleniumdemo/
└── src/
    └── test/
        └── java/
            └── com/example/seleniumdemo/
                ├── tests/
                ├── workflows/
                ├── webpage/
                ├── unitaire/
                ├── integration/
                └── custom/
                    ├── catalogue/
                    ├── reporting/
                    ├── scenarios/
                    ├── server/
                    ├── testdata/
                    ├── tests/
                    └── utils/
```

`tests` lance les scénarios, `workflows` porte la logique métier, `webpage` fait les actions
Selenium, `custom` regroupe le support commun. Le principe général est déjà couvert dans
[`architecture.md`](architecture.md) ; cette page se concentre sur "où mettre quoi" au quotidien.

`unitaire` et `integration` sont à part : pas des campagnes de test produit (contrairement à
`tests`/`custom/tests`), mais des tests **du moteur lui-même** — la garantie que
`WorkflowVariableScanner`, `JsonScenarioSource`/`ExcelScenarioSource`, `ServerDrivenScenarioTest`
etc. font bien ce qu'ils prétendent faire. `unitaire` : logique pure, aucun réseau ni
navigateur, rapide et déterministe. `integration` : dépend d'un système réel externe (le
serveur de variable en HTTP, par exemple), pas de navigateur pour autant. Une classe qui pilote
réellement un navigateur contre un vrai site (`WebFormTest`, `MasterWorkflowTest`,
`ServerDrivenScenarioTest`...) reste dans `tests`/`custom/tests` — ce n'est pas un test du
moteur, c'est le moteur en action.

## Ce que contient `custom/`

- `catalogue/` — génération et lecture du catalogue des workflows
- `reporting/` — listeners, aspects, suivi d'exécution
- `scenarios/` — lecture et transformation des fichiers de scénario (JSON/Excel)
- `server/` — client et configuration du serveur de variables
- `testdata/` — chargement des données de test (`JsonParams`, `MapParams`)
- `tests/` — tests techniques ou de pilotage (le générateur de catalogue, par exemple)
- `utils/` — utilitaires transverses sans logique métier

`custom` ne doit jamais devenir une deuxième couche métier déguisée en support technique — le
jour où une classe de `custom/` commence à connaître les règles d'un produit précis, c'est qu'elle
est mal placée.

## Où va une nouvelle classe

Si elle pilote l'exécution → `tests`. Si elle enchaîne du métier → `workflows`. Si elle parle au
navigateur → `webpage`. Si c'est du support transversal, indépendant du produit → `custom`.

```text
un test
→ lit les variables serveur
→ appelle un workflow
→ le workflow utilise une page web
→ la page exécute l'action UI
→ custom sert de support en arrière-plan, à chaque étage
```
