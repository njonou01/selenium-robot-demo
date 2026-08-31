# Vision — état des lieux et pistes

Ce document répond à une question différente de celle du [`guide-scenariseur.md`](guide-scenariseur.md) :
pas "comment ça marche", mais "où on en est, qu'est-ce qui est solide, qu'est-ce qui manque".
Destiné à quiconque doit décider quoi construire ensuite, ou juger si le moteur est prêt pour
tel usage.

## Ce qui est fait

**Architecture en couches, tenue dans tout le code.** `tests` orchestre, `workflows` porte le
métier, `webpage` exécute l'UI, `custom` reste un support technique sans logique produit. Cette
règle n'est pas juste écrite dans `architecture.md` — une relecture classe par classe de tout
`custom/`, `workflows/`, `webpage/` et `tests/` n'a trouvé aucune violation.

**Scénarios pilotés par variable serveur**, sans toucher au code : JSON ou Excel, détecté à
l'extension, avec :

- isolation d'erreur par scénario (un scénario cassé n'empêche jamais les autres de tourner,
  côté JSON comme côté Excel) ;
- validation à sec avant ouverture d'un navigateur (dataSet incomplet, code inconnu, nom de
  scénario dupliqué, référence de chaînage mal ordonnée — tout détecté en quelques secondes) ;
- override de dataSet par workflow (`dataSet[code][clé]`) et mapping nom métier / nom Java
  (`@Workflow(params = {...})`) ;
- chaînage de résultats entre workflows (`${result:code.champ}`, interface `WorkflowResult`,
  records typés — pas de `Map` générique) ;
- alias de scénario optionnels, portée à un seul scénario, JSON et Excel.

**Catalogue auto-généré**, jamais tenu à la main : `WorkflowCatalogueGenerator` scanne les
classes `*Workflow`, produit 4 formats de sortie (`sheets`/`blocks`/`json`/`matrix`), et reste
lisible même en exécution packagée (repli `@Workflow(variables = {...})` quand la détection
automatique par lecture du source échoue en jar).

**Données de site** via `JsonParams`/`MapParams` : résolution par chemin pointé, recherche par
suffixe (`$.`), recherche approximative tolérante aux fautes de frappe (`getFuzzy`, en dépannage
seulement), extraction de sous-arbre (`getSubtree`).

## Les bonnes choses

**Aucune deuxième source de vérité tenue à la main.** Le catalogue en est la preuve vivante —
voir [`architecture.md`](architecture.md) pour le principe.

**Les erreurs métier disent quoi et où**, jamais une stack trace brute jetée au visage d'un
rédacteur de scénario non-dev : "le workflow 'hr.full' attend le parametre 'employee', absent
du dataSet" plutôt qu'une `NullPointerException` à la ligne 47.

**Le code documente le pourquoi, jamais le quoi.** Zéro commentaire qui reformule ce que le code
fait déjà ; chaque commentaire existant justifie un choix non évident (pourquoi `Lazy`, pourquoi
un classloader inversé, pourquoi la validation `${result:...}` se fait à deux endroits
différents).

## Ce qui manque

**Aucune détection de flakiness.** Le moteur ne fait aujourd'hui aucune différence entre "ce
site est instable" et "on a cassé quelque chose" — chaque échec est traité pareil, qu'il vienne
d'une vraie régression ou d'un aléa du site testé.

**Pas d'éditeur visuel.** Composer un scénario demande de connaître le format JSON/Excel à la
main — pas de formulaire généré depuis le catalogue qui garantirait les bons types sans
connaître la syntaxe.

**Le retry par workflow peut se cumuler avec le retry TestNG.** Un site réellement en panne peut
prendre plusieurs minutes avant que le test abandonne — connu, documenté, jamais traité.

## Pistes, pas commencées

Discutées mais pas construites, par ordre de coût croissant :

1. **Détection de flakiness** — historique d'exécution par scénario, distinction site-instable
   vs régression réelle.
2. **Score de santé par scénario** — combinaison stabilité historique + fraîcheur des données,
   affiché dans le rapport.
3. **Éditeur visuel de scénario** — le plus gros chantier, celui qui changerait le plus la vie
   d'un rédacteur non-dev, mais qui demande une vraie interface, pas juste du code serveur.
