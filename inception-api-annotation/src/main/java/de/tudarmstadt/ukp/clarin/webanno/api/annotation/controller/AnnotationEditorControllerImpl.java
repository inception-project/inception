package de.tudarmstadt.ukp.clarin.webanno.api.annotation.controller;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.*;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.Set;

import static org.apache.commons.collections4.SetUtils.unmodifiableSet;

@RestController
@RequestMapping(value = "/annotation/data")
public class AnnotationEditorControllerImpl implements AnnotationEditorController {

    private final ServletContext servletContext;

    private final DocumentService documentService;
    private final ProjectService projectService;

    private User currentUser;
    private SourceDocument currentDocument;
    private long currentProject;

    private final Set<String> annotationEvents = unmodifiableSet( //
        SpanCreatedEvent.class.getSimpleName(), //
        SpanDeletedEvent.class.getSimpleName(), //
        RelationCreatedEvent.class.getSimpleName(), //
        RelationDeletedEvent.class.getSimpleName(), //
        ChainLinkCreatedEvent.class.getSimpleName(), //
        ChainLinkDeletedEvent.class.getSimpleName(), //
        ChainSpanCreatedEvent.class.getSimpleName(), //
        ChainSpanDeletedEvent.class.getSimpleName(), //
        FeatureValueUpdatedEvent.class.getSimpleName(), //
        "DocumentMetadataCreatedEvent", //
        "DocumentMetadataDeletedEvent");

    @Autowired
    public AnnotationEditorControllerImpl(DocumentService aDocumentService, ProjectService aProjectService, ServletContext aServletContext)
    {
        super();
        servletContext = aServletContext;
        documentService = aDocumentService;
        projectService = aProjectService;
    }

    @Override
    public void initController(User aUsername, long aProject)
    {
        currentUser = aUsername;
        currentProject = aProject;
    }

    @Override
    public void updateDocumentService(SourceDocument aSourceDocument) {
        currentDocument = aSourceDocument;
    }

    @Override
    public CAS getEditorCasService() throws IOException {
        return getEditorCas();
    }

    @GetMapping("/editor/document/CAS")
    @ResponseBody
    public CAS getEditorCas() throws IOException {
        return documentService.readAnnotationCas(currentDocument,
            currentUser.getUsername());
    }

    @Override
    public void createAnnotationService(CAS aCas, Type aType, int aBegin, int aEnd)
    {
        /* TESTING only
        servletContext.getContextPath() + BASE_URL
            + CREATE_ANNOTATION_PATH.replace("{projectId}", String.valueOf(currentUsername));

         */
        createAnnotation(aCas, aType, aBegin, aEnd);
    }

    @GetMapping(CREATE_ANNOTATION_PATH)
    @ResponseBody
    public void createAnnotation(CAS aCas, Type aType, int aBegin, int aEnd)
    {
        aCas.createAnnotation(aType, aBegin, aEnd);
    }

    @Override
    public AnnotationDocument getDocumentService(String aProject, String aDocument)
    {
        return getDocument(aProject, aDocument);
    }

    @GetMapping(DOCUMENT_PATH)
    @ResponseBody
    public AnnotationDocument getDocument(@RequestParam String aProject, @RequestParam String aDocument)
    {
        AnnotationDocument doc = documentService.
            getAnnotationDocument(documentService.getSourceDocument(projectService.getProject(aProject), aDocument), "admin");
        return doc;
    }

}
