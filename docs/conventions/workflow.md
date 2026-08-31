# Écrire un workflow

Un workflow porte la logique métier et les données du cas d'usage. Il enchaîne les pages web,
sans prendre la main sur l'exécution globale — ça, c'est le rôle du test.

## L'annotation `@Workflow`

`name` décrit l'étape telle qu'elle doit apparaître dans le rapport — c'est ce que lit un testeur
manuel, pas un dev, donc en français et sans jargon. `description` ajoute un complément quand le
nom seul ne suffit pas à comprendre l'étape. `code` identifie la méthode de façon stable pour le
catalogue et pour l'exécution pilotée par scénario (`workflow.scenarios`).

On ne met un `code` que si la méthode doit être appelable depuis un scénario JSON, ou listée dans
le catalogue comme point d'entrée durable. Une étape interne, qui n'a pas vocation à être
déclenchée de l'extérieur, se contente de `name` :

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

`ouvrirContrat()` reste visible dans le rapport mais n'a pas de point d'entrée catalogue.
`ouvrirContratComplet()` en a un, parce que c'est elle qu'un scénario externe doit pouvoir
appeler.

## Ce qu'un workflow fait, et ce qu'il ne fait pas

Un workflow compose des pages, applique une règle métier, choisit une branche, fait circuler et
transforme la donnée du cas. Il ne pilote pas la campagne de test, ne décide pas quels scénarios
tournent, et ne fait pas de Selenium direct si une page existe déjà pour ça — sinon on se retrouve
avec deux endroits qui savent parler au navigateur, et le jour où le sélecteur change il faut
penser aux deux.

### Point de reprise commun

Pour pouvoir chaîner plusieurs workflows dans un même scénario, chacun doit se terminer dans un
état lisible et réutilisable par le suivant — en général la page de synthèse de déclaration
quand elle existe dans le parcours. Un workflow qui laisse le navigateur sur un écran
intermédiaire difficile à identifier casse cet enchaînement pour celui d'après.

## Ordre conseillé dans une méthode de workflow

```text
1. recevoir ou construire les données utiles
2. ouvrir ou récupérer la bonne page
3. exécuter les actions métier dans l'ordre
4. vérifier le résultat métier local si nécessaire
5. retourner une page, un état, ou this
```

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

Les vérifications métier peuvent vivre dans le workflow. Celles qui ne servent qu'à contrôler
l'état d'une page restent dans la page. Celles qui valident la campagne de test restent dans le
test — le workflow ne construit jamais la donnée métier du cas à la place du test, et le test
n'appelle jamais une page directement.

## Lire des données via `MapParams` / `JsonParams`

Deux formes selon la structure des données : `MapParams` pour des valeurs plates
(`cle=valeur`/`cle:valeur`), `JsonParams` quand on veut garder une vraie hiérarchie. Le détail de
l'API (`getFuzzy`, `getSubtree`, résolution `$.`, cache, piège fichier/texte côté serveur) est
dans [`params.md`](params.md) — ici on montre juste l'usage côté workflow.

Un cas simple, avec `MapParams` :

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

Un cas plus structuré, avec `JsonParams` :

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

Et un workflow qui combine plusieurs sources dans une même exécution — `PageObject.param(...)`
pour une valeur simple, `MapParams`/`JsonParams` pour des blocs de données, et `Params` en
paramètre de méthode privée quand on veut rester agnostique sur la source exacte :

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

L'intérêt de prendre `Params` plutôt que `MapParams`/`JsonParams` en paramètre de méthode privée:
la méthode ne dépend plus de la source exacte, tant que la forme de lecture (chemin pointé) reste
compatible.

## Nommage

Le nom du workflow doit refléter le métier réel du flux, pas un détail technique — et rester
stable dans le temps, parce que c'est ce qui apparaît partout dans les logs. Pour les variables,
on préfère un nom directement identifiable (`sinistreAssurance`, `dossierSinistre`,
`numeroContrat`) à un suffixe générique comme `Data` (`sinistreData`) qui n'ajoute rien.

## Repères rapides

```text
test → choisit
workflow → porte la donnée et enchaîne
page → agit
```

Ce qu'on évite dans un workflow : des méthodes qui mélangent navigation, pilotage et support, des
helpers cachés qui devraient être ailleurs, et des règles qui changent selon qui appelle la
méthode.
