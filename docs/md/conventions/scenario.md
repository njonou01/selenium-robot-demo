# Scénariser avec JSON ou Excel

Cette page explique comment lancer les tests pilotés par le serveur de variables.

L’idée est simple :

- le scénario choisit les workflows à enchaîner ;
- le workflow lit ses données ;
- le serveur de variables reste la source de vérité ;
- les tests ne touchent jamais directement aux pages.

## 1. Quel test lancer

Le scénario piloté par variable passe par :

- `com.example.seleniumdemo.custom.tests.ServerDrivenScenarioTest`

Le plus simple pour ne lancer que ce mode est d’utiliser le suite XML :

- `data/seleniumdemo/testng/seleniumdemo-vds-only.xml`

Le suite principal `data/seleniumdemo/testng/seleniumdemo.xml` lance aussi les autres tests du projet.

## 2. Où mettre les scénarios

Le test lit une variable serveur dont le nom par défaut est :

- `workflow.scenarios`

On peut changer ce nom avec la propriété système :

```text
-Dscenarios=mon.autre.variable
```

## 3. JSON ou Excel

Le moteur choisit le format à partir du nom du fichier chargé depuis le serveur de variables :

- `*.xlsx` ou `*.xls` → lecture Excel ;
- sinon → lecture JSON.

### Format JSON

Le contenu attendu est une liste d’objets avec :

- `name` : nom lisible du scénario ;
- `sinistre` : valeur métier simple passée au premier workflow ;
- `steps` : liste des codes de workflows à enchaîner.

Exemple :

```json
[
  {
    "name": "Scenario Bancaire RH",
    "steps": [
      "banking.full",
      "hr.full"
    ],
    "sinistre": "5EWOOI5B"
  }
]
```

### Format Excel

Le fichier Excel doit contenir au minimum une feuille avec les colonnes :

- `scenario_name`
- `sinistre`
- `step`

Principe de lecture :

- une nouvelle valeur dans `scenario_name` démarre un scénario ;
- les lignes suivantes complètent les `step` du même scénario ;
- `sinistre` est repris pour le scénario courant.

## 4. Comment l’exécution se fait

Quand le test tourne :

1. il lit la variable serveur `workflow.scenarios` ;
2. il choisit le parseur JSON ou Excel ;
3. il construit la liste des scénarios ;
4. il récupère les codes de workflows ;
5. il appelle les workflows dans l’ordre ;
6. chaque workflow lit ses propres données métier.

## 5. Règles à garder

- le scénario ne transporte pas les données métier détaillées ;
- les workflows ne sont pas appelés avec des arguments depuis le JSON actuel ;
- les données restent dans le serveur de variables ;
- les pages restent derrière les workflows.

## 6. Résumé

```text
serveur de variables
→ scénario JSON ou Excel
→ ServerDrivenScenarioTest
→ workflows
→ pages
```
