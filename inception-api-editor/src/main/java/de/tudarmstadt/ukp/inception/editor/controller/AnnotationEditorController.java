package de.tudarmstadt.ukp.inception.editor.controller;

import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.w3c.dom.events.Event;

import java.io.IOException;

public interface AnnotationEditorController {
    String BASE_URL = "/de.tudarmstadt.ukp.inception.editor.AnnotationEditorController";
    String DOCUMENT_PATH = "/project/{projectId}/document/{documentID}";
    String CREATE_ANNOTATION_PATH = "/project/{projectId}/document/{documentID}/Type/{type}/begin/{begin}/end/{end}";

    void initController(User aUser, Project aProject, SourceDocument aSourceDocument, CasProvider aCasProvider);

    void updateDocumentService(SourceDocument aSourceDocument);

    public CAS getEditorCasService() throws IOException;

    AnnotationDocument getDocumentService(String aProject, String aDocument);

    void createAnnotationService(CAS aCas, Type aType, int aBegin, int aEnd);

    void getRequest(Event aEvent);


}
