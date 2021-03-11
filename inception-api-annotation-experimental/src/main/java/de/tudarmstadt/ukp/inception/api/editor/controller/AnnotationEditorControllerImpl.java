/*
 * Licensed to the Technische Universität Darmstadt under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The Technische Universität Darmstadt
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.inception.api.editor.controller;

import com.fasterxml.jackson.databind.JsonNode;
import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.CasProvider;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.AnnotationEditorExtensionRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.*;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.feature.FeatureSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.layer.LayerSupportRegistry;
import de.tudarmstadt.ukp.clarin.webanno.brat.config.BratAnnotationEditorProperties;
import de.tudarmstadt.ukp.clarin.webanno.brat.metrics.BratMetrics;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationDocument;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.w3c.dom.events.Event;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.Set;

import static org.apache.commons.collections4.SetUtils.unmodifiableSet;

@RestController
@RequestMapping(value = "/annotation/data")
public class AnnotationEditorControllerImpl implements AnnotationEditorController {


    private static final Logger LOG = LoggerFactory.getLogger(AnnotationEditorControllerImpl.class);


    private static final String PARAM_ACTION = "action";
    private static final String PARAM_ARC_ID = "arcId";
    private static final String PARAM_ID = "id";
    private static final String PARAM_OFFSETS = "offsets";
    private static final String PARAM_TARGET_SPAN_ID = "targetSpanId";
    private static final String PARAM_ORIGIN_SPAN_ID = "originSpanId";
    private static final String PARAM_SPAN_TYPE = "type";

    private static final String ACTION_CONTEXT_MENU = "contextMenu";

    private final ServletContext servletContext;

    private final DocumentService documentService;
    private final ProjectService projectService;
    private final AnnotationSchemaService annotationService;
    private final ColoringService coloringService;
    private final AnnotationEditorExtensionRegistry extensionRegistry;
    private final LayerSupportRegistry layerSupportRegistry;
    private final FeatureSupportRegistry featureSupportRegistry;
    private final BratMetrics metrics;
    private final BratAnnotationEditorProperties bratProperties;
    private CasProvider casProvider;

    private User currentUser;
    private SourceDocument currentSourceDocument;
    private Project currentProject;


    private transient JsonNode lastRenederedJsonParsed;
    private String lastRenderedJson;
    private int lastRenderedWindowStart = -1;


    //TODO Create custom in /annotation/event
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
    public AnnotationEditorControllerImpl(DocumentService aDocumentService, ProjectService aProjectService, ServletContext aServletContext,
                                          AnnotationSchemaService aAnnotationService,
                                          ColoringService aColoringService,
                                          AnnotationEditorExtensionRegistry aExtensionRegistry,
                                          LayerSupportRegistry aLayerSupportRegistry,
                                          FeatureSupportRegistry aFeatureSupportRegistry,
                                          BratMetrics aMetrics,
                                          BratAnnotationEditorProperties aBratProperties)
    {
        super();
        annotationService = aAnnotationService;
        coloringService = aColoringService;
        extensionRegistry = aExtensionRegistry;
        layerSupportRegistry = aLayerSupportRegistry;
        featureSupportRegistry = aFeatureSupportRegistry;
        metrics = aMetrics;
        bratProperties = aBratProperties;
        servletContext = aServletContext;
        documentService = aDocumentService;
        projectService = aProjectService;

        currentProject = projectService.getProject("Annotation Study");
    }

    @Override
    public void initController(User aUser, Project aProject, SourceDocument aSourceDocument, CasProvider aCasProvider)
    {
        currentProject = aProject;
        currentSourceDocument = aSourceDocument;
        currentUser = aUser;
        casProvider = aCasProvider;
    }


    @Override
    public void getRequest(Event aEvent)
    {
        //get the request and perform corresponding action
        switch (aEvent.getType())
        {
            case (""):
                break;
            default:
        }
    }

    @Override
    public void updateDocumentService(SourceDocument aSourceDocument) {
        currentSourceDocument = aSourceDocument;
    }

    @Override
    public CAS getEditorCasService() throws IOException
    {
        return getEditorCas();
    }

    @ResponseBody
    public CAS getEditorCas() throws IOException
    {
        return documentService.readAnnotationCas(currentSourceDocument,
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

    @ResponseBody
    public AnnotationDocument getDocument(@RequestParam String aProject, @RequestParam String aDocument)
    {
        AnnotationDocument doc = documentService.
            getAnnotationDocument(documentService.getSourceDocument(projectService.getProject(aProject), aDocument), "admin");
        return doc;
    }

    //---------------------------------------------//

}
