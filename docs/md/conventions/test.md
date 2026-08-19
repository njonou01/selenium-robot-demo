# Écrire un test

Un test est le point d’entrée d’exécution.

Il orchestre le scénario, choisit le flux à lancer et vérifie le résultat attendu.

## Rôle d’un test

- choisir le scénario à exécuter ;
- appeler le bon workflow ;
- vérifier un résultat métier ;
- rester lisible comme point d’entrée.

## Ce qu’un test doit faire

- décider quel workflow lancer ;
- vérifier le résultat final ;
- rester au niveau orchestration.

## Ce qu’un test ne doit pas faire

- cliquer dans l’IU directement ;
- appeler une page web directement ;
- manipuler un sélecteur ;
- porter ou fabriquer la logique métier détaillée ;
- réécrire le comportement d’une page ou d’un workflow ;
- devenir un deuxième workflow.

## Règle de base

Le test parle au niveau du cas d’usage.

Il ne doit pas descendre dans le détail technique de la navigation.

## Ordre conseillé

```text
1. choisir le scénario ou le flux
2. appeler le workflow
3. vérifier le résultat métier final
4. remonter les informations utiles au diagnostic
```

## Bon usage

- un test = un objectif d’exécution ;
- peu de données manipulées localement ;
- une vérification finale claire ;
- peu de logique cachée.

## Ce qu’on évite

- des tests qui pilotent eux-mêmes le navigateur ;
- des tests qui contiennent toute la logique métier ;
- des tests qui mélangent données métier, UI et orchestration ;
- des tests qui deviennent difficiles à lire.

## Vérifications

Les vérifications métier finales doivent rester dans le test.

Le test ne doit pas construire les données métier du cas.
Il délègue cette responsabilité au workflow.

Les vérifications locales liées à l’IU restent dans les pages ou dans le workflow.
Le test, lui, ne parle jamais directement aux pages.

## Point de reprise commun

Quand plusieurs workflows s’enchaînent, le test doit seulement piloter l’ordre des workflows.

Il ne doit pas gérer l’écran intermédiaire entre deux workflows.
Il considère que chaque workflow laisse le parcours sur un point de reprise commun, comme la synthèse de déclaration quand ce point existe.

En pratique :

- le test lance les workflows dans l’ordre ;
- le workflow s’arrête sur son point de reprise ;
- le test passe au workflow suivant ;
- le test ne reprend pas la main sur la page de synthèse.

## Rapport entre test et workflow

```text
test = choisit et vérifie
workflow = porte la donnée et enchaîne le métier
page = exécute l’IU
```

## Exemple simple

```java
public class DemandeTest {

    public void creerUneDemande() {
        DemandeWorkflow workflow = new DemandeWorkflow();
        workflow.creerDemande();
    }
}
```

## Règle de nommage

Le nom du test doit décrire ce qu’il vérifie ou quel flux il pilote.

Il doit être stable et lisible dans les rapports.
