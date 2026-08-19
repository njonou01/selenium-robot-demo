# Vue d’ensemble

L’architecture du projet repose sur des couches séparées, chacune avec un rôle précis.

## Flux principal

```text
Variables serveur → Tests → Flux métier → Pages web
```

Ce flux résume la chaîne normale d’exécution :

- les variables serveur pilotent l’exécution ;
- les tests choisissent le scénario et appellent le bon flux ;
- le flux métier porte et transforme les données métier puis enchaîne les actions ;
- les pages web exécutent les interactions UI.

Un test ne parle jamais directement à une page web : il passe toujours par un flux métier.

## Ce qui ne doit pas arriver

- une page web qui lit directement une variable serveur ;
- un test qui porte la logique métier détaillée ;
- un flux métier qui devient un gestionnaire de campagne ;
- `custom` qui devient un deuxième cœur métier ;
- plusieurs façons différentes de définir le même code de workflow.

## Rôle des couches

### Tests

- orchestrent les scénarios ;
- appellent les flux métier ;
- vérifient le résultat attendu ;
- ne font pas de Selenium direct ;
- ne fabriquent pas la donnée métier du cas ;
- ne touchent jamais directement aux pages.

### Flux métier

- portent la logique métier ;
- portent aussi les données du cas d’usage ;
- enchaînent les pages web ;
- appliquent les règles du domaine ;
- ne pilotent pas la campagne de test.

### Pages web

- font les actions UI ;
- cliquent, saisissent, lisent et attendent ;
- ne lisent pas les variables serveur ;
- ne portent pas la logique métier.

### Custom

- contient le support technique ;
- regroupe le catalogue des workflows, le client des variables serveur, les logs et les helpers ;
- ne porte pas la logique métier du produit.

## Exemples concrets

- un test peut choisir le scénario à lancer ;
- un test ne doit pas ouvrir une page web lui-même ;
- un flux métier peut lire ou recevoir une donnée métier commune comme un profil ou un jeu de paramètres ;
- une page web peut recevoir un sélecteur ou une valeur, mais pas aller chercher elle-même la configuration ;
- un catalogue peut lister les codes des workflows, mais il ne doit pas devenir la source de vérité manuelle si le code sait déjà produire cette information.

## Diagramme associé

- [Diagramme Draw.io](../architecture.drawio)
