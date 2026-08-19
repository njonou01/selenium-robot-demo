# Structure du projet

Cette page montre la structure utile du projet `seleniumdemo` et explique le rôle des grandes branches.

## Arborescence principale

```text
seleniumdemo/
└── src/
    └── test/
        └── java/
            └── com/example/seleniumdemo/
                ├── tests/
                ├── workflows/
                ├── webpage/
                └── custom/
                    ├── catalogue/
                    ├── reporting/
                    ├── scenarios/
                    ├── server/
                    ├── testdata/
                    ├── tests/
                    └── utils/
```

## Schéma de lecture

```text
seleniumdemo
└── src/test/java/com/example/seleniumdemo
    ├── tests        → lance les scénarios
    ├── workflows    → porte la logique métier
    ├── webpage      → fait les actions Selenium
    └── custom       → support commun
        ├── catalogue
        ├── reporting
        ├── scenarios
        ├── server
        ├── testdata
        ├── tests
        └── utils
```

## À quoi servent les grandes branches

### `tests/`

Les tests sont les points d’entrée d’exécution.

Ils servent à :

- choisir quel scénario lancer ;
- lire les variables serveur ;
- appeler le bon workflow ;
- vérifier le résultat attendu.

Ils ne doivent pas :

- faire de Selenium direct ;
- contenir toute la logique métier ;
- gérer le détail des pages web.

### `workflows/`

Les workflows portent la logique métier du projet.

Ils servent à :

- enchaîner plusieurs pages ;
- appliquer les règles métier ;
- composer des séquences d’actions cohérentes.

Ils ne doivent pas :

- décider de la campagne de test ;
- lire eux-mêmes la configuration globale ;
- devenir un fourre-tout technique.

### `webpage/`

Les pages web représentent l’interface Selenium.

Elles servent à :

- cliquer ;
- saisir ;
- lire des valeurs affichées ;
- attendre un état de page ;
- encapsuler les sélecteurs.

Elles ne doivent pas :

- choisir le scénario ;
- porter la logique métier ;
- lire les variables serveur.

### `custom/`

`custom` contient les briques de support spécifiques au projet.

On y trouve :

- `catalogue/` : génération et gestion du catalogue des workflows ;
- `reporting/` : listeners, aspects et suivi d’exécution ;
- `scenarios/` : lecture et transformation des scénarios ;
- `server/` : client et configuration du serveur de variables ;
- `testdata/` : objets de données de test ;
- `tests/` : tests techniques ou de pilotage ;
- `utils/` : utilitaires transverses.

`custom` ne doit pas devenir une deuxième couche métier cachée.

## Résumé rapide

| Dossier | Rôle | Règle |
|---|---|---|
| `tests/` | orchestre les scénarios | pas de Selenium direct |
| `workflows/` | enchaîne la logique métier | ne pilote pas la campagne globale |
| `webpage/` | parle au navigateur | pas de logique métier |
| `custom/` | support commun | pas de métier caché |
| `custom/catalogue/` | catalogue des workflows | codes stables et uniques |
| `custom/reporting/` | logs et suivi | doit aider au diagnostic |
| `custom/scenarios/` | lecture des scénarios | source claire et lisible |
| `custom/server/` | serveur de variables | accès centralisé |
| `custom/testdata/` | données de test | évite les objets inutiles |
| `custom/tests/` | tests techniques | reste limité au support/pilotage |
| `custom/utils/` | utilitaires | pas de logique métier |

## Schéma de lecture simple

```text
variables serveur
      ↓
tests
      ↓
workflows
      ↓
webpage
```

Et autour de ça :

```text
custom = support commun
```

## Règle d’or

Si une classe sert à piloter, elle va plutôt dans `tests`.  
Si elle sert à enchaîner le métier, elle va plutôt dans `workflows`.  
Si elle sert à parler au navigateur, elle va plutôt dans `webpage`.  
Si elle sert au support transversal, elle va dans `custom`.

## Exemple de lecture complète

```text
un test
→ lit les variables serveur
→ appelle un workflow
→ le workflow utilise une page web
→ la page web exécute l’action UI
→ custom sert de support en arrière-plan
```
