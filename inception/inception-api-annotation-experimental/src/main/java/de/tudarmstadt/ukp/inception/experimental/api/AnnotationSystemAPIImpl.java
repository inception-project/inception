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
package de.tudarmstadt.ukp.inception.experimental.api;

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.Palette.PALETTE_NORMAL_FILTERED;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectFsByAddr;
import static org.apache.uima.fit.util.CasUtil.selectAllFS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.RelationCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.RelationDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.SpanCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.SpanDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.preferences.UserPreferencesService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.DocumentRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.arc.CreateArcRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.arc.DeleteArcRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.arc.SelectArcRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.arc.UpdateArcRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.CreateSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.DeleteSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.SelectSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.UpdateSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.DocumentResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.ErrorMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.arc.CreateArcMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.arc.DeleteArcMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.arc.SelectArcResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.arc.UpdateArcMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.CreateSpanMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.DeleteSpanMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.SelectSpanResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.UpdateSpanMessage;
import de.tudarmstadt.ukp.inception.experimental.api.model.Arc;
import de.tudarmstadt.ukp.inception.experimental.api.model.FeatureX;
import de.tudarmstadt.ukp.inception.experimental.api.model.Span;
import de.tudarmstadt.ukp.inception.experimental.api.model.Viewport;
import de.tudarmstadt.ukp.inception.experimental.api.websocket.AnnotationProcessAPI;

@Component
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true")
public class AnnotationSystemAPIImpl
    implements AnnotationSystemAPI
{
    private static final Logger LOG = getLogger(MethodHandles.lookup().lookupClass());

    private final AnnotationSchemaService annotationService;
    private final ProjectService projectService;
    private final DocumentService documentService;
    private final UserDao userDao;
    private final RepositoryProperties repositoryProperties;
    private final AnnotationProcessAPI annotationProcessAPI;
    private final ColoringService coloringService;
    private final UserPreferencesService userPreferencesService;

    public AnnotationSystemAPIImpl(ProjectService aProjectService, DocumentService aDocumentService,
            UserDao aUserDao, RepositoryProperties aRepositoryProperties,
            AnnotationProcessAPI aAnnotationProcessAPI,
            AnnotationSchemaService aAnnotationSchemaService, ColoringService aColoringService,
            UserPreferencesService aUserPreferencesService)
    {
        projectService = aProjectService;
        documentService = aDocumentService;
        userDao = aUserDao;
        repositoryProperties = aRepositoryProperties;
        annotationProcessAPI = aAnnotationProcessAPI;
        annotationService = aAnnotationSchemaService;
        coloringService = aColoringService;
        userPreferencesService = aUserPreferencesService;
    }

    @Override
    public void handleDocumentRequest(DocumentRequest aDocumentRequest) throws IOException
    {
        try {

            CAS cas = getCasForDocument(aDocumentRequest.getClientName(),
                    aDocumentRequest.getProjectId(), aDocumentRequest.getDocumentId());

            String documentText = cas.getDocumentText();

            List<List<Integer>> viewportList = new ArrayList<>();
            ArrayList<Integer> documentOffset = new ArrayList<>();

            documentOffset.add(0);
            documentOffset.add(documentText.toCharArray().length);

            viewportList.add(documentOffset);
            Viewport viewport = new Viewport(viewportList, documentText,
                    aDocumentRequest.getViewport().getDisabledLayers());

            List<String> validLayers = aDocumentRequest.getViewport().getDisabledLayers();

            Pair<List<Span>, List<Arc>> annotations = getAnnotations(cas,
                    aDocumentRequest.getProjectId(),
                    getValidLayers(aDocumentRequest.getProjectId(), validLayers));

            List<Span> spans = filterSpans(annotations.getKey(), viewport);

            List<Arc> relations = filterArcs(annotations.getValue(), cas, viewport);

            DocumentResponse message = new DocumentResponse(viewport,
                    aDocumentRequest.getDocumentId(), spans, relations);
            annotationProcessAPI.sendDocumentResponse(message, aDocumentRequest.getClientName());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleNewDocument()", e);
            createErrorMessage(e.getMessage(), aDocumentRequest.getClientName());
        }

    }

    @Override
    public void handleSelectSpan(SelectSpanRequest aSelectSpanRequest) throws IOException
    {
        try {
            CAS cas = getCasForDocument(aSelectSpanRequest.getClientName(),
                    aSelectSpanRequest.getProjectId(), aSelectSpanRequest.getDocumentId());
            AnnotationFS span = selectAnnotationByAddr(cas, aSelectSpanRequest.getSpanId().getId());

            List<FeatureX> features = new ArrayList<>();

            for (Feature f : span.getType().getFeatures()) {
                if (f.getRange().isStringOrStringSubtype()) {
                    features.add(new FeatureX(f.getShortName(), span.getFeatureValueAsString(f)));
                }
            }
            SelectSpanResponse message = new SelectSpanResponse(new VID(span.getAddress()),
                    span.getType().getShortName(), features);

            annotationProcessAPI.sendSelectSpanResponse(message,
                    aSelectSpanRequest.getClientName());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleSelectSpan()", e);
            createErrorMessage(e.getMessage(), aSelectSpanRequest.getClientName());
        }
    }

    @Override
    public void handleUpdateSpan(UpdateSpanRequest aUpdateSpanRequest) throws Exception
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aUpdateSpanRequest.getProjectId(), aUpdateSpanRequest.getDocumentId());
            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aUpdateSpanRequest.getClientName());

            AnnotationFS annotation = selectAnnotationByAddr(cas,
                    aUpdateSpanRequest.getSpanId().getId());

            int i = 0;

            for (Feature f : annotation.getType().getFeatures()) {

                if (f.getRange().getName().equals("uima.cas.String")) {
                    annotation.setFeatureValueFromString(f, aUpdateSpanRequest.getNewFeature()[i]);
                    i++;
                }
            }
            annotationService.upgradeCas(cas,
                    projectService.getProject(aUpdateSpanRequest.getProjectId()));

            UpdateSpanMessage response = new UpdateSpanMessage(aUpdateSpanRequest.getSpanId(),
                    aUpdateSpanRequest.getNewFeature());
            this.annotationProcessAPI.sendUpdateSpan(response,
                    String.valueOf(aUpdateSpanRequest.getProjectId()),
                    String.valueOf(aUpdateSpanRequest.getDocumentId()));
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleUpdateSpan()", e);
            throw new Exception();
        }
    }

    @Override
    public void handleCreateSpan(CreateSpanRequest aCreateSpanRequest) throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {

            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aCreateSpanRequest.getProjectId(), aCreateSpanRequest.getDocumentId());

            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aCreateSpanRequest.getClientName());

            TypeAdapter adapter = annotationService.getAdapter(
                    getLayer(aCreateSpanRequest.getProjectId(), aCreateSpanRequest.getLayer()));

            ((SpanAdapter) adapter).add(
                    documentService.getSourceDocument(aCreateSpanRequest.getProjectId(),
                            aCreateSpanRequest.getDocumentId()),
                    aCreateSpanRequest.getClientName(), cas, aCreateSpanRequest.getBegin(),
                    aCreateSpanRequest.getEnd());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleCreateSpan()", e);
            createErrorMessage(e.getMessage(), aCreateSpanRequest.getClientName());
        }
    }

    @Override
    public void handleDeleteSpan(DeleteSpanRequest aDeleteSpanRequest) throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aDeleteSpanRequest.getProjectId(), aDeleteSpanRequest.getDocumentId());

            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aDeleteSpanRequest.getClientName());

            TypeAdapter adapter = annotationService.getAdapter(
                    getLayer(aDeleteSpanRequest.getProjectId(), aDeleteSpanRequest.getLayer()));

            adapter.delete(
                    documentService.getSourceDocument(aDeleteSpanRequest.getProjectId(),
                            aDeleteSpanRequest.getDocumentId()),
                    aDeleteSpanRequest.getClientName(), cas, aDeleteSpanRequest.getSpanId());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleDeleteSpan()", e);
            createErrorMessage(e.getMessage(), aDeleteSpanRequest.getClientName());
        }
    }

    @Override
    public void handleSelectArc(SelectArcRequest aSelectArcRequest) throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aSelectArcRequest.getProjectId(), aSelectArcRequest.getDocumentId());

            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aSelectArcRequest.getClientName());

            FeatureStructure arc = selectFsByAddr(cas, aSelectArcRequest.getArcId().getId());

            AnnotationFS annotation = WebAnnoCasUtil.selectAnnotationByAddr(cas,
                    WebAnnoCasUtil.getAddr(arc));

            AnnotationFS source = null;
            AnnotationFS target = null;
            List<FeatureX> arcFeatures = new ArrayList<>();

            for (Feature f : arc.getType().getFeatures()) {
                if (f.getName().contains("Governor")) {
                    source = WebAnnoCasUtil.selectAnnotationByAddr(cas,
                            WebAnnoCasUtil.getAddr(arc.getFeatureValue(f)));
                }
                if (f.getName().contains("Dependent")) {
                    target = WebAnnoCasUtil.selectAnnotationByAddr(cas,
                            WebAnnoCasUtil.getAddr(arc.getFeatureValue(f)));
                }
                if (f.getRange().isStringOrStringSubtype()) {
                    arcFeatures.add(new FeatureX(f.getName(), arc.getFeatureValueAsString(f)));
                }
            }
            SelectArcResponse message = new SelectArcResponse(new VID(arc._id()),
                    new VID(source._id()), target.getCoveredText(), new VID(target._id()),
                    target.getCoveredText(),
                    getColorForAnnotation(annotation,
                            projectService.getProject(aSelectArcRequest.getProjectId())),

                    annotation.getType().getName(), arcFeatures);

            annotationProcessAPI.sendSelectArcResponse(message, aSelectArcRequest.getClientName());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleSelectRelation()", e);
            createErrorMessage(e.getMessage(), aSelectArcRequest.getClientName());
        }
    }

    @Override
    public void handleUpdateArc(UpdateArcRequest aUpdateArcRequest) throws Exception
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aUpdateArcRequest.getProjectId(), aUpdateArcRequest.getDocumentId());
            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aUpdateArcRequest.getClientName());

            AnnotationFS annotation = selectAnnotationByAddr(cas,
                    aUpdateArcRequest.getArcId().getId());

            for (Feature f : annotation.getType().getFeatures()) {
                for (FeatureX feat : aUpdateArcRequest.getFeatures()) {
                    if (f.getShortName().equals(feat.getName())) {
                        annotation.setFeatureValueFromString(f, feat.getValue());
                    }
                }
            }
            annotationService.upgradeCas(cas,
                    projectService.getProject(aUpdateArcRequest.getProjectId()));

            UpdateArcMessage response = new UpdateArcMessage(aUpdateArcRequest.getArcId(),
                    aUpdateArcRequest.getFeatures());

            this.annotationProcessAPI.sendUpdateArc(response,
                    String.valueOf(aUpdateArcRequest.getProjectId()),
                    String.valueOf(aUpdateArcRequest.getDocumentId()));

        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleUpdateRelation()", e);
            throw new Exception();
        }
    }

    @Override
    public void handleCreateArc(CreateArcRequest aCreateArcRequest) throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aCreateArcRequest.getProjectId(), aCreateArcRequest.getDocumentId());

            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aCreateArcRequest.getClientName());

            AnnotationFS source = selectAnnotationByAddr(cas,
                    aCreateArcRequest.getSourceId().getId());
            AnnotationFS target = selectAnnotationByAddr(cas,
                    aCreateArcRequest.getTargetId().getId());

            TypeAdapter adapter = annotationService
                    .getAdapter(
                            annotationService
                                    .getLayer(annotationService
                                            .listAttachedRelationLayers(
                                                    getLayer(aCreateArcRequest.getProjectId(),
                                                            aCreateArcRequest.getLayer()))
                                            .get(0).getId()));

            ((RelationAdapter) adapter).add(
                    documentService.getSourceDocument(aCreateArcRequest.getProjectId(),
                            aCreateArcRequest.getDocumentId()),
                    aCreateArcRequest.getClientName(), source, target, cas);
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleCreateRelation()", e);
            createErrorMessage(e.getMessage(), aCreateArcRequest.getClientName());
        }
    }

    @Override
    public void handleDeleteArc(DeleteArcRequest aDeleteArcRequest) throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aDeleteArcRequest.getProjectId(), aDeleteArcRequest.getDocumentId());

            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aDeleteArcRequest.getClientName());

            TypeAdapter adapter = annotationService.getAdapter(
                    getLayer(aDeleteArcRequest.getProjectId(), aDeleteArcRequest.getLayer()));

            adapter.delete(
                    documentService.getSourceDocument(aDeleteArcRequest.getProjectId(),
                            aDeleteArcRequest.getDocumentId()),
                    aDeleteArcRequest.getClientName(), cas, aDeleteArcRequest.getArcId());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleDeleteRelation()", e);
            createErrorMessage(e.getMessage(), aDeleteArcRequest.getClientName());
        }
    }

    @Override
    @EventListener
    public void onSpanCreatedEventHandler(SpanCreatedEvent aEvent) throws IOException
    {
        List<String> features = new ArrayList<>();
        for (Feature feature : aEvent.getAnnotation().getType().getFeatures()) {
            if (feature.getRange().isStringOrStringSubtype()) {
                features.add(aEvent.getAnnotation().getFeatureValueAsString(feature));
            }
        }

        CreateSpanMessage response = new CreateSpanMessage(
                new VID(aEvent.getAnnotation().getAddress()),
                aEvent.getAnnotation().getCoveredText(), aEvent.getAnnotation().getBegin(),
                aEvent.getAnnotation().getEnd(), aEvent.getAnnotation().getType().getName(),
                getColorForAnnotation(aEvent.getAnnotation(), aEvent.getProject()), features);
        annotationProcessAPI.sendCreateSpan(response, String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()));
    }

    @Override
    @EventListener
    public void onSpanDeletedEventHandler(SpanDeletedEvent aEvent) throws IOException
    {
        DeleteSpanMessage response = new DeleteSpanMessage(
                new VID(aEvent.getAnnotation().getAddress()));
        annotationProcessAPI.sendDeleteSpan(response, String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()));
    }

    @Override
    @EventListener
    public void onArcCreatedEventHandler(RelationCreatedEvent aEvent) throws IOException
    {
        List<FeatureX> features = new ArrayList<>();
        for (Feature feature : aEvent.getAnnotation().getType().getFeatures()) {
            if (feature.getRange().isStringOrStringSubtype()) {

                features.add(new FeatureX(feature.getShortName(),
                        aEvent.getAnnotation().getFeatureValueAsString(feature)));
            }
        }
        CreateArcMessage response = new CreateArcMessage(new VID(aEvent.getAnnotation()._id()),
                aEvent.getUser(), aEvent.getProject().getId(), aEvent.getDocument().getId(),
                new VID(aEvent.getSourceAnnotation()._id()),
                new VID(aEvent.getTargetAnnotation()._id()),
                getColorForAnnotation(aEvent.getAnnotation(), aEvent.getProject()),
                aEvent.getSourceAnnotation().getCoveredText(),
                aEvent.getTargetAnnotation().getCoveredText(),
                aEvent.getAnnotation().getType().getName(), features);

        annotationProcessAPI.sendCreateArc(response, String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()));
    }

    @Override
    @EventListener
    public void onArcDeletedEventHandler(RelationDeletedEvent aEvent) throws IOException
    {
        DeleteArcMessage response = new DeleteArcMessage(
                new VID(aEvent.getAnnotation().getAddress()));

        annotationProcessAPI.sendDeleteArc(response, String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()));
    }

    @Override
    public void createErrorMessage(String aMessage, String aUser) throws IOException
    {
        annotationProcessAPI.sendErrorMessage(new ErrorMessage(aMessage), aUser);
    }

    /**
     * Private support methods
     **/

    public CAS getCasForDocument(String aUser, long aProject, long aDocument) throws Exception
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(aProject, aDocument);
            CAS cas = documentService.readAnnotationCas(sourceDocument, aUser);
            annotationService.upgradeCas(cas, projectService.getProject(aProject));
            return cas;
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during getCasForDocument()", e);
            throw new Exception();
        }
    }

    public AnnotationLayer getLayer(long aProjectId, String aLayer)
    {
        try {
            List<AnnotationLayer> layers = annotationService
                    .listAnnotationLayer(projectService.getProject(aProjectId));

            for (AnnotationLayer layer : layers) {
                if (aLayer.equals(layer.getUiName()) || aLayer.equals(layer.getName())) {
                    return layer;
                }
            }
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during getLayer()", e);
            return null;
        }

        LOG.error("Exception has been thrown during getLayer(): Not a layer with the name " + aLayer
                + " found.");
        return null;
    }

    public List<AnnotationLayer> getValidLayers(long aProjectId, List<String> disabledLayers)
    {
        if (disabledLayers.isEmpty()) {
            return annotationService.listAnnotationLayer(projectService.getProject(aProjectId));
        }

        List<AnnotationLayer> validLayers = new ArrayList<>();
        List<AnnotationLayer> layers = annotationService
                .listAnnotationLayer(projectService.getProject(aProjectId));
        for (AnnotationLayer layer : layers) {
            if (!disabledLayers.contains(layer.getType())) {
                validLayers.add(layer);
            }
        }
        return validLayers;
    }

    public List<Span> filterSpans(List<Span> aAnnotations, Viewport aViewport) throws Exception
    {
        try {
            List<Span> filteredAnnotations = new ArrayList<>();
            for (Span annotation : aAnnotations) {
                for (int i = 0; i < aViewport.getViewport().size(); i++) {
                    for (int j = aViewport.getViewport().get(i).get(0); j <= aViewport.getViewport()
                            .get(i).get(1); j++) {
                        if (annotation.getBegin() == j) {
                            filteredAnnotations.add(annotation);
                            break;
                        }
                    }
                }
            }
            return filteredAnnotations;
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during filterSpans()", e);
            throw new Exception();
        }
    }

    public List<Arc> filterArcs(List<Arc> aRelations, CAS aCas, Viewport aViewport) throws Exception
    {
        try {
            List<Arc> filteredArcs = new ArrayList<>();

            for (Arc arc : aRelations) {
                AnnotationFS annotation = selectAnnotationByAddr(aCas, arc.getGovernorId().getId());

                for (int i = 0; i < aViewport.getViewport().size(); i++) {
                    for (int j = aViewport.getViewport().get(i).get(0); j <= aViewport.getViewport()
                            .get(i).get(1); j++) {
                        if (annotation.getBegin() == j) {
                            filteredArcs.add(arc);
                            break;
                        }
                    }
                }
            }
            return filteredArcs;

        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during filterArcs()", e);
            throw new Exception();
        }
    }

    public String getColorForAnnotation(AnnotationFS aAnnotation, Project aProject)
    {
        // TODO proper coloring strategy (without VTypes)
        try {
            TypeAdapter adapter = annotationService.getAdapter(annotationService.findLayer(
                    projectService.getProject(aProject.getId()), aAnnotation.getType().getName()));

            return PALETTE_NORMAL_FILTERED[adapter.getLayer().getId().intValue()
                    % PALETTE_NORMAL_FILTERED.length];
        }
        catch (Exception e) {
            LOG.info(
                    "First entry of PALETTE_NORMAL_FILTERED WAS PICKED DUE TO ERROR. SEE SERVER LOG FOR FURTHER DETAILS.");
            return PALETTE_NORMAL_FILTERED[0];
        }
    }

    public Pair<List<Span>, List<Arc>> getAnnotations(CAS aCas, long aProjectId,
            List<AnnotationLayer> aValidLayers)
    {
        List<Span> spans = new ArrayList<>();
        List<Arc> relations = new ArrayList<>();

        for (FeatureStructure fs : selectAllFS(aCas)) {
            if (fs.getType().getShortName().equals("Token")) {
                continue;
            }
            for (AnnotationLayer layer : aValidLayers) {
                if (fs.getType().getName().equals(layer.getName())) {
                    AnnotationFS annotation = WebAnnoCasUtil.selectAnnotationByAddr(aCas,
                            WebAnnoCasUtil.getAddr(fs));
                    switch (layer.getType()) {
                    case "span":
                        List<String> spanFeatures = new ArrayList<>();
                        for (Feature f : annotation.getType().getFeatures()) {
                            if (f.getRange().isStringOrStringSubtype()) {
                                spanFeatures.add(fs.getFeatureValueAsString(f));
                            }
                        }
                        spans.add(new Span(new VID(annotation._id()), annotation.getBegin(),
                                annotation.getEnd(), annotation.getType().getShortName(),
                                getColorForAnnotation(annotation,
                                        projectService.getProject(aProjectId)),
                                annotation.getCoveredText(), spanFeatures));
                        break;
                    case "relation":
                        AnnotationFS source = null;
                        AnnotationFS target = null;
                        List<String> arcFeatures = new ArrayList<>();

                        for (Feature f : annotation.getType().getFeatures()) {
                            if (f.getName().contains("Governor")) {
                                source = WebAnnoCasUtil.selectAnnotationByAddr(aCas,
                                        WebAnnoCasUtil.getAddr(fs.getFeatureValue(f)));
                            }
                            if (f.getName().contains("Dependent")) {
                                target = WebAnnoCasUtil.selectAnnotationByAddr(aCas,
                                        WebAnnoCasUtil.getAddr(fs.getFeatureValue(f)));
                            }
                            if (f.getRange().isStringOrStringSubtype()) {
                                arcFeatures.add(fs.getFeatureValueAsString(f));
                            }
                        }
                        relations.add(new Arc(new VID(annotation._id()), new VID(source._id()),
                                new VID(target._id()),
                                getColorForAnnotation(annotation,
                                        projectService.getProject(aProjectId)),
                                source.getCoveredText(), target.getCoveredText(), layer.getName(),
                                arcFeatures));
                        break;
                    default:
                        break;
                    }
                }
            }
        }

        return new ImmutablePair<>(spans, relations);
    }

}
