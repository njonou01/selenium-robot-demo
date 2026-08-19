# Framework SeleniumRobot

Cette page regroupe les conventions techniques utiles pour écrire les pages du projet.

## `PageObject`

`PageObject` représente une page ou un écran manipulé par le framework. Il sert à :

- vérifier qu’on est sur la bonne page ;
- porter l’élément identifiant de la page ;
- proposer des assertions et vérifications de base ;
- centraliser des actions de navigation simples.

Concepts utiles :

- `present` = existe ;
- `displayed` = visible ;
- `enabled` = utilisable ;
- `selected` = choisi ou coché.

Méthodes importantes :

| Méthode | Rôle |
|---|---|
| `assertCurrentPage(...)` | Vérifie que la page attendue est bien active. |
| `close()` / `close(Class<T>)` | Ferme la page ou revient à la page précédente. |
| `wait(Integer)` | Pause contrôlée. |
| `assertForVisible(...)` | Vérifie présence + visibilité d’un élément. |
| `assertForInvisible(...)` | Vérifie qu’un élément n’est pas visible. |
| `assertForEnabled(...)` / `assertForDisabled(...)` | Vérifie l’état interactif. |
| `assertForValue(...)` | Vérifie le texte ou la valeur d’un élément. |
| `assertSelectedOption(...)` | Vérifie la sélection d’une liste. |
| `assertChecked(...)` / `assertNotChecked(...)` | Vérifie une case ou un radio. |
| `assertTableCellValue(...)` | Vérifie une cellule de tableau. |
| `assertTextPresentInPage(...)` / `assertTextNotPresentInPage(...)` | Vérifie un texte global dans la page. |
| `assertCookiePresent(...)` | Vérifie la présence d’un cookie. |
| `assertElementCount(...)` | Vérifie le nombre d’éléments. |
| `assertPageTitleMatches(...)` | Vérifie le titre de page. |
| `assertHtmlSource(...)` | Vérifie un texte dans le HTML source. |
| `assertLocation(...)` | Vérifie l’URL courante. |

Règle d’usage :

- `PageObject` peut vérifier qu’on est sur la bonne page ;
- les vraies assertions métier vont plutôt dans les tests ou les workflows ;
- les méthodes du `PageObject` servent surtout à des contrôles locaux de page.

## Éléments HTML utiles

Classes principales utilisées dans le projet :

- `HtmlElement` : base générique pour les éléments HTML ;
- `FrameElement` : contexte de frame / iframe ;
- `ButtonElement` : bouton d’action ;
- `TextFieldElement` : champ de saisie ;
- `LinkElement` : lien de navigation ;
- `CheckBoxElement` : case à cocher ;
- `RadioButtonElement` : bouton radio ;
- `SelectList` : liste déroulante native ou custom ;
- `LabelElement` : texte affiché ;
- `FileUploadElement` : upload de fichier ;
- `DatePickerElement` : widget de date ;
- `Table` : tableau de données ;
- `ImageElement` : image HTML classique ;
- `PictureElement` : image reconnue par capture ;
- `ScreenZone` : zone détectée sur l’écran ;
- `GenericPictureElement` : base des éléments visuels ;
- `CachedHtmlElement` : élément HTML capturé en cache ;
- `SeleniumElement` : wrapper technique d’un `WebElement` ou `Select`.

Méthodes distinctives utiles :

| Classe | Méthodes utiles |
|---|---|
| `TextFieldElement` | `clear()`, `type()`, `clearAndType()`, `sendKeys(clear, blurAfter, ...)` |
| `CheckBoxElement` | `check()`, `uncheck()`, `isSelected()` |
| `RadioButtonElement` | `check()`, `isSelected()` |
| `LinkElement` | `getUrl()` |
| `ImageElement` | `getUrl()` |
| `FileUploadElement` | `sendKeys(...)` |
| `DatePickerElement` | `clear()`, `sendKeys(clear, blurAfter, ...)` |
| `SelectList` | `selectByText(...)`, `selectByValue(...)`, `selectByIndex(...)`, `deselect...`, `getSelected...`, `isMultiple()` |
| `Table` | `getRowCount()`, `getColumnCount()`, `getCell(...)`, `getContent(...)`, `getRows()`, `getColumns()` |
| `PictureElement` | `clickAt(...)`, `doubleClickAt(...)`, `swipe(...)`, `tap()`, `sendKeys(...)`, `isElementPresentOnDesktop()` |
| `ScreenZone` | mêmes actions visuelles que `PictureElement`, avec variantes `mainScreen` |
| `CachedHtmlElement` | `isDisplayed()`, `getRealElement()`, `getText()`, `getCssValue(...)` |
| `SeleniumElement` | `getName()`, `getDescription()` |

Règle simple :

- prendre le type le plus précis possible ;
- utiliser `HtmlElement` seulement si aucun type plus spécifique n’existe ;
- utiliser `FrameElement` dès qu’un écran est encapsulé dans une frame ;
- utiliser les classes visuelles seulement si le DOM ne suffit pas.

## Sélecteurs

Ordre recommandé :

1. `seleniumId`
2. `attribute`
3. `cssSelector`
4. `label` / `labelForward` / `labelBackward`
5. `text` / `partialText` / `textInside`
6. `and`
7. `shadow`

Règle pratique :

- privilégier un sélecteur stable ;
- éviter les XPath longs et fragiles ;
- éviter les sélecteurs qui dépendent d’un texte instable ;
- utiliser `shadow` seulement si le DOM est réellement encapsulé ;
- combiner plusieurs critères seulement si un seul critère ne suffit pas.

À retenir :

- `seleniumId` si l’application expose un identifiant dédié ;
- `attribute` si l’attribut DOM est stable ;
- `cssSelector` si c’est plus lisible ;
- `label` si le formulaire est bien structuré ;
- `text` si le texte visible est stable ;
- `and` pour croiser deux critères ;
- `shadow` pour les composants dans le Shadow DOM.

## Reporting

Ce qui apparaît dans les traces :

- le nom de la page ;
- le nom de l’élément ;
- le type d’élément ;
- la description de l’élément ;
- le nom de l’action ;
- les exceptions et leur message ;
- les valeurs utiles au diagnostic.

Règle de nommage :

- une action sur une page s’affiche avec le nom simple de la classe de page ;
- une action sur un élément utilise son `getName()` ;
- si le `label` existe, il est prioritaire ;
- sinon, le nom du champ peut servir ;
- sinon, le sélecteur devient la référence de secours.

Règle de fond :

- les noms doivent être stables ;
- ils doivent parler métier ;
- ils doivent être courts ;
- ils doivent aider à retrouver vite la page ou l’élément en cause.

