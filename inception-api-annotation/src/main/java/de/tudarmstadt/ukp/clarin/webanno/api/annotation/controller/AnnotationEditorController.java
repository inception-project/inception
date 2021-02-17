package de.tudarmstadt.ukp.clarin.webanno.api.annotation.controller;

public interface AnnotationEditorController {
    String BASE_URL = "/de.tudarmstadt.ukp.inception.editor.AnnotationEditorController";
    String LIST_PATH = "/project/{projectId}/document/{documentID}";

    String listEditorURL(long aProjectId);
}
