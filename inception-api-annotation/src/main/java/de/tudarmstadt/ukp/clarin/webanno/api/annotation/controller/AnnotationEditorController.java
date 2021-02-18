package de.tudarmstadt.ukp.clarin.webanno.api.annotation.controller;

import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;

import java.io.IOException;

public interface AnnotationEditorController {
    String BASE_URL = "/de.tudarmstadt.ukp.inception.editor.AnnotationEditorController";
    String DOCUMENT_PATH = "/project/{projectId}/document/{documentID}";
    String CREATE_ANNOTATION_PATH = "/project/{projectId}/document/{documentID}/Type/{type}/begin/{begin}/end/{end}";

    void initController(User aUsername, long aProject);

    void updateDocumentService(SourceDocument aSourceDocument);

    public CAS getEditorCasService() throws IOException;

    AnnotationDocument getDocumentService(String aProject, String aDocument);

    void createAnnotationService(CAS aCas, Type aType, int aBegin, int aEnd);


}
