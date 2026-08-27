# Scénarios pilotés par le serveur de variable

Comment composer et lancer des scénarios de test en enchaînant des workflows (banking,
ecommerce, hr, petstore, electronics) sans toucher au code ni au testng.xml. Tout se pilote
depuis le serveur de variable (seleniumRobot-server).

Les parties 1 à 3 s'adressent aux rédacteurs de scénarios / testeurs manuels. La partie 4 est
pour les devs.

---

## 1. Composer un scénario

Un scénario, c'est un nom, une liste ordonnée de workflows à enchaîner, et les données dont ces
workflows ont besoin. Tout ça vit dans la variable serveur `workflow.scenarios` (l'application et
la version sont résolues automatiquement depuis le contexte du test, l'environnement par défaut
est `DEV`), au format JSON :

```json
[
  {
    "name": "Scenario Complet SBC",
    "sinistre": "09E5YUHV",
    "sbc": true,
    "steps": [
      "ecommerce.full",
      "electronics.full",
      "banking.full",
      "hr.full"
    ],
    "dataSet": {
      "employee": {
        "firstName": "Robert",
        "lastName": "Wilson"
      }
    }
  }
]
```

`name` devient le nom du test dans le rapport, donc autant y mettre quelque chose de lisible
plutôt qu'un camelCase collé. `sinistre` est la référence de dossier utilisée par l'étape de
déclaration (`FormWorkflow`) et doit être renseignée. `sbc` à `true` ajoute un workflow
supplémentaire (`SbcWorkflow`) en tout début de scénario. `steps` liste les codes de workflow,
joués dans l'ordre écrit. `dataSet` porte les données nécessaires aux workflows de la liste — il
peut être vide ou absent si aucun d'entre eux n'attend de paramètre.

Le fichier peut aussi être en Excel : on uploade le `.xlsx` à la place du JSON dans la même
variable, le format est détecté tout seul à l'extension du fichier. Voir la section 3 plus bas
pour le détail du format Excel — les colonnes, comment mettre une fiche complète dans une
cellule, tout ça mérite mieux qu'une ligne.

Un détail qui a son importance : si un scénario du fichier est mal formé, seul celui-là échoue à
l'exécution, avec un message qui dit lequel et pourquoi. Les autres continuent de tourner
normalement — ce n'était pas le cas avant, une seule faute de frappe suffisait à tout bloquer.

Autre chose utile : tout est vérifié avant même d'ouvrir un navigateur. Workflows connus, données
présentes, bon type — une erreur de configuration remonte en quelques secondes plutôt qu'après
plusieurs minutes d'exécution Selenium.

### Les codes de workflow disponibles

On ne les liste pas ici — c'est justement ce qui a fait pourrir l'ancienne version de ce document.
La liste à jour, avec la description et les paramètres attendus de chaque workflow, est générée
automatiquement (test `Documentation` / `WorkflowCatalogueGenerator`) et uploadée sur le serveur
de variable à chaque exécution de la suite. Un code inconnu dans `steps` fait échouer le scénario
concerné avec la liste des codes valides dans le message.

### Modifier un scénario existant ou en ajouter un

Ça se fait dans l'admin du serveur (`/admin/`, section VARIABLESERVER → Variable) : soit
directement dans le champ `value` en texte JSON, soit en uploadant un fichier JSON ou Excel. Pas
de redéploiement ni de recompilation à prévoir, le prochain lancement relit la variable.

### Plusieurs jeux de scénarios en même temps

Le nom de la variable qu'on lit n'est pas figé sur `workflow.scenarios` : `-Dscenarios=nom.de.la.
variable` au lancement redirige vers une autre. Pratique si plusieurs équipes veulent leur propre
jeu de scénarios sur le même serveur sans dupliquer quoi que ce soit côté code.

---

## 2. Renseigner les données d'un scénario (dataSet)

Quand un workflow attend un paramètre — `hr.full` a besoin d'un employé, par exemple — il va le
chercher dans le `dataSet` du scénario, sous la clé qui porte le nom de ce paramètre.

Les valeurs peuvent être du texte simple (`"firstName": "Robert"`), un nombre ou un booléen, une
date au format AAAA-MM-JJ, un choix parmi une liste de valeurs fixes (à vérifier dans le
catalogue), une fiche complète avec plusieurs champs liés comme l'employé de `hr.full` :

```json
"employee": { "firstName": "Robert", "lastName": "Wilson" }
```

ou encore une liste de valeurs ou de fiches (`"produits": ["A", "B", "C"]`).

Si deux workflows attendent un paramètre du même nom mais avec des valeurs différentes, on peut
en préciser une spécifique à un workflow en la mettant sous son code plutôt qu'à la racine :

```json
"dataSet": {
  "hr.full": { "employee": { "firstName": "Robert", "lastName": "Wilson" } },
  "employee": { "firstName": "Jean", "lastName": "Dupont" }
}
```

`hr.full` prendra Robert Wilson, n'importe quel autre workflow demandant `employee` prendra Jean
Dupont. La valeur spécifique gagne toujours face à la générale.

### Données propres à un site

Certains sites ont leurs propres données (montant de virement, nom du produit...) qui ne
dépendent pas du scénario en cours, dans leur propre variable serveur :

| Variable serveur | Site | Format |
|---|---|---|
| `banking.params` | Parabank | JSON imbriqué |
| `ecommerce.params` | SauceDemo | JSON imbriqué |
| `electronics.params` | Demoblaze | JSON imbriqué |
| `petstore.params` | PetStore | texte plat `cle=valeur;cle=valeur` |

`banking.params`, par exemple :

```json
{
  "transfer": { "amount": "100" },
  "payBill": { "payeeName": "EDF Electricite", "accountNumber": "987654", "amount": "75" }
}
```

`petstore.params`, en texte plat cette fois :

```
browse.category=Fish;browse.productId=Angelfish;browse.itemId=EST-1
```

Changer une valeur ici prend effet au prochain lancement, sans toucher au code. Les identifiants
de connexion sont dans des variables à part : `parabank.username`/`parabank.password`,
`sauceDemo.username`/`sauceDemo.password`, `orangeHRM.username`/`orangeHRM.password`,
`petStore.username`/`petStore.password`.

---

## 3. Le format Excel

Mêmes idées que le JSON (name, sinistre, sbc, steps, dataSet), mais en colonnes. Un scénario
occupe un bloc de lignes consécutives :

| scenario_name | sinistre | step | sbc | dataSet |
|---|---|---|---|---|
| Scenario Sinistre Auto | AUTO-2026-04871 | auto.declaration | false | `employee={"firstName":"Robert","lastName":"Wilson"}` |
| | | auto.chiffrage | | |
| | | auto.recours | | |

La première ligne du bloc porte `scenario_name` (ce qui démarre le scénario), `sinistre`, `sbc`,
et le `dataSet` complet du scénario. Les lignes suivantes du même bloc n'ont que `step` rempli —
chacune ajoute une étape de plus à `steps`, dans l'ordre des lignes. Point important : le
`dataSet` **ne se lit qu'une fois**, sur la première ligne du bloc. Une valeur mise dans la
colonne `dataSet` d'une ligne suivante est ignorée — tout doit tenir dans cette première cellule.

La cellule `dataSet` est une liste `cle=valeur,cle=valeur`. Une valeur peut être du texte simple
(`code=R00`) ou un fragment JSON si la donnée est structurée — une fiche complète, une liste, ou
une valeur propre à un workflow précis (même mécanisme d'override qu'en JSON,
`dataSet["code.du.workflow"]`) :

```text
employee={"firstName":"Robert","lastName":"Wilson"},montantDevis=1450.00,auto.recours={"code":"R00"}
```

Ici, `montantDevis` est disponible à tous les workflows du bloc qui le demandent, alors que
`{"code":"R00"}` n'est pris en compte que par le workflow `auto.recours` précisément — la virgule
sépare les paires au niveau racine de la cellule, elle est ignorée à l'intérieur d'un fragment
`{...}`/`[...]`, donc pas de souci si une valeur JSON contient elle-même des virgules.

Un scénario ne peut pas apparaître dans deux blocs séparés (deux plages de lignes non
consécutives avec le même `scenario_name`) — le fichier échoue au chargement dans ce cas,
message à l'appui. Une cellule `dataSet` invalide (JSON mal formé dans un fragment, paire sans
`=`) n'affecte que ce scénario, pas les autres lignes du fichier.

---

## 4. Dans le rapport

Chaque scénario devient une ligne de test TestNG à part, nommée d'après son `name` ("Scenario
Complet SBC", par exemple), et chaque workflow de la chaîne apparaît comme une étape imbriquée
dans le détail du test.

---

## 5. Pour les devs

`ServerDrivenScenarioTest` récupère `workflow.scenarios` via un `@DataProvider` (JSON ou Excel,
détecté à l'extension), valide chaque scénario à sec, puis lance une exécution TestNG par
scénario. Un scénario dont le parsing a échoué (son `ScenarioDef` porte alors un `parseError`
non nul) échoue tout seul à l'exécution sans emporter les autres.

`JsonScenarioSource` et `ExcelScenarioSource` transforment le contenu brut en
`List<ScenarioDef>` en isolant les erreurs scénario par scénario plutôt que de faire tomber tout
le fichier d'un coup.

`WorkflowVariableScanner` fait le plus gros du travail : il résout par réflexion (sur le
bytecode, pas en lisant le source — ça marche donc aussi bien depuis un jar packagé) les noms de
paramètres d'une méthode `@Workflow(code=...)`, applique le mapping nom métier/nom Java déclaré
via `@Workflow(params = {"nomMetier=cheminJava"})`, et convertit chaque valeur brute du `dataSet`
vers le type attendu — String, enum, nombre, date, tableau, `List<T>`, ou record imbriqué
récursivement.

`JsonParams` et `MapParams` sont les deux formats pour charger les données propres à un site :
`JsonParams` pour du JSON imbriqué (`.get("chemin.complet")`), `MapParams` pour du texte plat
`cle=valeur;cle=valeur`. Les deux partagent la même interface, `AbstractParams`.

`WorkflowCatalogueGenerator` scanne toutes les classes `*Workflow` du package `workflows` et
génère le catalogue (code, description, classe, paramètres attendus) qu'il uploade sur le
serveur à chaque run — personne n'a besoin de le tenir à jour à la main, et c'est tant mieux vu
que l'ancienne doc montre ce que ça donne quand on essaie.

`PageDetector` sert à distinguer plusieurs pages possibles après une action (succès ou erreur,
par exemple). Il attend qu'une seule candidate reste visible sur plusieurs contrôles d'affilée
avant de trancher, pour ne pas se faire piéger par l'instant où deux pages semblent visibles en
même temps pendant une transition.

### Ajouter un nouveau workflow pilotable

Il suffit d'ajouter `code = "monsite.full"` sur l'annotation `@Workflow` de la méthode
`fullXxxFlow` concernée. Si elle prend des paramètres, ils sont résolus automatiquement depuis le
`dataSet` (nom du paramètre Java par défaut, ou nom métier via `params = {"nomMetier=nomJava"}`).
Si le site a besoin de données propres indépendantes du scénario, on crée une variable
`monsite.params` et on la lit via `JsonParams.load(...)` ou `MapParams.load(...)`. Rien d'autre à
changer : la classe est déjà repérée par scan du package `workflows`, et le catalogue se met à
jour tout seul au run suivant.

### Ce qui ne marche pas encore / limites connues

Seuls les workflows complets (`fullXxxFlow`) sont pilotables depuis un scénario, pas les étapes
individuelles — c'est voulu, ça évite les scénarios cassés par un mauvais ordre d'étapes. Le
support Excel pour les types complexes (record ou tableau en cellule) est écrit mais n'a été
vérifié qu'en JSON, jamais par un run réel en Excel. Et le retry par workflow peut se cumuler
avec le retry TestNG au niveau du test — un vrai site en panne peut prendre plusieurs minutes
avant qu'on abandonne.
