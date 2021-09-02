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
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.AnnotationPredicates;
import org.slf4j.Logger;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.WebAnnoConst;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.RelationAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.coloring.ColoringService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.FeatureValueUpdatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.RelationCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.RelationDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.SpanCreatedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.SpanDeletedEvent;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationFeature;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.Project;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.DeleteAnnotationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.DocumentRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.UpdateFeaturesRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.create.CreateArcRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.create.CreateSpanRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.AdviceMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.DeleteAnnotationMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.DocumentMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.UpdateFeaturesMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.create.ArcCreatedMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.create.SpanCreatedMessage;
import de.tudarmstadt.ukp.inception.experimental.api.model.Arc;
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
    private final RepositoryProperties repositoryProperties;
    private final AnnotationProcessAPI annotationProcessAPI;
    private final ColoringService coloringService;

    public AnnotationSystemAPIImpl(ProjectService aProjectService, DocumentService aDocumentService,
            RepositoryProperties aRepositoryProperties, AnnotationProcessAPI aAnnotationProcessAPI,
            AnnotationSchemaService aAnnotationSchemaService, ColoringService aColoringService)
    {
        projectService = aProjectService;
        documentService = aDocumentService;
        repositoryProperties = aRepositoryProperties;
        annotationProcessAPI = aAnnotationProcessAPI;
        annotationService = aAnnotationSchemaService;
        coloringService = aColoringService;
    }

    @Override
    public void handleDocumentRequest(DocumentRequest aDocumentRequest) throws IOException
    {
        try {
            CAS cas = getCasForDocument(aDocumentRequest.getAnnotatorName(),
                    aDocumentRequest.getProjectId(), aDocumentRequest.getSourceDocumentId());

            Pair<List<Span>, List<Arc>> annotations = null;
            for (Viewport viewport : aDocumentRequest.getViewport()) {
                Pair<List<Span>, List<Arc>> retrievedAnnotations = getAnnotations(cas,
                        aDocumentRequest.getProjectId(), viewport);
                annotations.getLeft().addAll(retrievedAnnotations.getLeft());
                annotations.getRight().addAll(retrievedAnnotations.getRight());
            }

            DocumentMessage message = new DocumentMessage(aDocumentRequest.getViewport(),
                    aDocumentRequest.getSourceDocumentId(), annotations.getLeft(),
                    annotations.getRight());
            annotationProcessAPI.sendDocumentResponse(message, aDocumentRequest.getAnnotatorName());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleNewDocument()", e);
            createAdviceMessage(e.getMessage(), aDocumentRequest.getAnnotatorName());
        }

    }

    @Override
    public void handleCreateSpan(CreateSpanRequest aCreateSpanRequest) throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {

            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aCreateSpanRequest.getProjectId(), aCreateSpanRequest.getSourceDocumentId());

            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aCreateSpanRequest.getAnnotatorName());

            TypeAdapter adapter = annotationService
                    .getAdapter(annotationService.getLayer(aCreateSpanRequest.getLayerId()));

            ((SpanAdapter) adapter).add(
                    documentService.getSourceDocument(aCreateSpanRequest.getProjectId(),
                            aCreateSpanRequest.getSourceDocumentId()),
                    aCreateSpanRequest.getAnnotatorName(), cas, aCreateSpanRequest.getBegin(),
                    aCreateSpanRequest.getEnd());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleCreateSpan()", e);
            createAdviceMessage(e.getMessage(), aCreateSpanRequest.getAnnotatorName());
        }
    }

    @Override
    public void handleCreateArc(CreateArcRequest aCreateArcRequest) throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aCreateArcRequest.getProjectId(), aCreateArcRequest.getSourceDocumentId());

            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aCreateArcRequest.getAnnotatorName());

            AnnotationFS source = selectAnnotationByAddr(cas,
                    aCreateArcRequest.getSourceId().getId());
            AnnotationFS target = selectAnnotationByAddr(cas,
                    aCreateArcRequest.getTargetId().getId());

            TypeAdapter adapter = annotationService
                    .getAdapter(annotationService.getLayer(aCreateArcRequest.getLayerId()));

            ((RelationAdapter) adapter).add(
                    documentService.getSourceDocument(aCreateArcRequest.getProjectId(),
                            aCreateArcRequest.getSourceDocumentId()),
                    aCreateArcRequest.getAnnotatorName(), source, target, cas);
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleCreateRelation()", e);
            createAdviceMessage(e.getMessage(), aCreateArcRequest.getAnnotatorName());
        }
    }

    @Override
    public void handleDeleteAnnotation(DeleteAnnotationRequest aDeleteAnnotationRequest)
        throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aDeleteAnnotationRequest.getProjectId(),
                    aDeleteAnnotationRequest.getSourceDocumentId());

            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aDeleteAnnotationRequest.getAnnotatorName());

            TypeAdapter adapter = annotationService
                    .getAdapter(annotationService.getLayer(aDeleteAnnotationRequest.getLayerId()));

            adapter.delete(
                    documentService.getSourceDocument(aDeleteAnnotationRequest.getProjectId(),
                            aDeleteAnnotationRequest.getSourceDocumentId()),
                    aDeleteAnnotationRequest.getAnnotatorName(), cas,
                    aDeleteAnnotationRequest.getAnnotationId());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleDeleteSpan()", e);
            createAdviceMessage(e.getMessage(), aDeleteAnnotationRequest.getAnnotatorName());
        }
    }

    @Override
    public void handleUpdateFeatures(UpdateFeaturesRequest aUpdateFeaturesRequest)
        throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aUpdateFeaturesRequest.getProjectId(),
                    aUpdateFeaturesRequest.getSourceDocumentId());
            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aUpdateFeaturesRequest.getAnnotatorName());

            TypeAdapter adapter = annotationService
                    .getAdapter(annotationService.getLayer(aUpdateFeaturesRequest.getLayerId()));

            adapter.setFeatureValue(sourceDocument, aUpdateFeaturesRequest.getAnnotatorName(), cas,
                    aUpdateFeaturesRequest.getAnnotationId().getId(),
                    aUpdateFeaturesRequest.getFeature(), aUpdateFeaturesRequest.getValue());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleUpdateSpan()", e);
            createAdviceMessage(e.getMessage(), aUpdateFeaturesRequest.getAnnotatorName());
        }
    }

    @Override
    @EventListener
    public void onSpanCreatedEventHandler(SpanCreatedEvent aEvent) throws IOException
    {
        List<AnnotationFeature> features = new ArrayList<>();
        AnnotationFS annotation = aEvent.getAnnotation();
        for (Feature f : annotation.getType().getFeatures()) {
            features.add(annotationService.getAdapter(aEvent.getLayer()).getFeatureValue(
                    (AnnotationFeature) annotation, annotation.getFeatureValue(f)));
        }

        SpanCreatedMessage response = new SpanCreatedMessage(
                new VID(aEvent.getAnnotation().getAddress()), aEvent.getAnnotation().getBegin(),
                aEvent.getAnnotation().getEnd(), aEvent.getLayer().getId(),
                getColorForAnnotation(aEvent.getAnnotation(), aEvent.getProject()), features);
        annotationProcessAPI.sendCreateSpan(response, String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()));
    }

    @Override
    @EventListener
    public void onSpanDeletedEventHandler(SpanDeletedEvent aEvent) throws IOException
    {
        DeleteAnnotationMessage response = new DeleteAnnotationMessage(
                new VID(aEvent.getAnnotation().getAddress()));
        annotationProcessAPI.sendDeleteAnnotation(response,
                String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()));
    }

    @Override
    @EventListener
    public void onArcCreatedEventHandler(RelationCreatedEvent aEvent) throws IOException
    {
        List<AnnotationFeature> features = new ArrayList<>();
        for (Feature f : aEvent.getAnnotation().getType().getFeatures()) {
            System.out.println(aEvent.getAnnotation().getFeatureValue(f));
        }

        ArcCreatedMessage response = new ArcCreatedMessage(new VID(aEvent.getAnnotation()._id()),
                aEvent.getProject().getId(), new VID(aEvent.getSourceAnnotation()._id()),
                new VID(aEvent.getTargetAnnotation()._id()),
                getColorForAnnotation(aEvent.getAnnotation(), aEvent.getProject()),
                aEvent.getLayer().getId(), features);

        annotationProcessAPI.sendCreateArc(response, String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()));
    }

    @Override
    @EventListener
    public void onArcDeletedEventHandler(RelationDeletedEvent aEvent) throws IOException
    {
        DeleteAnnotationMessage response = new DeleteAnnotationMessage(
                new VID(aEvent.getAnnotation().getAddress()));

        annotationProcessAPI.sendDeleteAnnotation(response,
                String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()));
    }

    @Override
    @EventListener
    public void onFeatureUpdatedEventHandler(FeatureValueUpdatedEvent aEvent) throws IOException
    {
        UpdateFeaturesMessage response = new UpdateFeaturesMessage(
                new VID(((AnnotationFS) aEvent.getSource())._id()), aEvent.getFeature(),
                aEvent.getNewValue());
        annotationProcessAPI.sendUpdateFeatures(response,
                String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()));
    }

    @Override
    public void createAdviceMessage(String aMessage, String aUser) throws IOException
    {
        annotationProcessAPI
                .sendAdviceMessage(new AdviceMessage(aMessage, AdviceMessage.TYPE.ERROR), aUser);
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

    public Pair<List<Span>, List<Arc>> getAnnotations(CAS aCas, long aProjectId, Viewport aViewport)
    {
        List<Span> spans = new ArrayList<>();
        List<Arc> arcs = new ArrayList<>();

        List<AnnotationLayer> requestedLayers = new ArrayList<>();
        for (Long layerId : aViewport.getLayers()) {
            requestedLayers.add(annotationService.getLayer(layerId));
        }

        for (AnnotationLayer layer : requestedLayers) {
            TypeAdapter adapter = annotationService.getAdapter(layer);

            List<AnnotationFS> annotations = aCas.select(adapter.getAnnotationType(aCas))
                    .coveredBy(0, aViewport.getEnd()).includeAnnotationsWithEndBeyondBounds()
                    .map(fs -> (AnnotationFS) fs).filter(ann -> AnnotationPredicates
                            .overlapping(ann, aViewport.getBegin(), aViewport.getEnd()))
                    .collect(toList());

            for (AnnotationFS annotation : annotations) {
                if (adapter instanceof SpanAdapter) {
                    List<AnnotationFeature> features = new ArrayList<>();
                    for (Feature f : annotation.getType().getFeatures()) {
                        features.add(adapter.getFeatureValue((AnnotationFeature) annotation,
                                annotation.getFeatureValue(f)));
                    }
                    spans.add(new Span(new VID(annotation._id()), annotation.getBegin(),
                            annotation.getEnd(), adapter.getTypeId(),
                            getColorForAnnotation(annotation,
                                    projectService.getProject(aProjectId)),
                            annotation.getCoveredText(), features));
                }

                if (adapter instanceof RelationAdapter) {
                    System.out.println(adapter.getAttachTypeName());

                    AnnotationFS source = null;
                    AnnotationFS target = null;
                    List<AnnotationFeature> features = new ArrayList<>();

                    for (Feature f : annotation.getType().getFeatures()) {
                        if (f.getName().contains(WebAnnoConst.FEAT_REL_SOURCE)) {
                            source = WebAnnoCasUtil.selectAnnotationByAddr(aCas,
                                    annotation.getAddress());
                        }
                        if (f.getName().contains(WebAnnoConst.FEAT_REL_TARGET)) {
                            target = WebAnnoCasUtil.selectAnnotationByAddr(aCas,
                                    annotation.getAddress());
                        }
                        features.add(adapter.getFeatureValue((AnnotationFeature) annotation,
                                annotation.getFeatureValue(f)));
                    }

                    arcs.add(new Arc(new VID(annotation._id()), new VID(source._id()),
                            new VID(target._id()),
                            getColorForAnnotation(annotation,
                                    projectService.getProject(aProjectId)),
                            adapter.getLayer().getId(), features));
                }
            }
        }
        return new ImmutablePair<>(spans, arcs);
    }
}
