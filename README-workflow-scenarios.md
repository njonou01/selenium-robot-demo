# Scénarios pilotés par le serveur de variable

Ce document explique comment composer et lancer des scénarios de test en enchaînant des
workflows (banking, ecommerce, hr, petstore, electronics), **sans toucher au code ni au fichier
testng.xml**. Tout se pilote depuis le serveur de variable (seleniumRobot-server).

Public: testeurs manuels (partie "Composer un scénario" et "Modifier les données métier") et
développeurs (partie "Fonctionnement interne").

---

## 1. Composer un scénario

Un scénario = un nom + une liste ordonnée de workflows à enchaîner.

Ils sont définis dans la variable serveur **`workflow.scenarios`** (application `seleniumdemo`,
version `1.0`, environnement `DEV`), au format JSON:

```json
[
  {
    "name": "Scenario Ecommerce Electronics",
    "chain": "ecommerce.full,electronics.full"
  },
  {
    "name": "Scenario Bancaire RH",
    "chain": "banking.full,hr.full"
  }
]
```

- **`name`** — nom du scénario. Devient le nom du test tel qu'affiché dans le rapport TestNG.
  Mettre des espaces, un nom lisible (pas de camelCase collé).
- **`chain`** — liste de codes de workflow séparés par des virgules, joués **dans l'ordre**.

### Codes de workflow disponibles

| Code | Workflow | Site |
|---|---|---|
| `banking.full` | Workflow bancaire complet | Parabank |
| `ecommerce.full` | Processus e-commerce complet | SauceDemo |
| `hr.full` | Workflow RH complet | OrangeHRM |
| `petstore.full` | Workflow animalerie complet | PetStore |
| `electronics.full` | Workflow électronique | Demoblaze |

Ces codes sont **découverts automatiquement** (réflexion sur l'annotation `@Workflow(code=...)`
des méthodes `fullXxxFlow`) — cette liste n'est jamais à maintenir à la main. Si un code inconnu
est utilisé dans `chain`, le test échoue avec un message clair listant les codes valides.

### Pour ajouter/modifier un scénario

Éditer la variable `workflow.scenarios` dans l'admin du serveur (`/admin/`, section
`VARIABLESERVER` → `Variable`) — soit directement le champ `value` (texte JSON), soit en
uploadant un fichier JSON dans `uploadFile` (les deux formats sont supportés).

Aucun redéploiement, aucune recompilation nécessaire — le prochain lancement de test relit la
variable.

### Comment ça s'affiche dans le rapport

Chaque scénario devient une **ligne de test TestNG distincte**, nommée d'après `name`
(ex: "Scenario Ecommerce Electronics"). Chaque workflow de la chaîne apparaît comme une étape
imbriquée dans le détail du test.

---

## 2. Modifier les données métier (montants, noms, etc.)

Chaque site a ses propres données métier (montant de virement, nom du produit, informations
employé...) dans **sa propre variable serveur**, au format JSON:

| Variable serveur | Site |
|---|---|
| `banking.params` | Parabank |
| `ecommerce.params` | SauceDemo |
| `hr.params` | OrangeHRM |
| `petstore.params` | PetStore |
| `electronics.params` | Demoblaze |

Exemple, `banking.params`:

```json
{
  "transfer": { "amount": "100" },
  "payBill": { "payeeName": "EDF Electricite", "accountNumber": "987654", "amount": "75" },
  "findTransaction": { "amount": "75" },
  "updateContact": { "phone": "310-447-9999" },
  "requestLoan": { "loanAmount": "5000", "downPayment": "1000" }
}
```

Modifier une valeur ici change directement le comportement du workflow au prochain lancement —
aucun code à toucher.

Les identifiants de connexion (login/mot de passe) sont dans des variables séparées:
`parabank.username`/`parabank.password`, `sauceDemo.username`/`sauceDemo.password`,
`orangeHRM.username`/`orangeHRM.password`, `petStore.username`/`petStore.password`.

---

## 3. Fonctionnement interne (pour développeurs)

### Architecture

- **`VariableDrivenScenarioTest.java`** — un `@DataProvider` lit `workflow.scenarios` (appel HTTP
  direct au serveur — voir "Pourquoi un appel HTTP fait main" ci-dessous), génère une exécution de
  test TestNG par scénario. Chaque exécution découvre par réflexion les méthodes
  `@Workflow(code=...)` disponibles, résout la `chain` demandée, et invoque les méthodes dans
  l'ordre.
- **`ConfigUtils.java`** — helper minimal pour lire une variable serveur simple
  (`ConfigUtils.get("cle")`), utilisable uniquement à l'intérieur d'une méthode `@Test` (le
  contexte seleniumRobot doit être prêt).
- **`JsonParam.java`** — charge une variable serveur JSON imbriquée et permet de retrouver une
  valeur par chemin. `JsonParam.load("banking")` charge `banking.params`. Trois niveaux de
  recherche pour `.get(chemin)`:
  - chemin complet: `"transfer.amount"`
  - chemin court (préfixe `$.`): `"$.amount"` → premier chemin qui **se termine par** `amount`
  - recherche floue (juste un nom): `"amount"` → dernier segment du chemin qui correspond
    (exact insensible à la casse, sinon "contient")

### Pourquoi un appel HTTP fait main dans le DataProvider

Le contexte de test standard de seleniumRobot (`ConfigUtils`/`TestTasks.paramFile`) n'est pas
encore initialisé quand un `@DataProvider` TestNG s'exécute: TestNG résout les données AVANT de
créer le `ITestResult` associé à chaque exécution, et c'est ce `ITestResult` qui déclenche la
connexion au serveur de variable (`SeleniumTestsContext.configureContext()`).

`fetchScenariosJson()` contourne ça avec un appel HTTP direct
(`GET /variable/api/variable/?application=seleniumdemo&version=1.0&environment=DEV&name=workflow.scenarios`,
header `Authorization: Token <token>`), en lisant l'URL/le token depuis les paramètres XML
(disponibles immédiatement via `ITestContext`, sans dépendre du cycle de vie du test). Supporte
variable texte inline (`value`) ou fichier uploadé (`uploadFile`, téléchargé via un second appel).

À l'intérieur des méthodes `@Test`/`fullXxxFlow`, le contexte est prêt: `ConfigUtils`/`JsonParam`
utilisent le mécanisme standard, pas de bidouille HTTP.

### Ajouter un nouveau workflow "complet" pilotable

1. Dans la classe `Workflow` concernée, ajouter `code = "monsite.full"` sur l'annotation
   `@Workflow` de la méthode `fullXxxFlow`.
2. Si besoin de données métier configurables, créer `monsite.params` sur le serveur et lire via
   `JsonParam.load("monsite")`.
3. Rien d'autre à modifier — la classe est déjà découverte automatiquement (scan des classes
   `*Workflow.class` dans le package `workflows`), le code est déjà découvert par réflexion.

### Limitations connues

- Seuls les workflows **complets** (`fullXxxFlow`) sont pilotables, pas les étapes individuelles
  (décision volontaire: évite les scénarios cassés par un mauvais ordre d'étapes).
- `application`/`version`/`environment` sont codés en dur (`seleniumdemo`/`1.0`/`DEV`) dans
  `VariableDrivenScenarioTest`.
- Le retry par workflow (2 tentatives, driver recréé) peut se cumuler avec le retry TestNG
  existant au niveau test — un vrai échec de site peut prendre plusieurs minutes avant d'abandonner.
