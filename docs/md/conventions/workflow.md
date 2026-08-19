# Écrire un workflow

Un workflow porte la logique métier du projet et les données de ce cas d’usage.

Il sert à enchaîner les pages web de manière cohérente, sans prendre la main sur l’exécution globale.

## Utiliser l’annotation `@Workflow`

Chaque méthode de workflow qui doit apparaître dans le rapport ou être appelée comme étape métier reçoit `@Workflow`.

Cette annotation sert à deux choses différentes :

- `name` décrit l’étape telle qu’elle doit apparaître dans le rapport ;
- `description` ajoute un complément utile au rapport quand le nom ne suffit pas ;
- `code` identifie l’étape de façon stable pour le catalogue et l’exécution pilotée par scénario.

Le principe est simple :

- si la méthode doit juste être lisible dans les rapports, `name` suffit ;
- si on veut ajouter un contexte lisible au rapport, on remplit aussi `description` ;
- si la méthode doit être retrouvée par un scénario JSON ou listée dans le catalogue, il faut aussi un `code`.

### Quand mettre `code`

On met `code` quand la méthode doit être :

- appelée depuis un scénario piloté par code ;
- retrouvée par le registre des workflows ;
- exposée dans un catalogue ou une liste de références stables ;
- utilisée comme point d’entrée durable entre configuration et exécution.

Le `code` doit être :

- unique ;
- stable ;
- court ;
- sans dépendre du texte du rapport.

### Quand mettre `description`

On met `description` quand on veut compléter le `name` avec une phrase plus explicite, par exemple :

- préciser le but exact de l’étape ;
- donner un contexte métier utile à la lecture du rapport ;
- aider à comprendre une action qui serait trop courte si elle n’était nommée que par `name`.

On peut laisser `description` vide si `name` suffit déjà.

### Quand ne pas mettre `code`

On ne met pas `code` si la méthode :

- n’a pas vocation à être appelée par un scénario externe ;
- sert seulement de petite étape métier interne ;
- n’a pas besoin d’être cataloguée ;
- doit rester libre d’évolution sans contrat externe.

Dans ce cas, `name` suffit pour le rapport.

### Ce qu’on retient

- `name` = visibilité et lisibilité dans le rapport ;
- `description` = précision supplémentaire dans le rapport ;
- `code` = stabilité pour l’orchestration externe et le catalogue ;
- pas de `code` inutile sur une méthode qui n’a pas de point d’entrée externe.

### Exemple

```java
public class AssuranceWorkflow {

    @Workflow(name = "Étape: Ouvrir le contrat")
    public void ouvrirContrat() {
        new ContratPage()._ouvrir();
    }

    @Workflow(name = "Workflow contrat complet", code = "assurance.contrat.ouverture")
    public void ouvrirContratComplet() {
        ouvrirContrat();
        new ContratPage()._verifierContratCharge();
    }
}
```

Dans cet exemple :

- `ouvrirContrat()` est une étape visible dans le rapport, mais sans point d’entrée catalogue ;
- `ouvrirContratComplet()` est une vraie entrée stable pour un scénario externe, donc on lui donne un `code`.

## Rôle d’un workflow

- composer plusieurs pages ;
- appliquer une règle métier ;
- choisir une branche métier ;
- faire circuler les données nécessaires au flux ;
- transformer ou enrichir les données métier du flux si besoin.

## Ce qu’un workflow doit faire

- appeler les bonnes pages ;
- enchaîner des actions UI dans le bon ordre ;
- garder la logique métier lisible ;
- retourner une page ou un résultat utile si besoin ;
- porter la donnée métier utile au cas.

## Ce qu’un workflow ne doit pas faire

- piloter la campagne de test ;
- décider quels scénarios sont exécutés ;
- porter la campagne de test globale ;
- faire du Selenium direct si une page existe déjà ;
- devenir un fourre-tout de helpers ;
- porter du code de support transversal.

## Règle de base

Un workflow ne doit contenir que la logique métier du cas et les données qui vont avec.

Il ne doit pas :

- réécrire ce que devrait faire une page web.
- remplacer le scénario que le test doit choisir.

### Point de reprise commun

Pour des raisons de compatibilité et de chaînage, les workflows doivent se terminer sur la page de synthèse de déclaration quand ce point existe dans le parcours.

L’idée est de laisser chaque workflow dans un état lisible et réutilisable par le workflow suivant.

Règle pratique :

- le workflow exécute son flux ;
- il utilise la page métier qui mène à la synthèse de déclaration ;
- il s’arrête sur la synthèse de déclaration ;
- le workflow suivant repart depuis cet état commun ;
- on évite de laisser un workflow bloqué sur un écran intermédiaire difficile à reprendre.

## Ordre conseillé

```text
1. recevoir ou construire les données utiles
2. ouvrir ou récupérer la bonne page
3. exécuter les actions métier dans l’ordre
4. vérifier le résultat métier local si nécessaire
5. retourner une page, un état, ou `this`
```

## Bon usage des pages

Le workflow doit utiliser les pages métier, pas les détails de Selenium.

Exemple :

```java
public class DemandeWorkflow {

    public DemandePage creerDemande(String nom, String type) {
        return new AccueilPage()
            ._ouvrirDemande()
            ._saisirNom(nom)
            ._choisirType(type)
            ._valider();
    }
}
```

## Bon usage des vérifications

- si la vérification est métier, elle peut être dans le workflow ;
- si la vérification sert juste à contrôler l’état d’une page, elle peut rester dans la page ;
- si la vérification sert à valider la campagne de test, elle doit rester dans le test.

## Gestion des données

Le workflow est l’endroit normal pour :

- recevoir les données métier ;
- les normaliser si besoin ;
- les répartir entre les pages ;
- appliquer une transformation simple utile au cas ;
- porter la donnée métier jusqu’au bout du flux.

Le test ne doit pas devenir l’endroit où l’on fabrique la donnée métier du cas.
Le test ne doit pas appeler les pages directement : il passe par le workflow.

## Entrées via params et json params

Quand le workflow doit lire des valeurs venant du serveur de variables, il le fait ici.

Cette section montre l'usage côté workflow. Pour le détail de l'API (`getFuzzy`, résolution
`$.`, cache, piège fichier/texte côté serveur), voir [`params.md`](params.md).

Deux formes sont surtout utilisées :

- `MapParams` pour les paramètres simples, plats, au format `cle=valeur` ou `cle:valeur` ;
- `JsonParams` pour les paramètres structurés, quand on veut garder une hiérarchie de données.

L’idée est simple :

- le workflow lit les données ;
- le workflow choisit comment les utiliser ;
- le test reste au-dessus et ne manipule pas ces détails.

### Exemple avec `MapParams`

Un cas de souscription d’assurance peut n’avoir besoin que de quelques valeurs simples :

```java
public class SouscriptionAssuranceWorkflow {

    public void souscrire() throws Exception {
        MapParams params = MapParams.load("assurance.souscription");

        new SouscriptionAssurancePage()
            ._ouvrir()
            ._identifierAssure(params.get("assure.numeroContrat"))
            ._saisirCoordonnees(params.get("assure.nom"), params.get("assure.prenom"))
            ._choisirOffre(params.get("contrat.offre"))
            ._valider();
    }
}
```

### Exemple avec `JsonParams`

Un cas de gestion de sinistre peut avoir une structure plus riche :

```java
public class SinistreAssuranceWorkflow {

    public void declarerSinistre() throws Exception {
        JsonParams params = JsonParams.load("assurance.sinistre");

        new SinistreAssurancePage()
            ._ouvrir()
            ._ouvrirDossier(params.get("dossier.id"))
            ._decrireIncident(
                params.get("incident.date"),
                params.get("incident.lieu"),
                params.get("incident.description")
            )
            ._ajouterPieces(params.get("justificatifs.photo"))
            ._envoyer();
    }
}
```

### Quand les utiliser

- `MapParams` quand on veut une liste simple de paires clé/valeur ;
- `JsonParams` quand on veut regrouper plusieurs niveaux de données ;
- `get(path)` quand on veut lire une valeur précise ;
- `getSubtree(path)` quand on veut récupérer un sous-ensemble cohérent de données.

### Exemple long avec plusieurs sources

Un workflow d’assurance peut combiner plusieurs sources de données dans une même exécution :

```java
public class DossierAssuranceWorkflow {

    @Workflow(name = "Workflow assurance complet", code = "assurance.dossier.complet")
    public void traiterDossier() throws Exception {
        String numeroContrat = PageObject.param("assurance.contrat.numero");
        String codeProduit = PageObject.param("assurance.contrat.produit");

        MapParams souscription = MapParams.load("assurance.souscription");
        JsonParams sinistre = JsonParams.load("assurance.sinistre");

        ouvrirContrat(numeroContrat, codeProduit);
        renseignerSouscription(souscription);
        declarerSinistre(sinistre);
        validerSynthese(souscription, sinistre);
    }

    private void ouvrirContrat(String numeroContrat, String codeProduit) {
        new ContratAssurancePage()
            ._ouvrir()
            ._rechercherContrat(numeroContrat)
            ._selectionnerProduit(codeProduit);
    }

    private void renseignerSouscription(Params souscription) {
        new SouscriptionAssurancePage()
            ._renseignerNom(souscription.get("assure.nom"))
            ._renseignerPrenom(souscription.get("assure.prenom"))
            ._renseignerAdresse(souscription.get("assure.adresse"))
            ._confirmer();
    }

    private void declarerSinistre(JsonParams sinistre) {
        new SinistreAssurancePage()
            ._ouvrir()
            ._ouvrirDossier(sinistre.get("dossier.id"))
            ._decrireIncident(
                sinistre.get("incident.date"),
                sinistre.get("incident.lieu"),
                sinistre.get("incident.description")
            )
            ._ajouterPieces(sinistre.get("justificatifs.photo"))
            ._envoyer();
    }

    private void validerSynthese(Params souscription, Params sinistre) {
        Map<String, String> dossier = souscription.getSubtree("assure");
        String numeroDossier = sinistre.get("dossier.id");

        new SyntheseAssurancePage()
            ._ouvrir()
            ._verifierNumeroContrat(dossier.get("numeroContrat"))
            ._verifierDossier(numeroDossier)
            ._valider();
    }
}
```

Dans cet exemple :

- `PageObject.param(...)` sert pour une valeur simple et directe ;
- `MapParams` sert pour les paramètres plats d’un bloc métier ;
- `JsonParams` sert pour les données structurées ;
- `Params` sert dans les méthodes privées quand on veut rester agnostique sur la source exacte.

L’intérêt de `Params` est simple :

- la méthode de workflow reçoit juste ce dont elle a besoin ;
- la méthode ne dépend pas de l’implémentation précise ;
- on peut lui passer aussi bien un `MapParams` qu’un `JsonParams` si la forme de lecture est compatible.

## Ce qu’on privilégie

- des méthodes courtes ;
- des noms métier clairs ;
- une seule responsabilité par méthode ;
- un retour lisible pour chaîner si nécessaire.

## Ce qu’on évite

- des workflows trop longs ;
- des méthodes qui mélangent navigation, pilotage et support ;
- des helpers cachés dans le workflow ;
- des règles différentes selon les appels.

## Règle de nommage

Le nom du workflow doit rester stable et parlant.

Il doit refléter le métier réel du flux, pas le détail technique.

### Règle pour les noms de variables

- le nom doit être identifiable à la lecture ;
- il doit dire tout de suite ce qu’il représente ;
- on évite les suffixes génériques comme `Data` quand le nom métier suffit ;
- on garde un suffixe seulement s’il apporte une vraie précision ;
- on préfère `sinistreAssurance`, `dossierSinistre`, `numeroContrat` à des noms vagues comme `sinistreData` ou `assuranceData`.

## Raccourci de lecture

```text
test → choisit
workflow → porte la donnée et enchaîne
page → agit
```
