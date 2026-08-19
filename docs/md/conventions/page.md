# Écrire une page SeleniumRobot

Cette page contient les conventions projet pour écrire une page `seleniumdemo`.

Les détails techniques du framework sont documentés à part dans :

- `../framework.md`

## 0. Choisir le nom de la page

Le nom de la page doit être déterministe.

Pourquoi on fixe ça :

- dans les logs, le framework affiche le nom simple de la classe de page ;
- dans IntelliJ, on doit pouvoir retrouver la page très vite avec la recherche globale ;
- en cas d’erreur, il faut identifier la page sans hésiter ;
- une IA doit pouvoir générer le même nom à partir du même écran.

### Source du nom

On ne choisit pas le nom au hasard.  
On prend la source la plus significative selon le type d’écran.

#### Cas 1 — écran optimisé

Sur un écran optimisé, on prend d’abord le texte central principal de la page.

Pourquoi :

- c’est souvent le vrai nom fonctionnel de l’écran ;
- c’est ce qui saute aux yeux de l’utilisateur ;
- c’est ce qui aide le plus au diagnostic dans un rapport.

#### Cas 2 — écran non optimisé

Sur un écran non optimisé, on prend dans cet ordre :

1. le code ou la référence affichée sur la page si elle identifie l’écran ;
2. le titre de la page s’il existe ;
3. le nom de l’onglet du navigateur ;
4. le titre principal visible en haut de page ;
5. le texte central si rien de mieux n’existe.

### Règles de normalisation

Une fois le texte source choisi, on le transforme en nom de page stable.

Règles :

- enlever les articles et mots faibles : `le`, `la`, `les`, `un`, `une`, `des`, `du`, `de`, `d'`, `l'`, `au`, `aux`, `à`, `en`, `pour`, `sur`, `avec` ;
- garder les mots métier utiles ;
- retirer les ponctuations inutiles ;
- compacter les espaces ;
- si le nom contient plus de 6 mots, garder seulement les 6 mots les plus utiles ;
- enlever les accents ;
- écrire le nom final en `PascalCase` ;
- ajouter le suffixe `Page`.

### Format final recommandé

```text
NomSourceNettoyee -> NomPageFinal
```

Exemples :

```text
Connexion utilisateur -> ConnexionUtilisateurPage
Créer un dossier sinistre -> CreerDossierSinistrePage
Suivi de paiement -> SuiviPaiementPage
```

### Pourquoi ce format

- il est lisible dans les logs ;
- il est facile à retrouver dans IntelliJ ;
- il donne une trace courte dans les rapports d’erreur ;
- il reste stable même si le texte affiché varie un peu.

### À éviter

- les noms trop longs ;
- les articles ;
- les mots de liaison inutiles ;
- les accents dans le nom final ;
- les variantes libres d’une page à l’autre.

## 1. Travailler avec les frames

Dans notre projet, toutes les pages vivent dans une frame.

La règle d’écriture est la suivante :

- la page métier va d’abord chercher la frame dans `MainPage` ;
- la page déclare ensuite son propre `private static final FrameElement FRAME = ...` ;
- tous les éléments de cette page utilisent ce `FRAME` ;
- on garde ainsi la page autonome, même si la frame source est centralisée.

Exemple :

```java
public class SomePage extends PageObject {

    private static final FrameElement FRAME = MainPage.mainFrame;

    private static final TextFieldElement USERNAME_FIELD =
        new TextFieldElement("username", ByC.label("Nom d'utilisateur"), FRAME);

    private static final ButtonElement SAVE_BUTTON =
        new ButtonElement("save", ByC.attribute("data-selenium-id", "save"), FRAME);
}
```

### Règle pratique

- si un écran est dans une frame, on la passe explicitement à tous les éléments de la page ;
- si une sous-frame est nécessaire, on la déclare localement dans la page concernée ;
- si la page n’utilise pas de frame, on le documente clairement au lieu de le laisser implicite.

## 2. Comment écrire une page

Une page doit représenter une seule zone fonctionnelle.

### Identifiant de page obligatoire

Chaque page doit exposer un `public HtmlElement IDENTIFIER`.

C’est la règle de base du projet.

Pourquoi :

- ça donne à la page un point d’identification unique ;
- ça permet de vérifier rapidement qu’on est sur la bonne page ;
- ça aide à savoir quel écran est actif ;
- ça rend les pages homogènes entre elles.

Exemple :

```java
public final HtmlElement IDENTIFIER = new HtmlElement("loginForm", By.cssSelector(".login-form"), FRAME);
```

Règles associées :

- l’identifiant doit être public ;
- il doit être simple et stable ;
- il doit pointer vers un élément qui prouve que la page est bien chargée ;
- il doit être le même concept sur toutes les pages du projet.

Le plus souvent, `IDENTIFIER` sert aussi d’élément de base au constructeur de la page.

### Ce que la page contient

- un élément identifiant la page ;
- les sélecteurs utiles ;
- les méthodes d’action ;
- éventuellement des vérifications locales simples.

### Ce que la page ne doit pas contenir

- la logique métier du scénario ;
- le pilotage global du test ;
- la lecture du serveur de variables ;
- des règles de décision qui appartiennent au workflow.

### Structure recommandée

```text
1. identifier la page
2. déclarer les sélecteurs statiques
3. écrire les actions UI
4. annoter les actions avec @Step
5. vérifier le résultat local si nécessaire
```

## 3. Méthodes publiques de page

Les pages héritent de `PageObject`, donc l’IDE peut proposer beaucoup de méthodes
héritées qui n’appartiennent pas au métier de la page.

Si on veut rendre la vraie API de page plus visible, on peut adopter cette règle :

- les méthodes publiques métier de la page commencent par `_` ;
- les helpers techniques restent `private` ou `protected` ;
- on ne met pas de `_` sur les getters techniques du framework ;
- on ne met pas de `_` sur les méthodes qui ne sont pas destinées à être appelées depuis le scénario.

Exemple :

```java
@Step(name = "Connexion utilisateur")
public LoginPage _login(String username, String password) {
    USERNAME_FIELD.sendKeys(username);
    PASSWORD_FIELD.sendKeys(password);
    SAVE_BUTTON.click();
    return this;
}
```

Pourquoi cette convention peut aider :

- elle sépare visuellement l’API métier des méthodes héritées ;
- elle réduit le bruit dans la lecture du code ;
- elle rend les méthodes de page plus faciles à repérer.

Mais il faut garder en tête un point important :

- le `_` fera aussi partie du nom affiché dans les logs et les rapports ;
- donc si on veut un rapport très “propre” pour l’utilisateur final, il faut vérifier que ce choix nous convient vraiment.

En pratique, on peut l’utiliser comme convention d’équipe, mais il faut rester cohérent sur tout le projet.

## 4. Point de reprise commun

Certaines pages servent de point d’arrivée ou de reprise pour plusieurs workflows.

Dans ce cas, la page doit fournir des méthodes claires pour :

- atteindre cet écran de synthèse ;
- vérifier que la synthèse est bien affichée ;
- repartir depuis un état stable si le workflow suivant doit continuer le parcours.

L’idée n’est pas de mettre la logique métier dans la page.
L’idée est de donner au workflow un point d’appui stable pour chaîner proprement plusieurs flux.

Exemples de responsabilités côté page :

- `_allerVersSyntheseDeDecla()` ;
- `_verifierSyntheseDeDecla()` ;
- `_confirmerQueLeDossierEstPretPourLaSuite()`.

## 5. Règles de nommage des éléments

Pour que la page reste lisible, on nomme les éléments avec le métier d’abord,
puis le type d’élément.

La forme recommandée est :

```text
NOM_METIER_TYPE
```

Exemples :

- `USERNAME_FIELD`
- `PASSWORD_FIELD`
- `LOGIN_BUTTON`
- `CART_LINK`
- `COUNTRY_SELECT`
- `ERROR_MESSAGE`
- `SUCCESS_TOAST`
- `LOGIN_FORM`

### Types à utiliser

- champ texte : `*_FIELD`
- bouton : `*_BUTTON`
- lien : `*_LINK`
- liste déroulante : `*_SELECT`
- case à cocher : `*_CHECKBOX`
- bouton radio : `*_RADIO`
- message / statut : `*_MESSAGE` ou `*_TEXT`
- conteneur / bloc : `*_FORM`, `*_CONTAINER`, `*_CARD`, `*_PANEL`
- menu / onglet : `*_MENU`, `*_TAB`

### Pourquoi ce format

- on lit d’abord le sens métier ;
- on voit ensuite le rôle technique de l’élément ;
- c’est homogène dans toute la page ;
- une IA peut l’appliquer facilement sans hésiter.

### À éviter

- `FIELD_USERNAME`
- `BUTTON_LOGIN`
- `LINK_CART`
- des noms trop vagues ;
- des noms qui ne disent pas le type de l’élément.

### Le label de l’élément dans les rapports

Le premier argument passé à un élément ne sert pas seulement à l’initialiser.
Il sert aussi à écrire des traces propres dans les rapports.

Donc le label doit être pensé pour la lecture du rapport.

Règle recommandée :

- court ;
- stable ;
- compréhensible sans ouvrir le code ;
- aligné avec le métier ;
- différent du sélecteur ;
- différent du texte technique du DOM quand ce texte change souvent.

On veut un nom qui aide à lire le rapport, pas un nom qui répète le HTML.

Exemples de bons labels :

- `Nom d'utilisateur`
- `Mot de passe`
- `Connexion`
- `Référence dossier`
- `Montant total`

Exemples à éviter :

- le nom brut du CSS ;
- un texte trop long ;
- une phrase complète ;
- une valeur qui change selon la page ou la langue sans raison métier ;
- un nom trop générique comme `field1` ou `input`.

Pour résumer :

- le nom Java de la variable sert au code ;
- le label sert au rapport ;
- les deux doivent raconter la même intention métier.

## 6. Règles pour `@Step`

`@Step` sert à décrire l’action de manière lisible dans les rapports.

Il faut préférer :

```java
@Step(name = "Ajouter un employé: ${firstName} ${lastName}")
```

Plutôt que :

```java
@Step(name = "Faire le truc")
```

### Bon usage

- un nom de step court et explicite ;
- des paramètres utiles au log ;
- une action métier lisible ;
- si besoin, ajouter `description` et `expectedResult`.

### Ce qu’on évite

- les noms vagues ;
- les phrases trop longues ;
- les commentaires non neutres ;
- les steps qui décrivent trop de choses à la fois.

## 7. Ce qu’une IA doit retenir

Si une IA doit écrire une page SeleniumRobot, elle doit garder cette chaîne en tête :

```text
page = 1 zone fonctionnelle
frame = explicitement passée
sélecteurs = stables en priorité
méthodes = actions UI courtes
workflow = logique métier au-dessus
test = orchestration au-dessus encore
```

### Règle finale pour une IA

Si tu lui donnes une page HTML, elle doit écrire la page dans cet ordre :

```text
1. identifier le bloc principal de la page
2. choisir les sélecteurs les plus stables
3. identifier la frame dans laquelle la page travaille
4. déclarer la frame dans la page
5. déclarer les éléments en haut de la classe en utilisant cette frame
6. écrire des méthodes courtes et nommées par intention
7. mettre @Step sur les actions importantes
8. ne pas mettre de logique métier dans la page
```
