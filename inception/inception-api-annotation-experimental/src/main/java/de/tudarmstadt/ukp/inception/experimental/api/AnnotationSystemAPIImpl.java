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

import static de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil.selectAnnotationByAddr;
import static java.lang.Math.toIntExact;
import static org.apache.uima.fit.util.CasUtil.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.text.AnnotationFS;
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
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.event.*;
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
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.*;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.*;
import de.tudarmstadt.ukp.inception.experimental.api.model.Relation;
import de.tudarmstadt.ukp.inception.experimental.api.model.Span;
import de.tudarmstadt.ukp.inception.experimental.api.util.Offsets;
import de.tudarmstadt.ukp.inception.experimental.api.websocket.AnnotationProcessAPI;

@Component
@ConditionalOnProperty(prefix = "websocket", name = "enabled", havingValue = "true")
public class AnnotationSystemAPIImpl
    implements AnnotationSystemAPI
{
    private final AnnotationSchemaService annotationService;
    private final ProjectService projectService;
    private final DocumentService documentService;
    private final UserDao userDao;
    private final RepositoryProperties repositoryProperties;
    private final AnnotationProcessAPI annotationProcessAPI;
    private final AnnotationSystemAPIService annotationSystemAPIService;
    private final ColoringService coloringService;
    private final UserPreferencesService userPreferencesService;

    public AnnotationSystemAPIImpl(ProjectService aProjectService, DocumentService aDocumentService,
            UserDao aUserDao, RepositoryProperties aRepositoryProperties,
            AnnotationProcessAPI aAnnotationProcessAPI,
            AnnotationSchemaService aAnnotationSchemaService,
            AnnotationSystemAPIService aAnnotationSystemAPIService,
            ColoringService aColoringService, UserPreferencesService aUserPreferencesService)
    {
        projectService = aProjectService;
        documentService = aDocumentService;
        userDao = aUserDao;
        repositoryProperties = aRepositoryProperties;
        annotationProcessAPI = aAnnotationProcessAPI;
        annotationService = aAnnotationSchemaService;
        annotationSystemAPIService = aAnnotationSystemAPIService;
        coloringService = aColoringService;
        userPreferencesService = aUserPreferencesService;
    }

    @Override
    public void handleNewDocument(NewDocumentRequest aNewDocumentRequest) throws IOException
    {
        try {
            CAS cas;

            NewDocumentResponse message = new NewDocumentResponse();

            cas = getCasForDocument(aNewDocumentRequest.getUserName(),
                    aNewDocumentRequest.getProjectId(), aNewDocumentRequest.getDocumentId());

            message.setDocumentId(
                    toIntExact(documentService.getSourceDocument(aNewDocumentRequest.getProjectId(),
                            aNewDocumentRequest.getDocumentId()).getId()));

            message.setViewportText(getViewportText(cas, aNewDocumentRequest.getViewportType(),
                    aNewDocumentRequest.getViewport()));

            message.setSpans(filterSpans(getSpans(cas, aNewDocumentRequest.getProjectId()),
                    aNewDocumentRequest.getViewport()));

            int[] min_max = getMinimumOffset(aNewDocumentRequest.getViewportType(), aNewDocumentRequest.getViewport(), cas);
            System.out.println(min_max[0]);
            System.out.println(min_max[1]);

            message.setRelations(getRelations(cas, aNewDocumentRequest.getProjectId(), min_max[0], min_max[1]));
            annotationProcessAPI.sendNewDocumentResponse(message,
                    aNewDocumentRequest.getClientName());
        }
        catch (Exception e) {
            e.printStackTrace();
            createErrorMessage(e.getMessage(), aNewDocumentRequest.getClientName());
        }

    }

    @Override
    public void handleNewViewport(NewViewportRequest aNewViewportRequest) throws IOException
    {
        CAS cas = getCasForDocument(aNewViewportRequest.getUserName(),
                aNewViewportRequest.getProjectId(), aNewViewportRequest.getDocumentId());

        NewViewportResponse message = new NewViewportResponse(
                getViewportText(cas, aNewViewportRequest.getViewportType(),
                        aNewViewportRequest.getViewport()),
                filterSpans(getSpans(cas, aNewViewportRequest.getProjectId()),
                        aNewViewportRequest.getViewport()),
                null);

        getRelations(cas, aNewViewportRequest.getProjectId(), 0, 30);

        annotationProcessAPI.sendNewViewportResponse(message, aNewViewportRequest.getClientName());
    }

    @Override
    public void handleSelectSpan(SelectSpanRequest aSelectSpanRequest) throws IOException
    {
        CAS cas = getCasForDocument(aSelectSpanRequest.getUserName(),
                aSelectSpanRequest.getProjectId(), aSelectSpanRequest.getDocumentId());
        AnnotationFS annotation = selectAnnotationByAddr(cas,
                aSelectSpanRequest.getSpanAddress().getId());

        // TODO retrieve correct feature
        SelectSpanResponse message = new SelectSpanResponse(
                VID.parse(String.valueOf(annotation.getAddress())),
                annotation.getType().getShortName(), null);

        annotationProcessAPI.sendSelectAnnotationResponse(message,
                aSelectSpanRequest.getClientName());
    }

    @Override
    public void handleUpdateSpan(UpdateSpanRequest aUpdateSpanRequest) throws IOException
    {
        try {
            CAS cas = getCasForDocument(aUpdateSpanRequest.getUserName(),
                    aUpdateSpanRequest.getProjectId(), aUpdateSpanRequest.getDocumentId());

            AnnotationFS annotation = selectAnnotationByAddr(cas,
                    aUpdateSpanRequest.getSpanAddress().getId());

            System.out.println("FEATURES for Type " + annotation.getType());
            for (Feature f : getFeaturesForFeatureStructure(getFeatureStructure(cas,
                    aUpdateSpanRequest.getProjectId(), aUpdateSpanRequest.getNewFeature()))) {
                System.out.println(f.getDomain());
                System.out.println(f.getRange());
                if (f.getRange().getShortName().equals("String")) {
                    System.out.println(" ----------- ");
                }

            }
        }
        catch (Exception e) {
            e.printStackTrace();
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

            TypeAdapter adapter = annotationService.getAdapter(annotationService.findLayer(
                    projectService.getProject(aCreateSpanRequest.getProjectId()),
                    getType(cas, aCreateSpanRequest.getType()).getName()));

            ((SpanAdapter) adapter).add(
                    documentService.getSourceDocument(aCreateSpanRequest.getProjectId(),
                            aCreateSpanRequest.getDocumentId()),
                    aCreateSpanRequest.getUserName(), cas, aCreateSpanRequest.getBegin(),
                    aCreateSpanRequest.getEnd());
        }
        catch (Exception e) {
            e.printStackTrace();
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

            AnnotationFS annotation = selectAnnotationByAddr(cas,
                    aDeleteSpanRequest.getSpanAddress().getId());

            TypeAdapter adapter = annotationService.getAdapter(annotationService.findLayer(
                    projectService.getProject(aDeleteSpanRequest.getProjectId()),
                    getType(cas, annotation.getType().getName()).getName()));

            adapter.delete(
                    documentService.getSourceDocument(aDeleteSpanRequest.getProjectId(),
                            aDeleteSpanRequest.getDocumentId()),
                    aDeleteSpanRequest.getUserName(), cas,
                    VID.parse(String.valueOf(aDeleteSpanRequest.getSpanAddress())));
        }
        catch (Exception e) {
            e.printStackTrace();
            createErrorMessage(e.getMessage(), aDeleteSpanRequest.getClientName());
        }
    }

    @Override
    public void handleSelectRelation(SelectSpanRequest aSelectSpanRequest) throws IOException
    {

    }

    @Override
    public void handleUpdateRelation(UpdateSpanRequest aUpdateSpanRequest) throws IOException
    {

    }

    @Override
    public void handleCreateRelation(CreateSpanRequest aCreateSpanRequest) throws IOException
    {
        // newRelation = ((RelationAdapter) adapter).add()
    }

    @Override
    public void handleDeleteRelation(DeleteSpanRequest aDeleteSpanRequest) throws IOException
    {

    }

    @Override
    public void handleSaveWordAlignment(SaveWordAlignmentRequest aSaveWordAlignmentRequest)
    {
        System.out.println("Backend handles now save_word_alignment");
        System.out.println("DATA: ");
        System.out.println(aSaveWordAlignmentRequest.getClientName());
        System.out.println(aSaveWordAlignmentRequest.getUserName());
        System.out.println(aSaveWordAlignmentRequest.getProjectId());
        System.out.println(aSaveWordAlignmentRequest.getSentence());
        System.out.println(aSaveWordAlignmentRequest.getAlignments());
    }

    @Override
    public void onSpanUpdatedEventHandler(FeatureValueUpdatedEvent aEvent)
    {
        // TODO Check What event that really is, and retrieve color
        /*
         * UpdateSpanResponse response = new UpdateSpanResponse(
         * VID.parse(String.valueOf(aEvent.getFS().getAddress())), aEvent.getNewValue().toString(),
         * "#888888");
         * annotationProcessAPI.sendUpdateAnnotationResponse(response,aEvent.getProject().getId(),
         * aEvent.getDocument().getId(), aEvent.get);
         *
         */

    }

    @EventListener
    @Override
    public void onSpanCreatedEventHandler(SpanCreatedEvent aEvent) throws IOException
    {
        // TODO Coloring service
        CreateSpanResponse response = new CreateSpanResponse(
                VID.parse(String.valueOf(aEvent.getAnnotation().getAddress())),
                aEvent.getAnnotation().getCoveredText(), aEvent.getAnnotation().getBegin(),
                aEvent.getAnnotation().getEnd(), aEvent.getAnnotation().getType().getShortName(),
                "#79313E");
        annotationProcessAPI.sendCreateAnnotationResponse(response,
                String.valueOf(aEvent.getProject().getId()),
                String.valueOf(aEvent.getDocument().getId()),
                String.valueOf(aEvent.getAnnotation().getBegin()));
    }

    @Override
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
    public void onRelationCreatedEventHandler(RelationCreatedEvent aEvent)
    {

    }

    @Override
    public void onRelationDeletedEventHandler(RelationDeletedEvent aEvent)
    {

    }

    @Override
    public void createErrorMessage(String aMessage, String aUser) throws IOException
    {
        annotationProcessAPI.sendErrorMessage(new ErrorMessage(aMessage), aUser);
    }

    // ---------- Private support methods ---------- //

    public CAS getCasForDocument(String aUser, long aProject, long aDocument)
    {
        try (CasStorageSession session = CasStorageSession.open()) {
            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(aProject, aDocument);
            return documentService.readAnnotationCas(sourceDocument, aUser);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Character[] getViewportText(CAS aCas, String aOffsetType, int[][] aViewport)
    {

        ArrayList<Character> visibleSentences = new ArrayList<>();

        String text = aCas.getDocumentText().replace("\n", "");

        switch (Offsets.valueOf(aOffsetType)) {
        case CHAR:

            char[] textInChars = aCas.getDocumentText().replace("\n", "").toCharArray();

            for (int i = 0; i < aViewport.length; i++) {
                for (int j = aViewport[i][0]; j < aViewport[i][1]; j++) {
                    visibleSentences.add(textInChars[j]);
                }
                visibleSentences.add('|');
            }
            return visibleSentences.toArray(new Character[0]);

        case WORD:
            String seperatorWord = " ";
            String[] textInWords = text.split(seperatorWord);

            for (int i = 1; i < textInWords.length; i++) {
                textInWords[i] = seperatorWord + textInWords[i];
            }

            for (int i = 0; i < aViewport.length; i++) {
                for (int j = aViewport[i][0]; j < aViewport[i][1]; j++) {
                    for (Character c : textInWords[j].toCharArray()) {
                        visibleSentences.add(c);
                    }
                }
                visibleSentences.add('|');
            }
            return visibleSentences.toArray(new Character[0]);

        case SENTENCE:
            String seperatorSentence = "\\.";
            String[] textInSentences = text.split(seperatorSentence);

            for (int i = 1; i < textInSentences.length; i++) {
                textInSentences[i] = textInSentences[i] + ".";
            }

            for (int i = 0; i < aViewport.length; i++) {

                for (Character c : textInSentences[aViewport[i][0]].toCharArray()) {
                    visibleSentences.add(c);
                }

                visibleSentences.add('|');
            }
            return visibleSentences.toArray(new Character[0]);
        default:
            System.err.println("Offset type not found");
        }
        return null;
    }

    public List<Span> getSpans(CAS aCas, long aProject)
    {
        List<AnnotationLayer> metadataLayers = annotationService
                .listAnnotationLayer(projectService.getProject(aProject));

        List<Span> annotations = new ArrayList<>();

        for (AnnotationLayer layer : metadataLayers) {
            if (layer.getUiName().equals("Token")) {
                continue;
            }

            TypeAdapter adapter = annotationService.getAdapter(layer);

            for (FeatureStructure fs : selectFS(aCas, adapter.getAnnotationType(aCas))) {
                AnnotationFS annotation = WebAnnoCasUtil.selectAnnotationByAddr(aCas,
                        WebAnnoCasUtil.getAddr(fs));

                annotations.add(new Span(annotation._id(), annotation.getBegin(),
                        annotation.getEnd(), annotation.getType().getShortName(), "#A32A9E",
                        annotation.getCoveredText(), null));
            }
        }

        return annotations;
    }

    public List<Span> filterSpans(List<Span> aAnnotations, int[][] aViewport)
    {
        List<Span> filteredAnnotations = new ArrayList<>();
        for (Span annotation : aAnnotations) {
            for (int i = 0; i < aViewport.length; i++) {
                for (int j = aViewport[i][0]; j <= aViewport[i][1]; j++) {
                    if (annotation.getBegin() == j) {
                        filteredAnnotations.add(annotation);
                        break;
                    }
                }
            }
        }
        return filteredAnnotations;
    }

    public String getColorForSpan(Span aAnnotation, Project aProject, String aUser, CAS aCas)
        throws IOException
    {
        System.out.println("GET COLOR");
        // TODO COLORING
        /*
         * TypeAdapter adapter = annotationService
         * .getAdapter(annotationService.findLayer(projectService.getProject(aProject.getId()),
         * getType(aCas, "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS") .getName()));
         * 
         * System.out.println(adapter.getLayer().getName()); ColoringRules coloringRules =
         * adapter.getTraits(ColoringRulesTrait.class)
         * .map(ColoringRulesTrait::getColoringRules).orElse(null);
         * 
         * System.out.println(coloringRules); for (ColoringRule r : coloringRules.getRules()) {
         * System.out.println(r.getPattern()); System.out.println(r.getColor()); }
         * System.out.println(adapter.getLayer().getTraits());
         * System.out.println("...............");
         * System.out.println(coloringRules.findColor("POS")); System.out.println(
         * coloringRules.findColor("de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS"));
         * System.out.println(coloringRules.findColor(adapter.getLayer().getTraits()));
         * System.out.println(coloringRules.findColor(adapter.getLayer().getType()));
         * System.out.println(coloringRules .findColor(getType(aCas,
         * "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS") .getName()));
         * System.out.println(coloringRules .findColor(getType(aCas,
         * "de.tudarmstadt.ukp.dkpro.core.api.lexmorph.type.pos.POS") .getShortName()));
         * 
         * Map<String[], Queue<String>> colorQueues = new HashMap<>(); ColoringStrategy
         * coloringStrategy = coloringService.getStrategy(adapter.getLayer(),
         * userPreferencesService.loadPreferences(aProject, aUser, Mode.ANNOTATION), colorQueues);
         * System.out.println(coloringStrategy); VDocument vdoc = new VDocument();
         * System.out.println(vdoc); System.out.println(vdoc.spans()); String color = null; for
         * (VSpan vspan : vdoc.spans()) { System.out.println(vspan);
         * 
         * String labelText = getUiLabelText(adapter, vspan); System.out.println(labelText); color =
         * coloringStrategy.getColor(vspan, labelText, coloringRules); System.out.println(color); }
         * 
         */

        return null;
    }

    public List<Relation> getRelations(CAS aCas, long aProject, int aViewportBegin,
            int aViewportEnd)
    {
        List<AnnotationLayer> metadataLayers = annotationService
                .listAnnotationLayer(projectService.getProject(aProject));

        List<Relation> relations = new ArrayList<>();

        for (AnnotationLayer layer : metadataLayers) {
            if (layer.getUiName().equals("Token")) {
                continue;
            }
            TypeAdapter adapter = annotationService.getAdapter(layer);
            for (AnnotationFS fs : selectCovered(aCas, adapter.getAnnotationType(aCas),
                    aViewportBegin, aViewportEnd)) {

                if (adapter instanceof RelationAdapter) {

                    String attachedFeature = adapter.getAttachFeatureName();
                    FeatureStructure governor = fs
                            .getFeatureValue(fs.getType().getFeatureByBaseName(
                                    ((RelationAdapter) adapter).getSourceFeatureName()));
                    FeatureStructure dependent = fs
                            .getFeatureValue(fs.getType().getFeatureByBaseName(
                                    ((RelationAdapter) adapter).getTargetFeatureName()));

                    String dependencyType = fs
                            .getFeatureValueAsString(fs.getType().getFeatures().get(5));
                    String flavor = fs.getFeatureValueAsString(fs.getType().getFeatures().get(6));

                    // TODO color
                    Relation relation = new Relation(VID.parse(String.valueOf(fs._id())),
                            VID.parse(String.valueOf(governor.getFeatureValue(
                                    governor.getType().getFeatureByBaseName(attachedFeature))._id())),
                            VID.parse(String.valueOf(dependent.getFeatureValue(
                                    dependent.getType().getFeatureByBaseName(attachedFeature))._id())),
                            "#888888", dependencyType, flavor);

                    relations.add(relation);
                }
            }
        }

        return relations;
    }

    public void getRecommendations(CAS aCas, long aProject)
    {

    }

    public FeatureStructure getFeatureStructure(CAS aCas, long aProject, String aAnnotationType)
    {

        List<AnnotationLayer> metadataLayers = annotationService
                .listAnnotationLayer(projectService.getProject(aProject));
        for (AnnotationLayer layer : metadataLayers) {
            if (layer.getUiName().equals("Token")) {
                continue;
            }

            TypeAdapter adapter = annotationService.getAdapter(layer);

            for (FeatureStructure fs : selectFS(aCas, adapter.getAnnotationType(aCas))) {
                if (fs.getType().getShortName().equals(aAnnotationType)) {
                    return fs;
                }
            }
        }
        return null;
    }

    public List<Feature> getFeaturesForFeatureStructure(FeatureStructure aFeatureStructure)
    {
        return aFeatureStructure.getType().getFeatures();
    }

    public int[] getMinimumOffset(String aOffsetType, int[][] aViewport, CAS aCas) {
        int[] min_max = new int[2];

        String text = aCas.getDocumentText();

        for (int i = 0; i < aViewport.length; i++) {
            if (min_max[0] > aViewport[i][0]) {
                min_max[0] = aViewport[i][0];
            }
            if (min_max[1] < aViewport[i][1]) {
                min_max[1] = aViewport[i][1];
            }
        }

        switch (Offsets.valueOf(aOffsetType)) {
            case CHAR:
                break;
            case WORD:
                String[] words = text.split(" ");
                int offsetWords = 0;

                for (int i = 0; i < min_max[1]; i++) {
                    if (i == min_max[0]) {
                        min_max[0] = offsetWords;
                    }
                    offsetWords += words[i].length();
                }
                min_max[1] = offsetWords;
                break;
            case SENTENCE:
                String[] sentences = text.split("\\.");
                int offsetSentences = 0;

                for (int i = 0; i < min_max[1]; i++) {
                    if (i == min_max[0]) {
                        min_max[0] = offsetSentences;
                    }
                    offsetSentences += sentences[i].length();
                }
                min_max[1] = offsetSentences;

                break;
            default:
                System.err.println("Offset type not found");

        }

        return min_max;
    }
}
