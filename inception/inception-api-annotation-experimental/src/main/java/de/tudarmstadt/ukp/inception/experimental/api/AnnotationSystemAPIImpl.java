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
import static java.lang.Math.toIntExact;
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
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.NewDocumentRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.NewViewportRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.relation.AllRelationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.relation.CreateRelationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.relation.DeleteRelationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.relation.SelectRelationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.relation.UpdateRelationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.AllSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.CreateSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.DeleteSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.SelectSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.span.UpdateSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.ErrorMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.NewDocumentResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.NewViewportResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.relation.CreateRelationResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.relation.DeleteRelationResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.relation.SelectRelationResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.relation.UpdateRelationResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.CreateSpanResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.DeleteSpanResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.SelectSpanResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.span.UpdateSpanResponse;
import de.tudarmstadt.ukp.inception.experimental.api.model.Relation;
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
    public void handleNewDocument(NewDocumentRequest aNewDocumentRequest) throws IOException
    {
        try {
            // TODO server decide on next document
            if (aNewDocumentRequest.getDocumentId() == 0) {
                aNewDocumentRequest.setDocumentId(41714);
            }

            CAS cas = getCasForDocument(aNewDocumentRequest.getUserName(),
                    aNewDocumentRequest.getProjectId(), aNewDocumentRequest.getDocumentId());

            if (aNewDocumentRequest.getViewport().getDisabledLayers() == null) {
                aNewDocumentRequest.getViewport().setDisabledLayers(new ArrayList<>());
            }
            Pair<List<Span>, List<Relation>> annotations = getAnnotations(cas,
                    aNewDocumentRequest.getProjectId(),
                    getValidLayers(aNewDocumentRequest.getProjectId(),
                            aNewDocumentRequest.getViewport().getDisabledLayers()));

            List<Span> spans = filterSpans(annotations.getKey(), aNewDocumentRequest.getViewport());

            List<Relation> relations = filterRelations(annotations.getValue(), cas,
                    aNewDocumentRequest.getViewport());

            NewDocumentResponse message = new NewDocumentResponse(
                    toIntExact(documentService.getSourceDocument(aNewDocumentRequest.getProjectId(),
                            aNewDocumentRequest.getDocumentId()).getId()),
                    getViewportText(cas, aNewDocumentRequest.getViewport()), spans, relations);
            annotationProcessAPI.sendNewDocumentResponse(message,
                    aNewDocumentRequest.getClientName());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleNewDocument()");
            e.printStackTrace();
            createErrorMessage(e.getMessage(), aNewDocumentRequest.getClientName());
        }

    }

    @Override
    public void handleNewViewport(NewViewportRequest aNewViewportRequest) throws IOException
    {
        try {
            CAS cas = getCasForDocument(aNewViewportRequest.getUserName(),
                    aNewViewportRequest.getProjectId(), aNewViewportRequest.getDocumentId());

            if (aNewViewportRequest.getViewport().getDisabledLayers() == null) {
                aNewViewportRequest.getViewport().setDisabledLayers(new ArrayList<>());
            }
            Pair<List<Span>, List<Relation>> annotations = getAnnotations(cas,
                    aNewViewportRequest.getProjectId(),
                    getValidLayers(aNewViewportRequest.getProjectId(),
                            aNewViewportRequest.getViewport().getDisabledLayers()));

            List<Span> spans = filterSpans(annotations.getKey(), aNewViewportRequest.getViewport());

            List<Relation> relations = filterRelations(annotations.getValue(), cas,
                    aNewViewportRequest.getViewport());

            NewViewportResponse message = new NewViewportResponse(
                    getViewportText(cas, aNewViewportRequest.getViewport()), spans, relations);

            annotationProcessAPI.sendNewViewportResponse(message,
                    aNewViewportRequest.getClientName());

        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleNewViewport()");
            createErrorMessage(e.getMessage(), aNewViewportRequest.getClientName());
        }

    }

    @Override
    public void handleSelectSpan(SelectSpanRequest aSelectSpanRequest) throws IOException
    {
        try {
            CAS cas = getCasForDocument(aSelectSpanRequest.getUserName(),
                    aSelectSpanRequest.getProjectId(), aSelectSpanRequest.getDocumentId());
            AnnotationFS span = selectAnnotationByAddr(cas,
                    aSelectSpanRequest.getSpanAddress().getId());

            List<String> features = new ArrayList<>();

            for (Feature f : span.getType().getFeatures()) {
                if (f.getRange().isStringOrStringSubtype()) {
                    features.add(span.getFeatureValueAsString(f));
                }
            }
            SelectSpanResponse message = new SelectSpanResponse(
                    VID.parse(String.valueOf(span.getAddress())), span.getType().getShortName(),
                    features);

            annotationProcessAPI.sendSelectAnnotationResponse(message,
                    aSelectSpanRequest.getClientName());

        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleSelectSpan()");
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
                    aUpdateSpanRequest.getUserName());

            AnnotationFS annotation = selectAnnotationByAddr(cas,
                    aUpdateSpanRequest.getSpanAddress().getId());

            int i = 0;

            for (Feature f : annotation.getType().getFeatures()) {

                if (f.getRange().getName().equals("uima.cas.String")) {
                    annotation.setFeatureValueFromString(f, aUpdateSpanRequest.getNewFeature()[i]);
                    i++;
                }
            }
            annotationService.upgradeCas(cas,
                    projectService.getProject(aUpdateSpanRequest.getProjectId()));
            UpdateSpanResponse response = new UpdateSpanResponse(
                    aUpdateSpanRequest.getSpanAddress(), aUpdateSpanRequest.getNewFeature());
            this.annotationProcessAPI.sendUpdateAnnotationResponse(response,
                    String.valueOf(aUpdateSpanRequest.getProjectId()),
                    String.valueOf(aUpdateSpanRequest.getDocumentId()),
                    String.valueOf(annotation.getBegin()));
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleUpdateSpan()");
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
                    aCreateSpanRequest.getUserName());

            TypeAdapter adapter = annotationService.getAdapter(
                    getLayer(aCreateSpanRequest.getProjectId(), aCreateSpanRequest.getLayer()));

            ((SpanAdapter) adapter).add(
                    documentService.getSourceDocument(aCreateSpanRequest.getProjectId(),
                            aCreateSpanRequest.getDocumentId()),
                    aCreateSpanRequest.getUserName(), cas, aCreateSpanRequest.getBegin(),
                    aCreateSpanRequest.getEnd());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleCreateSpan()");
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
                    aDeleteSpanRequest.getUserName());

            TypeAdapter adapter = annotationService.getAdapter(
                    getLayer(aDeleteSpanRequest.getProjectId(), aDeleteSpanRequest.getLayer()));

            adapter.delete(
                    documentService.getSourceDocument(aDeleteSpanRequest.getProjectId(),
                            aDeleteSpanRequest.getDocumentId()),
                    aDeleteSpanRequest.getUserName(), cas,
                    VID.parse(String.valueOf(aDeleteSpanRequest.getSpanAddress())));
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleDeleteSpan()");
            createErrorMessage(e.getMessage(), aDeleteSpanRequest.getClientName());
        }
    }

    @Override
    public void handleSelectRelation(SelectRelationRequest aSelectRelationRequest)
        throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aSelectRelationRequest.getProjectId(), aSelectRelationRequest.getDocumentId());

            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aSelectRelationRequest.getUserName());

            FeatureStructure relation = selectFsByAddr(cas,
                    aSelectRelationRequest.getRelationAddress().getId());

            AnnotationFS annotation = WebAnnoCasUtil.selectAnnotationByAddr(cas,
                    WebAnnoCasUtil.getAddr(relation));

            AnnotationFS governor = null;
            AnnotationFS dependent = null;
            List<String> relationFeatures = new ArrayList<>();

            for (Feature f : relation.getType().getFeatures()) {
                if (f.getName().contains("Governor")) {
                    governor = WebAnnoCasUtil.selectAnnotationByAddr(cas,
                            WebAnnoCasUtil.getAddr(relation.getFeatureValue(f)));
                }
                if (f.getName().contains("Dependent")) {
                    dependent = WebAnnoCasUtil.selectAnnotationByAddr(cas,
                            WebAnnoCasUtil.getAddr(relation.getFeatureValue(f)));
                }
                if (f.getRange().isStringOrStringSubtype()) {
                    relationFeatures.add(relation.getFeatureValueAsString(f));
                }
            }
            SelectRelationResponse message = new SelectRelationResponse(
                    VID.parse(String.valueOf(relation._id())),
                    VID.parse(String.valueOf(governor._id())), governor.getCoveredText(),
                    VID.parse(String.valueOf(dependent._id())), dependent.getCoveredText(),
                    getColorForAnnotation(annotation,
                            projectService.getProject(aSelectRelationRequest.getProjectId())),

                    annotation.getType().getName(), relationFeatures);

            annotationProcessAPI.sendSelectRelationResponse(message,
                    aSelectRelationRequest.getClientName());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleSelectRelation()");
            createErrorMessage(e.getMessage(), aSelectRelationRequest.getClientName());
        }
    }

    @Override
    public void handleUpdateRelation(UpdateRelationRequest aUpdateRelationRequest) throws Exception
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aUpdateRelationRequest.getProjectId(), aUpdateRelationRequest.getDocumentId());
            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aUpdateRelationRequest.getUserName());

            AnnotationFS annotation = selectAnnotationByAddr(cas,
                    aUpdateRelationRequest.getRelationAddress().getId());

            int i = 0;
            for (Feature f : annotation.getType().getFeatures()) {
                if (f.getRange().getName().equals("uima.cas.String")) {
                    if (i == 0) {
                        annotation.setFeatureValueFromString(f,
                                aUpdateRelationRequest.getNewDependencyType());
                        i++;
                    }
                    else {
                        annotation.setFeatureValueFromString(f,
                                aUpdateRelationRequest.getNewFlavor());
                        break;
                    }
                }
            }
            annotationService.upgradeCas(cas,
                    projectService.getProject(aUpdateRelationRequest.getProjectId()));

            UpdateRelationResponse response = new UpdateRelationResponse(
                    aUpdateRelationRequest.getRelationAddress(),
                    aUpdateRelationRequest.getNewDependencyType(),
                    aUpdateRelationRequest.getNewFlavor());

            this.annotationProcessAPI.sendUpdateRelationResponse(response,
                    String.valueOf(aUpdateRelationRequest.getProjectId()),
                    String.valueOf(aUpdateRelationRequest.getDocumentId()),
                    String.valueOf(annotation.getBegin()));
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleUpdateRelation()");
            throw new Exception();
        }
    }

    @Override
    public void handleCreateRelation(CreateRelationRequest aCreateRelationRequest)
        throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aCreateRelationRequest.getProjectId(), aCreateRelationRequest.getDocumentId());

            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aCreateRelationRequest.getUserName());

            AnnotationFS governor = selectAnnotationByAddr(cas,
                    aCreateRelationRequest.getGovernorId().getId());
            AnnotationFS dependent = selectAnnotationByAddr(cas,
                    aCreateRelationRequest.getDependentId().getId());

            TypeAdapter adapter = annotationService.getAdapter(getLayer(
                    aCreateRelationRequest.getProjectId(), aCreateRelationRequest.getLayer()));

            ((RelationAdapter) adapter).add(
                    documentService.getSourceDocument(aCreateRelationRequest.getProjectId(),
                            aCreateRelationRequest.getDocumentId()),
                    aCreateRelationRequest.getUserName(), governor, dependent, cas);
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleCreateRelation()");
            createErrorMessage(e.getMessage(), aCreateRelationRequest.getClientName());
        }
    }

    @Override
    public void handleDeleteRelation(DeleteRelationRequest aDeleteRelationRequest)
        throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aDeleteRelationRequest.getProjectId(), aDeleteRelationRequest.getDocumentId());

            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aDeleteRelationRequest.getUserName());

            TypeAdapter adapter = annotationService.getAdapter(getLayer(
                    aDeleteRelationRequest.getProjectId(), aDeleteRelationRequest.getLayer()));

            adapter.delete(
                    documentService.getSourceDocument(aDeleteRelationRequest.getProjectId(),
                            aDeleteRelationRequest.getDocumentId()),
                    aDeleteRelationRequest.getUserName(), cas,
                    VID.parse(String.valueOf(aDeleteRelationRequest.getRelationAddress())));
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleDeleteRelation()");
            createErrorMessage(e.getMessage(), aDeleteRelationRequest.getClientName());
        }
    }

    @Override
    public void handleAllSpans(AllSpanRequest aAllSpanRequest) throws IOException
    {
        try {
            // TODO maybe to remove?
            CAS cas = getCasForDocument(aAllSpanRequest.getUserName(),
                    aAllSpanRequest.getProjectId(), aAllSpanRequest.getDocumentId());
            /*
             * AllSpanResponse message = new AllSpanResponse(getSpans(cas,
             * aAllSpanRequest.getProjectId(), aAllSpanRequest.getDisabledLayers()));
             * 
             * annotationProcessAPI.sendAllSpansResponse(message, aAllSpanRequest.getClientName());
             * 
             */

        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleAllSpans()");
            createErrorMessage(e.getMessage(), aAllSpanRequest.getClientName());
        }
    }

    @Override
    public void handleAllRelations(AllRelationRequest aAllRelationRequest) throws IOException
    {
        try {
            // TODO maybe to remove?
            CAS cas = getCasForDocument(aAllRelationRequest.getUserName(),
                    aAllRelationRequest.getProjectId(), aAllRelationRequest.getDocumentId());

            /*
             * AllRelationResponse message = new AllRelationResponse( getRelations(cas,
             * aAllRelationRequest.getProjectId()));
             * 
             * 
             * annotationProcessAPI.sendAllRelationsResponse(message,
             * aAllRelationRequest.getClientName());
             * 
             */
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleAllRelations()");
            createErrorMessage(e.getMessage(), aAllRelationRequest.getClientName());
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
        CreateSpanResponse response = new CreateSpanResponse(
                VID.parse(String.valueOf(aEvent.getAnnotation().getAddress())),
                aEvent.getAnnotation().getCoveredText(), aEvent.getAnnotation().getBegin(),
                aEvent.getAnnotation().getEnd(), aEvent.getAnnotation().getType().getName(),
                getColorForAnnotation(aEvent.getAnnotation(), aEvent.getProject()), features);
        annotationProcessAPI.sendCreateAnnotationResponse(response,
                String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()),
                String.valueOf(aEvent.getAnnotation().getBegin()));
    }

    @Override
    @EventListener
    public void onSpanDeletedEventHandler(SpanDeletedEvent aEvent) throws IOException
    {
        DeleteSpanResponse response = new DeleteSpanResponse(
                VID.parse(String.valueOf(aEvent.getAnnotation().getAddress())));
        annotationProcessAPI.sendDeleteAnnotationResponse(response,
                String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()),
                String.valueOf(aEvent.getAnnotation().getBegin()));
    }

    @Override
    @EventListener
    public void onRelationCreatedEventHandler(RelationCreatedEvent aEvent) throws IOException
    {
        List<String> features = new ArrayList<>();
        for (Feature feature : aEvent.getAnnotation().getType().getFeatures()) {
            if (feature.getRange().isStringOrStringSubtype()) {
                features.add(aEvent.getAnnotation().getFeatureValueAsString(feature));
            }
        }
        CreateRelationResponse response = new CreateRelationResponse(
                VID.parse(String.valueOf(aEvent.getAnnotation()._id())), aEvent.getUser(),
                aEvent.getUser(), aEvent.getProject().getId(), aEvent.getDocument().getId(),
                VID.parse(String.valueOf(aEvent.getSourceAnnotation()._id())),
                VID.parse(String.valueOf(aEvent.getTargetAnnotation()._id())),
                getColorForAnnotation(aEvent.getAnnotation(), aEvent.getProject()),
                aEvent.getSourceAnnotation().getCoveredText(),
                aEvent.getTargetAnnotation().getCoveredText(),
                aEvent.getAnnotation().getType().getName(), features);

        annotationProcessAPI.sendCreateRelationResponse(response,
                String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()),
                String.valueOf(aEvent.getAnnotation().getBegin()));
    }

    @Override
    @EventListener
    public void onRelationDeletedEventHandler(RelationDeletedEvent aEvent) throws IOException
    {
        DeleteRelationResponse response = new DeleteRelationResponse(
                VID.parse(String.valueOf(aEvent.getAnnotation().getAddress())));

        annotationProcessAPI.sendDeleteRelationResponse(response,
                String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()),
                String.valueOf(aEvent.getAnnotation().getBegin()));
    }

    @Override
    public void createErrorMessage(String aMessage, String aUser) throws IOException
    {
        annotationProcessAPI.sendErrorMessage(new ErrorMessage(aMessage), aUser);
    }

    /**
     *
     * Private support methods
     *
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
            LOG.error("Exception has been thrown during getCasForDocument()");
            throw new Exception();
        }
    }

    public AnnotationLayer getLayer(long aProjectId, String aLayer)
    {
        try {
            List<AnnotationLayer> layers = annotationService
                    .listAnnotationLayer(projectService.getProject(aProjectId));

            for (AnnotationLayer layer : layers) {
                if (aLayer.equals(layer.getName())) {
                    return layer;
                }
            }

        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during getLayer()");
            return null;
        }

        LOG.error("Exception has been thrown during getLayer(): Not a layer with the name " + aLayer
                + " found.");
        return null;
    }

    public List<AnnotationLayer> getValidLayers(long aProjectId, List<String> disabledLayers)
    {

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

    public List<String> getViewportText(CAS aCas, Viewport aViewport) throws Exception
    {
        try {
            List<String> viewport = new ArrayList<>();
            String text = aCas.getDocumentText();
            for (int i = 0; i < aViewport.getViewport().size(); i++) {
                String sentence = text.substring(aViewport.getViewport().get(i).get(0),
                        aViewport.getViewport().get(i).get(1));
                viewport.add(sentence);
            }
            return viewport;
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during getViewportText()");
            throw new Exception();
        }
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
            LOG.error("Exception has been thrown during filterSpans()");
            throw new Exception();
        }
    }

    public List<Relation> filterRelations(List<Relation> aRelations, CAS aCas, Viewport aViewport)
        throws Exception
    {
        try {
            List<Relation> filteredRelations = new ArrayList<>();

            for (Relation relation : aRelations) {
                AnnotationFS annotation = selectAnnotationByAddr(aCas,
                        relation.getGovernorId().getId());

                for (int i = 0; i < aViewport.getViewport().size(); i++) {
                    for (int j = aViewport.getViewport().get(i).get(0); j <= aViewport.getViewport()
                            .get(i).get(1); j++) {
                        if (annotation.getBegin() == j) {
                            filteredRelations.add(relation);
                            break;
                        }
                    }
                }
            }
            return filteredRelations;

        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during filterRelations()");
            LOG.error("CAUSE: " + e.getCause());
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

    public Pair<List<Span>, List<Relation>> getAnnotations(CAS aCas, long aProjectId,
            List<AnnotationLayer> validLayers)
    {
        List<Span> spans = new ArrayList<>();
        List<Relation> relations = new ArrayList<>();

        for (FeatureStructure fs : selectAllFS(aCas)) {
            if (fs.getType().getShortName().equals("Token")) {
                continue;
            }
            for (AnnotationLayer layer : validLayers) {
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
                        spans.add(new Span(annotation._id(), annotation.getBegin(),
                                annotation.getEnd(), annotation.getType().getShortName(),
                                getColorForAnnotation(annotation,
                                        projectService.getProject(aProjectId)),
                                annotation.getCoveredText(), spanFeatures));
                        break;
                    case "relation":
                        AnnotationFS governor = null;
                        AnnotationFS dependent = null;
                        List<String> relationFeatures = new ArrayList<>();

                        for (Feature f : annotation.getType().getFeatures()) {
                            if (f.getName().contains("Governor")) {
                                governor = WebAnnoCasUtil.selectAnnotationByAddr(aCas,
                                        WebAnnoCasUtil.getAddr(fs.getFeatureValue(f)));
                            }
                            if (f.getName().contains("Dependent")) {
                                dependent = WebAnnoCasUtil.selectAnnotationByAddr(aCas,
                                        WebAnnoCasUtil.getAddr(fs.getFeatureValue(f)));
                            }
                            if (f.getRange().isStringOrStringSubtype()) {
                                relationFeatures.add(fs.getFeatureValueAsString(f));
                            }
                        }
                        relations.add(new Relation(VID.parse(String.valueOf(annotation._id())),
                                VID.parse(String.valueOf(governor._id())),
                                VID.parse(String.valueOf(dependent._id())),
                                getColorForAnnotation(annotation,
                                        projectService.getProject(aProjectId)),
                                governor.getCoveredText(), dependent.getCoveredText(),
                                layer.getName(), relationFeatures));
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
