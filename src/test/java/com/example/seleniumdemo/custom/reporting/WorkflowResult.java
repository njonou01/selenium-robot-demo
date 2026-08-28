package com.example.seleniumdemo.custom.reporting;

/**
 * Marqueur implemente par le record de retour d'un workflow '@Workflow(code = ...)' qui doit
 * pouvoir etre chaine dans un scenario, via '${result:code.champ}' dans le 'dataSet' d'un
 * workflow ulterieur de la meme liste 'steps'. Toujours un record concret (pas de Map), pour que
 * le champ reference soit verifie a la compilation Java cote workflow, et statiquement (avant
 * ouverture du navigateur) cote scenario via reflexion sur les 'RecordComponent'.
 */
public interface WorkflowResult {
}
