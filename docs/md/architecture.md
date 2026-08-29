# Vue d'ensemble

Le projet est organisé en couches séparées, chacune avec un rôle précis :

```text
Variables serveur → Tests → Workflows → Pages web
```

Les variables serveur pilotent l'exécution (quel scénario, quelles données). Les tests choisissent
le scénario et appellent le bon workflow. Le workflow porte et transforme la donnée métier puis
enchaîne les pages. Les pages exécutent les interactions UI. Un test ne parle jamais directement à
une page web — il passe toujours par un workflow.

Cette séparation ne sert à rien si elle n'est pas tenue : une page qui va lire une variable serveur
elle-même, un test qui porte le détail de la logique métier, ou `custom` qui devient une deuxième
couche métier cachée, et l'architecture perd son intérêt aussi vite qu'elle a été mise en place.

## Rôle de chaque couche

**Tests** — orchestrent les scénarios, appellent les workflows, vérifient le résultat attendu.
Pas de Selenium direct, pas de fabrication de la donnée métier du cas.

**Workflows** — portent la logique métier et les données du cas d'usage, enchaînent les pages,
appliquent les règles du domaine. Ils ne pilotent pas la campagne de test.

**Pages web** — cliquent, saisissent, lisent, attendent. Elles ne lisent pas les variables
serveur et ne portent pas de logique métier.

**Custom** — le support technique commun : catalogue des workflows, client du serveur de
variables, logs, helpers. Il ne porte jamais la logique métier du produit.

## En pratique

Un test peut choisir quel scénario lancer, mais ne doit jamais ouvrir une page lui-même. Un
workflow peut recevoir une donnée métier commune (un profil, un jeu de paramètres) et
l'utiliser pour enchaîner les pages. Une page reçoit un sélecteur ou une valeur, elle ne va pas
chercher elle-même sa configuration. Et le catalogue peut lister les codes de workflow, mais ne
doit jamais devenir une source de vérité tenue à la main si le code sait déjà produire cette
information tout seul — une liste de codes recopiée à la main dans une doc diverge du code réel
dès le premier workflow ajouté sans qu'on pense à la mettre à jour.

## Diagramme associé

[Diagramme Draw.io](../architecture.drawio)
