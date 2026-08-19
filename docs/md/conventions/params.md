# Lire des paramètres avec `Params`

Cette page documente le mécanisme technique derrière `MapParams` et `JsonParams`, utilisés
dans les workflows (voir [`workflow.md`](workflow.md#entrées-via-params-et-json-params) pour
les exemples d'usage côté métier). Ici, on décrit l'API et les pièges d'implémentation.

## Hiérarchie

```text
Params (interface)
└── AbstractParams (logique commune)
    ├── JsonParams  → source: fichier JSON attaché à une variable serveur
    └── MapParams   → source: texte plat "cle=valeur;cle=valeur" sur une variable serveur
```

Package: `com.example.seleniumdemo.custom.testdata`.

## Interface `Params`

| Méthode | Rôle |
|---|---|
| `get(String path)` | Lit une valeur. Lève une exception si absente ou si `path` désigne un sous-arbre. |
| `get(String path, String defaultValue)` | Comme `get(path)`, mais renvoie `defaultValue` au lieu de lever une exception. |
| `getSubtree(String path)` | Renvoie toutes les valeurs sous `path.` sous forme de `Map<String, String>` (clés relatives, préfixe retiré). |

Une méthode de workflow qui doit rester agnostique sur la source (`MapParams` ou `JsonParams`)
prend `Params` en paramètre plutôt que le type concret — voir l'exemple `validerSynthese(...)`
dans `workflow.md`.

## `AbstractParams` — logique commune

Toutes les valeurs sont aplaties dans une seule `Map<String, String>` (`flatValues`), avec des
clés en chemin pointé (`assure.nom`, `dossier.id`, ...). `JsonParams` aplatit un JSON imbriqué ;
`MapParams` est déjà plat par construction.

### Résolution de `get(path)`

1. correspondance exacte de `path` dans `flatValues` ;
2. si `path` commence par `$.` : recherche par **suffixe** — `$.nom` correspond à
   `assure.nom`, `beneficiaire.nom`, etc. (le premier trouvé gagne, dans l'ordre d'insertion) ;
3. sinon, si `path` est un préfixe de sous-arbre (ex: `assure` alors que seules `assure.nom`,
   `assure.prenom` existent), lève une exception qui redirige vers `getSubtree(path)` — c'est
   volontaire, `get()` ne doit jamais renvoyer une valeur ambiguë ;
4. sinon, exception `IllegalStateException` listant tous les chemins disponibles.

### `getFuzzy(path)` — recherche approximative

Existe sur `AbstractParams` (donc disponible sur `MapParams` et `JsonParams`, pas sur
l'interface `Params`). À utiliser en dépannage seulement, pas dans du code de workflow stable :
la doc `workflow.md` déconseille explicitement ce raccourci pour un `get()` normal.

Ordre de recherche :

1. mêmes règles que `get(path)` (exact, puis `$.suffixe`) ;
2. correspondance insensible à la casse sur le dernier segment (`Nom` → `assure.nom`) ;
3. correspondance par sous-chaîne (`nomAssure` trouve `assure.nomAssure`) ;
4. similarité Jaro-Winkler sur le dernier segment, seuil `0.85` — tolère une faute de frappe
   (`asure.nom` peut retrouver `assure.nom`).

Si rien ne dépasse le seuil, exception explicite ("introuvable même en recherche floue").

### `getSubtree(path)`

Filtre `flatValues` sur le préfixe `path + "."`, renvoie une map avec les clés relatives
(préfixe retiré). Lève une exception si le sous-arbre est vide — pas de map vide silencieuse.

## `JsonParams`

- source: **fichier** JSON attaché à la variable serveur ;
- `load(variableName)` appelle `PageObject.paramFile(variableName)`, lit le fichier, parse le
  JSON et aplatit toutes les feuilles (`node.isValueNode()`) en chemins pointés ;
- résultat mis en cache statique (`Map<String, JsonParams>`) par `variableName` — un seul appel
  serveur par nom de variable, même si `load()` est appelé plusieurs fois dans le run.

Exemple de valeur serveur pour `assurance.sinistre` (fichier JSON attaché) :

```json
{"dossier": {"id": "SIN-2026-001"}, "incident": {"date": "2026-08-01", "lieu": "Lyon"}}
```

## `MapParams`

- source: **texte simple** sur la variable serveur, format `cle=valeur;cle=valeur;...`
  (`:` accepté aussi comme séparateur clé/valeur : `cle:valeur`) ;
- `load(variableName)` appelle `PageObject.param(variableName)` (valeur texte, pas fichier) ;
- une paire mal formée (pas de `=` ni `:`, ou clé contenant `=`/`:`) lève une
  `IllegalArgumentException` immédiate à la construction — pas d'échec silencieux ;
- valeur vide acceptée (`cle=` → chaîne vide) ;
- clé dupliquée : la dernière valeur écrase silencieusement la précédente (comportement `Map`
  standard, assumé) ;
- même cache statique par `variableName` que `JsonParams`.

Exemple de valeur serveur pour `assurance.souscription` (texte, pas de fichier) :

```text
assure.nom=Dupont;assure.prenom=Julie;assure.adresse=12 rue des Lilas
```

## Piège à connaître : type de la variable serveur

`JsonParams` et `MapParams` ne lisent **pas** la variable serveur de la même façon :

| | `JsonParams.load()` | `MapParams.load()` |
|---|---|---|
| Appel `PageObject` | `paramFile(name)` | `param(name)` |
| Attend côté serveur | un **fichier** attaché à la variable | une **valeur texte** simple |

Si la variable serveur est configurée dans le mauvais sens (ex: un fichier reste attaché alors
que le workflow utilise `MapParams`), l'erreur côté serveur est
`ConfigurationException: value is not of type String` — pas une erreur côté `MapParams`
lui-même. En cas de migration `JsonParams` → `MapParams` sur une variable existante, il faut
donc aussi vider le fichier attaché côté serveur, pas seulement changer sa valeur texte.

## Quand choisir quoi

- `MapParams` : quelques valeurs plates, pas de hiérarchie réelle, on veut éditer la variable
  serveur en une ligne de texte ;
- `JsonParams` : données structurées sur plusieurs niveaux, ou déjà au format JSON en amont.

Les deux exposent la même interface `Params` ensuite — le choix ne change rien au code du
workflow au-delà de la ligne `load(...)`.
