# Framework SeleniumRobot

Conventions techniques pour écrire les pages du projet.

## `PageObject`

Représente une page ou un écran. Sert à vérifier qu'on est sur la bonne page, porter l'élément
identifiant, et proposer quelques assertions et actions de navigation de base.

- `present` = existe, `displayed` = visible, `enabled` = utilisable, `selected` = choisi/coché.

| Méthode | Rôle |
|---|---|
| `assertCurrentPage(...)` | Vérifie que la page attendue est bien active. |
| `close()` / `close(Class<T>)` | Ferme la page ou revient à la précédente. |
| `wait(Integer)` | Pause contrôlée. |
| `assertForVisible(...)` / `assertForInvisible(...)` | Présence + visibilité d'un élément. |
| `assertForEnabled(...)` / `assertForDisabled(...)` | État interactif. |
| `assertForValue(...)` | Texte ou valeur d'un élément. |
| `assertSelectedOption(...)` | Sélection d'une liste. |
| `assertChecked(...)` / `assertNotChecked(...)` | Case ou radio. |
| `assertTableCellValue(...)` | Cellule de tableau. |
| `assertTextPresentInPage(...)` / `assertTextNotPresentInPage(...)` | Texte global dans la page. |
| `assertCookiePresent(...)` | Présence d'un cookie. |
| `assertElementCount(...)` | Nombre d'éléments. |
| `assertPageTitleMatches(...)` | Titre de page. |
| `assertHtmlSource(...)` | Texte dans le HTML source. |
| `assertLocation(...)` | URL courante. |

`PageObject` peut vérifier qu'on est sur la bonne page ; les vraies assertions métier vont dans
les tests ou les workflows, pas ici.

## Éléments HTML

`HtmlElement` est la base générique — on l'utilise seulement si aucun type plus précis n'existe.
Sinon : `FrameElement` (contexte de frame/iframe), `ButtonElement`, `TextFieldElement`,
`LinkElement`, `CheckBoxElement`, `RadioButtonElement`, `SelectList`, `LabelElement`,
`FileUploadElement`, `DatePickerElement`, `Table`, `ImageElement`. Pour du visuel pur (le DOM ne
suffit pas) : `PictureElement`, `ScreenZone`, `GenericPictureElement`. `CachedHtmlElement` pour un
élément capturé en cache, `SeleniumElement` en wrapper technique d'un `WebElement`/`Select`.

| Classe | Méthodes utiles |
|---|---|
| `TextFieldElement` | `clear()`, `type()`, `clearAndType()`, `sendKeys(clear, blurAfter, ...)` |
| `CheckBoxElement` | `check()`, `uncheck()`, `isSelected()` |
| `RadioButtonElement` | `check()`, `isSelected()` |
| `LinkElement` / `ImageElement` | `getUrl()` |
| `FileUploadElement` | `sendKeys(...)` |
| `DatePickerElement` | `clear()`, `sendKeys(clear, blurAfter, ...)` |
| `SelectList` | `selectByText/Value/Index(...)`, `deselect...`, `getSelected...`, `isMultiple()` |
| `Table` | `getRowCount()`, `getColumnCount()`, `getCell(...)`, `getContent(...)`, `getRows()`, `getColumns()` |
| `PictureElement` | `clickAt(...)`, `doubleClickAt(...)`, `swipe(...)`, `tap()`, `sendKeys(...)`, `isElementPresentOnDesktop()` |
| `ScreenZone` | mêmes actions que `PictureElement`, variantes `mainScreen` |
| `CachedHtmlElement` | `isDisplayed()`, `getRealElement()`, `getText()`, `getCssValue(...)` |
| `SeleniumElement` | `getName()`, `getDescription()` |

## Sélecteurs

Ordre à privilégier, du plus stable au moins stable : `seleniumId` (si l'appli en expose un),
`attribute`, `cssSelector`, `label`/`labelForward`/`labelBackward`, `text`/`partialText`/
`textInside`, `and` (croiser deux critères), `shadow` (seulement si le DOM est réellement
encapsulé dans un Shadow DOM). Éviter les XPath longs et fragiles, et les sélecteurs qui
dépendent d'un texte instable — on ne combine plusieurs critères que si un seul ne suffit pas.

## Reporting

Ce qui apparaît dans les traces : nom de la page, nom et type de l'élément, sa description,
l'action effectuée, les exceptions avec leur message, les valeurs utiles au diagnostic.

Pour nommer une action dans le rapport : le `label` de l'élément s'il existe, sinon le nom du
champ, sinon le sélecteur en dernier recours. L'objectif reste le même à chaque niveau — un nom
stable, qui parle métier, assez court pour qu'on retrouve vite la page ou l'élément en cause sans
avoir à ouvrir le code.
