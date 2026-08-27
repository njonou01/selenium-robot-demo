# Écrire un test

Un test est le point d'entrée d'exécution : il choisit le scénario ou le flux à lancer, appelle
le bon workflow, et vérifie le résultat métier attendu. Il reste au niveau du cas d'usage — pas
de clic direct dans l'UI, pas de sélecteur manipulé, pas de logique métier détaillée. Ça, c'est le
rôle du workflow et des pages.

```java
public class DemandeTest {

    public void creerUneDemande() {
        DemandeWorkflow workflow = new DemandeWorkflow();
        workflow.creerDemande();
    }
}
```

## Ordre conseillé

```text
1. choisir le scénario ou le flux
2. appeler le workflow
3. vérifier le résultat métier final
4. remonter les informations utiles au diagnostic
```

Les vérifications métier finales restent dans le test — c'est lui qui sait ce qu'on est censé
obtenir. Le test ne construit pas la donnée métier du cas, il délègue ça au workflow ; il ne parle
jamais directement à une page.

## Quand plusieurs workflows s'enchaînent

Le test se contente de piloter l'ordre : il lance un workflow, celui-ci s'arrête sur son point de
reprise (la synthèse de déclaration, par exemple, quand elle existe), et le test enchaîne le
suivant. Il ne gère pas l'écran intermédiaire entre les deux — ce n'est pas son rôle.

```text
test = choisit et vérifie
workflow = porte la donnée et enchaîne le métier
page = exécute l'UI
```

Un test qui commence à cliquer dans l'UI, à porter toute la logique métier, ou à mélanger
données/UI/orchestration a cessé d'être un test — c'est devenu un deuxième workflow, avec les
mêmes responsabilités mal réparties. Le nom du test doit décrire ce qu'il vérifie ou quel flux il
pilote, et rester stable dans le rapport.
