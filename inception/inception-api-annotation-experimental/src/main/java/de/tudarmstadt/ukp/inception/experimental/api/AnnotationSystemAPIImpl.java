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
import static org.apache.uima.fit.util.CasUtil.selectFS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import de.tudarmstadt.ukp.clarin.webanno.api.AnnotationSchemaService;
import de.tudarmstadt.ukp.clarin.webanno.api.DocumentService;
import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.SpanAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.adapter.TypeAdapter;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.model.VID;
import de.tudarmstadt.ukp.clarin.webanno.api.annotation.util.WebAnnoCasUtil;
import de.tudarmstadt.ukp.clarin.webanno.api.config.RepositoryProperties;
import de.tudarmstadt.ukp.clarin.webanno.api.dao.casstorage.CasStorageSession;
import de.tudarmstadt.ukp.clarin.webanno.model.AnnotationLayer;
import de.tudarmstadt.ukp.clarin.webanno.model.SourceDocument;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.support.logging.Logging;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.CreateAnnotationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.DeleteAnnotationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.NewDocumentRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.NewViewportRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.SelectAnnotationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.request.UpdateAnnotationRequest;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.CreateAnnotationResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.DeleteAnnotationResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.ErrorMessage;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.NewDocumentResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.NewViewportResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.SelectAnnotationResponse;
import de.tudarmstadt.ukp.inception.experimental.api.messages.response.UpdateAnnotationResponse;
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

    private List<String> annotationLayerList;

    public AnnotationSystemAPIImpl(ProjectService aProjectService, DocumentService aDocumentService,
            UserDao aUserDao, RepositoryProperties aRepositoryProperties,
            AnnotationProcessAPI aAnnotationProcessAPI,
            AnnotationSchemaService aAnnotationSchemaService,
            AnnotationSystemAPIService aAnnotationSystemAPIService)
    {
        projectService = aProjectService;
        documentService = aDocumentService;
        userDao = aUserDao;
        repositoryProperties = aRepositoryProperties;
        annotationProcessAPI = aAnnotationProcessAPI;
        annotationService = aAnnotationSchemaService;
        annotationSystemAPIService = aAnnotationSystemAPIService;
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

            message.setSpanAnnotations(
                    filterAnnotations(getAnnotations(cas, aNewDocumentRequest.getProjectId()),
                            aNewDocumentRequest.getViewport()));

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

        NewViewportResponse message = new NewViewportResponse();

        message.setViewportText(getViewportText(cas, aNewViewportRequest.getViewportType(),
                aNewViewportRequest.getViewport()));

        message.setSpanAnnotations(
                filterAnnotations(getAnnotations(cas, aNewViewportRequest.getProjectId()),
                        aNewViewportRequest.getViewport()));

        annotationProcessAPI.sendNewViewportResponse(message, aNewViewportRequest.getClientName());
    }

    @Override
    public void handleSelectAnnotation(SelectAnnotationRequest aSelectAnnotationRequest)
        throws IOException
    {
        CAS cas = getCasForDocument(aSelectAnnotationRequest.getUserName(),
                aSelectAnnotationRequest.getProjectId(), aSelectAnnotationRequest.getDocumentId());
        AnnotationFS annotation = selectAnnotationByAddr(cas,
                aSelectAnnotationRequest.getAnnotationAddress().getId());
        SelectAnnotationResponse message = new SelectAnnotationResponse();
        annotationProcessAPI.sendSelectAnnotationResponse(message,
                aSelectAnnotationRequest.getClientName());
    }

    @Override
    public void handleUpdateAnnotation(UpdateAnnotationRequest aUpdateAnnotationRequest)
        throws IOException
    {
        try {
            CAS cas = getCasForDocument(aUpdateAnnotationRequest.getUserName(),
                    aUpdateAnnotationRequest.getProjectId(),
                    aUpdateAnnotationRequest.getDocumentId());

            AnnotationFS annotation = selectAnnotationByAddr(cas,
                    aUpdateAnnotationRequest.getAnnotationAddress().getId());

            System.out.println("FEATURES for Type " + annotation.getType());
            for (Feature f : getFeaturesForFeatureStructure(
                    getFeatureStructure(cas, aUpdateAnnotationRequest.getProjectId(),
                            aUpdateAnnotationRequest.getNewType().getShortName()))) {
                System.out.println(f.getDomain());
                System.out.println(f.getRange());
                if (f.getRange().getShortName().equals("String")) {
                    System.out.println(" ----------- ");
                }

            }

            UpdateAnnotationResponse message = new UpdateAnnotationResponse();
            message.setAnnotationAddress(aUpdateAnnotationRequest.getAnnotationAddress());
            message.setType(aUpdateAnnotationRequest.getNewType());

            annotationProcessAPI.sendUpdateAnnotationResponse(message,
                    String.valueOf(aUpdateAnnotationRequest.getProjectId()),
                    String.valueOf(aUpdateAnnotationRequest.getDocumentId()),
                    String.valueOf(annotation.getBegin()));
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    public void handleCreateAnnotation(CreateAnnotationRequest aCreateAnnotationRequest)
        throws IOException
    {
        try (CasStorageSession session = CasStorageSession.open()) {

            MDC.put(Logging.KEY_REPOSITORY_PATH, repositoryProperties.getPath().toString());
            SourceDocument sourceDocument = documentService.getSourceDocument(
                    aCreateAnnotationRequest.getProjectId(),
                    aCreateAnnotationRequest.getDocumentId());

            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aCreateAnnotationRequest.getUserName());

            // TODO get LayerID
            TypeAdapter adapter = null; // annotationService.getAdapter(annotationService.getLayer(getType(cas,
                                        // aCreateAnnotationRequest.getNewType().getName()).get);

            AnnotationFS newAnnotation = null;

            // TODO more adapater instances
            if (adapter instanceof SpanAdapter) {
                newAnnotation = ((SpanAdapter) adapter).add(
                        documentService.getSourceDocument(aCreateAnnotationRequest.getProjectId(),
                                aCreateAnnotationRequest.getDocumentId()),
                        aCreateAnnotationRequest.getUserName(), cas,
                        aCreateAnnotationRequest.getBegin(), aCreateAnnotationRequest.getEnd());
            }

            CreateAnnotationResponse message = new CreateAnnotationResponse();
            // TODO get VID from int
            message.setAnnotationAddress(VID.parse(String.valueOf(newAnnotation.getAddress())));
            annotationProcessAPI.sendCreateAnnotationResponse(message,
                    String.valueOf(aCreateAnnotationRequest.getDocumentId()),
                    String.valueOf(aCreateAnnotationRequest.getDocumentId()),
                    String.valueOf(newAnnotation.getBegin()));
        }
        catch (Exception e) {
            e.printStackTrace();
            createErrorMessage(e.getMessage(), aCreateAnnotationRequest.getClientName());
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
                    aDeleteAnnotationRequest.getDocumentId());

            CAS cas = documentService.readAnnotationCas(sourceDocument,
                    aDeleteAnnotationRequest.getUserName());

            AnnotationFS annotation = selectAnnotationByAddr(cas,
                    aDeleteAnnotationRequest.getAnnotationAddress().getId());

            // TODO get LayerID
            TypeAdapter adapter = null; // annotationService.getAdapter(annotationService.getLayer(getType(cas,
                                        // aCreateAnnotationRequest.getNewType().getName()).get);

            DeleteAnnotationResponse message = new DeleteAnnotationResponse();
            message.setAnnotationAddress(aDeleteAnnotationRequest.getAnnotationAddress());

            adapter.delete(
                    documentService.getSourceDocument(aDeleteAnnotationRequest.getProjectId(),
                            aDeleteAnnotationRequest.getDocumentId()),
                    aDeleteAnnotationRequest.getUserName(), cas,
                    VID.parse(String.valueOf(aDeleteAnnotationRequest.getAnnotationAddress())));

            annotationProcessAPI.sendDeleteAnnotationResponse(message,
                    String.valueOf(aDeleteAnnotationRequest.getProjectId()),
                    String.valueOf(aDeleteAnnotationRequest.getDocumentId()),
                    String.valueOf(annotation.getBegin()));

        }
        catch (Exception e) {
            e.printStackTrace();
            createErrorMessage(e.getMessage(), aDeleteAnnotationRequest.getClientName());
        }
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

            CAS cas = documentService.readAnnotationCas(sourceDocument, aUser);

            return cas;
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

    public List<Span> getAnnotations(CAS aCas, long aProject)
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
                        annotation.getEnd(), annotation.getType().getShortName()));
            }
        }

        return annotations;
    }


    public List<Span> filterAnnotations(List<Span> aAnnotations, int[][] aViewport)
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

    public void getRelations(CAS aCas, long aProject)
    {

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

    public Type getType(CAS aCas, String aAnnotationType)
    {
        for (Iterator<Feature> it = aCas.getTypeSystem().getFeatures(); it.hasNext();) {

            Feature s = it.next();
            if (s.getDomain().getShortName().equals(aAnnotationType)) {
                return s.getDomain();

            }
        }
        return null;
    }
}
