# Écrire une page SeleniumRobot

Conventions projet pour écrire une page `seleniumdemo`. Les détails techniques du framework
(éléments, sélecteurs, assertions) sont documentés à part dans [`../framework.md`](../framework.md).

## Nommer la page

L'algorithme de nommage déterministe (comment on tire un nom de classe stable à partir d'un
écran OPTIM ou RSI, la gestion des collisions, pourquoi on abrège plutôt que de sélectionner les
"mots importants") est documenté dans `REGLES_EQUIPE.md` à la racine du projet — pas ici, pour
éviter d'avoir deux versions de la même règle qui finissent par diverger.

## Travailler avec les frames

Toutes les pages du projet vivent dans une frame. La page va chercher la frame dans `MainPage`,
puis déclare son propre `FRAME` local, et tous ses éléments l'utilisent — la page reste autonome
même si la frame source est centralisée ailleurs :

```java
public class SomePage extends PageObject {

    private static final FrameElement FRAME = MainPage.mainFrame;

    private static final TextFieldElement USERNAME_FIELD =
        new TextFieldElement("username", ByC.label("Nom d'utilisateur"), FRAME);

    private static final ButtonElement SAVE_BUTTON =
        new ButtonElement("save", ByC.attribute("data-selenium-id", "save"), FRAME);
}
```

Si une sous-frame existe, elle se déclare localement dans la page concernée. Si une page n'utilise
pas de frame, autant le dire explicitement dans le code plutôt que de laisser deviner.

## Écrire une page

Une page représente une seule zone fonctionnelle et expose un `public HtmlElement IDENTIFIER` —
c'est la règle de base du projet, ce qui permet de vérifier rapidement qu'on est sur le bon écran
et de garder les pages homogènes entre elles :

```java
public final HtmlElement IDENTIFIER = new HtmlElement("loginForm", By.cssSelector(".login-form"), FRAME);
```

L'identifiant doit être public, simple, stable, et pointer vers un élément qui prouve vraiment que
la page est chargée — c'est souvent aussi lui qui sert de base au constructeur.

Une page contient l'élément identifiant, les sélecteurs, les méthodes d'action, et
éventuellement des vérifications locales simples. Elle ne contient pas la logique métier du
scénario, ne pilote pas le test, et ne lit pas le serveur de variables — ces décisions
appartiennent au workflow.

## Distinguer l'API métier des méthodes héritées

`PageObject` amène pas mal de méthodes héritées dans l'autocomplétion, qui n'ont rien à voir avec
le métier de la page. Une convention possible pour clarifier : préfixer les méthodes publiques
métier par `_`, et laisser les helpers techniques en `private`/`protected` :

```java
@Step(name = "Connexion utilisateur")
public LoginPage _login(String username, String password) {
    USERNAME_FIELD.sendKeys(username);
    PASSWORD_FIELD.sendKeys(password);
    SAVE_BUTTON.click();
    return this;
}
```

Ça sépare visuellement l'API métier du bruit hérité — mais attention, le `_` finit aussi dans le
nom affiché en rapport. Si on veut un rapport "propre" pour l'utilisateur final, vérifier que ce
choix convient avant de le généraliser à tout le projet, et rester cohérent une fois décidé.

## Point de reprise commun

Certaines pages servent de point d'arrivée pour plusieurs workflows (un écran de synthèse, par
exemple). Dans ce cas, la page expose des méthodes claires pour y arriver, vérifier qu'elle est
bien affichée, et repartir d'un état stable — sans pour autant porter de logique métier elle-même,
juste donner au workflow un point d'appui fiable pour chaîner proprement :

```java
_allerVersSyntheseDeDecla()
_verifierSyntheseDeDecla()
_confirmerQueLeDossierEstPretPourLaSuite()
```

## Nommer les éléments

Métier d'abord, type ensuite : `NOM_METIER_TYPE` (`USERNAME_FIELD`, `LOGIN_BUTTON`, `CART_LINK`,
`ERROR_MESSAGE`...) plutôt que l'inverse (`FIELD_USERNAME`, `BUTTON_LOGIN`) — on lit le sens avant
le rôle technique, et c'est homogène dans toute la page.

Suffixes courants : `*_FIELD`, `*_BUTTON`, `*_LINK`, `*_SELECT`, `*_CHECKBOX`, `*_RADIO`,
`*_MESSAGE`/`*_TEXT`, `*_FORM`/`*_CONTAINER`/`*_CARD`/`*_PANEL`, `*_MENU`/`*_TAB`.

### Le label

Le premier argument passé à un élément ne sert pas qu'à l'initialiser — c'est aussi ce qui
apparaît dans les traces du rapport. Un bon label est court, stable, compréhensible sans ouvrir
le code, et différent du sélecteur ou du texte brut du DOM (surtout si ce texte change souvent) :
`Nom d'utilisateur`, `Référence dossier`, `Montant total` plutôt que le CSS brut, une phrase
complète, ou `field1`. Le nom Java sert au code, le label sert au rapport — les deux doivent
raconter la même intention métier.

## `@Step`

Un nom de step court et explicite, avec les paramètres utiles au log :

```java
@Step(name = "Ajouter un employé: ${firstName} ${lastName}")
```

plutôt que `@Step(name = "Faire le truc")`. `description` et `expectedResult` peuvent compléter
si besoin. Un step qui décrit trop de choses à la fois devient illisible dans le rapport — autant
le découper.
