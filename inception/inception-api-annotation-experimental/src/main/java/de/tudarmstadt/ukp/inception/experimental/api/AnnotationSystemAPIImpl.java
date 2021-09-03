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
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.UpdateFeatureMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.create.ArcCreatedMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.create.SpanCreatedMessage;
import de.tudarmstadt.ukp.inception.experimental.api.model.Arc;
import de.tudarmstadt.ukp.inception.experimental.api.model.Span;
import de.tudarmstadt.ukp.inception.experimental.api.model.Viewport;
import de.tudarmstadt.ukp.inception.experimental.api.websocket.AnnotationProcessAPI;

/**
 * Implementation of the Interface AnnotationSystemAPI within that package.
 *
 * In order to activate this class, add 'websocket.enabled = true' in the
 * application.yml file
 * @see inception-app-webapp/src/main/resources/application.yml
 *
 * For further details @see interface class (AnnotationSystemAPI.class).
 *
 * @NOTE: This class also contains private support methods that are NOT contained in
 * the Interface
 * @see AnnotationSystemAPI
 * These can be found on the bottom end of this class.
 *
 **/
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


    /**
     *  --------------------------------- handle() methods ------------------------------
     *
     * @params aRequest: handle() methods always get a class representation containing
     *                all the data required to perform their specific task. The classes
     *                have been generated from the JSON string within a clients message
     *                payload.
     *
     * @NOTE: All handle() methods have a try-catch-block. Each catch-block logs
     * the error and sends a detailed message back to the client via createAdviceMessage()
     *
     * For further details see Interface class
     * @see AnnotationSystemAPI
     *
     **/
    @Override
    public void handleDocumentRequest(DocumentRequest aDocumentRequest) throws IOException
    {
        try {
            //Retrieve the CAS
            CAS cas = getCasForDocument(aDocumentRequest.getAnnotatorName(),
                    aDocumentRequest.getProjectId(), aDocumentRequest.getSourceDocumentId());
            Pair<List<Span>, List<Arc>> annotations = null;

            //For every viewport retrieve the annotations
            //REMARK: Every viewport has its own layers it is enabled to retrieve annotation from
            for (Viewport viewport : aDocumentRequest.getViewport()) {

                //getAnnotations retrieve a two lists, one with Spans and the other with Arcs
                //Therefore, a Pair element has been chosen
                Pair<List<Span>, List<Arc>> retrievedAnnotations = getAnnotations(cas,
                        aDocumentRequest.getProjectId(), viewport);
                annotations.getLeft().addAll(retrievedAnnotations.getLeft());
                annotations.getRight().addAll(retrievedAnnotations.getRight());
            }

            //Create the corresponding class and forward the data to the AnnotationProcessAPI
            DocumentMessage message = new DocumentMessage(aDocumentRequest.getViewport(),
                    aDocumentRequest.getSourceDocumentId(), annotations.getLeft(),
                    annotations.getRight());
            annotationProcessAPI.sendDocumentResponse(message, aDocumentRequest.getAnnotatorName());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleNewDocument()", e);
            createAdviceMessage(e.getMessage(), aDocumentRequest.getAnnotatorName(), AdviceMessage.TYPE.ERROR);
        }

    }

    @Override
    public void handleCreateSpan(CreateSpanRequest aCreateSpanRequest) throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {

            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            //Retrieve the SourceDocument
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aCreateSpanRequest.getProjectId(), aCreateSpanRequest.getSourceDocumentId());

            //Retrieve the CAS
            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aCreateSpanRequest.getAnnotatorName());

            //Retrieve the TypeAdapter
            TypeAdapter adapter = annotationService
                    .getAdapter(annotationService.getLayer(aCreateSpanRequest.getLayerId()));

            //Create the new Span in the TypeAdapter
            ((SpanAdapter) adapter).add(
                    documentService.getSourceDocument(aCreateSpanRequest.getProjectId(),
                            aCreateSpanRequest.getSourceDocumentId()),
                    aCreateSpanRequest.getAnnotatorName(), cas, aCreateSpanRequest.getBegin(),
                    aCreateSpanRequest.getEnd());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleCreateSpan()", e);
            createAdviceMessage(e.getMessage(), aCreateSpanRequest.getAnnotatorName(), AdviceMessage.TYPE.ERROR);
        }
    }

    @Override
    public void handleCreateArc(CreateArcRequest aCreateArcRequest) throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            //Retrieve the SourceDocument
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aCreateArcRequest.getProjectId(), aCreateArcRequest.getSourceDocumentId());

            //Retrieve the CAS
            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aCreateArcRequest.getAnnotatorName());

            //Retrieve the Typeadapter
            TypeAdapter adapter = annotationService
                .getAdapter(annotationService.getLayer(aCreateArcRequest.getLayerId()));

            //Get the source annotation and target annotation of the Arc
            //as they are required for creating a new Arc
            AnnotationFS source = selectAnnotationByAddr(cas,
                    aCreateArcRequest.getSourceId().getId());
            AnnotationFS target = selectAnnotationByAddr(cas,
                    aCreateArcRequest.getTargetId().getId());

            //Create the new Arc in the TypeAdapter
            ((RelationAdapter) adapter).add(
                    documentService.getSourceDocument(aCreateArcRequest.getProjectId(),
                            aCreateArcRequest.getSourceDocumentId()),
                    aCreateArcRequest.getAnnotatorName(), source, target, cas);
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleCreateRelation()", e);
            createAdviceMessage(e.getMessage(), aCreateArcRequest.getAnnotatorName(), AdviceMessage.TYPE.ERROR);
        }
    }

    @Override
    public void handleDeleteAnnotation(DeleteAnnotationRequest aDeleteAnnotationRequest)
        throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            //Retrieve the SourceDocument
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aDeleteAnnotationRequest.getProjectId(),
                    aDeleteAnnotationRequest.getSourceDocumentId());

            //Retrieve the CAS
            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aDeleteAnnotationRequest.getAnnotatorName());

            //Retrieve the TypeAdapter
            TypeAdapter adapter = annotationService
                    .getAdapter(annotationService.getLayer(aDeleteAnnotationRequest.getLayerId()));

            //Delete the annotation via the TypeAdapter
            adapter.delete(
                    documentService.getSourceDocument(aDeleteAnnotationRequest.getProjectId(),
                            aDeleteAnnotationRequest.getSourceDocumentId()),
                    aDeleteAnnotationRequest.getAnnotatorName(), cas,
                    aDeleteAnnotationRequest.getAnnotationId());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleDeleteSpan()", e);
            createAdviceMessage(e.getMessage(), aDeleteAnnotationRequest.getAnnotatorName(), AdviceMessage.TYPE.ERROR);
        }
    }

    @Override
    public void handleUpdateFeatures(UpdateFeaturesRequest aUpdateFeaturesRequest)
        throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());

            //Retrieve the SourceDocument
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aUpdateFeaturesRequest.getProjectId(),
                    aUpdateFeaturesRequest.getSourceDocumentId());

            //Retrieve the CAS
            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aUpdateFeaturesRequest.getAnnotatorName());

            //Retrieve the TypeAdapter
            TypeAdapter adapter = annotationService
                    .getAdapter(annotationService.getLayer(aUpdateFeaturesRequest.getLayerId()));

            //Set the new feature value
            adapter.setFeatureValue(sourceDocument, aUpdateFeaturesRequest.getAnnotatorName(), cas,
                    aUpdateFeaturesRequest.getAnnotationId().getId(),
                    aUpdateFeaturesRequest.getFeature(), aUpdateFeaturesRequest.getValue());
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during handleUpdateSpan()", e);
            createAdviceMessage(e.getMessage(), aUpdateFeaturesRequest.getAnnotatorName(), AdviceMessage.TYPE.ERROR);
        }
    }


    /**
     *  ------------------------------ onEventListener() methods ------------------------------
     *
     * @param aEvent: Always contains the event that has been triggered on the server.
     *              The event listening is realized via the @EventListener annotation.
     *
     * For further details see Interface class @see AnnotationSystemAPI.class
     *
     **/

    @Override
    @EventListener
    public void onSpanCreatedEventHandler(SpanCreatedEvent aEvent) throws IOException
    {
        List<AnnotationFeature> features = new ArrayList<>();
        AnnotationFS annotation = aEvent.getAnnotation();

        //Fetches all features of the span
        for (Feature f : annotation.getType().getFeatures()) {
            features.add(annotationService.getAdapter(aEvent.getLayer()).getFeatureValue(
                    (AnnotationFeature) annotation, annotation.getFeatureValue(f)));
        }

        //Create response from event
        SpanCreatedMessage response = new SpanCreatedMessage(
                new VID(aEvent.getAnnotation().getAddress()), aEvent.getAnnotation().getBegin(),
                aEvent.getAnnotation().getEnd(), aEvent.getLayer().getId(),
                getColorForAnnotation(aEvent.getAnnotation(), aEvent.getProject()), features);

        //Forward data to the Process API
        annotationProcessAPI.sendCreateSpan(response, String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()));
    }

    @Override
    @EventListener
    public void onSpanDeletedEventHandler(SpanDeletedEvent aEvent) throws IOException
    {
        //Create response from event
        DeleteAnnotationMessage response = new DeleteAnnotationMessage(
                new VID(aEvent.getAnnotation().getAddress()));

        //Forward data to the Process API
        annotationProcessAPI.sendDeleteAnnotation(response,
                String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()));
    }

    @Override
    @EventListener
    public void onArcCreatedEventHandler(RelationCreatedEvent aEvent) throws IOException
    {
        List<AnnotationFeature> features = new ArrayList<>();

        //Fetch all features of the Relation
        for (Feature f : aEvent.getAnnotation().getType().getFeatures()) {
            System.out.println(aEvent.getAnnotation().getFeatureValue(f));
        }

        //Create response from event
        ArcCreatedMessage response = new ArcCreatedMessage(new VID(aEvent.getAnnotation()._id()),
                aEvent.getProject().getId(), new VID(aEvent.getSourceAnnotation()._id()),
                new VID(aEvent.getTargetAnnotation()._id()),
                getColorForAnnotation(aEvent.getAnnotation(), aEvent.getProject()),
                aEvent.getLayer().getId(), features);

        //Forward data to the Process API
        annotationProcessAPI.sendCreateArc(response, String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()));
    }

    @Override
    @EventListener
    public void onArcDeletedEventHandler(RelationDeletedEvent aEvent) throws IOException
    {
        //Create response from event
        DeleteAnnotationMessage response = new DeleteAnnotationMessage(
                new VID(aEvent.getAnnotation().getAddress()));

        //Forward data to the Process API
        annotationProcessAPI.sendDeleteAnnotation(response,
                String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()));
    }

    @Override
    @EventListener
    public void onFeatureUpdatedEventHandler(FeatureValueUpdatedEvent aEvent) throws IOException
    {
        //Create response from event
        UpdateFeatureMessage response = new UpdateFeatureMessage(
                new VID(((AnnotationFS) aEvent.getSource())._id()), aEvent.getFeature(),
                aEvent.getNewValue());

        //Forward data to the Process API
        annotationProcessAPI.sendUpdateFeatures(response,
                String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()));
    }

    @Override
    public void createAdviceMessage(String aMessage, String aUser, AdviceMessage.TYPE aType) throws IOException
    {
        //Forward data to the Process API
        annotationProcessAPI
                .sendAdviceMessage(new AdviceMessage(aMessage, aType), aUser);
    }

    /**
     * ------------------------ PRIVATE SUPPORT METHODS --------------------------------
     *
     * The following methods are private support methods and NOT contained in the Interface
     * @see AnnotationSystemAPI
     *
     * @NOTE: All support methods should have a try-catch block surrounding them.
     * The catch block shall always log the exception for error-analysis and debugging
     * purposes.
     *
     **/




    /**
     * This method simply fetches the CAS for a given user, project and SourceDocument.
     * Every handle() method in this class calls getCasForDocument().
     * The method 'document.readAnnotationCas()' requires all of the methods parameters.
     * Also 'annotation.upgradeCas()' is performed after it was retrieved.
     *
     * @param aUser: The username is required to retrieve the correct CAS
     * @param aProject: The projectId is required to retrieve the correct CAS
     * @param aDocument: The sourcedocumentId is required to retrieve the correct CAS.
     * @return CAS cas
     * @throws Exception when it was possible to retrieve the CAS
     **/
    private CAS getCasForDocument(String aUser, long aProject, long aDocument) throws Exception
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(aProject, aDocument);

            //Retrieve the CAS
            CAS cas = documentService.readAnnotationCas(sourceDocument, aUser);

            //Upgrade CAS
            annotationService.upgradeCas(cas, projectService.getProject(aProject));

            return cas;
        }
        catch (Exception e) {
            LOG.error("Exception has been thrown during getCasForDocument()", e);
            throw new Exception();
        }
    }

    /**
     * This method retrieves all data within a given viewport. The viewport always
     * has a begin- and an end-offset.
     * As spans AND arcs are required to be found, the return value is a Pair
     * containing a List of all spans and another List of all arcs found within
     * the boundaries of the Viewport.
     *
     * @param aCas Required to retrieve the annotations
     * @param aProjectId Required to create a custom Span / Arc from the annotation suitable
     *                   for the data transfer
     * @param aViewport Viewport within its borders (begin, end) is searched for
     *                  annotation for specific layers
     *
     * @return Pair<List<Span>,List<Arc>>
     */
    private Pair<List<Span>, List<Arc>> getAnnotations(CAS aCas, long aProjectId, Viewport aViewport)
    {
        //Lists to be returned together in a Pair
        List<Span> spans = new ArrayList<>();
        List<Arc> arcs = new ArrayList<>();

        //Retrieve layers of the viewport via the IDs saved in viewport.layerId
        List<AnnotationLayer> requestedLayers = new ArrayList<>();
        for (Long layerId : aViewport.getLayers()) {
            requestedLayers.add(annotationService.getLayer(layerId));
        }

        //Iterate over all layers for which annotations shall be received
        for (AnnotationLayer layer : requestedLayers) {

            //Get the adapter for the layer
            TypeAdapter adapter = annotationService.getAdapter(layer);


            //Obtain a list with annotations
            //Only annotations will be added that are within the viewports bounds
            List<AnnotationFS> annotations = aCas.select(adapter.getAnnotationType(aCas))
                .coveredBy(0, aViewport.getEnd()).includeAnnotationsWithEndBeyondBounds()
                .map(fs -> (AnnotationFS) fs).filter(ann -> AnnotationPredicates
                    .overlapping(ann, aViewport.getBegin(), aViewport.getEnd()))
                .collect(toList());

            //Iterate over all annotation found for that layer within the bounds of the viewport
            for (AnnotationFS annotation : annotations) {

                //Check the type of the TypeAdapter (either SpanAdapter or RelationAdapter)
                if (adapter instanceof SpanAdapter) {

                    //Get features for the span
                    List<AnnotationFeature> features = new ArrayList<>();
                    for (Feature f : annotation.getType().getFeatures()) {
                        features.add(adapter.getFeatureValue((AnnotationFeature) annotation,
                            annotation.getFeatureValue(f)));
                    }
                    //Add the span to the list
                    spans.add(new Span(new VID(annotation._id()), annotation.getBegin(),
                        annotation.getEnd(), adapter.getTypeId(),
                        getColorForAnnotation(annotation,
                            projectService.getProject(aProjectId)), features));
                }

                if (adapter instanceof RelationAdapter) {

                    //Both the source and target annotation of the Relation are required
                    AnnotationFS source = null;
                    AnnotationFS target = null;
                    List<AnnotationFeature> features = new ArrayList<>();

                    //Iterate through the annotations features to retrieve the addresses
                    //of the source and target annotations
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

                    //Add the relation
                    arcs.add(new Arc(new VID(annotation._id()), new VID(source._id()),
                        new VID(target._id()),
                        getColorForAnnotation(annotation,
                            projectService.getProject(aProjectId)),
                        adapter.getLayer().getId(), features));
                }
            }
        }

        //Return the Pair of lists
        return new ImmutablePair<>(spans, arcs);
    }

    /**
     * This method returns the String representation of the color the annotation
     * should have, according to the coloringStrategy.
     *
     * @param aAnnotation Annotation to get the correct color for
     * @param aProject ProjectId required to retrieve the correct adapter
     * @return String representation of a color
     */
    private String getColorForAnnotation(AnnotationFS aAnnotation, Project aProject)
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
}
