package de.tudarmstadt.ukp.clarin.webanno.api.annotation.controller;

import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import org.apache.uima.cas.CAS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.ServletContext;
import java.io.IOException;

@RestController
@RequestMapping(value = "/annotation/data")
public class AnnotationEditorControllerImpl implements AnnotationEditorController {

    private final ServletContext servletContext;

    private final DocumentService documentService;
    private final ProjectService projectService;
    private String currentDocumentName;
    private String currentUserName;
    private SourceDocument currentSourceDocument;


    @Autowired
    public AnnotationEditorControllerImpl(DocumentService aDocumentService, ProjectService aProjectService, ServletContext aServletContext) {
        super();
        servletContext = aServletContext;
        documentService = aDocumentService;
        projectService = aProjectService;
    }

    //NOTE: Was only for self training purpose
    @GetMapping(LIST_PATH)
    @ResponseBody
    public AnnotationDocument getDocument(@RequestParam String aProject, @RequestParam String aDocument) {
        AnnotationDocument doc = documentService.
            getAnnotationDocument(documentService.getSourceDocument(projectService.getProject(aProject), aDocument), "admin");
        currentDocumentName = doc.getName();
        currentUserName = doc.getUser();
        currentSourceDocument = documentService.getSourceDocument(projectService.getProject(aProject), aDocument);
        return doc;
    }

    //NOTE: Was only for self training purpose
    @GetMapping("/editor/document/CAS")
    @ResponseBody
    public CAS getEditorCas() throws IOException {
        return documentService.readAnnotationCas(currentSourceDocument,
            currentUserName);
    }


    //NOTE: Was only for self training purpose
    @Override
    public String listEditorURL(long aProjectId) {
        return servletContext.getContextPath() + BASE_URL
            + LIST_PATH.replace("{projectId}", String.valueOf(aProjectId));
    }
}
