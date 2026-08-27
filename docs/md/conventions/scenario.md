# Scénariser avec JSON ou Excel

Comment lancer les tests pilotés par le serveur de variables. Le scénario choisit les workflows
à enchaîner et leurs données, le serveur de variables reste la source de vérité, les tests ne
touchent jamais directement aux pages.

Ce document couvre le format des scénarios. Pour le détail complet (types de données, override
par workflow, isolation des erreurs), voir `README-workflow-scenarios.md` à la racine du projet.

## Quel test lancer

Le scénario piloté par variable passe par `ServerDrivenScenarioTest`
(`com.example.seleniumdemo.custom.tests`). Pour ne lancer que ce mode, utiliser le suite
`data/seleniumdemo/testng/seleniumdemo-vds-only.xml`. Le suite principal
(`seleniumdemo.xml`) lance aussi les autres tests du projet.

## Où sont les scénarios

Le test lit une variable serveur, `workflow.scenarios` par défaut. On peut en lire une autre avec
`-Dscenarios=nom.de.variable` — utile pour donner à chaque équipe son propre jeu de scénarios sur
le même serveur.

## JSON ou Excel

Le format est détecté à partir du nom du fichier chargé : `.xlsx`/`.xls` déclenche la lecture
Excel, tout le reste passe en JSON.

### Format JSON

```json
[
  {
    "name": "Scenario Bancaire RH",
    "sinistre": "5EWOOI5B",
    "sbc": false,
    "steps": [
      "banking.full",
      "hr.full"
    ],
    "dataSet": {
      "employee": { "firstName": "Robert", "lastName": "Wilson" }
    }
  }
]
```

`name` est le nom lisible du scénario, `sinistre` la référence de dossier utilisée par la
déclaration, `sbc` déclenche un workflow supplémentaire en tête de scénario si `true`, `steps`
liste les codes de workflow dans l'ordre voulu, et `dataSet` porte les données dont ces workflows
ont besoin.

### Format Excel

Colonnes minimales : `scenario_name`, `sinistre`, `step`, `sbc`, `dataSet`. Une nouvelle valeur
dans `scenario_name` démarre un scénario, les lignes suivantes ajoutent des `step` au même
scénario, `sinistre` et `sbc` sont repris pour tout le bloc.

## Ce qui se passe à l'exécution

Le test lit la variable, choisit le parseur JSON ou Excel, construit la liste des scénarios, et
enchaîne les workflows demandés — en résolvant pour chacun les données dont il a besoin dans
`dataSet`. Un scénario mal formé n'empêche pas les autres de s'exécuter : l'erreur reste
attachée à celui qui a un problème.

## Résumé

```text
serveur de variables
→ scénario JSON ou Excel (name, sinistre, sbc, steps, dataSet)
→ ServerDrivenScenarioTest
→ workflows (avec leurs paramètres résolus depuis dataSet)
→ pages
```
