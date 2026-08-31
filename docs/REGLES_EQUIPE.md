# Règles de code — Pages, Workflows, Tests

## 1. Pages (PageObject)

### Règle de nommage

Le nom de la classe doit permettre de **retrouver l'écran réel sans lire le code**, en cas de bug.

| Application | Source du nom |
|---|---|
| **OPTIM** | Titre affiché **au centre de l'écran** |
| **RSI** | **Code technique + nom affiché à l'écran** (les deux, combinés) |

**Format**: infinitif, sans conjonction/mot de liaison, PascalCase, suffixe `Page` obligatoire.

### Algorithme de construction du nom (déterministe)

Chaque étape doit donner le **même résultat peu importe qui l'applique** — pas de jugement
("mot important" / "mot secondaire"), sinon deux devs nomment différemment le même écran.

1. **Source brute** — OPTIM: texte exact du titre affiché au centre de l'écran. RSI: code
   technique suivi du nom affiché à l'écran, concaténés dans cet ordre (code d'abord).
   Les codes écran RSI sont en **majuscules avec underscores** (`SIN_DECL_01`) — chaque segment
   séparé par `_` devient un mot (première lettre majuscule, reste minuscule), les nombres restent
   tels quels : `SIN_DECL_01` → mots `Sin`, `Decl`, `01`.
2. **Découpage en mots**, ponctuation retirée, accents retirés (`é`→`e`, `à`→`a`...).
3. **Retrait des mots vides** — liste fixe, la même pour tout le monde:
   `et, ou, la, le, les, un, une, des, du, de, d', à, au, aux, pour, avec, dans, sur, en, ce, cette`.
   (Cette liste vit dans ce document — si un mot vide manque, on l'ajoute ici, pas au cas par cas.)
4. **Verbes à l'infinitif** — si un mot restant est un verbe conjugué, le remettre à l'infinitif.
   (Les noms/adjectifs restent tels quels — pas d'ambiguïté possible sur la forme infinitive d'un
   verbe, contrairement à "quel mot est important".)
5. **Si ≤ 6 mots** → PascalCase, direct.
6. **Si > 6 mots** → garder les **4 premiers mots entiers** (ordre d'origine), puis **abréger
   chaque mot restant à 3 caractères maximum** (pas de mot jeté, juste compressé). Objectif:
   aucune info perdue silencieusement, contrairement à une troncature qui efface des mots entiers.
7. **Si collision** (deux écrans arrivent au même nom) → préfixer avec le module/parcours métier
   parent. Ce cas est de toute façon détecté automatiquement : **deux classes avec le même nom
   dans le même package ne compilent pas** — Java force la correction au moment où ça arrive,
   impossible de la manquer silencieusement.
8. **Si titre vide/absent** (écran de chargement, popup sans titre...) → pas d'automatisation
   possible, nommage manuel par le lead technique, à documenter en commentaire dans la classe
   (`// nom choisi manuellement: titre absent a l'ecran, ecran de type XXX`).

### Pourquoi l'étape 6 (abréviation, pas sélection par importance)

Une version antérieure de cette règle disait "garde le verbe + sujet principal, lâche le
secondaire" — rejetée : deux devs choisissent des mots différents comme "importants" sur le même
titre → incohérence. Une version suivante ("garde les 6 premiers, jette le reste") réglait
l'incohérence mais perdait de l'info : si le mot jeté distinguait deux écrans proches
("Informations **Personnelles**" vs "Informations **Professionnelles**"), on recréait une
collision. **Abréger au lieu de jeter** règle les deux problèmes : déterministe (4 premiers mots
entiers + reste à 3 caractères, aucun jugement) ET aucune info totalement perdue — même abrégé,
"Per" ≠ "Pro" reste visible.

### Exemples

**OPTIM** — titre à l'écran: *"Déclarer et Saisir un Sinistre"*
```
"Déclarer et Saisir un Sinistre"
  → retire conjonctions ("et", "un") : Déclarer Saisir Sinistre
  → déjà à l'infinitif, PascalCase
  → DeclarerSaisirSinistrePage
```

**RSI** — code technique: `SIN_DECL_01`, nom affiché à l'écran: "Déclarer un Sinistre"
```
Code:  SIN_DECL_01        → SinDecl01
Nom:   "Déclarer un Sinistre" → retire conjonction ("un") → Declarer Sinistre
Combiné (code + nom)      → SinDecl01DeclarerSinistrePage
```

**Collision** — deux écrans OPTIM ont le même titre "Résultat", l'un dans le parcours Sinistre, l'autre dans le parcours Contrat:
```
Résultat (parcours Sinistre)  → SinistreResultatPage
Résultat (parcours Contrat)   → ContratResultatPage
```

**Titre long** (>6 mots même après retrait des mots vides) — titre OPTIM:
*"Consulter et Modifier les Informations Personnelles du Contrat d'Assurance Habitation"*
```
Titre brut (10 mots):
  Consulter et Modifier les Informations Personnelles du Contrat d'Assurance Habitation

Etape 3 - retire mots vides ("et", "les", "du", "d'") → 7 mots:
  Consulter Modifier Informations Personnelles Contrat Assurance Habitation

Etape 6 - encore >6 mots → garde les 4 premiers entiers, abrège le reste a 3 caracteres:
  entiers (1-4)  : Consulter Modifier Informations Personnelles
  abreges (5-7)  : Contrat → Con | Assurance → Ass | Habitation → Hab

Résultat → ConsulterModifierInformationsPersonnellesConAssHabPage
```

*(Aucun mot totalement perdu — "Con"/"Ass"/"Hab" restent visibles dans le nom, contrairement à
une troncature qui les aurait fait disparaître.)*

**Collision malgré abréviation** — deux écrans partagent les 4 premiers mots, différent seulement
sur le 5e ("Personnelles" vs "Professionnelles") :
```
"...Informations Personnelles..."    → ...Per...
"...Informations Professionnelles..." → ...Pro...
```
Distinction encore visible (`Per` ≠ `Pro`) → pas de collision réelle. Si malgré tout deux noms
finissaient identiques, l'étape 7 (préfixe module) et le compilateur Java prennent le relais.

### Structure de la classe

```java
public class DeclarerSaisirSinistrePage extends PageObject {

    private static final HtmlElement TITRE = new HtmlElement("titre", By.id("page-title"));
    private static final ButtonElement BOUTON_VALIDER = new ButtonElement("valider", By.id("btn-valider"));

    public DeclarerSaisirSinistrePage() throws Exception {
        super(TITRE, "https://optim.exemple.fr/sinistre/declarer");
    }

    @Step(name = "Valider la déclaration")
    public DeclarerSaisirSinistrePage valider() {
        BOUTON_VALIDER.click();
        return this;
    }
}
```

Points clés visibles dans l'exemple:
- Locators en `static final`, en haut de la classe.
- Constructeur = élément identifiant + URL.
- `@Step(name = "...")` sur chaque méthode publique — devient une entrée du rapport.
- Retour `this` pour permettre le chaînage (`page.valider().autreEtape()`).

---

## 2. Workflow

*(Section provisoire — nommage à valider ensemble, même logique que Pages en attendant.)*

### Règle de nommage (proposition)

Un Workflow représente un **parcours métier** (peut traverser plusieurs pages), pas un écran —
donc source différente des Pages : le nom du parcours, pas un titre d'écran.

Même algorithme déterministe que pour les Pages (mots vides retirés, infinitif, ≤6 mots direct,
>6 mots = 4 entiers + reste abrégé 3 caractères), appliqué au **nom du parcours métier**, suffixe
`Workflow` au lieu de `Page`.

*(À confirmer : c'est bien le nom du parcours qui sert de source, ou autre chose côté OPTIM/RSI ?)*

### Règle obligatoire : instanciation paresseuse de la Page

**Ne jamais instancier une PageObject dans le constructeur du Workflow ni dans un champ
initialisé directement.**

**Pourquoi**: le constructeur d'une `PageObject` navigue immédiatement vers son URL
(`openPage()`). Si cette navigation a lieu avant qu'un step `@Workflow` soit ouvert (donc dans le
constructeur du Workflow, exécuté avant tout step), le framework crée un step "openPage" orphelin
à la racine du rapport au lieu de l'imbriquer dans le step en cours — pollue le rapport, rend le
diagnostic plus dur.

**Solution** : `Lazy<T>` (`utils/Lazy.java`) — la page se crée seulement au premier vrai appel,
donc pendant qu'un step `@Workflow` est déjà actif.

```java
public class DeclarerSaisirSinistreWorkflow {

    private final Lazy<DeclarerSaisirSinistrePage> page = new Lazy<>(DeclarerSaisirSinistrePage::new);

    @Workflow(name = "Étape 1: Renseigner le numéro de contrat ${numeroContrat}")
    public void step1_renseignerContrat(String numeroContrat) throws Exception {
        page.get().saisirNumeroContrat(numeroContrat);
    }

    @Workflow(name = "Étape 2: Valider la déclaration")
    public void step2_valider() throws Exception {
        page.get().valider();
    }

    @Workflow(name = "Déclaration de sinistre complète", code = "sinistre.declaration")
    public void fullFlow(String numeroContrat) throws Exception {
        step1_renseignerContrat(numeroContrat);
        step2_valider(); // <- se termine sur SyntheseDeclarationPage, voir regle suivante
    }
}
```

Points clés:
- `Lazy<T>` sur la page, jamais `new` direct dans le constructeur.
- `@Workflow(name = "...")` sur chaque step — libellé pour un testeur manuel, pas un dev.
- `${nom}` dans le libellé doit correspondre exactement à un paramètre de la méthode.
- `code = "..."` seulement si le workflow doit être pilotable depuis le serveur de variable
  (sinon, pas de `code`).
- Toute méthode qui appelle `page.get()` doit déclarer `throws Exception`.

### Règle obligatoire : finir sur une page de synthèse

**Chaque `fullXxxFlow()` (workflow complet, avec `code=`) doit se terminer sur un écran de
synthèse/confirmation** (ex: `SyntheseDeclarationPage`), jamais sur un état intermédiaire ou
imprévisible.

**Pourquoi**: l'orchestrateur côté serveur peut **enchaîner plusieurs workflows à la suite**
(variable `workflow.scenarios`, ex: `chain: "sinistre.declaration,contrat.consultation"`). Le
workflow suivant démarre là où le précédent s'est arrêté. Si un workflow s'arrête sur un état
imprévisible (popup ouverte, formulaire à moitié rempli, écran d'erreur), le workflow suivant
dans la chaîne ne peut pas fiabilement continuer — il faut un point de repère stable et connu à
chaque transition. La page de synthèse joue ce rôle de checkpoint.

```java
@Workflow(name = "Déclaration de sinistre complète", code = "sinistre.declaration")
public void fullFlow(String numeroContrat) throws Exception {
    step1_renseignerContrat(numeroContrat);
    step2_valider();
    step3_confirmerEtAllerSurSynthese(); // <- obligatoire, dernier appel de tout fullXxxFlow()
}
```

---

## 4. Documentation (génération automatique)

Le catalogue des workflows pilotables (`code=...`) est **généré automatiquement**, jamais tenu à
la main. Une classe dédiée (type `WorkflowCatalogueGenerator`, package `tests/` — c'est un point
d'entrée TestNG même si elle n'assert rien) scanne par réflexion toutes les méthodes annotées
`@Workflow(code=...)` du projet, produit un fichier (Excel) listant code / classe / méthode /
libellé, et l'upload sur le serveur de variable.

**Pourquoi automatique et pas à la main**: si c'était une liste tenue manuellement, elle se
désynchronise dès qu'un dev ajoute/retire un `code=` sans penser à mettre à jour la doc — la
réflexion garantit que le catalogue reflète toujours exactement les workflows réellement
disponibles dans le code, jamais en retard.

**Conséquence pour vous**: ajouter un `code = "xxx.full"` sur un `fullXxxFlow()` suffit — il
apparaît automatiquement dans le catalogue au prochain lancement, aucune doc à modifier à la main.

---

## 5. Test

### Règle de nommage

Suffixe `Test` obligatoire, PascalCase, **anglais** (contrairement aux Pages/Workflows dont le
nom vient de l'appli métier, une classe Test est un objet purement technique — pas de titre
écran à retrouver, donc pas la même contrainte de nommage).

### Structure

```java
public class DeclarerSinistreTest extends SeleniumTestPlan {

    @Test(dataProvider = "dataset")
    public void testDeclarationSinistre(String numeroContrat) throws Exception {
        DeclarerSaisirSinistreWorkflow workflow = new DeclarerSaisirSinistreWorkflow();
        workflow.fullFlow(numeroContrat);
    }
}
```

Le paramètre `numeroContrat` vient du fichier
`data/<app>/dataset/<environnement>/testDeclarationSinistre.csv` (fourni par `dataProvider =
"dataset"`, déjà dans `SeleniumTestPlan`) — pas une valeur en dur dans le code.

Points clés:
- **Jamais de valeur métier en dur dans le Test** — toujours via `@DataProvider` (CSV/Excel,
  jeu de données par environnement) ou variable serveur (`JsonParams`/`MapParams`, comme dans
  les Workflows). Le Test ne connaît que la mécanique, pas les valeurs.
- Étend `SeleniumTestPlan` (fournit le cycle de vie du framework, dataProviders CSV/Excel...).
- Une méthode `@Test` appelle un ou plusieurs Workflows — **jamais une PageObject directement**.
- Assertions réelles (pass/fail) vivent dans les Pages (`Assert.assertXxx` dans les méthodes
  `@Step`) — le Test lui-même orchestre juste, il n'a généralement pas d'assertion visible.
- Déclarée dans un `<class>` d'un `testng.xml` pour être réellement exécutable.

---

## Rôle de chaque couche

| Couche | Sait... | Change quand... | Ne change PAS quand... |
|---|---|---|---|
| **Page** | Comment interagir avec **un seul écran** (locators, clics, saisies) | L'écran change (nouveau bouton, ID modifié) | Le parcours métier ou les scénarios changent |
| **Workflow** | **Quel enchaînement métier** exécuter (quelles étapes, dans quel ordre) | Le processus métier change (nouvelle étape, ordre différent) | L'écran change juste visuellement, ou les scénarios de test changent |
| **Test** | **Quel workflow lancer, avec quelles données**, et si c'est un succès | Les scénarios/jeux de données à tester changent | L'écran ou le processus métier changent |

### Pourquoi séparer en 3 (et pas tout mettre dans une seule classe)

**Isolation du changement** — un ID de bouton qui change dans l'appli ne touche que la Page.
Un nouvel ordre d'étapes métier ne touche que le Workflow. Un nouveau scénario à tester ne touche
que le Test. Sans cette séparation, un seul changement (souvent le plus fréquent : l'IHM) oblige
à re-toucher du code de test ET du code métier ET du code d'orchestration en même temps.

**Réutilisabilité** — un même Workflow peut être appelé depuis plusieurs Test (un test standard,
un test avec données limites, un scénario composé qui enchaîne plusieurs Workflows comme
`MasterWorkflow`) sans dupliquer la logique métier. Une même Page peut être utilisée par plusieurs
Workflows si deux parcours métier passent par le même écran.

**Lisibilité du rapport** — chaque couche produit un niveau de granularité différent dans le
rapport : la Page produit les actions techniques (clic, saisie), le Workflow les regroupe en
étapes métier lisibles (`@Workflow(name=...)`), le Test donne juste le verdict final. Un testeur
manuel lit le niveau Workflow sans avoir besoin de voir le détail Page, sauf en cas d'échec.
